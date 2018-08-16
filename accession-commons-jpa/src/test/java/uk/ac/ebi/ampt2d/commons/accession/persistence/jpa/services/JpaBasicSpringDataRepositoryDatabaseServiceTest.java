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
import uk.ac.ebi.ampt2d.test.persistence.TestInactiveAccessionEntity;
import uk.ac.ebi.ampt2d.test.persistence.TestInactiveAccessionRepository;
import uk.ac.ebi.ampt2d.test.persistence.TestRepository;
import uk.ac.ebi.ampt2d.test.persistence.TestStringHistoryRepository;
import uk.ac.ebi.ampt2d.test.persistence.TestStringOperationEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
        assertEquals(0, service.findAllByAccession(Arrays.asList("a1", "a2")).size());
        assertEquals(0, service.findAllByHash(Arrays.asList("h1", "h2")).size());
    }

    @Test
    public void saveUniqueElementsAndFindByAccessionReturnsEachAccession() {
        service.save(Arrays.asList(TEST_MODEL_1, TEST_MODEL_2, TEST_MODEL_3));

        List<AccessionWrapper<TestModel, String, String>> result = service.findAllByAccession(
                Arrays.asList("a1", "a2", "a3"));
        assertEquals(3, result.size());
        List<AccessionWrapper<TestModel, String, String>> resultAccession1 = service.findAllByAccession(Arrays.asList("a1"));
        assertEquals(1, resultAccession1.size());
        assertEquals("something1", resultAccession1.get(0).getData().getValue());
        List<AccessionWrapper<TestModel, String, String>> resultAccession2 = service.findAllByAccession(
                Arrays.asList("a2"));
        assertEquals(1, resultAccession2.size());
        assertEquals("something2", resultAccession2.get(0).getData().getValue());
    }

    @Test
    public void saveUniqueElementsAndFindByAccessionThatDoesNotExistReturnsNothing() {
        service.save(Arrays.asList(TEST_MODEL_1, TEST_MODEL_2, TEST_MODEL_3));

        List<AccessionWrapper<TestModel, String, String>> result = service.findAllByAccession(Arrays.asList("a0"));
        assertEquals(0, result.size());
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
        repository.delete("h1");
        repository.delete("h2");
        repository.delete("h3");
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
        assertEquals("a1", repository.findOne("h0").getAccession());
        assertEquals("a1", repository.findOne("h1").getAccession());

        TestTransaction.start();
        TestTransaction.flagForCommit();
        repository.delete("h0");
        repository.delete("h1");
        repository.delete("h2");
        repository.delete("h3");
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
        service.save(Arrays.asList(new AccessionWrapper<>("a1", "h1", TestModel.of("original"))));
        service.save(Arrays.asList(new AccessionWrapper<>("a2", "h2", TestModel.of("unrelated"))));
        service.update("a1", "h2", TestModel.of("modification on 'original' that yields the hash of 'unrelated'"), 1);
    }

    @Test
    public void updateIdentifyingFieldInOnlyOneVersion() throws AccessionDoesNotExistException,
            AccessionDeprecatedException, AccessionMergedException, HashAlreadyExistsException {
        service.save(Arrays.asList(new AccessionWrapper<>("a2", "h1", TestModel.of("something1", "1"), 1)));
        service.save(Arrays.asList(new AccessionWrapper<>("a2", "h2", TestModel.of("something2", "2"), 2)));
        AccessionVersionsWrapper<TestModel, String, String> update = service.update("a2", "h3",
                                                                                    TestModel.of("something3", "3"), 1);

        assertEquals(2, update.getModelWrappers().size());
        assertEquals(new HashSet<>(Arrays.asList("h2", "h3")),
                     update.getModelWrappers().stream().map(AccessionWrapper::getHash).collect(Collectors.toSet()));
        assertEquals(0, service.findAllByHash(Collections.singletonList("h1")).size());
        assertEquals(1, service.findAllByHash(Collections.singletonList("h2")).size());
        assertEquals(1, service.findAllByHash(Collections.singletonList("h3")).size());
    }

    @Test
    public void updateIdentifyingFieldInOnlyOneVersionWithHashCollisionFails()
            throws AccessionDoesNotExistException, AccessionDeprecatedException, AccessionMergedException {
        service.save(Arrays.asList(new AccessionWrapper<>("a2", "h1", TestModel.of("something1", "1"), 1)));
        service.save(Arrays.asList(new AccessionWrapper<>("a2", "h2", TestModel.of("something2", "2"), 2)));
        try {
            service.update("a2", "h2",
                           TestModel.of("modification of 'something1' that yields hash of 'something2'", "3"), 1);
            fail("update should fail because hash would make collision on two objects with different versions");
        } catch (HashAlreadyExistsException e) {
            assertEquals(0, inactiveRepository.count());
        }
    }

    @Test
    public void updateNonIdentifyingField() throws AccessionDoesNotExistException,
            HashAlreadyExistsException, AccessionDeprecatedException, AccessionMergedException {
        service.save(Arrays.asList(new AccessionWrapper<>("a2", "h1", TestModel.of("something2", "nonIdentifying 1"))));
        AccessionVersionsWrapper<TestModel, String, String> update = service.update(
                "a2", "h1", TestModel.of("something2", "nonIdentifying 2"), 1);

        assertEquals(1, update.getModelWrappers().size());
        assertEquals("nonIdentifying 2", update.getModelWrappers().get(0).getData().getNonIdentifyingValue());
        List<AccessionWrapper<TestModel, String, String>> h1 = service.findAllByHash(Arrays.asList("h1"));
        assertEquals(1, h1.size());
        assertEquals("nonIdentifying 2", h1.get(0).getData().getNonIdentifyingValue());
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
        assertEquals(2, service.findAllByAccession(Arrays.asList("a2")).get(0).getVersion());
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
        service.update("a2", "h2", TestModel.of("something2b"), 1);
        service.update("a2", "h3", TestModel.of("something2c"), 1);

        List<AccessionWrapper<TestModel, String, String>> accessions = service.findAllByAccession(Arrays.asList("a2"));
        assertEquals(1, accessions.size());
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

        List<AccessionWrapper<TestModel, String, String>> accessions = service.findAllByAccession(Arrays.asList("a2"));
        assertEquals(1, accessions.size());
        assertEquals(3, accessions.get(0).getVersion());
    }

    @Test
    public void testFindByAccessionAndVersion() throws AccessionDoesNotExistException,
            HashAlreadyExistsException, AccessionMergedException, AccessionDeprecatedException {
        service.save(Arrays.asList(
                new AccessionWrapper("a2", "h1", TestModel.of("something2"), 1),
                new AccessionWrapper("a2", "h2", TestModel.of("something2b"), 2)));

        List<AccessionWrapper<TestModel, String, String>> accessions = service.findAllByAccession(Arrays.asList("a2"));
        assertEquals(1, accessions.size());

        AccessionWrapper<TestModel, String, String> accessionOfVersion1 = service.findByAccessionVersion("a2", 1);
        assertEquals(1, accessionOfVersion1.getVersion());
        AccessionWrapper<TestModel, String, String> accessionsOfVersion2 = service.findByAccessionVersion("a2", 2);
        assertEquals(2, accessionsOfVersion2.getVersion());
    }

    @Test(expected = AccessionDoesNotExistException.class)
    public void testDeprecateNotExisting() throws AccessionDoesNotExistException, AccessionDeprecatedException, AccessionMergedException {
        assertEquals(0, service.findAllByAccession(Arrays.asList("a1")).size());
        service.deprecate("a1", "reasons");
    }

    @Test
    public void testDeprecateOneVersion() throws AccessionDoesNotExistException, AccessionDeprecatedException, AccessionMergedException {
        service.save(Arrays.asList(new AccessionWrapper("a1", "h1", TestModel.of("something2"), 1)));
        assertEquals(1, service.findAllByAccession(Arrays.asList("a1")).size());
        service.deprecate("a1", "reasons");
        assertEquals(0, service.findAllByAccession(Arrays.asList("a1")).size());

        final TestStringOperationEntity entity = historyRepository.findAll().iterator().next();
        assertEquals(EventType.DEPRECATED, entity.getEventType());
        assertEquals("a1", entity.getAccession());
        assertEquals("reasons", entity.getReason());

        final List<TestInactiveAccessionEntity> deprecated = inactiveRepository.findAllByHistoryId(entity.getId());
        assertEquals(1, deprecated.size());
        assertEquals("something2", deprecated.get(0).getValue());
    }

    @Test
    public void testDeprecateMultipleVersion() throws AccessionDoesNotExistException, AccessionMergedException, AccessionDeprecatedException, HashAlreadyExistsException {
        service.save(Arrays.asList(new AccessionWrapper("a1", "h1", TestModel.of("something2"), 1)));
        service.patch("a1", "h2", TestModel.of("something2b"), "patch");
        assertEquals(1, service.findAllByAccession(Arrays.asList("a1")).size());
        assertEquals(2, service.findAllByAccession(Arrays.asList("a1")).get(0).getVersion());
        service.deprecate("a1", "reasons");
        assertEquals(0, service.findAllByAccession(Arrays.asList("a1")).size());

        final TestStringOperationEntity entity = historyRepository.findTopByAccessionOrderByCreatedDateDesc("a1");
        assertEquals(EventType.DEPRECATED, entity.getEventType());
        assertEquals("a1", entity.getAccession());
        assertEquals("reasons", entity.getReason());

        final List<TestInactiveAccessionEntity> deprecated = inactiveRepository.findAllByHistoryId(entity.getId());
        assertEquals(2, deprecated.size());
    }

    @Test
    public void testDeprecateAndAccessionSameObjectMultipleTimes() throws AccessionDoesNotExistException,
            AccessionDeprecatedException, AccessionMergedException {
        service.save(Arrays.asList(new AccessionWrapper("a1", "h1", TestModel.of("something2"), 1)));
        assertEquals(1, service.findAllByAccession(Arrays.asList("a1")).size());
        service.deprecate("a1", "reasons");
        assertEquals(0, service.findAllByAccession(Arrays.asList("a1")).size());
        service.save(Arrays.asList(new AccessionWrapper("a1", "h1", TestModel.of("something2"), 1)));
        assertEquals(1, service.findAllByAccession(Arrays.asList("a1")).size());
        service.deprecate("a1", "reasons");
        assertEquals(0, service.findAllByAccession(Arrays.asList("a1")).size());

        assertEquals(2, historyRepository.count());
    }

    @Test
    public void testMerge() throws AccessionDoesNotExistException, AccessionDeprecatedException,
            AccessionMergedException {
        service.save(Arrays.asList(new AccessionWrapper("a1", "h1", TestModel.of("something1"), 1)));
        service.save(Arrays.asList(new AccessionWrapper("a2", "h2", TestModel.of("something2"), 1)));
        assertEquals(1, service.findAllByAccession(Arrays.asList("a1")).size());
        assertEquals(1, service.findAllByAccession(Arrays.asList("a2")).size());

        service.merge("a1", "a2", "reasons");

        assertEquals(0, service.findAllByAccession(Arrays.asList("a1")).size());
        assertEquals(1, service.findAllByAccession(Arrays.asList("a2")).size());
    }

    @Test(expected = AccessionDoesNotExistException.class)
    public void testMergeAccessionDoesNotExistOrigin() throws AccessionDoesNotExistException,
            AccessionDeprecatedException, AccessionMergedException {
        service.save(Arrays.asList(new AccessionWrapper("a1", "h1", TestModel.of("something1"), 1)));
        assertEquals(1, service.findAllByAccession(Arrays.asList("a1")).size());

        service.merge("doesnotexist", "a1", "reasons");
    }

    @Test(expected = AccessionDoesNotExistException.class)
    public void testMergeAccessionDoesNotExistDestination() throws AccessionDoesNotExistException,
            AccessionDeprecatedException, AccessionMergedException {
        service.save(Arrays.asList(new AccessionWrapper("a1", "h1", TestModel.of("something1"), 1)));
        assertEquals(1, service.findAllByAccession(Arrays.asList("a1")).size());

        service.merge("a1", "doesnotexist", "reasons");
    }

    @Test(expected = AccessionDeprecatedException.class)
    public void testMergeAccessionDeprecatedOrigin() throws AccessionDoesNotExistException,
            AccessionDeprecatedException, AccessionMergedException {
        service.save(Arrays.asList(new AccessionWrapper("a1", "h1", TestModel.of("something1"), 1)));
        service.save(Arrays.asList(new AccessionWrapper("a2", "h2", TestModel.of("something2"), 1)));
        assertEquals(1, service.findAllByAccession(Arrays.asList("a1")).size());
        assertEquals(1, service.findAllByAccession(Arrays.asList("a2")).size());
        service.deprecate("a1", "blah");

        service.merge("a1", "a2", "reasons");
    }

    @Test(expected = AccessionDeprecatedException.class)
    public void testMergeAccessionDeprecatedDestination() throws AccessionDoesNotExistException,
            AccessionDeprecatedException, AccessionMergedException {
        service.save(Arrays.asList(new AccessionWrapper("a1", "h1", TestModel.of("something1"), 1)));
        service.save(Arrays.asList(new AccessionWrapper("a2", "h2", TestModel.of("something2"), 1)));
        assertEquals(1, service.findAllByAccession(Arrays.asList("a1")).size());
        assertEquals(1, service.findAllByAccession(Arrays.asList("a2")).size());
        service.deprecate("a2", "blah");

        service.merge("a1", "a2", "reasons");
    }

}
