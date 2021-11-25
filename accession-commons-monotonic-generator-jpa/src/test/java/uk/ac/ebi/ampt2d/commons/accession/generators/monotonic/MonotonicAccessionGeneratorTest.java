/*
 *
 * Copyright 2018 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package uk.ac.ebi.ampt2d.commons.accession.generators.monotonic;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionIsNotPendingException;
import uk.ac.ebi.ampt2d.commons.accession.core.models.AccessionWrapper;
import uk.ac.ebi.ampt2d.commons.accession.core.models.SaveResponse;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.entities.ContiguousIdBlock;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.repositories.ContiguousIdBlockRepository;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.service.ContiguousIdBlockService;
import uk.ac.ebi.ampt2d.commons.accession.utils.exceptions.ExponentialBackOffMaxRetriesRuntimeException;
import uk.ac.ebi.ampt2d.test.configuration.MonotonicAccessionGeneratorTestConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = {MonotonicAccessionGeneratorTestConfiguration.class})
public class MonotonicAccessionGeneratorTest {

    private static final int BLOCK_SIZE = 1000;
    private static final int TENTH_BLOCK_SIZE = (int) (BLOCK_SIZE / 10);
    private static final String CATEGORY_ID = "cat-test";
    private static final String INSTANCE_ID = "inst-01";
    private static final String INSTANCE_2_ID = "inst-02";
    private static final String CATEGORY_ID_2 = "eva";
    private static final Integer NUM_OF_ACCESSIONS = 4;

    @Autowired
    private ContiguousIdBlockRepository repository;

    @Autowired
    private ContiguousIdBlockService service;

    @Test
    public void assertNoBlockGeneratedAtLoadIfNoneExists() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        assertEquals(0, repository.count());
    }

    @Test
    public void assertBlockGeneratedAtGenerateOperationIfNoBlockExists() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        generator.generateAccessions(TENTH_BLOCK_SIZE);
        assertEquals(1, repository.count());
        ContiguousIdBlock block = repository.findFirstByCategoryIdOrderByLastValueDesc(CATEGORY_ID);
        assertEquals(0, block.getFirstValue());
        assertEquals(BLOCK_SIZE - 1, block.getLastValue());
        assertEquals(block.getLastValue(), block.getLastCommitted());
    }

    @Test
    public void assertBlockNotGeneratedIfPreviousExists() throws Exception {
        assertEquals(0, repository.count());
        repository.save(new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 0, BLOCK_SIZE));
        assertEquals(1, repository.count());
        MonotonicAccessionGenerator generator = new MonotonicAccessionGenerator(
                CATEGORY_ID, INSTANCE_ID, service);

        assertEquals(1, repository.count());
    }

    @Test
    public void assertNewBlockGeneratedInSecondInstance() throws Exception {
        ContiguousIdBlock block;
        assertEquals(0, repository.count());

        MonotonicAccessionGenerator generator1 = new MonotonicAccessionGenerator(
                CATEGORY_ID, INSTANCE_ID, service);
        MonotonicAccessionGenerator generator2 = new MonotonicAccessionGenerator(
                CATEGORY_ID, INSTANCE_2_ID, service);

        generator1.generateAccessions(TENTH_BLOCK_SIZE);
        assertEquals(1, repository.count());
        block = repository.findFirstByCategoryIdAndApplicationInstanceIdOrderByLastValueDesc(CATEGORY_ID, INSTANCE_ID);
        assertEquals(0, block.getFirstValue());
        assertEquals(BLOCK_SIZE - 1, block.getLastValue());
        assertEquals(999, block.getLastCommitted());

        generator2.generateAccessions(TENTH_BLOCK_SIZE);
        assertEquals(2, repository.count());
        block = repository.findFirstByCategoryIdAndApplicationInstanceIdOrderByLastValueDesc(CATEGORY_ID, INSTANCE_2_ID);
        assertEquals(BLOCK_SIZE, block.getFirstValue());
        assertEquals(2 * BLOCK_SIZE - 1, block.getLastValue());
        assertEquals(block.getLastValue(), block.getLastCommitted());
    }

    @Test
    public void assertGenerateAccessions() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();

        long[] accessions = generator.generateAccessions(TENTH_BLOCK_SIZE);
        assertEquals(TENTH_BLOCK_SIZE, accessions.length);
    }

    @Test
    public void assertGenerateMoreAccessionsThanBlockSizeGeneratesTwoBlocks() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();

        //Generate BLOCK_SIZE accessions in BLOCK_SIZE/10 increments
        for (int i = 0; i < 10; i++) {
            long[] accessions = generator.generateAccessions(TENTH_BLOCK_SIZE);
            assertEquals(i * TENTH_BLOCK_SIZE, accessions[0]);
        }
        long[] accessions = generator.generateAccessions(TENTH_BLOCK_SIZE);
        assertEquals(BLOCK_SIZE, accessions[0]);
        assertEquals(2, repository.count());
    }

    @Test
    public void assertGenerateMoreAccessionsThanBlockSizeGeneratesInOneCall() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        long[] accessions = generator.generateAccessions(BLOCK_SIZE + (BLOCK_SIZE / 2));

        assertEquals(2, repository.count());
    }

    @Test
    public void assertCommitModifiesLastCommitted() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        long[] accessions = generator.generateAccessions(TENTH_BLOCK_SIZE);

        generator.commit(accessions);

        ContiguousIdBlock block =
                repository.findFirstByCategoryIdAndApplicationInstanceIdOrderByLastValueDesc(CATEGORY_ID, INSTANCE_ID);
        assertEquals(TENTH_BLOCK_SIZE - 1, block.getLastCommitted());
    }

    @Test
    public void assertNotCommittingDoesNotModifyLastCommitted() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        generator.generateAccessions(TENTH_BLOCK_SIZE);

        ContiguousIdBlock block =
                repository.findFirstByCategoryIdAndApplicationInstanceIdOrderByLastValueDesc(CATEGORY_ID, INSTANCE_ID);
        assertEquals(999, block.getLastCommitted());
    }

    @Test
    public void assertCommitOutOfOrderDoesNotModifyLastCommittedUntilTheSequenceIsComplete() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        long[] accessions1 = generator.generateAccessions(TENTH_BLOCK_SIZE);
        long[] accessions2 = generator.generateAccessions(TENTH_BLOCK_SIZE);

        generator.commit(accessions2);

        ContiguousIdBlock block =
                repository.findFirstByCategoryIdAndApplicationInstanceIdOrderByLastValueDesc(CATEGORY_ID, INSTANCE_ID);
        assertEquals(999, block.getLastCommitted());

        generator.commit(accessions1);

        block = repository.findFirstByCategoryIdAndApplicationInstanceIdOrderByLastValueDesc(CATEGORY_ID, INSTANCE_ID);
        assertEquals(2 * TENTH_BLOCK_SIZE - 1, block.getLastCommitted());
    }

    @Test
    public void assertCommitOutOfOrderDoesNotModifyLastCommittedUntilTheSequenceIsCompleteMultipleBlocks() throws
            Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        long[] accessions1 = generator.generateAccessions(BLOCK_SIZE + TENTH_BLOCK_SIZE);
        long[] accessions2 = generator.generateAccessions(TENTH_BLOCK_SIZE);

        generator.commit(accessions2);

        ContiguousIdBlock block =
                repository.findFirstByCategoryIdAndApplicationInstanceIdOrderByLastValueDesc(CATEGORY_ID, INSTANCE_ID);
        assertEquals(BLOCK_SIZE, block.getFirstValue());
        //Last committed should be at the end of the second block (999 to 1999)
        assertEquals(block.getLastValue(), block.getLastCommitted());

        generator.commit(accessions1);

        block = repository.findFirstByCategoryIdAndApplicationInstanceIdOrderByLastValueDesc(CATEGORY_ID, INSTANCE_ID);
        assertEquals(BLOCK_SIZE, block.getFirstValue());
        assertEquals(BLOCK_SIZE + 2 * TENTH_BLOCK_SIZE - 1, block.getLastCommitted());
    }

    @Test
    public void assertGenerateDoesNotReuseIds() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        generator.generateAccessions(TENTH_BLOCK_SIZE);
        long[] accessions2 = generator.generateAccessions(TENTH_BLOCK_SIZE);
        assertEquals(TENTH_BLOCK_SIZE, accessions2[0]);
    }

    @Test
    public void assertReleaseMakesGenerateReuseIds() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        long[] accessions1 = generator.generateAccessions(TENTH_BLOCK_SIZE);
        generator.release(accessions1);
        long[] accessions2 = generator.generateAccessions(TENTH_BLOCK_SIZE);
        assertEquals(0, accessions2[0]);
    }

    @Test
    public void assertReleaseSomeIdsMakesGenerateReuseReleasedIdsAndNewOnes() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        generator.generateAccessions(TENTH_BLOCK_SIZE);
        generator.release(0, 1);
        long[] accessions2 = generator.generateAccessions(TENTH_BLOCK_SIZE);
        assertEquals(0, accessions2[0]);
        assertEquals(1, accessions2[1]);
        assertEquals(TENTH_BLOCK_SIZE, accessions2[2]);
    }

    @Test
    public void assertMultipleReleaseSomeIdsMakesGenerateReuseReleasedIdsAndNewOnes() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        long[] accessions1 = generator.generateAccessions(TENTH_BLOCK_SIZE);
        generator.release(0, 1);
        generator.release(3, 4, 5);
        generator.release(8, 9, 10);
        long[] accessions2 = generator.generateAccessions(TENTH_BLOCK_SIZE);
        assertEquals(0, accessions2[0]);
        assertEquals(1, accessions2[1]);
        assertEquals(3, accessions2[2]);
        assertEquals(4, accessions2[3]);
        assertEquals(5, accessions2[4]);
        assertEquals(8, accessions2[5]);
        assertEquals(9, accessions2[6]);
        assertEquals(10, accessions2[7]);
        assertEquals(TENTH_BLOCK_SIZE, accessions2[8]);
    }

    @Test
    public void assertMultipleReleaseAndCommitsWorks() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        generator.generateAccessions(BLOCK_SIZE);
        generator.commit(2);
        generator.release(0, 1);
        generator.release(3, 4, 5);
        generator.commit(6, 7, 11);
        generator.release(8, 9, 10);
        generator.commit(getLongArray(12, 998));
        //999 is waiting somewhere taking a big nap and no elements have been confirmed due to element 0 being released
        ContiguousIdBlock block =
                repository.findFirstByCategoryIdAndApplicationInstanceIdOrderByLastValueDesc(CATEGORY_ID, INSTANCE_ID);
        assertEquals(999, block.getLastCommitted());

        long[] accessions2 = generator.generateAccessions(BLOCK_SIZE);
        assertEquals(0, accessions2[0]);
        assertEquals(1, accessions2[1]);
        assertEquals(3, accessions2[2]);
        assertEquals(4, accessions2[3]);
        assertEquals(5, accessions2[4]);
        assertEquals(8, accessions2[5]);
        assertEquals(9, accessions2[6]);
        assertEquals(10, accessions2[7]);
        assertEquals(1000, accessions2[8]);
        assertEquals(1092, accessions2[100]);

        // Only 998 elements have been confirmed due to 999 not being confirmed, reread the block to assert it
        generator.commit(accessions2);
        Optional<ContiguousIdBlock> blockResult = repository.findById(block.getId());
        assertTrue(blockResult.isPresent());
        assertEquals(998, blockResult.get().getLastCommitted());
        // 999 is committed and then the remaining elements get confirmed
        generator.commit(999);
        block = repository.findFirstByCategoryIdAndApplicationInstanceIdOrderByLastValueDesc(CATEGORY_ID, INSTANCE_ID);
        assertEquals(1991, block.getLastCommitted());
    }

    /**
     * New block created and marked as used (first_value = 0, last_committed = 999, last_value = 999)
     * Application failed -> No updated performed in contiguous_id_blocks tables (the whole block remain as used)
     *
     * Application run again using the same category and instance
     * ids (2, 3, 5) were used in the previous run
     * The last block (0 to 999) is marked as used so there are no available ranges
     */
    @Test
    public void assertRecoverNoPendingCommit() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        generator.generateAccessions(BLOCK_SIZE);
        // Now assume that the db layer has stored some elements and that the application has died and restarted.

        MonotonicAccessionGenerator generatorRecovering =
                new MonotonicAccessionGenerator(CATEGORY_ID, INSTANCE_ID, service, new long[]{2, 3, 5});
        ContiguousIdBlock block =
                repository.findFirstByCategoryIdAndApplicationInstanceIdOrderByLastValueDesc(CATEGORY_ID, INSTANCE_ID);

        //New generated block marked as used
        assertEquals(999, block.getLastCommitted());
        //Last block is marked as used and can't be recovered so there are no ranges available
        assertTrue(generatorRecovering.getAvailableRanges().isEmpty());
    }

    @Test
    public void assertRecoverPendingCommit() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        generator.generateAccessions(BLOCK_SIZE);
        generator.commit(0, 1);
        // Now assume that the db layer has stored some elements and that the application has died and restarted.

        MonotonicAccessionGenerator generatorRecovering = new MonotonicAccessionGenerator(
                CATEGORY_ID, INSTANCE_ID, service, new long[]{2, 3, 5});
        ContiguousIdBlock block =
                repository.findFirstByCategoryIdAndApplicationInstanceIdOrderByLastValueDesc(CATEGORY_ID, INSTANCE_ID);
        assertEquals(3, block.getLastCommitted());
        assertThat(generatorRecovering.getAvailableRanges(),
                contains(new MonotonicRange(4, 4), new MonotonicRange(6, BLOCK_SIZE - 1)));
    }

    @Test(expected = AccessionIsNotPendingException.class)
    public void assertReleaseAndCommitSameElement() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        generator.generateAccessions(BLOCK_SIZE);
        generator.release(2);
        generator.commit(2);
    }

    @Test(expected = AccessionIsNotPendingException.class)
    public void assertCommitAndReleaseSameElement() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        generator.generateAccessions(BLOCK_SIZE);
        generator.commit(2);
        generator.release(2);
    }

    @Test(expected = AccessionIsNotPendingException.class)
    public void releaseSomeIdsTwice() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        generator.generateAccessions(TENTH_BLOCK_SIZE);
        generator.release(0, 1);
        generator.release(0, 1);
    }

    private long[] getLongArray(int start, int end) {
        final int totalElements = end - start + 1;
        long[] temp = new long[totalElements];
        for (int i = 0; i < totalElements; i++) {
            temp[i] = i + start;
        }
        return temp;
    }

    @Test
    public void assertGenerateWithObjects() throws Exception {
        assertEquals(0, repository.count());

        MonotonicAccessionGenerator<String> generator =
                new MonotonicAccessionGenerator(CATEGORY_ID, INSTANCE_ID, service);

        HashMap<String, String> objects = new HashMap<>();
        objects.put("hash1", "object2");
        objects.put("hash2", "object2");

        List<AccessionWrapper<String, String, Long>> generatedAccessions = generator.generateAccessions(objects);

        assertEquals(1, repository.count());
        assertEquals(0L, (long) generatedAccessions.get(0).getAccession());
        assertEquals(1L, (long) generatedAccessions.get(1).getAccession());
    }

    @Test
    public void postSaveAction() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        generator.generateAccessions(BLOCK_SIZE);
        Set<Long> committed = new HashSet<>();
        committed.add(0L);
        committed.add(1L);
        committed.add(3L);
        committed.add(4L);
        Set<Long> released = new HashSet<>();
        released.add(2L);
        released.add(5L);
        generator.postSave(new SaveResponse(committed, released));
        long[] accessions = generator.generateAccessions(BLOCK_SIZE);
        assertEquals(2, accessions[0]);
        assertEquals(5, accessions[1]);
        assertEquals(BLOCK_SIZE, accessions[2]);
    }

    @Test
    public void assertReleaseInAlternateRanges() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGeneratorForCategoryHavingBlockInterval();
        long[] accessions1 = generator.generateAccessions(NUM_OF_ACCESSIONS);
        assertEquals(1, accessions1[0]);
        assertEquals(2, accessions1[1]);
        assertEquals(3, accessions1[2]);
        assertEquals(4, accessions1[3]);
        generator.release(new long[]{2, 3});
        long[] accessions2 = generator.generateAccessions(NUM_OF_ACCESSIONS);
        assertEquals(2, accessions2[0]);
        assertEquals(3, accessions2[1]);
        assertEquals(5, accessions2[2]);
        assertEquals(11, accessions2[3]);
    }

    /**
     * Block (1 to 5) was marked and used
     * generatorRecovering can't recover the unused ids
     * A new block (11 to 15) is created to issue requested ids
     */
    @Test
    public void assertRecoverInAlternateRanges() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGeneratorForCategoryHavingBlockInterval();
        generator.generateAccessions(NUM_OF_ACCESSIONS);
        // Now assume that the db layer has stored some elements and that the application has died and restarted.

        MonotonicAccessionGenerator generatorRecovering =
                new MonotonicAccessionGenerator(CATEGORY_ID_2, INSTANCE_ID, service, new long[]{2, 3});
        long[] accessions2 = generatorRecovering.generateAccessions(NUM_OF_ACCESSIONS);

        assertEquals(11, accessions2[0]);
        assertEquals(12, accessions2[1]);
        assertEquals(13, accessions2[2]);
        assertEquals(14, accessions2[3]);
    }

    private MonotonicAccessionGenerator getMonotonicAccessionGenerator() throws Exception {
        assertEquals(0, repository.count());

        MonotonicAccessionGenerator generator = new MonotonicAccessionGenerator(
                CATEGORY_ID, INSTANCE_ID, service);
        return generator;
    }

    private MonotonicAccessionGenerator getMonotonicAccessionGeneratorForCategoryHavingBlockInterval() {
        assertEquals(0, repository.count());
        return new MonotonicAccessionGenerator(CATEGORY_ID_2, INSTANCE_ID, service);
    }

    @Test
    public void assertAbortExecutionWhenDBConstraintExceptionThrown() {
        ContiguousIdBlockService mockService = Mockito.mock(ContiguousIdBlockService.class, Answers.RETURNS_DEEP_STUBS);
        MonotonicAccessionGenerator mockGenerator = new MonotonicAccessionGenerator(CATEGORY_ID, INSTANCE_ID, mockService);
        when(mockService.reserveNewBlock(anyString(), anyString())).thenThrow(ConstraintViolationException.class);
        assertThrows(ExponentialBackOffMaxRetriesRuntimeException.class, () -> mockGenerator.generateAccessions(1));
        assertEquals(0, repository.count());
    }
}
