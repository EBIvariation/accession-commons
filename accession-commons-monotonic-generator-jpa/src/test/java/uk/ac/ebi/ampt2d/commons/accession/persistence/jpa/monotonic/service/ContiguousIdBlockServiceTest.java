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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.entities.ContiguousIdBlock;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.repositories.ContiguousIdBlockRepository;
import uk.ac.ebi.ampt2d.test.configuration.MonotonicAccessionGeneratorTestConfiguration;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = {MonotonicAccessionGeneratorTestConfiguration.class})
public class ContiguousIdBlockServiceTest {

    private static final String CATEGORY_ID = "cat-test";
    private static final String CATEGORY_ID_2 = "contiguous-block-test";
    private static final String INSTANCE_ID = "test-instance";
    private static final String INSTANCE_ID_2 = "test-instance2";

    @Autowired
    private ContiguousIdBlockRepository repository;

    @Autowired
    private ContiguousIdBlockService service;

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
        // Save a block
        service.save(Arrays.asList(new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 0, 5)));
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
    public void testGetUncompleteBlocks() {
        ContiguousIdBlock uncompletedBlock = new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 0, 5);
        ContiguousIdBlock completedBlock = new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 10, 5);
        completedBlock.setLastCommitted(14);

        service.save(Arrays.asList(uncompletedBlock));
        service.save(Arrays.asList(new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID_2, 5, 5)));
        service.save(Arrays.asList(completedBlock));
        service.save(Arrays.asList(new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 15, 5)));

        List<ContiguousIdBlock> contiguousBlocks =
                service.getUncompletedBlocksByCategoryIdAndApplicationInstanceIdOrderByEndAsc(CATEGORY_ID, INSTANCE_ID);

        assertEquals(2, contiguousBlocks.size());
        assertEquals(0, contiguousBlocks.get(0).getFirstValue());
        assertEquals(4, contiguousBlocks.get(0).getLastValue());
        assertEquals(15, contiguousBlocks.get(1).getFirstValue());
        assertEquals(19, contiguousBlocks.get(1).getLastValue());
    }

    @Test
    public void testBlockSizeAndIntervalForCategory() {
        ContiguousIdBlock block1 = service.reserveNewBlock(CATEGORY_ID_2, INSTANCE_ID);
        assertEquals(0, block1.getFirstValue());
        assertEquals(999, block1.getLastValue());
        ContiguousIdBlock block2 = service.reserveNewBlock(CATEGORY_ID_2, INSTANCE_ID);
        assertEquals(2000, block2.getFirstValue());
        assertEquals(2999, block2.getLastValue());

        List<ContiguousIdBlock> contiguousBlocks = service
                .getUncompletedBlocksByCategoryIdAndApplicationInstanceIdOrderByEndAsc(CATEGORY_ID_2, INSTANCE_ID);
        assertEquals(2, contiguousBlocks.size());
        assertTrue(contiguousBlocks.get(0).isNotFull());
        assertTrue(contiguousBlocks.get(1).isNotFull());
    }

    @Test
    public void testBlockSizeAndIntervalWithMultipleInstances() {
        ContiguousIdBlock block1 = service.reserveNewBlock(CATEGORY_ID_2, INSTANCE_ID);
        assertEquals(0, block1.getFirstValue());
        assertEquals(999, block1.getLastValue());
        ContiguousIdBlock block2 = service.reserveNewBlock(CATEGORY_ID_2, INSTANCE_ID_2);
        assertEquals(2000, block2.getFirstValue());
        assertEquals(2999, block2.getLastValue());
        assertEquals(block2, repository.findFirstByCategoryIdOrderByLastValueDesc(CATEGORY_ID_2));

        //Manually save a block of size 500, so for the current range only a block size of 500 reserved
        repository.save(new ContiguousIdBlock(CATEGORY_ID_2, INSTANCE_ID, 4000, 500));
        //Reserve a new block with size 1000
        ContiguousIdBlock block3 = service.reserveNewBlock(CATEGORY_ID_2, INSTANCE_ID_2);

        assertEquals(4500, block3.getFirstValue());
        // the block is reserved only with size 500 the remainingSize in the range is 500 only
        assertEquals(4999, block3.getLastValue());

        // for remaining elements service would reserve new block interleaved by 1000.
        ContiguousIdBlock block4 = service.reserveNewBlock(CATEGORY_ID_2, INSTANCE_ID_2);
        assertEquals(6000, block4.getFirstValue());
        // the block is reserved only with size full size 1000
        assertEquals(6999, block4.getLastValue());
    }

    @Test
    public void testNextBlock() {
        // Reserving initial block
        ContiguousIdBlock block1 = new ContiguousIdBlock(CATEGORY_ID,INSTANCE_ID,0,1000);
        assertEquals(0, block1.getFirstValue());
        assertEquals(999, block1.getLastValue());

        ContiguousIdBlock block2 = block1.nextBlock(INSTANCE_ID, 2000, 0, 0);
        assertEquals(1000, block2.getFirstValue()); // does not interleave as interleaveInterval = 0
        assertEquals(2999, block2.getLastValue()); // as there is no interleaving any size can be reserved for a block

        //Test different instance and different size
        block2 = block1.nextBlock(INSTANCE_ID, 500, 1000, 0);
        assertEquals(2000, block2.getFirstValue()); // interleaves as interleavingPoint is multiple of 1000
        assertEquals(2499, block2.getLastValue());
        //Reserving block with different instance and different size
        ContiguousIdBlock block3 = block2.nextBlock(INSTANCE_ID_2, 1000, 1000, 0);
        assertEquals(2500, block3.getFirstValue()); // doesn't interleave as interleavingPoint is multiple of 1000
        assertEquals(2999, block3.getLastValue()); // Available size is only 500 before interleaving point

        block2 = block1.nextBlock(INSTANCE_ID, 2000, 2000, 0);
        assertEquals(1000, block2.getFirstValue()); // does not interleave as interleavingPoint is multiple of 1000
        assertEquals(1999, block2.getLastValue()); // available size is only 1000 before interleaving point
        block3 = block2.nextBlock(INSTANCE_ID, 2000, 2000, 0);
        //Interleaves as interleavingPoint is multiple of 2000 and interleaved 2000
        assertEquals(4000, block3.getFirstValue());
        assertEquals(5999, block3.getLastValue()); // full 2000 is reserved as the new range contains 2000 values

        // Reserving initial block start other than 0
        block1 = new ContiguousIdBlock(CATEGORY_ID,INSTANCE_ID,15,10);
        assertEquals(15, block1.getFirstValue());
        assertEquals(24, block1.getLastValue());

        block2 = block1.nextBlock(INSTANCE_ID, 10, 0, 15);
        assertEquals(25, block2.getFirstValue()); // does not interleave as interleaveInterval = 0
        assertEquals(34, block2.getLastValue());

        block2 = block1.nextBlock(INSTANCE_ID, 10, 10, 15);
        assertEquals(35, block2.getFirstValue()); // interleaves as interleavingPoint is at 25
        assertEquals(44, block2.getLastValue());


    }

}
