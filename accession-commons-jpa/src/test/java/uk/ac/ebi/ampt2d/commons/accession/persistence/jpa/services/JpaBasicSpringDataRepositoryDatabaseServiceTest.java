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
package uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.services;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.TestTransaction;
import uk.ac.ebi.ampt2d.commons.accession.core.DatabaseService;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionCouldNotBeGeneratedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionDeprecatedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionDoesNotExistException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionMergedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.HashAlreadyExistsException;
import uk.ac.ebi.ampt2d.commons.accession.core.models.AccessionVersionsWrapper;
import uk.ac.ebi.ampt2d.commons.accession.core.models.AccessionWrapper;
import uk.ac.ebi.ampt2d.commons.accession.core.models.EventType;
import uk.ac.ebi.ampt2d.commons.accession.core.models.SaveResponse;
import uk.ac.ebi.ampt2d.test.configuration.TestJpaDatabaseServiceTestConfiguration;
import uk.ac.ebi.ampt2d.test.models.TestModel;
import uk.ac.ebi.ampt2d.test.persistence.TestEntity;
import uk.ac.ebi.ampt2d.test.persistence.TestInactiveAccessionEntity;
import uk.ac.ebi.ampt2d.test.persistence.TestInactiveAccessionRepository;
import uk.ac.ebi.ampt2d.test.persistence.TestRepository;
import uk.ac.ebi.ampt2d.test.persistence.TestStringHistoryRepository;
import uk.ac.ebi.ampt2d.test.persistence.TestStringOperationEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = {TestJpaDatabaseServiceTestConfiguration.class})
public class JpaBasicSpringDataRepositoryDatabaseServiceTest {

    public static final AccessionWrapper<TestModel, String, String> TEST_MODEL_1 =
            new AccessionWrapper("a1", "h1", TestModel.of("something1"));
    public static final AccessionWrapper<TestModel, String, String> TEST_MODEL_2 =
            new AccessionWrapper("a2", "h2", TestModel.of("something2"));
    public static final AccessionWrapper<TestModel, String, String> TEST_MODEL_3 =
            new AccessionWrapper("a3", "h3", TestModel.of("something3"));

    @Autowired
    private DatabaseService<TestModel, String, String> service;

    @Autowired
    private TestStringHistoryRepository historyRepository;

    @Autowired
    private TestInactiveAccessionRepository inactiveRepository;

    @Autowired
    private TestRepository repository;

    @Test
    public void testFindInEmptyRepository() {
        assertEquals(0, repository.findByAccession("a1").size());
        assertEquals(0, service.findAllByHash(Arrays.asList("h1")).size());
    }

    @Test
    public void saveUniqueElementsAndFindByAccessionReturnsEachAccession() throws Exception {
        service.save(Arrays.asList(TEST_MODEL_1, TEST_MODEL_2, TEST_MODEL_3));

        List<AccessionWrapper<TestModel, String, String>> result = new ArrayList<>();
        result.add(service.findLastVersionByAccession("a1"));
        result.add(service.findLastVersionByAccession("a2"));
        result.add(service.findLastVersionByAccession("a3"));
        assertEquals(3, result.size());
        assertEquals("something1", result.get(0).getData().getValue());
        assertEquals("something2", result.get(1).getData().getValue());
        assertEquals("something3", result.get(2).getData().getValue());
    }

    @Test(expected = AccessionDoesNotExistException.class)
    public void saveUniqueElementsAndFindByAccessionThatDoesNotExistThrowsException() throws Exception {
        service.save(Arrays.asList(TEST_MODEL_1, TEST_MODEL_2, TEST_MODEL_3));
        service.findLastVersionByAccession("doesnotexist");
    }

    @Test
    public void saveUniqueElementsAndFindByHashReturnsEachAccession() {
        service.save(Arrays.asList(TEST_MODEL_1, TEST_MODEL_2, TEST_MODEL_3));

        List<AccessionWrapper<TestModel, String, String>> results = service.findAllByHash(
                Arrays.asList("h1", "h2"));
        assertEquals(2, results.size());
    }

    @Test
    public void saveUniqueElementsAndFindByHashThatDoesNotExistReturnsNothing() {
        service.save(Arrays.asList(TEST_MODEL_1, TEST_MODEL_2, TEST_MODEL_3));

        List<AccessionWrapper<TestModel, String, String>> results = service.findAllByHash(
                Arrays.asList("h0"));
        assertEquals(0, results.size());
    }

    @Test
    public void testUnsaveIfExistPreviousWithSameHash() throws AccessionCouldNotBeGeneratedException {
        TestTransaction.flagForCommit();
        service.save(Arrays.asList(new AccessionWrapper("a0", "h1", TestModel.of("something1"))));
        TestTransaction.end();

        SaveResponse<String> accessions = service.save(Arrays.asList(TEST_MODEL_1, TEST_MODEL_2, TEST_MODEL_3));
        assertEquals(2, accessions.getSavedAccessions().size());
        assertEquals(1, accessions.getSaveFailedAccessions().size());

        TestTransaction.start();
        TestTransaction.flagForCommit();
        repository.deleteById("h1");
        repository.deleteById("h2");
        repository.deleteById("h3");
        TestTransaction.end();
    }

    @Test
    public void testCompleteSaveIfSameAccessionDifferentHash() throws AccessionCouldNotBeGeneratedException {
        TestTransaction.flagForCommit();
        service.save(Arrays.asList(new AccessionWrapper("a1", "h0", TestModel.of("something1"))));
        TestTransaction.end();

        SaveResponse<String> accessions = service.save(Arrays.asList(TEST_MODEL_1, TEST_MODEL_2, TEST_MODEL_3));

        assertEquals(3, accessions.getSavedAccessions().size());
        assertEquals(0, accessions.getSaveFailedAccessions().size());
        Optional<TestEntity> document = repository.findById("h0");
        assertTrue(document.isPresent());
        assertEquals("a1", document.get().getAccession());

        document = repository.findById("h1");
        assertTrue(document.isPresent());
        assertEquals("a1", document.get().getAccession());

        TestTransaction.start();
        TestTransaction.flagForCommit();
        repository.deleteById("h0");
        repository.deleteById("h1");
        repository.deleteById("h2");
        repository.deleteById("h3");
        TestTransaction.end();
    }

    @Test(expected = AccessionDoesNotExistException.class)
    public void updateWithoutExistingAccessionFails() throws AccessionDoesNotExistException,
            HashAlreadyExistsException, AccessionDeprecatedException, AccessionMergedException {
        service.update("a2", "h1", TestModel.of("something2"), 1);
    }

    @Test(expected = HashAlreadyExistsException.class)
    public void updateWithExistingObjectFails() throws AccessionDoesNotExistException,
            HashAlreadyExistsException, AccessionDeprecatedException, AccessionMergedException {
        service.save(Arrays.asList(new AccessionWrapper<>("a2", "h1", TestModel.of("something2"))));
        service.update("a2", "h1", TestModel.of("something2"), 1);
    }

    @Test
    public void updateDoesNotCreateNewVersion() throws AccessionDoesNotExistException,
            HashAlreadyExistsException, AccessionDeprecatedException, AccessionMergedException {
        service.save(Arrays.asList(new AccessionWrapper<>("a2", "h1", TestModel.of("something2"))));
        final AccessionVersionsWrapper<TestModel, String, String> update =
                service.update("a2", "h2", TestModel.of("something2b"), 1);
        assertEquals(1, update.getModelWrappers().size());
        assertEquals(1, update.getModelWrappers().get(0).getVersion());
    }

    @Test
    public void patchCreatesNewVersion() throws AccessionDoesNotExistException,
            HashAlreadyExistsException, AccessionDeprecatedException, AccessionMergedException {
        service.save(Arrays.asList(new AccessionWrapper<>("a2", "h1", TestModel.of("something2"))));
        final AccessionVersionsWrapper<TestModel, String, String> patch =
                service.patch("a2", "h2", TestModel.of("something2b"), "patch");
        assertEquals(2, patch.getModelWrappers().size());
        assertEquals(1, patch.getModelWrappers().get(0).getVersion());
        assertEquals(2, patch.getModelWrappers().get(1).getVersion());
    }

    @Test
    public void findAllAccessionReturnsOnlyLastPatch() throws AccessionDoesNotExistException,
            HashAlreadyExistsException, AccessionDeprecatedException, AccessionMergedException {
        service.save(Arrays.asList(new AccessionWrapper<>("a2", "h1", TestModel.of("something2"))));
        service.patch("a2", "h2", TestModel.of("something2b"), "patch");
        assertEquals(2, service.findLastVersionByAccession("a2").getVersion());
    }

    @Test
    public void findAccessionShowsAllPatches() throws AccessionDoesNotExistException,
            HashAlreadyExistsException, AccessionDeprecatedException, AccessionMergedException {
        service.save(Arrays.asList(new AccessionWrapper<>("a2", "h1", TestModel.of("something2"))));
        service.patch("a2", "h2", TestModel.of("something2b"), "patch");
        assertEquals(2, service.findByAccession("a2").getModelWrappers().size());
    }

    @Test
    public void findAccessionVersion() throws AccessionDoesNotExistException,
            HashAlreadyExistsException, AccessionDeprecatedException, AccessionMergedException {
        service.save(Arrays.asList(new AccessionWrapper<>("a2", "h1", TestModel.of("something2"))));
        service.patch("a2", "h2", TestModel.of("something2b"), "patch");
        assertEquals(1, service.findByAccessionVersion("a2", 1).getVersion());
        assertEquals(2, service.findByAccessionVersion("a2", 2).getVersion());
    }

    @Test
    public void multipleUpdates() throws AccessionDoesNotExistException,
            HashAlreadyExistsException, AccessionDeprecatedException, AccessionMergedException {
        service.save(Arrays.asList(new AccessionWrapper<>("a2", "h1", TestModel.of("something2"))));
        AccessionWrapper<TestModel, String, String> accession = service.findLastVersionByAccession("a2");
        assertEquals("something2", accession.getData().getValue());
        assertEquals(1, accession.getVersion());

        service.update("a2", "h2", TestModel.of("something2b"), 1);
        service.update("a2", "h3", TestModel.of("something2c"), 1);

        AccessionWrapper<TestModel, String, String> updatedAccession = service.findLastVersionByAccession("a2");
        assertEquals("something2c", updatedAccession.getData().getValue());
        assertEquals(1, updatedAccession.getVersion());
    }

    @Test
    public void multiplePatches() throws AccessionDoesNotExistException,
            HashAlreadyExistsException, AccessionDeprecatedException, AccessionMergedException {
        service.save(Arrays.asList(new AccessionWrapper<>("a2", "h1", TestModel.of("something2"))));
        service.patch("a2", "h2", TestModel.of("something2b"), "patch");
        service.patch("a2", "h3", TestModel.of("something2c"), "patch");

        assertEquals(1, service.findByAccessionVersion("a2", 1).getVersion());
        assertEquals(2, service.findByAccessionVersion("a2", 2).getVersion());
        assertEquals(3, service.findByAccessionVersion("a2", 3).getVersion());

        assertEquals(3, service.findByAccession("a2").getModelWrappers().size());
    }

    @Test
    public void multiplePatchesFindAllReturnsLastVersion() throws AccessionDoesNotExistException,
            HashAlreadyExistsException, AccessionDeprecatedException, AccessionMergedException {
        service.save(Arrays.asList(new AccessionWrapper<>("a2", "h1", TestModel.of("something2"))));
        service.patch("a2", "h2", TestModel.of("something2b"), "patch");
        service.patch("a2", "h3", TestModel.of("something2c"), "patch");

        AccessionWrapper<TestModel, String, String> accession = service.findLastVersionByAccession("a2");
        assertEquals(3, accession.getVersion());
        assertEquals("something2c", accession.getData().getValue());
    }

    @Test
    public void testFindByAccessionAndVersion() throws AccessionDoesNotExistException,
            HashAlreadyExistsException, AccessionMergedException, AccessionDeprecatedException {
        service.save(Arrays.asList(
                new AccessionWrapper("a2", "h1", TestModel.of("something2"), 1),
                new AccessionWrapper("a2", "h2", TestModel.of("something2b"), 2)));

        AccessionWrapper<TestModel, String, String> accessionOfVersion1 = service.findByAccessionVersion("a2", 1);
        assertEquals(1, accessionOfVersion1.getVersion());
        AccessionWrapper<TestModel, String, String> accessionsOfVersion2 = service.findByAccessionVersion("a2", 2);
        assertEquals(2, accessionsOfVersion2.getVersion());
    }

    @Test(expected = AccessionDoesNotExistException.class)
    public void testDeprecateNotExisting() throws AccessionDoesNotExistException, AccessionDeprecatedException, AccessionMergedException {
        assertEquals(0, repository.findByAccession("a1").size());
        service.deprecate("a1", "reasons");
    }

    @Test
    public void testDeprecateOneVersion() throws AccessionDoesNotExistException, AccessionDeprecatedException, AccessionMergedException {
        service.save(Arrays.asList(new AccessionWrapper("a1", "h1", TestModel.of("something2"), 1)));
        assertEquals("something2", service.findLastVersionByAccession("a1").getData().getValue());
        service.deprecate("a1", "reasons");

        final TestStringOperationEntity entity = historyRepository.findAll().iterator().next();
        assertEquals(EventType.DEPRECATED, entity.getEventType());
        assertEquals("a1", entity.getAccession());
        assertEquals("reasons", entity.getReason());

        final List<TestInactiveAccessionEntity> deprecated = inactiveRepository.findAllByHistoryId(entity.getId());
        assertEquals(1, deprecated.size());
        assertEquals("something2", deprecated.get(0).getValue());

        assertThrows(AccessionDeprecatedException.class, () -> service.findLastVersionByAccession("a1"));
    }

    @Test
    public void testDeprecateMultipleVersion() throws AccessionDoesNotExistException, AccessionMergedException, AccessionDeprecatedException, HashAlreadyExistsException {
        service.save(Arrays.asList(new AccessionWrapper("a1", "h1", TestModel.of("something2"), 1)));
        service.patch("a1", "h2", TestModel.of("something2b"), "patch");
        assertEquals(2, service.findLastVersionByAccession("a1").getVersion());
        service.deprecate("a1", "reasons");

        final TestStringOperationEntity entity = historyRepository.findTopByAccessionOrderByCreatedDateDesc("a1");
        assertEquals(EventType.DEPRECATED, entity.getEventType());
        assertEquals("a1", entity.getAccession());
        assertEquals("reasons", entity.getReason());

        final List<TestInactiveAccessionEntity> deprecated = inactiveRepository.findAllByHistoryId(entity.getId());
        assertEquals(2, deprecated.size());

        assertThrows(AccessionDeprecatedException.class, () -> service.findLastVersionByAccession("a1"));
    }

    @Test
    public void testDeprecateAndAccessionSameObjectMultipleTimes() throws AccessionDoesNotExistException,
            AccessionDeprecatedException, AccessionMergedException {
        service.save(Arrays.asList(new AccessionWrapper("a1", "h1", TestModel.of("something2"), 1)));
        assertEquals(1, repository.findByAccession("a1").size());
        service.deprecate("a1", "reasons");
        assertEquals(0, repository.findByAccession("a1").size());
        service.save(Arrays.asList(new AccessionWrapper("a1", "h1", TestModel.of("something2"), 1)));
        assertEquals(1, repository.findByAccession("a1").size());
        service.deprecate("a1", "reasons");
        assertEquals(0, repository.findByAccession("a1").size());

        assertEquals(2, historyRepository.count());
    }

    @Test
    public void testMerge() throws AccessionDoesNotExistException, AccessionDeprecatedException,
            AccessionMergedException {
        service.save(Arrays.asList(new AccessionWrapper("a1", "h1", TestModel.of("something1"), 1)));
        service.save(Arrays.asList(new AccessionWrapper("a2", "h2", TestModel.of("something2"), 1)));
        assertEquals(2, repository.count());
        assertEquals(0, historyRepository.count());

        service.merge("a1", "a2", "reasons");
        assertEquals(1, repository.count());
        assertEquals(1, historyRepository.count());

        AccessionMergedException exception = assertThrows(AccessionMergedException.class,
                                                          () -> service.findLastVersionByAccession("a1"));
        assertEquals("a1 has been already merged into a2", exception.getMessage());
    }

    @Test(expected = AccessionDoesNotExistException.class)
    public void testMergeAccessionDoesNotExistOrigin() throws AccessionDoesNotExistException,
            AccessionDeprecatedException, AccessionMergedException {
        service.save(Arrays.asList(new AccessionWrapper("a1", "h1", TestModel.of("something1"), 1)));
        assertNotNull(service.findByAccessionVersion("a1", 1));

        service.merge("doesnotexist", "a1", "reasons");
    }

    @Test(expected = AccessionDoesNotExistException.class)
    public void testMergeAccessionDoesNotExistDestination() throws AccessionDoesNotExistException,
            AccessionDeprecatedException, AccessionMergedException {
        service.save(Arrays.asList(new AccessionWrapper("a1", "h1", TestModel.of("something1"), 1)));
        assertNotNull(service.findByAccessionVersion("a1", 1));

        service.merge("a1", "doesnotexist", "reasons");
    }

    @Test(expected = AccessionDeprecatedException.class)
    public void testMergeAccessionDeprecatedOrigin() throws AccessionDoesNotExistException,
            AccessionDeprecatedException, AccessionMergedException {
        service.save(Arrays.asList(new AccessionWrapper("a1", "h1", TestModel.of("something1"), 1)));
        service.save(Arrays.asList(new AccessionWrapper("a2", "h2", TestModel.of("something2"), 1)));
        assertNotNull(service.findByAccessionVersion("a1", 1));
        assertNotNull(service.findByAccessionVersion("a2", 1));
        service.deprecate("a1", "blah");

        service.merge("a1", "a2", "reasons");
    }

    @Test(expected = AccessionDeprecatedException.class)
    public void testMergeAccessionDeprecatedDestination() throws AccessionDoesNotExistException,
            AccessionDeprecatedException, AccessionMergedException {
        service.save(Arrays.asList(new AccessionWrapper("a1", "h1", TestModel.of("something1"), 1)));
        service.save(Arrays.asList(new AccessionWrapper("a2", "h2", TestModel.of("something2"), 1)));
        assertNotNull(service.findByAccessionVersion("a1", 1));
        assertNotNull(service.findByAccessionVersion("a2", 1));
        service.deprecate("a2", "blah");

        service.merge("a1", "a2", "reasons");
    }

}