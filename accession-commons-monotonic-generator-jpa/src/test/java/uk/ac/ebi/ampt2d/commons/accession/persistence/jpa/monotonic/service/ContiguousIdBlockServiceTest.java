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
package uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.service;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.entities.ContiguousIdBlock;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.repositories.ContiguousIdBlockRepository;
import uk.ac.ebi.ampt2d.test.configuration.MonotonicAccessionGeneratorTestConfiguration;

import javax.persistence.PersistenceException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static uk.ac.ebi.ampt2d.commons.accession.util.ContiguousIdBlockUtil.getAllBlocksForCategoryId;
import static uk.ac.ebi.ampt2d.commons.accession.util.ContiguousIdBlockUtil.getUnreservedContiguousIdBlock;

@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = {MonotonicAccessionGeneratorTestConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ContiguousIdBlockServiceTest {

    private static final String CATEGORY_ID = "cat-test";
    private static final String CATEGORY_ID_2 = "contiguous-block-test";
    private static final String INSTANCE_ID = "test-instance";
    private static final String INSTANCE_ID_2 = "test-instance2";

    @Autowired
    private ContiguousIdBlockRepository repository;

    @Autowired
    private ContiguousIdBlockService service;

    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    public void testReserveNewBlocks() {
        ContiguousIdBlock block = service.reserveNewBlock(CATEGORY_ID, INSTANCE_ID);
        assertEquals(0, block.getFirstValue());
        assertEquals(999, block.getLastValue());
        assertTrue(block.isNotFull());
        ContiguousIdBlock block2 = service.reserveNewBlock(CATEGORY_ID, INSTANCE_ID);
        assertEquals(1000, block2.getFirstValue());
        assertEquals(1999, block2.getLastValue());
        assertTrue(block.isNotFull());
    }

    @Test
    public void testReserveWithExistingData() {
        //Save a block
        service.save(Arrays.asList(new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 0, 5)));
        testEntityManager.flush();
        testEntityManager.getEntityManager().getTransaction().commit();

        ContiguousIdBlock block = service.reserveNewBlock(CATEGORY_ID, INSTANCE_ID);
        assertEquals(5, block.getFirstValue());
        assertEquals(1004, block.getLastValue());
        assertTrue(block.isNotFull());
    }

    @Test
    public void testReserveNewBlocksWithMultipleInstances() {
        ContiguousIdBlock block = service.reserveNewBlock(CATEGORY_ID, INSTANCE_ID);
        assertEquals(0, block.getFirstValue());
        assertEquals(999, block.getLastValue());
        assertTrue(block.isNotFull());
        ContiguousIdBlock block2 = service.reserveNewBlock(CATEGORY_ID, INSTANCE_ID_2);
        assertEquals(1000, block2.getFirstValue());
        assertEquals(1999, block2.getLastValue());
        assertTrue(block.isNotFull());
        ContiguousIdBlock block3 = service.reserveNewBlock(CATEGORY_ID, INSTANCE_ID);
        assertEquals(2000, block3.getFirstValue());
        assertEquals(2999, block3.getLastValue());
        assertTrue(block.isNotFull());
    }

    @Test
    public void testGetUncompletedBlocks() {
        // unreserved and uncompleted
        ContiguousIdBlock uncompletedBlock = getUnreservedContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 0, 5);
        // unreserved and uncompleted with different instance id
        ContiguousIdBlock uncompletedAndUnreservedWithDifferentInstanceId = getUnreservedContiguousIdBlock(CATEGORY_ID, INSTANCE_ID_2, 5, 5);

        // unreserved but completed
        ContiguousIdBlock unreservedButCompletedBlock = getUnreservedContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 10, 5);
        unreservedButCompletedBlock.setLastCommitted(14);
        // uncompleted but reserved
        ContiguousIdBlock UncompletedButReservedBlock = new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 15, 5);
        // completed and reserved
        ContiguousIdBlock completedAndReservedBlock = new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 20, 5);
        completedAndReservedBlock.setLastCommitted(24);

        service.save(Arrays.asList(uncompletedBlock));
        service.save(Arrays.asList(uncompletedAndUnreservedWithDifferentInstanceId));
        service.save(Arrays.asList(unreservedButCompletedBlock));
        service.save(Arrays.asList(UncompletedButReservedBlock));
        service.save(Arrays.asList(completedAndReservedBlock));

        // Reserve Uncompleted blocks - should only reserve uncompleted and unreserved
        String reservingInstanceId = "instance-Id-3";
        List<ContiguousIdBlock> contiguousBlocks = service.reserveUncompletedBlocksForCategoryIdAndApplicationInstanceId(CATEGORY_ID, reservingInstanceId);

        assertEquals(2, contiguousBlocks.size());
        assertEquals(0, contiguousBlocks.get(0).getFirstValue());
        assertEquals(4, contiguousBlocks.get(0).getLastValue());
        assertEquals(reservingInstanceId, contiguousBlocks.get(0).getApplicationInstanceId());
        assertTrue(contiguousBlocks.get(0).isReserved());
        assertEquals(5, contiguousBlocks.get(1).getFirstValue());
        assertEquals(9, contiguousBlocks.get(1).getLastValue());
        assertEquals(reservingInstanceId, contiguousBlocks.get(1).getApplicationInstanceId());
        assertTrue(contiguousBlocks.get(1).isReserved());
    }

    @Test
    public void testBlockSizeAndIntervalForCategory() {
        ContiguousIdBlock block1 = service.reserveNewBlock(CATEGORY_ID_2, INSTANCE_ID);
        assertEquals(0, block1.getFirstValue());
        assertEquals(999, block1.getLastValue());
        ContiguousIdBlock block2 = service.reserveNewBlock(CATEGORY_ID_2, INSTANCE_ID);
        assertEquals(2000, block2.getFirstValue());
        assertEquals(2999, block2.getLastValue());

        List<ContiguousIdBlock> contiguousBlocks = getAllBlocksForCategoryId(repository, CATEGORY_ID_2);
        assertEquals(2, contiguousBlocks.size());
        assertTrue(contiguousBlocks.get(0).isNotFull());
        assertEquals(0, contiguousBlocks.get(0).getFirstValue());
        assertEquals(-1, contiguousBlocks.get(0).getLastCommitted());
        assertEquals(999, contiguousBlocks.get(0).getLastValue());
        assertTrue(contiguousBlocks.get(1).isNotFull());
        assertEquals(2000, contiguousBlocks.get(1).getFirstValue());
        assertEquals(1999, contiguousBlocks.get(1).getLastCommitted());
        assertEquals(2999, contiguousBlocks.get(1).getLastValue());
    }

    @Test
    public void testBlockSizeAndIntervalWithMultipleInstances() {
        ContiguousIdBlock block1 = service.reserveNewBlock(CATEGORY_ID_2, INSTANCE_ID);
        assertEquals(0, block1.getFirstValue());
        assertEquals(999, block1.getLastValue());
        ContiguousIdBlock block2 = service.reserveNewBlock(CATEGORY_ID_2, INSTANCE_ID_2);
        assertEquals(2000, block2.getFirstValue());
        assertEquals(2999, block2.getLastValue());
        ContiguousIdBlock block2InDB = repository.findFirstByCategoryIdOrderByLastValueDesc(CATEGORY_ID_2);
        assertEquals(2000, block2InDB.getFirstValue());
        assertEquals(2999, block2InDB.getLastValue());

        //Manually save a block of size 500, so for the current range only a block size of 500 reserved
        repository.save(new ContiguousIdBlock(CATEGORY_ID_2, INSTANCE_ID, 4000, 500));
        testEntityManager.flush();
        testEntityManager.getEntityManager().getTransaction().commit();
        //Reserve a new block with size 1000
        ContiguousIdBlock block3 = service.reserveNewBlock(CATEGORY_ID_2, INSTANCE_ID_2);

        assertEquals(4500, block3.getFirstValue());
        //The block was reserved with size 1000, but only 500 were available due to the interleaving.
        assertEquals(4999, block3.getLastValue());

        //For remaining elements service would reserve new block interleaved by 1000.
        ContiguousIdBlock block4 = service.reserveNewBlock(CATEGORY_ID_2, INSTANCE_ID_2);
        assertEquals(6000, block4.getFirstValue());
        assertEquals(6999, block4.getLastValue());
    }

    @Test
    public void testNextBlockWithZeroInterleaveInterval() {
        //Reserving initial block
        ContiguousIdBlock block1 = new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 0, 1000);
        assertEquals(0, block1.getFirstValue());
        assertEquals(999, block1.getLastValue());

        ContiguousIdBlock block2 = block1.nextBlock(INSTANCE_ID, 2000, 0, 0);
        assertEquals(1000, block2.getFirstValue()); // does not interleave as nextBlockInterval = 0
        assertEquals(2999, block2.getLastValue()); // as there is no interleaving any size can be reserved for a block
    }

    @Test
    public void testNextBlockWithDifferentSizeAndInstance() {
        //Reserving initial block
        ContiguousIdBlock block1 = new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 0, 1000);
        assertEquals(0, block1.getFirstValue());
        assertEquals(999, block1.getLastValue());

        //Test different instance and different size
        ContiguousIdBlock block2 = block1.nextBlock(INSTANCE_ID, 500, 1000, 0);
        assertEquals(2000, block2.getFirstValue()); // interleaves as interleavingPoint is multiple of 1000
        assertEquals(2499, block2.getLastValue());
        //Reserving block with different instance and different size
        ContiguousIdBlock block3 = block2.nextBlock(INSTANCE_ID_2, 1000, 1000, 0);
        assertEquals(2500, block3.getFirstValue()); // does not interleave as interleavingPoint is multiple of 1000
        assertEquals(2999, block3.getLastValue()); // Available size is only 500 before interleaving point
    }

    @Test
    public void testNextBlockWithLargerInterleaveInterval() {
        //Reserving initial block
        ContiguousIdBlock block1 = new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 0, 1000);
        assertEquals(0, block1.getFirstValue());
        assertEquals(999, block1.getLastValue());

        ContiguousIdBlock block2 = block1.nextBlock(INSTANCE_ID, 2000, 2000, 0);
        assertEquals(1000, block2.getFirstValue()); // does not interleave as interleavingPoint is multiple of 1000
        assertEquals(1999, block2.getLastValue()); // available size is only 1000 before interleaving point
        ContiguousIdBlock block3 = block2.nextBlock(INSTANCE_ID, 2000, 2000, 0);
        //Interleaves as interleavingPoint is multiple of 2000 and interleaved 2000
        assertEquals(4000, block3.getFirstValue());
        assertEquals(5999, block3.getLastValue()); // full 2000 is reserved as the new range contains 2000 values
    }

    @Test
    public void testNextBlockWithStartingPointOtherThanZero() {
        ContiguousIdBlock block1 = new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 500, 10);
        assertEquals(500, block1.getFirstValue());
        assertEquals(509, block1.getLastValue());
        block1 = block1.nextBlock(INSTANCE_ID, 10, 20, 500);
        assertEquals(510, block1.getFirstValue());
        assertEquals(519, block1.getLastValue());

        block1 = block1.nextBlock(INSTANCE_ID, 10, 20, 500);
        assertEquals(540, block1.getFirstValue());
        assertEquals(549, block1.getLastValue());
        block1 = block1.nextBlock(INSTANCE_ID, 10, 20, 500);
        assertEquals(550, block1.getFirstValue());
        assertEquals(559, block1.getLastValue());

        block1 = block1.nextBlock(INSTANCE_ID, 10, 20, 500);
        assertEquals(580, block1.getFirstValue());
        assertEquals(589, block1.getLastValue());
        block1 = block1.nextBlock(INSTANCE_ID, 10, 20, 500);
        assertEquals(590, block1.getFirstValue());
        assertEquals(599, block1.getLastValue());

        block1 = block1.nextBlock(INSTANCE_ID, 10, 20, 500);
        assertEquals(620, block1.getFirstValue());
        assertEquals(629, block1.getLastValue());
    }

    @Test
    public void testBlocksWithDuplicateCategoryAndFirstValue() {
        ContiguousIdBlock block1 = new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 100, 1000);
        repository.save(block1);
        testEntityManager.flush();

        ContiguousIdBlock block2 = new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID_2, 100, 2000);
        repository.save(block2);

        Throwable exception = assertThrows(PersistenceException.class, () -> testEntityManager.flush());
        assertTrue(exception.getCause() instanceof ConstraintViolationException);
    }

    @Test
    public void testLastUpdateTimeStampAutoUpdate() {
        // block saved with initial value
        ContiguousIdBlock block = new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 100, 1000);
        repository.save(block);
        testEntityManager.flush();

        // assert block values
        List<ContiguousIdBlock> blockInDBList = StreamSupport.stream(repository.findAll().spliterator(), false)
                .collect(Collectors.toList());
        assertEquals(1, blockInDBList.size());
        ContiguousIdBlock blockInDB = blockInDBList.get(0);
        assertEquals(CATEGORY_ID, blockInDB.getCategoryId());
        assertEquals(INSTANCE_ID, blockInDB.getApplicationInstanceId());
        assertEquals(100, blockInDB.getFirstValue());
        assertEquals(1099, blockInDB.getLastValue());
        assertEquals(99, blockInDB.getLastCommitted());

        LocalDateTime blockInsertTime = blockInDB.getLastUpdatedTimestamp();

        // block updated with last committed 100
        block.setLastCommitted(100);
        repository.save(block);
        testEntityManager.flush();
        blockInDB = repository.findAll().iterator().next();
        assertEquals(100, blockInDB.getLastCommitted());

        LocalDateTime blockLastCommittedUpdateTime = blockInDB.getLastUpdatedTimestamp();

        // block updated with new instance id
        block.setApplicationInstanceId(INSTANCE_ID_2);
        repository.save(block);
        testEntityManager.flush();
        blockInDB = repository.findAll().iterator().next();
        assertEquals(INSTANCE_ID_2, blockInDB.getApplicationInstanceId());

        LocalDateTime blockApplicationInstanceUpdateTime = blockInDB.getLastUpdatedTimestamp();

        // block updated - release reserved
        block.releaseReserved();
        repository.save(block);
        testEntityManager.flush();
        blockInDB = repository.findAll().iterator().next();
        assertTrue(blockInDB.isNotReserved());

        LocalDateTime blockReleaseAsReservedUpdateTime = blockInDB.getLastUpdatedTimestamp();

        // block update - mark as reserved
        block.markAsReserved();
        repository.save(block);
        testEntityManager.flush();
        blockInDB = repository.findAll().iterator().next();
        assertTrue(blockInDB.isReserved());

        LocalDateTime blockMarkAsReservedUpdateTime = blockInDB.getLastUpdatedTimestamp();

        assertTrue(blockInsertTime.isBefore(blockLastCommittedUpdateTime));
        assertTrue(blockLastCommittedUpdateTime.isBefore(blockApplicationInstanceUpdateTime));
        assertTrue(blockApplicationInstanceUpdateTime.isBefore(blockReleaseAsReservedUpdateTime));
        assertTrue(blockReleaseAsReservedUpdateTime.isBefore(blockMarkAsReservedUpdateTime));
    }

    @Test
    public void testGetBlocksWithLastUpdatedTimeStampLessThan() throws InterruptedException {
        // reserved
        ContiguousIdBlock block1 = new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 0, 100);
        // reserved
        ContiguousIdBlock block2 = new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 100, 100);
        // not reserved
        ContiguousIdBlock block3 = getUnreservedContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 200, 100);
        // reserved but different category
        ContiguousIdBlock block4 = new ContiguousIdBlock(CATEGORY_ID_2, INSTANCE_ID, 300, 100);
        // reserved but after timestamp
        Thread.sleep(2000L);
        ContiguousIdBlock block5 = new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 400, 100);
        repository.save(block1);
        repository.save(block2);
        repository.save(block3);
        repository.save(block4);
        repository.save(block5);
        testEntityManager.flush();

        LocalDateTime cutOffTimestamp = block4.getLastUpdatedTimestamp();
        List<ContiguousIdBlock> blocksList = service.allBlocksForCategoryIdReservedBeforeTheGivenTimeFrame(CATEGORY_ID, cutOffTimestamp);

        assertEquals(2, blocksList.size());
        assertTrue(blocksList.get(0).isReserved());
        assertEquals(0, blocksList.get(0).getFirstValue());
        assertTrue(blocksList.get(1).isReserved());
        assertEquals(100, blocksList.get(1).getFirstValue());
    }

}
