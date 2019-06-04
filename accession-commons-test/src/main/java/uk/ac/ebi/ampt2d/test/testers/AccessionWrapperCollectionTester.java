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
package uk.ac.ebi.ampt2d.test.testers;

import uk.ac.ebi.ampt2d.commons.accession.core.models.AccessionWrapper;
import uk.ac.ebi.ampt2d.test.models.TestModel;
import uk.ac.ebi.ampt2d.test.utils.ThrowingSupplier;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class AccessionWrapperCollectionTester
        extends MethodResponseTester<List<? extends AccessionWrapper<TestModel, String, String>>> {

    public AccessionWrapperCollectionTester(
            ThrowingSupplier<List<? extends AccessionWrapper<TestModel, String, String>>> functionCall) {
        super(functionCall);
    }

    public void assertSize(int length) {
        assertEquals(length, getData().size());
    }

    public void assertAccessions(String... accessions) {
        assertSize(accessions.length);
        for (String accession : accessions) {
            assertAccessionIsPresent(accession);
        }
    }

    public void assertAccessionIsPresent(String accession) {
        for (AccessionWrapper<TestModel, String, String> wrapper : getData()) {
            if (Objects.equals(wrapper.getAccession(), accession)) {
                return;
            }
        }
        fail("Accession '" + accession + "' is not present, accessions in list '" + getAccessionList() + "'");
    }

    private List<String> getAccessionList() {
        return getData().stream().map(AccessionWrapper::getAccession).collect(Collectors.toList());
    }

    public void assertAccessionValues(String... values) {
        assertSize(values.length);
        for (String value : values) {
            assertAccessionWithValueIsPresent(value);
        }
    }

    public void assertAccessionWithValueIsPresent(String value) {
        for (AccessionWrapper<TestModel, String, String> wrapper : getData()) {
            if (Objects.equals(wrapper.getData().getValue(), value)) {
                return;
            }
        }
        fail("No object with value '" + value + "' was present in list '" + getAccessionList() + "'");
    }
}
