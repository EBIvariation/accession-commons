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

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = {TestJpaDatabaseServiceTestConfiguration.class})
public class DecoratedAccessioningServiceTest {

    @Autowired
    private AccessioningService<TestModel, String, String> accessioningService;

    @Test
    public void assertGetOrCreate() throws AccessionCouldNotBeGeneratedException {
        List<GetOrCreateAccessionWrapper<TestModel, String, String>> accessions = getPrefixedService().getOrCreate(
                Arrays.asList(
                        TestModel.of("service-test-1")
                ));
        assertEquals(1, accessions.size());
        assertEquals("prefix-id-service-service-test-1", accessions.get(0).getAccession());
    }

    private DecoratedAccessioningService<TestModel, String, String, String> getPrefixedService() {
        return DecoratedAccessioningService.buildPrefixAccessionService(accessioningService, "prefix-", s -> s);
    }

    @Test
    public void assertGet() throws AccessionCouldNotBeGeneratedException {
        assertGetOrCreate();
        List<AccessionWrapper<TestModel, String, String>> accessions =
                getPrefixedService().get(Arrays.asList(TestModel.of("service-test-1")));
        assertEquals(1, accessions.size());
        assertEquals("prefix-id-service-service-test-1", accessions.get(0).getAccession());
    }

    @Test
    public void assertGetByAccession()
            throws AccessionCouldNotBeGeneratedException, AccessionDeprecatedException,
            AccessionMergedException, AccessionDoesNotExistException {
        assertGetOrCreate();
        AccessionWrapper<TestModel, String, String> accession = getPrefixedService().getByAccession("prefix-id-service-service-test-1");
        assertEquals("prefix-id-service-service-test-1", accession.getAccession());
    }

    @Test(expected = AccessionDoesNotExistException.class)
    public void assertGetByAccessionWrongPrefix()
            throws AccessionCouldNotBeGeneratedException, AccessionDeprecatedException,
            AccessionMergedException, AccessionDoesNotExistException {
        assertGetOrCreate();
        getPrefixedService().getByAccession("service-service-test-1");
    }

    @Test(expected = AccessionDoesNotExistException.class)
    public void assertGetByAccessionAndVersionWrongPrefix() throws AccessionCouldNotBeGeneratedException,
            AccessionMergedException, AccessionDoesNotExistException, AccessionDeprecatedException {
        assertGetOrCreate();
        getPrefixedService().getByAccessionAndVersion("service-service-test-1", 1);
    }

    @Test(expected = AccessionDoesNotExistException.class)
    public void assertGetByAccessionWrongPrefixShort() throws AccessionCouldNotBeGeneratedException,
            AccessionMergedException, AccessionDoesNotExistException, AccessionDeprecatedException {
        assertGetOrCreate();
        getPrefixedService().getByAccessionAndVersion("s", 1);
    }

    @Test
    public void assertGetByAccessionAndVersion() throws AccessionCouldNotBeGeneratedException, AccessionMergedException,
            AccessionDoesNotExistException, AccessionDeprecatedException {
        assertGetOrCreate();
        AccessionWrapper<TestModel, String, String> accessions =
                getPrefixedService().getByAccessionAndVersion("prefix-id-service-service-test-1", 1);
        assertEquals("prefix-id-service-service-test-1", accessions.getAccession());
        assertEquals(1, accessions.getVersion());
    }

    @Test
    public void assertUpdate() throws AccessionCouldNotBeGeneratedException, AccessionMergedException,
            AccessionDoesNotExistException, AccessionDeprecatedException, HashAlreadyExistsException {
        assertGetOrCreate();
        AccessionVersionsWrapper<TestModel, String, String> accessions =
                getPrefixedService().update("prefix-id-service-service-test-1", 1, TestModel.of("service-test-1b"));
        assertEquals("prefix-id-service-service-test-1", accessions.getAccession());
        assertEquals(1, accessions.getModelWrappers().size());
    }

    @Test
    public void assertPatch() throws AccessionCouldNotBeGeneratedException, AccessionMergedException,
            AccessionDoesNotExistException, AccessionDeprecatedException, HashAlreadyExistsException {
        assertGetOrCreate();
        AccessionVersionsWrapper<TestModel, String, String> accessions =
                getPrefixedService().patch("prefix-id-service-service-test-1", TestModel.of("service-test-1b"));
        assertEquals("prefix-id-service-service-test-1", accessions.getAccession());
        assertEquals(2, accessions.getModelWrappers().size());
    }

    @Test
    public void assertMerge() throws AccessionMergedException, AccessionDoesNotExistException,
            AccessionDeprecatedException, AccessionCouldNotBeGeneratedException {
        List<GetOrCreateAccessionWrapper<TestModel, String, String>> accessions = getPrefixedService().getOrCreate(
                Arrays.asList(
                        TestModel.of("service-test-1"),
                        TestModel.of("service-test-2")
                ));
        assertEquals(2, accessions.size());
        getPrefixedService().merge("prefix-id-service-service-test-1", "prefix-id-service-service-test-2", "reason");
    }

    @Test
    public void assertDeprecate() throws AccessionMergedException, AccessionDoesNotExistException,
            AccessionDeprecatedException, AccessionCouldNotBeGeneratedException {
        assertGetOrCreate();
        getPrefixedService().deprecate("prefix-id-service-service-test-1", "reason");
    }

}
