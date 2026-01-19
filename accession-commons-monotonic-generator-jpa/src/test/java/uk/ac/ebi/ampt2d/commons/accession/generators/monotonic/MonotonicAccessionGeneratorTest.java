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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionCouldNotBeGeneratedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionGeneratorShutDownException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionIsNotPendingException;
import uk.ac.ebi.ampt2d.commons.accession.core.models.AccessionWrapper;
import uk.ac.ebi.ampt2d.commons.accession.core.models.SaveResponse;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.entities.ContiguousIdBlock;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.repositories.ContiguousIdBlockRepository;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.service.ContiguousIdBlockService;
import uk.ac.ebi.ampt2d.commons.accession.service.BasicSpringDataRepositoryMonotonicDatabaseService;
import uk.ac.ebi.ampt2d.commons.accession.utils.exceptions.ExponentialBackOffMaxRetriesRuntimeException;
import uk.ac.ebi.ampt2d.test.configuration.MonotonicAccessionGeneratorTestConfiguration;
import uk.ac.ebi.ampt2d.test.configuration.TestMonotonicDatabaseServiceTestConfiguration;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.ampt2d.commons.accession.util.ContiguousIdBlockUtil.getAllBlocksForCategoryId;
import static uk.ac.ebi.ampt2d.commons.accession.util.ContiguousIdBlockUtil.getUnreservedContiguousIdBlock;

@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = {MonotonicAccessionGeneratorTestConfiguration.class, TestMonotonicDatabaseServiceTestConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
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

    @Autowired
    private BasicSpringDataRepositoryMonotonicDatabaseService monotonicDBService;

    @Test
    public void assertNoBlockGeneratedAtLoadIfNoneExists() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        assertEquals(0, repository.count());
    }

    @Test
    public void assertBlockGeneratedAtGenerateOperationIfNoBlockExists() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        generator.generateAccessions(TENTH_BLOCK_SIZE, INSTANCE_ID);
        assertEquals(1, repository.count());
        ContiguousIdBlock block = repository.findFirstByCategoryIdOrderByLastValueDesc(CATEGORY_ID);
        assertEquals(0, block.getFirstValue());
        assertEquals(BLOCK_SIZE - 1, block.getLastValue());
        assertEquals(-1, block.getLastCommitted());
    }

    @Test
    public void assertBlockNotGeneratedIfPreviousExists() throws Exception {
        assertEquals(0, repository.count());
        repository.save(new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 0, BLOCK_SIZE));
        assertEquals(1, repository.count());
        MonotonicAccessionGenerator generator = new MonotonicAccessionGenerator(
                CATEGORY_ID, service, monotonicDBService);

        assertEquals(1, repository.count());
    }

    @Test
    public void assertNewBlockGeneratedInSecondInstance() throws Exception {
        ContiguousIdBlock block;
        assertEquals(0, repository.count());

        MonotonicAccessionGenerator generator1 = new MonotonicAccessionGenerator(
                CATEGORY_ID, service, monotonicDBService);
        MonotonicAccessionGenerator generator2 = new MonotonicAccessionGenerator(
                CATEGORY_ID, service, monotonicDBService);

        generator1.generateAccessions(TENTH_BLOCK_SIZE, INSTANCE_ID);
        assertEquals(1, repository.count());
        block = findFirstByCategoryIdAndApplicationInstanceIdOrderByLastValueDesc(CATEGORY_ID);
        assertEquals(0, block.getFirstValue());
        assertEquals(BLOCK_SIZE - 1, block.getLastValue());
        assertEquals(-1, block.getLastCommitted());

        generator2.generateAccessions(TENTH_BLOCK_SIZE, INSTANCE_2_ID);
        assertEquals(2, repository.count());
        block = findFirstByCategoryIdAndApplicationInstanceIdOrderByLastValueDesc(CATEGORY_ID);
        assertEquals(BLOCK_SIZE, block.getFirstValue());
        assertEquals(2 * BLOCK_SIZE - 1, block.getLastValue());
        assertEquals(BLOCK_SIZE - 1, block.getLastCommitted());
    }

    @Test
    public void assertGenerateAccessions() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();

        long[] accessions = generator.generateAccessions(TENTH_BLOCK_SIZE, INSTANCE_ID);
        assertEquals(TENTH_BLOCK_SIZE, accessions.length);
    }

    @Test
    public void assertGenerateMoreAccessionsThanBlockSizeGeneratesTwoBlocks() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();

        //Generate BLOCK_SIZE accessions in BLOCK_SIZE/10 increments
        for (int i = 0; i < 10; i++) {
            long[] accessions = generator.generateAccessions(TENTH_BLOCK_SIZE, INSTANCE_ID);
            assertEquals(i * TENTH_BLOCK_SIZE, accessions[0]);
        }
        long[] accessions = generator.generateAccessions(TENTH_BLOCK_SIZE, INSTANCE_ID);
        assertEquals(BLOCK_SIZE, accessions[0]);
        assertEquals(2, repository.count());
    }

    @Test
    public void assertGenerateMoreAccessionsThanBlockSizeGeneratesInOneCall() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        long[] accessions = generator.generateAccessions(BLOCK_SIZE + (BLOCK_SIZE / 2), INSTANCE_ID);

        assertEquals(2, repository.count());
    }

    @Test
    public void assertCommitModifiesLastCommitted() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        long[] accessions = generator.generateAccessions(TENTH_BLOCK_SIZE, INSTANCE_ID);

        generator.commit(accessions);

        ContiguousIdBlock block = findFirstByCategoryIdAndApplicationInstanceIdOrderByLastValueDesc(CATEGORY_ID);
        assertEquals(TENTH_BLOCK_SIZE - 1, block.getLastCommitted());
    }

    @Test
    public void assertNotCommittingDoesNotModifyLastCommitted() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        long[] accessions = generator.generateAccessions(TENTH_BLOCK_SIZE, INSTANCE_ID);

        ContiguousIdBlock block = findFirstByCategoryIdAndApplicationInstanceIdOrderByLastValueDesc(CATEGORY_ID);
        assertEquals(-1, block.getLastCommitted());
    }

    @Test
    public void assertCommitOutOfOrderDoesNotModifyLastCommittedUntilTheSequenceIsComplete() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        long[] accessions1 = generator.generateAccessions(TENTH_BLOCK_SIZE, INSTANCE_ID);
        long[] accessions2 = generator.generateAccessions(TENTH_BLOCK_SIZE, INSTANCE_ID);

        generator.commit(accessions2);

        ContiguousIdBlock block = findFirstByCategoryIdAndApplicationInstanceIdOrderByLastValueDesc(CATEGORY_ID);
        assertEquals(-1, block.getLastCommitted());

        generator.commit(accessions1);

        block = findFirstByCategoryIdAndApplicationInstanceIdOrderByLastValueDesc(CATEGORY_ID);
        assertEquals(2 * TENTH_BLOCK_SIZE - 1, block.getLastCommitted());
    }

    @Test
    public void assertCommitOutOfOrderDoesNotModifyLastCommittedUntilTheSequenceIsCompleteMultipleBlocks() throws
            Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        long[] accessions1 = generator.generateAccessions(BLOCK_SIZE + TENTH_BLOCK_SIZE, INSTANCE_ID);
        long[] accessions2 = generator.generateAccessions(TENTH_BLOCK_SIZE, INSTANCE_ID);

        generator.commit(accessions2);

        ContiguousIdBlock block = findFirstByCategoryIdAndApplicationInstanceIdOrderByLastValueDesc(CATEGORY_ID);
        assertEquals(BLOCK_SIZE, block.getFirstValue());
        assertEquals(BLOCK_SIZE - 1, block.getLastCommitted());

        generator.commit(accessions1);

        block = findFirstByCategoryIdAndApplicationInstanceIdOrderByLastValueDesc(CATEGORY_ID);
        assertEquals(BLOCK_SIZE, block.getFirstValue());
        assertEquals(BLOCK_SIZE + 2 * TENTH_BLOCK_SIZE - 1, block.getLastCommitted());
    }

    @Test
    public void assertGenerateDoesNotReuseIds() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        generator.generateAccessions(TENTH_BLOCK_SIZE, INSTANCE_ID);
        long[] accessions2 = generator.generateAccessions(TENTH_BLOCK_SIZE, INSTANCE_ID);
        assertEquals(TENTH_BLOCK_SIZE, accessions2[0]);
    }

    @Test
    public void assertReleaseMakesGenerateReuseIds() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        long[] accessions1 = generator.generateAccessions(TENTH_BLOCK_SIZE, INSTANCE_ID);
        generator.release(accessions1);
        long[] accessions2 = generator.generateAccessions(TENTH_BLOCK_SIZE, INSTANCE_ID);
        assertEquals(0, accessions2[0]);
    }

    @Test
    public void assertReleaseSomeIdsMakesGenerateReuseReleasedIdsAndNewOnes() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        generator.generateAccessions(TENTH_BLOCK_SIZE, INSTANCE_ID);
        generator.release(0, 1);
        long[] accessions2 = generator.generateAccessions(TENTH_BLOCK_SIZE, INSTANCE_ID);
        assertEquals(0, accessions2[0]);
        assertEquals(1, accessions2[1]);
        assertEquals(TENTH_BLOCK_SIZE, accessions2[2]);
    }

    @Test
    public void assertMultipleReleaseSomeIdsMakesGenerateReuseReleasedIdsAndNewOnes() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        long[] accessions1 = generator.generateAccessions(TENTH_BLOCK_SIZE, INSTANCE_ID);
        generator.release(0, 1);
        generator.release(3, 4, 5);
        generator.release(8, 9, 10);
        long[] accessions2 = generator.generateAccessions(TENTH_BLOCK_SIZE, INSTANCE_ID);
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
        generator.generateAccessions(BLOCK_SIZE, INSTANCE_ID);
        generator.commit(2);
        generator.release(0, 1);
        generator.release(3, 4, 5);
        generator.commit(6, 7, 11);
        generator.release(8, 9, 10);
        generator.commit(getLongArray(12, 998));
        //999 is waiting somewhere taking a big nap and no elements have been confirmed due to element 0 being released
        ContiguousIdBlock block = findFirstByCategoryIdAndApplicationInstanceIdOrderByLastValueDesc(CATEGORY_ID);
        assertEquals(-1, block.getLastCommitted());

        long[] accessions2 = generator.generateAccessions(BLOCK_SIZE, INSTANCE_ID);
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
        block = findFirstByCategoryIdAndApplicationInstanceIdOrderByLastValueDesc(CATEGORY_ID);
        assertEquals(1991, block.getLastCommitted());
    }

    @Test
    public void assertRecoverNoPendingCommit() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        long[] accessions1 = generator.generateAccessions(BLOCK_SIZE, INSTANCE_ID);
        generator.shutDownAccessionGenerator();
        // Now assume that the db layer has stored some elements and that the application has died and restarted.

        MonotonicAccessionGenerator generatorRecovering =
                new MonotonicAccessionGenerator(CATEGORY_ID, service, monotonicDBService);
        ContiguousIdBlock block = findFirstByCategoryIdAndApplicationInstanceIdOrderByLastValueDesc(CATEGORY_ID);
        assertEquals(-1, block.getLastCommitted());
        generatorRecovering.generateAccessions(1, INSTANCE_ID);
        assertFalse(generatorRecovering.getAvailableRanges().isEmpty());
    }

    @Test
    public void assertRecoverPendingCommit() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        long[] accessions1 = generator.generateAccessions(BLOCK_SIZE, INSTANCE_ID);
        generator.commit(0, 1);
        generator.shutDownAccessionGenerator();
        // Now assume that the db layer has stored some elements and that the application has died and restarted.

        MonotonicAccessionGenerator generatorRecovering = new MonotonicAccessionGenerator(
                CATEGORY_ID, service, monotonicDBService);
        ContiguousIdBlock block = findFirstByCategoryIdAndApplicationInstanceIdOrderByLastValueDesc(CATEGORY_ID);
        assertEquals(1, block.getLastCommitted());
        generatorRecovering.generateAccessions(1, INSTANCE_ID);
        assertEquals(1, generatorRecovering.getAvailableRanges().size());
        MonotonicRange monotonicRange = generatorRecovering.getAvailableRanges().peek();
        assertEquals(3, monotonicRange.getStart());
        assertEquals(BLOCK_SIZE - 1, monotonicRange.getEnd());
    }

    @Test(expected = AccessionIsNotPendingException.class)
    public void assertReleaseAndCommitSameElement() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        generator.generateAccessions(BLOCK_SIZE, INSTANCE_ID);
        generator.release(2);
        generator.commit(2);
    }

    @Test(expected = AccessionIsNotPendingException.class)
    public void assertCommitAndReleaseSameElement() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        generator.generateAccessions(BLOCK_SIZE, INSTANCE_ID);
        generator.commit(2);
        generator.release(2);
    }

    @Test(expected = AccessionIsNotPendingException.class)
    public void releaseSomeIdsTwice() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        generator.generateAccessions(TENTH_BLOCK_SIZE, INSTANCE_ID);
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
                new MonotonicAccessionGenerator(CATEGORY_ID, service, monotonicDBService);

        HashMap<String, String> objects = new HashMap<>();
        objects.put("hash1", "object2");
        objects.put("hash2", "object2");

        List<AccessionWrapper<String, String, Long>> generatedAccessions = generator.generateAccessions(objects, INSTANCE_ID);

        assertEquals(1, repository.count());
        assertEquals(0L, (long) generatedAccessions.get(0).getAccession());
        assertEquals(1L, (long) generatedAccessions.get(1).getAccession());
    }

    @Test
    public void postSaveAction() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGenerator();
        generator.generateAccessions(BLOCK_SIZE, INSTANCE_ID);
        Set<Long> committed = new HashSet<>();
        committed.add(0L);
        committed.add(1L);
        committed.add(3L);
        committed.add(4L);
        Set<Long> released = new HashSet<>();
        released.add(2L);
        released.add(5L);
        generator.postSave(new SaveResponse(committed, released));
        long[] accessions = generator.generateAccessions(BLOCK_SIZE, INSTANCE_ID);
        assertEquals(2, accessions[0]);
        assertEquals(5, accessions[1]);
        assertEquals(BLOCK_SIZE, accessions[2]);
    }

    @Test
    public void assertReleaseInAlternateRanges() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGeneratorForCategoryHavingBlockInterval();
        long[] accessions1 = generator.generateAccessions(NUM_OF_ACCESSIONS, INSTANCE_ID);
        assertEquals(1, accessions1[0]);
        assertEquals(2, accessions1[1]);
        assertEquals(3, accessions1[2]);
        assertEquals(4, accessions1[3]);
        generator.release(new long[]{2, 3});
        long[] accessions2 = generator.generateAccessions(NUM_OF_ACCESSIONS, INSTANCE_ID);
        assertEquals(2, accessions2[0]);
        assertEquals(3, accessions2[1]);
        assertEquals(5, accessions2[2]);
        assertEquals(11, accessions2[3]);
    }

    @Test
    public void assertRecoverInAlternateRanges() throws Exception {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGeneratorForCategoryHavingBlockInterval();
        long[] accessions1 = generator.generateAccessions(6, INSTANCE_ID);
        generator.shutDownAccessionGenerator();
        // Now assume that the db layer has stored some elements and that the application has died and restarted.
        MonotonicAccessionGenerator generatorRecovering =
                new MonotonicAccessionGenerator(CATEGORY_ID_2, service, monotonicDBService);
        long[] accessions2 = generatorRecovering.generateAccessions(6, INSTANCE_ID);
        assertEquals(1, accessions2[0]);
        assertEquals(2, accessions2[1]);
        assertEquals(3, accessions2[2]);
        assertEquals(4, accessions2[3]);
        assertEquals(5, accessions2[4]);
        assertEquals(11, accessions2[5]);
    }

    private MonotonicAccessionGenerator getMonotonicAccessionGenerator() throws Exception {
        assertEquals(0, repository.count());

        MonotonicAccessionGenerator generator = new MonotonicAccessionGenerator(
                CATEGORY_ID, service, monotonicDBService);
        return generator;
    }

    private MonotonicAccessionGenerator getMonotonicAccessionGeneratorForCategoryHavingBlockInterval() {
        assertEquals(0, repository.count());
        return new MonotonicAccessionGenerator(CATEGORY_ID_2, service, monotonicDBService);
    }

    @Test
    public void assertAbortExecutionWhenDBConstraintExceptionThrown() {
        ContiguousIdBlockService mockService = Mockito.mock(ContiguousIdBlockService.class, Answers.RETURNS_DEEP_STUBS);
        MonotonicAccessionGenerator mockGenerator = new MonotonicAccessionGenerator(CATEGORY_ID, mockService, monotonicDBService);
        when(mockService.reserveNewBlock(anyString(), anyString())).thenThrow(ConstraintViolationException.class);
        assertThrows(ExponentialBackOffMaxRetriesRuntimeException.class, () -> mockGenerator.generateAccessions(1, INSTANCE_ID));
        assertEquals(0, repository.count());
    }

    @Test
    public void testInitializeBlockManager() throws AccessionCouldNotBeGeneratedException {
        ContiguousIdBlock block = getUnreservedContiguousIdBlock(CATEGORY_ID_2, INSTANCE_ID, 0, 10);
        repository.save(block);

        // To start with Block is UnCompleted and UnReserved
        List<ContiguousIdBlock> blockInDBList = findAllByCategoryIdAndApplicationInstanceIdOrderByLastValueAsc(CATEGORY_ID_2);
        assertEquals(1, blockInDBList.size());
        List<ContiguousIdBlock> unreservedBlocks = blockInDBList.stream()
                .filter(b -> b.isNotFull() && b.isNotReserved())
                .collect(Collectors.toList());
        assertEquals(1, unreservedBlocks.size());
        assertEquals(0, unreservedBlocks.get(0).getFirstValue());
        assertEquals(9, unreservedBlocks.get(0).getLastValue());
        assertEquals(-1, unreservedBlocks.get(0).getLastCommitted());
        assertEquals(false, unreservedBlocks.get(0).isReserved());

        // Generator 1 starts
        MonotonicAccessionGenerator generator1 = new MonotonicAccessionGenerator(CATEGORY_ID_2, service, monotonicDBService);
        assertEquals(0, generator1.getAvailableRanges().size());
        // its recover state reserves the UnCompleted block
        generator1.generateAccessions(1, INSTANCE_ID);
        assertEquals(1, generator1.getAvailableRanges().size());
        assertEquals(new MonotonicRange(1, 9), generator1.getAvailableRanges().peek());

        // Block is currently reserved by Generator-1
        blockInDBList = findAllByCategoryIdAndApplicationInstanceIdOrderByLastValueAsc(CATEGORY_ID_2);
        assertEquals(1, blockInDBList.size());
        List<ContiguousIdBlock> reservedBlocks = blockInDBList.stream()
                .filter(b -> b.isNotFull() && b.isReserved())
                .collect(Collectors.toList());
        assertEquals(1, reservedBlocks.size());
        assertEquals(0, reservedBlocks.get(0).getFirstValue());
        assertEquals(9, reservedBlocks.get(0).getLastValue());
        assertEquals(-1, reservedBlocks.get(0).getLastCommitted());
        assertEquals(true, reservedBlocks.get(0).isReserved());

        // Generator-2 will not be able to reserve the un-completed block as it is currently reserved by Generator-1
        MonotonicAccessionGenerator generator2 = new MonotonicAccessionGenerator(CATEGORY_ID_2, service, monotonicDBService);
        generator2.generateAccessions(0, INSTANCE_ID);
        assertEquals(0, generator2.getAvailableRanges().size());

        // Generator-3 can reserve the same Uncompleted block, once Generator-1 releases it
        generator1.shutDownAccessionGenerator();
        MonotonicAccessionGenerator generator3 = new MonotonicAccessionGenerator(CATEGORY_ID_2, service, monotonicDBService);
        generator3.generateAccessions(1, INSTANCE_ID);
        assertEquals(1, generator3.getAvailableRanges().size());
        assertEquals(new MonotonicRange(1, 9), generator3.getAvailableRanges().peek());
    }

    @Test
    public void testShutDownAccessionGenerator() {
        MonotonicAccessionGenerator generator = getMonotonicAccessionGeneratorForCategoryHavingBlockInterval();
        generator.shutDownAccessionGenerator();

        assertThrows(AccessionGeneratorShutDownException.class, () -> generator.generateAccessions(24, INSTANCE_ID));
        assertThrows(AccessionGeneratorShutDownException.class, () -> generator.generateAccessions(new HashMap(), INSTANCE_ID));
        assertThrows(AccessionGeneratorShutDownException.class, () -> generator.commit());
        assertThrows(AccessionGeneratorShutDownException.class, () -> generator.release());
        assertThrows(AccessionGeneratorShutDownException.class, () -> generator.postSave(new SaveResponse<>()));
        assertThrows(AccessionGeneratorShutDownException.class, () -> generator.getAvailableRanges());
    }

    private List<ContiguousIdBlock> findAllByCategoryIdAndApplicationInstanceIdOrderByLastValueAsc(String categoryId) {
        return getAllBlocksForCategoryId(repository, categoryId).stream()
                .sorted(Comparator.comparing(ContiguousIdBlock::getLastValue))
                .collect(Collectors.toList());
    }

    private ContiguousIdBlock findFirstByCategoryIdAndApplicationInstanceIdOrderByLastValueDesc(String categoryId) {
        return getAllBlocksForCategoryId(repository, categoryId).stream()
                .sorted(Comparator.comparing(ContiguousIdBlock::getLastValue).reversed())
                .findFirst().get();
    }

}
