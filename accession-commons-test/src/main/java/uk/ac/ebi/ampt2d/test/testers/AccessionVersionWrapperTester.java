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

import uk.ac.ebi.ampt2d.commons.accession.core.models.AccessionVersionsWrapper;
import uk.ac.ebi.ampt2d.commons.accession.core.models.AccessionWrapper;
import uk.ac.ebi.ampt2d.test.models.TestModel;
import uk.ac.ebi.ampt2d.test.utils.ThrowingSupplier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class AccessionVersionWrapperTester
        extends MethodResponseTester<AccessionVersionsWrapper<TestModel, String, String>> {

    public AccessionVersionWrapperTester(
            ThrowingSupplier<AccessionVersionsWrapper<TestModel, String, String>> functionCall) {
        super(functionCall);
    }

    public AccessionVersionWrapperTester assertTotalVersions(int total) {
        assertEquals(total, getData().getModelWrappers().size());
        return this;
    }

    public AccessionVersionWrapperTester assertVersionsAreIncreased() {
        final List<AccessionWrapper<TestModel, String, String>> wrappers =
                new ArrayList<>(getData().getModelWrappers());

        wrappers.sort(Comparator.comparingInt(AccessionWrapper::getVersion));
        int minValue = wrappers.get(0).getVersion();
        for (int i = 1; i < wrappers.size(); i++) {
            if (wrappers.get(i).getVersion() <= minValue) {
                fail("Multiple patches with same version number '" + minValue + "'");
            }
        }
        return this;
    }

    public AccessionVersionWrapperTester assertAccession(String accession) {
        assertEquals(accession, getData().getAccession());
        return this;
    }

    public AccessionVersionWrapperTester assertHash(int version, String hash) {
        final AccessionWrapper<TestModel, String, String> wrapper = getData().getVersion(version)
                .orElseThrow(() -> new AssertionError("Version not found"));
        assertEquals(hash, wrapper.getHash());
        return this;
    }
}
