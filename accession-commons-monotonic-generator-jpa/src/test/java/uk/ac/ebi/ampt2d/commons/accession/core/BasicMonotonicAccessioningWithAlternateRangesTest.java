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
package uk.ac.ebi.ampt2d.commons.accession.core;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.ampt2d.commons.accession.block.initialization.BlockInitializationException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionCouldNotBeGeneratedException;
import uk.ac.ebi.ampt2d.commons.accession.core.models.AccessionWrapper;
import uk.ac.ebi.ampt2d.commons.accession.core.models.GetOrCreateAccessionWrapper;
import uk.ac.ebi.ampt2d.commons.accession.generators.monotonic.MonotonicAccessionGenerator;
import uk.ac.ebi.ampt2d.commons.accession.generators.monotonic.MonotonicRangePriorityQueue;
import uk.ac.ebi.ampt2d.commons.accession.hashing.SHA1HashingFunction;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.entities.ContiguousIdBlock;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.repositories.ContiguousIdBlockRepository;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.service.ContiguousIdBlockService;
import uk.ac.ebi.ampt2d.commons.accession.service.BasicSpringDataRepositoryMonotonicDatabaseService;
import uk.ac.ebi.ampt2d.test.configuration.TestMonotonicDatabaseServiceTestConfiguration;
import uk.ac.ebi.ampt2d.test.models.TestModel;
import uk.ac.ebi.ampt2d.test.persistence.TestMonotonicEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static uk.ac.ebi.ampt2d.commons.accession.util.ContiguousIdBlockUtil.getUncompletedBlocksByCategoryIdAndApplicationInstanceIdOrderByEndAsc;
import static uk.ac.ebi.ampt2d.commons.accession.util.ContiguousIdBlockUtil.getUnreservedContiguousIdBlock;

@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = {TestMonotonicDatabaseServiceTestConfiguration.class})
public class BasicMonotonicAccessioningWithAlternateRangesTest {

    private static final String INSTANCE_ID = "test-instance";

    @Autowired
    private BasicSpringDataRepositoryMonotonicDatabaseService<TestModel, TestMonotonicEntity> databaseService;

    @Autowired
    private ContiguousIdBlockService contiguousIdBlockService;

    @Test(expected = BlockInitializationException.class)
    public void testUnknownCategory() throws AccessionCouldNotBeGeneratedException {
        List<GetOrCreateAccessionWrapper<TestModel, String, Long>> evaAccessions =
                getAccessioningService("unknown-category", INSTANCE_ID)
                        .getOrCreate(getObjectsForAccessionsInRange(1, 10));
    }

    @Test
    public void testRecoverState() {
        String categoryId = "eva_2";
        String instanceId2 = "test-instance_2";

        // create 3 un-complete contiguous id blocks of size 10
        // block-1 : (100 to 109), block-2 : (110 to 119), block-3 : (120 to 129)
        List<ContiguousIdBlock> uncompletedBlocks = new ArrayList<>();
        uncompletedBlocks.add(getUnreservedContiguousIdBlock(categoryId, instanceId2, 100, 10));
        uncompletedBlocks.add(getUnreservedContiguousIdBlock(categoryId, instanceId2, 110, 10));
        uncompletedBlocks.add(getUnreservedContiguousIdBlock(categoryId, instanceId2, 120, 10));
        contiguousIdBlockService.save(uncompletedBlocks);

        assertEquals(3, getUncompletedBlocksByCategoryIdAndApplicationInstanceIdOrderByEndAsc(contiguousIdBlockService.getRepository(), categoryId, instanceId2).size());

        // create and save accessions in db (100 to 124) - save 2 sets of same accessions with different hashes
        List<AccessionWrapper<TestModel, String, Long>> accessionsSet1 = LongStream.range(100l, 125l)
                .boxed()
                .map(longAcc -> new AccessionWrapper<>(longAcc, "hash-1" + longAcc, TestModel.of("test-obj-1-" + longAcc)))
                .collect(Collectors.toList());
        List<AccessionWrapper<TestModel, String, Long>> accessionsSet2 = LongStream.range(100l, 125l)
                .boxed()
                .map(longAcc -> new AccessionWrapper<>(longAcc, "hash-2" + longAcc, TestModel.of("test-obj-2-" + longAcc)))
                .collect(Collectors.toList());
        databaseService.save(accessionsSet1);
        databaseService.save(accessionsSet2);

        // run recover state
        MonotonicAccessionGenerator generator = getGenerator(categoryId, instanceId2);

        // As we have already saved accessions in db from 100 to 124, the status should be
        // block-1 (100 to 109) : fully complete
        // block-2 (110 to 119) : fully complete
        // block-3 (120 to 124) : partially complete
        assertEquals(1, getUncompletedBlocksByCategoryIdAndApplicationInstanceIdOrderByEndAsc(contiguousIdBlockService.getRepository(), categoryId, instanceId2).size());
        ContiguousIdBlock uncompletedBlock = getUncompletedBlocksByCategoryIdAndApplicationInstanceIdOrderByEndAsc(contiguousIdBlockService.getRepository(), categoryId, instanceId2).get(0);
        assertEquals(120l, uncompletedBlock.getFirstValue());
        assertEquals(129l, uncompletedBlock.getLastValue());
        assertEquals(124l, uncompletedBlock.getLastCommitted());

        MonotonicRangePriorityQueue availableRanges = generator.getAvailableRanges();
        assertEquals(1, availableRanges.size());
        assertEquals(125l, availableRanges.peek().getStart());
        assertEquals(129l, availableRanges.peek().getEnd());
    }

    @Test
    public void testAlternateRangesWithDifferentGenerators() throws AccessionCouldNotBeGeneratedException {
        /* blockStartValue= 0, blockSize= 10 , nextBlockInterval= 20
          the new blocks are interleaved or jumped for each 20 items accessioned
          so the accesions will be in the range of 0-19,40-59,80-99 */
        String categoryId = "eva_2";
        String instanceId2 = "test-instance_2";
        BasicAccessioningService accService1 = getAccessioningService(categoryId, INSTANCE_ID);
        List<GetOrCreateAccessionWrapper<TestModel, String, Long>> evaAccessions = accService1.getOrCreate(getObjectsForAccessionsInRange(1, 9));
        assertEquals(9, evaAccessions.size());
        assertEquals(0, evaAccessions.get(0).getAccession().longValue());
        assertEquals(8, evaAccessions.get(8).getAccession().longValue());
        //BlockSize of 10 was reserved but only 9 elements have been accessioned
        assertEquals(1, getUncompletedBlocksByCategoryIdAndApplicationInstanceIdOrderByEndAsc(contiguousIdBlockService.getRepository(), categoryId, INSTANCE_ID)
                .size());
        accService1.shutDownAccessioning();

        //Get another service for same category
        BasicAccessioningService accService2 = getAccessioningService(categoryId, INSTANCE_ID);
        evaAccessions = accService2.getOrCreate(getObjectsForAccessionsInRange(11, 30));
        assertEquals(20, evaAccessions.size());
        //Previous block ended here as only 9 elements were accessioned out of a blocksize of 10
        assertEquals(9, evaAccessions.get(0).getAccession().longValue());

        //New Block still not interleaved or jumped as the interleave point is 20
        assertEquals(10, evaAccessions.get(1).getAccession().longValue());
        assertEquals(19, evaAccessions.get(10).getAccession().longValue());

        //New Block interleaved as it reached interleave point 20 so jumped 20 places to 40
        assertEquals(40, evaAccessions.get(11).getAccession().longValue());
        assertEquals(48, evaAccessions.get(19).getAccession().longValue());
        //BlockSize if 10 was reserved but only 9 elements have been accessioned
        assertEquals(1, getUncompletedBlocksByCategoryIdAndApplicationInstanceIdOrderByEndAsc(contiguousIdBlockService.getRepository(), categoryId, INSTANCE_ID).size());
        accService2.shutDownAccessioning();

        //Get another service for same category but different Instance
        BasicAccessioningService accService3 = getAccessioningService(categoryId, instanceId2);
        evaAccessions = accService3.getOrCreate(getObjectsForAccessionsInRange(31, 39));
        assertEquals(9, evaAccessions.size());
        //New Block from different instance have not jumped as still blocks are available before interleaving point
        assertNotEquals(80, evaAccessions.get(0).getAccession().longValue());
        assertEquals(50, evaAccessions.get(0).getAccession().longValue());
        assertEquals(58, evaAccessions.get(8).getAccession().longValue());
        assertEquals(1, getUncompletedBlocksByCategoryIdAndApplicationInstanceIdOrderByEndAsc(contiguousIdBlockService.getRepository(), categoryId, instanceId2).size());
        accService3.shutDownAccessioning();

        //Get previous uncompleted service from instance1 and create accessions
        BasicAccessioningService accService4 = getAccessioningService(categoryId, INSTANCE_ID);
        evaAccessions = accService4.getOrCreate(getObjectsForAccessionsInRange(40, 41));
        assertEquals(2, evaAccessions.size());
        assertEquals(49, evaAccessions.get(0).getAccession().longValue());  //Block ended here
        //New Block with 20 interval from last block made in instanceId2
        assertEquals(80, evaAccessions.get(1).getAccession().longValue());
    }

    @Test
    public void testInitializeBlockManagerInMonotonicAccessionGenerator() {
        String categoryId = "eva_2";
        String instanceId2 = "test-instance_2";
        ContiguousIdBlockRepository repository = contiguousIdBlockService.getRepository();

        ContiguousIdBlock block = getUnreservedContiguousIdBlock(categoryId, instanceId2, 0, 10);
        repository.save(block);

        // assert block is not full and not reserved
        List<ContiguousIdBlock> blockInDBList = repository
                .findAllByCategoryIdAndApplicationInstanceIdOrderByLastValueAsc(categoryId, instanceId2)
                .collect(Collectors.toList());
        assertEquals(1, blockInDBList.size());
        List<ContiguousIdBlock> unreservedAndNotFullBlocks = blockInDBList.stream()
                .filter(b -> b.isNotFull() && b.isNotReserved())
                .collect(Collectors.toList());
        assertEquals(1, unreservedAndNotFullBlocks.size());
        assertEquals(9, unreservedAndNotFullBlocks.get(0).getLastValue());
        assertEquals(-1, unreservedAndNotFullBlocks.get(0).getLastCommitted());
        assertEquals(Boolean.FALSE, unreservedAndNotFullBlocks.get(0).isReserved());

        // this will run the recover state
        BasicAccessioningService accService = getAccessioningService(categoryId, instanceId2);

        // assert block gets reserved after recover state
        blockInDBList = repository
                .findAllByCategoryIdAndApplicationInstanceIdOrderByLastValueAsc(categoryId, instanceId2)
                .collect(Collectors.toList());
        assertEquals(1, blockInDBList.size());
        unreservedAndNotFullBlocks = blockInDBList.stream()
                .filter(b -> b.isNotFull() && b.isNotReserved())
                .collect(Collectors.toList());
        assertEquals(0, unreservedAndNotFullBlocks.size());
        List<ContiguousIdBlock> reservedAndNotFullBlocks = blockInDBList.stream()
                .filter(b -> b.isNotFull() && b.isReserved())
                .collect(Collectors.toList());
        assertEquals(1, reservedAndNotFullBlocks.size());
        assertEquals(9, reservedAndNotFullBlocks.get(0).getLastValue());
        assertEquals(-1, reservedAndNotFullBlocks.get(0).getLastCommitted());
        assertEquals(Boolean.TRUE, reservedAndNotFullBlocks.get(0).isReserved());
    }

    private List<TestModel> getObjectsForAccessionsInRange(int startRange, int endRange) {
        return IntStream.range(startRange, endRange + 1).mapToObj(i -> TestModel.of("Test-" + i)).collect(Collectors
                .toList());
    }

    private BasicAccessioningService<TestModel, String, Long> getAccessioningService(String categoryId,
                                                                                String instanceId) {
        return new BasicAccessioningService<>(
                getGenerator(categoryId, instanceId),
                databaseService,
                TestModel::getValue,
                new SHA1HashingFunction()
        );
    }

    private MonotonicAccessionGenerator<TestModel> getGenerator(String categoryId, String instanceId) {
        return new MonotonicAccessionGenerator<>(categoryId, instanceId, contiguousIdBlockService, databaseService);
    }
}

