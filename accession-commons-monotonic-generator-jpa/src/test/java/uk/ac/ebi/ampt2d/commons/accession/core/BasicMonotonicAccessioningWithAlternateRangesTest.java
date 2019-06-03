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
import uk.ac.ebi.ampt2d.commons.accession.generators.monotonic.MonotonicAccessionGenerator;
import uk.ac.ebi.ampt2d.commons.accession.hashing.SHA1HashingFunction;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.service.ContiguousIdBlockService;
import uk.ac.ebi.ampt2d.commons.accession.service.BasicSpringDataRepositoryMonotonicDatabaseService;
import uk.ac.ebi.ampt2d.test.configuration.TestMonotonicDatabaseServiceTestConfiguration;
import uk.ac.ebi.ampt2d.test.models.TestModel;
import uk.ac.ebi.ampt2d.test.persistence.TestMonotonicEntity;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

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
        List<AccessionWrapper<TestModel, String, Long>> evaAccessions =
                getAccessioningService("unknown-category", INSTANCE_ID)
                        .getOrCreate(getObjectsForAccessionsInRange(1, 10));
    }

    @Test
    public void testAlternateRangesWithDifferentGenerators() throws AccessionCouldNotBeGeneratedException {
        /* blockStartValue= 0, blockSize= 10 , nextBlockInterval= 20
          the new blocks are interleaved or jumped for each 20 items accessioned
          so the accesions will be in the range of 0-19,40-59,80-99 */
        String categoryId = "eva_2";
        String instanceId2 = "test-instance_2";
        List<AccessionWrapper<TestModel, String, Long>> evaAccessions = getAccessioningService(categoryId, INSTANCE_ID)
                .getOrCreate(getObjectsForAccessionsInRange(1, 9));
        assertEquals(9, evaAccessions.size());
        assertEquals(0, evaAccessions.get(0).getAccession().longValue());
        assertEquals(8, evaAccessions.get(8).getAccession().longValue());
        //BlockSize of 10 was reserved but only 9 elements have been accessioned
        assertEquals(1, contiguousIdBlockService
                .getUncompletedBlocksByCategoryIdAndApplicationInstanceIdOrderByEndAsc(categoryId, INSTANCE_ID)
                .size());

        //Get another service for same category
        evaAccessions = getAccessioningService(categoryId, INSTANCE_ID)
                .getOrCreate(getObjectsForAccessionsInRange(11, 30));
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
        assertEquals(1, contiguousIdBlockService.getUncompletedBlocksByCategoryIdAndApplicationInstanceIdOrderByEndAsc
                (categoryId, INSTANCE_ID).size());

        //Get another service for same category but different Instance
        evaAccessions = getAccessioningService(categoryId, instanceId2)
                .getOrCreate(getObjectsForAccessionsInRange(31, 39));
        assertEquals(9, evaAccessions.size());
        //New Block from different instance have not jumped as still blocks are available before interleaving point
        assertNotEquals(80, evaAccessions.get(0).getAccession().longValue());
        assertEquals(50, evaAccessions.get(0).getAccession().longValue());
        assertEquals(58, evaAccessions.get(8).getAccession().longValue());
        assertEquals(1, contiguousIdBlockService
                .getUncompletedBlocksByCategoryIdAndApplicationInstanceIdOrderByEndAsc(categoryId, instanceId2).size());

        //Get previous uncompleted service from instance1 and create accessions
        evaAccessions = getAccessioningService(categoryId, INSTANCE_ID)
                .getOrCreate(getObjectsForAccessionsInRange(40, 41));
        assertEquals(2, evaAccessions.size());
        assertEquals(49, evaAccessions.get(0).getAccession().longValue());  //Block ended here
        //New Block with 20 interval from last block made in instanceId2
        assertEquals(80, evaAccessions.get(1).getAccession().longValue());
    }

    private List<TestModel> getObjectsForAccessionsInRange(int startRange, int endRange) {
        return IntStream.range(startRange, endRange + 1).mapToObj(i -> TestModel.of("Test-" + i)).collect(Collectors
                .toList());
    }

    private AccessioningService<TestModel, String, Long> getAccessioningService(String categoryId,
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

