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
import org.springframework.test.context.transaction.TestTransaction;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionCouldNotBeGeneratedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionDeprecatedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionDoesNotExistException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionMergedException;
import uk.ac.ebi.ampt2d.commons.accession.core.models.AccessionWrapper;
import uk.ac.ebi.ampt2d.commons.accession.core.models.GetOrCreateAccessionWrapper;
import uk.ac.ebi.ampt2d.commons.accession.generators.monotonic.MonotonicAccessionGenerator;
import uk.ac.ebi.ampt2d.commons.accession.hashing.SHA1HashingFunction;
import uk.ac.ebi.ampt2d.test.configuration.TestMonotonicDatabaseServiceTestConfiguration;
import uk.ac.ebi.ampt2d.test.models.TestModel;
import uk.ac.ebi.ampt2d.test.persistence.TestMonotonicRepository;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = {TestMonotonicDatabaseServiceTestConfiguration.class})
public class BasicMonotonicAccessioningTestWithPreFiltering {

    private static String APPLICATION_INSTANCE_ID = "TEST_APPPLICATION_INSTANCE_ID";

    @Autowired
    private TestMonotonicRepository repository;

    @Autowired
    private DatabaseService<TestModel, String, Long> databaseService;

    @Autowired
    private MonotonicAccessionGenerator<TestModel> monotonicAccessionGenerator;

    @Test
    public void testCreateAccessions() throws AccessionCouldNotBeGeneratedException {
        AccessioningService<TestModel, String, Long> accessioningService = getAccessioningService();

        List<GetOrCreateAccessionWrapper<TestModel, String, Long>> accessions = accessioningService.getOrCreate(
                Arrays.asList(
                        TestModel.of("service-test-1"),
                        TestModel.of("service-test-2"),
                        TestModel.of("service-test-3")
                ), APPLICATION_INSTANCE_ID);
        assertEquals(3, accessions.size());
    }

    private AccessioningService<TestModel, String, Long> getAccessioningService() {
        return new BasicAccessioningService<>(
                monotonicAccessionGenerator,
                databaseService,
                TestModel::getValue,
                new SHA1HashingFunction(),
                AccessionSaveMode.PREFILTER_EXISTING
        );
    }

    @Test
    public void testGetOrCreateFiltersRepeated() throws AccessionCouldNotBeGeneratedException {

        AccessioningService<TestModel, String, Long> accessioningService = getAccessioningService();

        List<GetOrCreateAccessionWrapper<TestModel, String, Long>> accessions = accessioningService.getOrCreate(
                Arrays.asList(
                        TestModel.of("service-test-1"),
                        TestModel.of("service-test-2"),
                        TestModel.of("service-test-2"),
                        TestModel.of("service-test-3")
                ), APPLICATION_INSTANCE_ID);
        assertEquals(3, accessions.size());
    }

    @Test
    public void testGetAccessions() throws AccessionCouldNotBeGeneratedException {
        AccessioningService<TestModel, String, Long> accessioningService = getAccessioningService();

        List<AccessionWrapper<TestModel, String, Long>> accessions = accessioningService.get(
                Arrays.asList(
                        TestModel.of("service-test-1"),
                        TestModel.of("service-test-2"),
                        TestModel.of("service-test-3")
                ));
        assertEquals(0, accessions.size());
    }

    @Test
    public void testGetWithExistingEntries() throws AccessionCouldNotBeGeneratedException {
        AccessioningService<TestModel, String, Long> accessioningService = getAccessioningService();

        List<GetOrCreateAccessionWrapper<TestModel, String, Long>> accessions1 = accessioningService.getOrCreate(
                Arrays.asList(
                        TestModel.of("service-test-3")
                ), APPLICATION_INSTANCE_ID);
        assertEquals(1, accessions1.size());

        List<AccessionWrapper<TestModel, String, Long>> accessions2 = accessioningService.get(
                Arrays.asList(
                        TestModel.of("service-test-1"),
                        TestModel.of("service-test-2"),
                        TestModel.of("service-test-3")
                ));
        assertEquals(1, accessions2.size());
        assertEquals("service-test-3", accessions2.get(0).getData().getValue());
    }

    @Test
    public void testGetByAccessionsWithExistingEntries() throws AccessionCouldNotBeGeneratedException,
            AccessionDoesNotExistException, AccessionMergedException, AccessionDeprecatedException {
        AccessioningService<TestModel, String, Long> accessioningService = getAccessioningService();

        List<GetOrCreateAccessionWrapper<TestModel, String, Long>> accessions1 = accessioningService.getOrCreate(
                Arrays.asList(
                        TestModel.of("service-test-3")
                ), APPLICATION_INSTANCE_ID);
        assertEquals(1, accessions1.size());

        AccessionWrapper<TestModel, String, Long> accession2 =
                accessioningService.getByAccession(accessions1.get(0).getAccession());
        assertEquals(accessions1.get(0).getAccession(), accession2.getAccession());
    }

    @Test
    public void testGetOrCreateWithExistingEntries() throws AccessionCouldNotBeGeneratedException {
        AccessioningService<TestModel, String, Long> accessioningService = getAccessioningService();
        TestTransaction.flagForCommit();
        List<GetOrCreateAccessionWrapper<TestModel, String, Long>> accessions1 = accessioningService.getOrCreate(
                Arrays.asList(
                        TestModel.of("service-test-3")
                ), APPLICATION_INSTANCE_ID);
        assertEquals(1, accessions1.size());
        assertEquals(true, accessions1.get(0).isNewAccession());
        TestTransaction.end();

        List<GetOrCreateAccessionWrapper<TestModel, String, Long>> accessions2 = accessioningService.getOrCreate(
                Arrays.asList(
                        TestModel.of("service-test-1"),
                        TestModel.of("service-test-2"),
                        TestModel.of("service-test-3")
                ), APPLICATION_INSTANCE_ID);
        assertEquals(3, accessions2.size());
        accessions2.stream().forEach(wrapper -> {
            if (!wrapper.isNewAccession()) {
                assertEquals("service-test-3",wrapper.getData().getValue());
            }
        });
        assertEquals(2,accessions2.stream().filter(GetOrCreateAccessionWrapper::isNewAccession).count());

        TestTransaction.start();
        for (AccessionWrapper<TestModel, String, Long> accession : accessions2) {
            repository.deleteById(accession.getHash());
        }
        TestTransaction.flagForCommit();
        TestTransaction.end();
    }

}
