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
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.HashAlreadyExistsException;
import uk.ac.ebi.ampt2d.commons.accession.core.models.AccessionVersionsWrapper;
import uk.ac.ebi.ampt2d.commons.accession.core.models.AccessionWrapper;
import uk.ac.ebi.ampt2d.commons.accession.core.models.GetOrCreateAccessionWrapper;
import uk.ac.ebi.ampt2d.test.configuration.TestJpaDatabaseServiceTestConfiguration;
import uk.ac.ebi.ampt2d.test.models.TestModel;
import uk.ac.ebi.ampt2d.test.persistence.TestRepository;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = {TestJpaDatabaseServiceTestConfiguration.class})
public class BasicAccessioningServiceTest {

    @Autowired
    private TestRepository repository;

    @Autowired
    private AccessioningService<TestModel, String, String> accessioningService;

    @Test
    public void accessionNotRepeatedElements() throws AccessionCouldNotBeGeneratedException {
        List<GetOrCreateAccessionWrapper<TestModel, String, String>> accessions = accessioningService.getOrCreate(
                Arrays.asList(
                        TestModel.of("service-test-1"),
                        TestModel.of("service-test-2"),
                        TestModel.of("service-test-3")
                ));
        assertEquals(3, accessions.size());
    }

    @Test
    public void accessionWithRepeatedElementsReturnsUnique() throws AccessionCouldNotBeGeneratedException {
        List<GetOrCreateAccessionWrapper<TestModel, String, String>> accessions = accessioningService.getOrCreate(
                Arrays.asList(
                        TestModel.of("service-test-1"),
                        TestModel.of("service-test-2"),
                        TestModel.of("service-test-2"),
                        TestModel.of("service-test-3")
                ));
        assertEquals(3, accessions.size());
    }

    @Test
    public void getNonGeneratedAccessionsReturnsNothing() throws AccessionCouldNotBeGeneratedException {
        List<AccessionWrapper<TestModel, String, String>> accessions = accessioningService.get(
                Arrays.asList(
                        TestModel.of("service-test-1"),
                        TestModel.of("service-test-2"),
                        TestModel.of("service-test-3")
                ));
        assertEquals(0, accessions.size());
    }

    @Test
    public void getAlreadyGeneratedAccessionsReturnsGeneratedOnly() throws AccessionCouldNotBeGeneratedException {
        accessioningService.getOrCreate(
                Arrays.asList(
                        TestModel.of("service-test-3")
                ));

        List<AccessionWrapper<TestModel, String, String>> accessions = accessioningService.get(Arrays.asList(
                TestModel.of("service-test-1"),
                TestModel.of("service-test-2"),
                TestModel.of("service-test-3")
        ));
        assertEquals(1, accessions.size());
    }

    @Test
    public void accessioningMultipleTimesTheSameObjectReturnsTheSameAccession()
            throws AccessionCouldNotBeGeneratedException {
        TestTransaction.flagForCommit();
        List<GetOrCreateAccessionWrapper<TestModel, String, String>> accession1 = accessioningService.getOrCreate(
                Arrays.asList(
                        TestModel.of("service-test-3")
                ));
        TestTransaction.end();

        List<GetOrCreateAccessionWrapper<TestModel, String, String>> accession2 = accessioningService.getOrCreate(
                Arrays.asList(
                        TestModel.of("service-test-3")
                ));
        assertEquals(1, accession2.size());
        assertEquals(accession1.get(0).getAccession(), accession2.get(0).getAccession());

        TestTransaction.start();
        TestTransaction.flagForCommit();
        repository.deleteById(accession1.get(0).getHash());
        TestTransaction.end();
    }

    @Test(expected = AccessionDoesNotExistException.class)
    public void updateFailsWhenAccessionDoesNotExist() throws AccessionDoesNotExistException,
            HashAlreadyExistsException, AccessionMergedException, AccessionDeprecatedException {
        accessioningService.update("id-service-test-3", 1, TestModel.of("test-3"));
    }

    @Test(expected = HashAlreadyExistsException.class)
    public void updateFailsWhenAccessionAlreadyExists() throws AccessionDoesNotExistException,
            HashAlreadyExistsException, AccessionCouldNotBeGeneratedException, AccessionMergedException, AccessionDeprecatedException {
        accessioningService.getOrCreate(Arrays.asList(TestModel.of("test-3"), TestModel.of("test-4")));
        accessioningService.update("id-service-test-3", 1, TestModel.of("test-4"));
    }

    @Test
    public void testUpdate() throws AccessionDoesNotExistException,
            HashAlreadyExistsException, AccessionCouldNotBeGeneratedException, AccessionMergedException, AccessionDeprecatedException {
        accessioningService.getOrCreate(Arrays.asList(TestModel.of("test-3")));
        final AccessionVersionsWrapper<TestModel, String, String> updatedAccession =
                accessioningService.update("id-service-test-3", 1, TestModel.of("test-3b"));

        assertEquals(1, updatedAccession.getModelWrappers().size());
        assertEquals(1, updatedAccession.getModelWrappers().get(0).getVersion());
        assertEquals("test-3b", updatedAccession.getModelWrappers().get(0).getData().getValue());

        final AccessionWrapper<TestModel, String, String> wrappedAccesion =
                accessioningService.getByAccession("id-service-test-3");
        assertNotNull(wrappedAccesion);

        //We only find the new object information, not the old one
        assertEquals(0, accessioningService.get(Arrays.asList(TestModel.of("test-3"))).size());
        assertEquals(1, accessioningService.get(Arrays.asList(TestModel.of("test-3b"))).size());
    }

    @Test
    public void testPatch() throws AccessionCouldNotBeGeneratedException, AccessionDeprecatedException,
            AccessionDoesNotExistException, AccessionMergedException, HashAlreadyExistsException {
        accessioningService.getOrCreate(Arrays.asList(TestModel.of("test-3")));
        final AccessionVersionsWrapper<TestModel, String, String> accession =
                accessioningService.patch("id-service-test-3", TestModel.of("test-3b"));
        assertEquals(2, accession.getModelWrappers().size());

        //We only find the new object information, not the old one
        final List<AccessionWrapper<TestModel, String, String>> firstVersion =
                accessioningService.get(Arrays.asList(TestModel.of("test-3")));
        final List<AccessionWrapper<TestModel, String, String>> secondVersion =
                accessioningService.get(Arrays.asList(TestModel.of("test-3b")));
        assertEquals(1, firstVersion.size());
        assertEquals(1, secondVersion.size());
        assertEquals(1, firstVersion.get(0).getVersion());
        assertEquals(2, secondVersion.get(0).getVersion());
    }

    @Test
    public void testGetAccessionVersion() throws AccessionCouldNotBeGeneratedException, AccessionDoesNotExistException,
            HashAlreadyExistsException, AccessionMergedException, AccessionDeprecatedException {
        accessioningService.getOrCreate(Arrays.asList(TestModel.of("test-accession-version")));
        accessioningService.patch("id-service-test-accession-version", TestModel.of("test-accession-version-b"));

        final AccessionWrapper<TestModel, String, String> version1 = accessioningService
                .getByAccessionAndVersion("id-service-test-accession-version", 1);
        final AccessionWrapper<TestModel, String, String> version2 = accessioningService
                .getByAccessionAndVersion("id-service-test-accession-version", 2);
        assertEquals("test-accession-version", version1.getData().getValue());
        assertEquals("test-accession-version-b", version2.getData().getValue());
    }

    @Test
    public void testDeprecate() throws AccessionCouldNotBeGeneratedException, AccessionMergedException,
            AccessionDoesNotExistException, AccessionDeprecatedException {
        accessioningService.getOrCreate(Arrays.asList(TestModel.of("test-deprecate-version")));
        doDeprecateAndAssert("id-service-test-deprecate-version");
    }

    private void doDeprecateAndAssert(String accession) throws AccessionMergedException,
            AccessionDoesNotExistException, AccessionDeprecatedException {
        accessioningService.deprecate(accession, "Reasons");
        try {
            accessioningService.getByAccession(accession);
            fail();
        } catch (Exception exception) {
            assertTrue(exception instanceof AccessionDeprecatedException);
        }
    }

    @Test(expected = AccessionDoesNotExistException.class)
    public void testDeprecateAccessionDoesNotExist() throws AccessionCouldNotBeGeneratedException,
            AccessionMergedException, AccessionDoesNotExistException, AccessionDeprecatedException {
        accessioningService.deprecate("id-does-not-exist", "Reasons");
    }

    @Test(expected = AccessionDeprecatedException.class)
    public void testDeprecateTwice() throws AccessionCouldNotBeGeneratedException, AccessionMergedException,
            AccessionDoesNotExistException, AccessionDeprecatedException {
        accessioningService.getOrCreate(Arrays.asList(TestModel.of("test-deprecate-version-2")));
        accessioningService.deprecate("id-service-test-deprecate-version-2", "Reasons");
        accessioningService.deprecate("id-service-test-deprecate-version-2", "Reasons");
    }

    @Test
    public void testDeprecateUpdated() throws AccessionCouldNotBeGeneratedException, AccessionMergedException,
            AccessionDoesNotExistException, AccessionDeprecatedException, HashAlreadyExistsException {
        accessioningService.getOrCreate(Arrays.asList(TestModel.of("test-deprecate-update-version")));
        accessioningService.update("id-service-test-deprecate-update-version", 1,
                TestModel.of("test-deprecate-update-version-updated!"));
        doDeprecateAndAssert("id-service-test-deprecate-update-version");
    }

    @Test
    public void testDeprecatePatched() throws AccessionCouldNotBeGeneratedException, AccessionMergedException,
            AccessionDoesNotExistException, AccessionDeprecatedException, HashAlreadyExistsException {
        accessioningService.getOrCreate(Arrays.asList(TestModel.of("test-deprecate-patch-version")));
        accessioningService.patch("id-service-test-deprecate-patch-version",
                TestModel.of("test-deprecate-update-version-patched!"));
        doDeprecateAndAssert("id-service-test-deprecate-patch-version");
    }

}
