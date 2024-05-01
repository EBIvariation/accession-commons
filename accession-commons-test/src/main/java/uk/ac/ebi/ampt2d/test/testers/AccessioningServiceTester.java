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

import uk.ac.ebi.ampt2d.commons.accession.core.AccessioningService;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionDeprecatedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionDoesNotExistException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionMergedException;
import uk.ac.ebi.ampt2d.commons.accession.core.models.AccessionWrapper;
import uk.ac.ebi.ampt2d.test.models.TestModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class AccessioningServiceTester {
    private static String APPLICATION_INSTANCE_ID = "TEST_APPPLICATION_INSTANCE_ID";

    private final AccessioningService<TestModel, String, String> accessioningService;

    private List<AccessionWrapperCollectionTester> singleVersionResults;

    private List<AccessionVersionWrapperTester> multipleVersionResults;

    private IMethodTester lastMethodResponse;

    public AccessioningServiceTester(AccessioningService<TestModel, String, String> accessioningService) {
        this.accessioningService = accessioningService;
        this.singleVersionResults = new ArrayList<>();
        this.multipleVersionResults = new ArrayList<>();
    }

    public AccessioningServiceTester getOrCreate(String... values) {
        return getOrCreate(toModels(values));
    }

    public AccessioningServiceTester getOrCreate(List<TestModel> models) {
        addToCollection(singleVersionResults,
                new AccessionWrapperCollectionTester(() -> accessioningService.getOrCreate(models, APPLICATION_INSTANCE_ID)));
        return this;
    }

    public AccessionWrapperCollectionTester getSingleVersionResults() {
        return singleVersionResults.get(singleVersionResults.size() - 1);
    }

    public AccessioningServiceTester patch(String accession, String patchData) {
        return patch(accession, TestModel.of(patchData));
    }

    public AccessioningServiceTester patch(String accession, TestModel patchData) {
        addToCollection(multipleVersionResults,
                new AccessionVersionWrapperTester(() -> accessioningService.patch(accession, patchData)));
        return this;
    }

    public AccessionVersionWrapperTester getLastMultipleVersionResult() {
        return multipleVersionResults.get(multipleVersionResults.size() - 1);
    }

    public AccessioningServiceTester update(String accession, int version, String data) {
        addToCollection(multipleVersionResults,
                new AccessionVersionWrapperTester(() ->
                        accessioningService.update(accession, version, TestModel.of(data))));
        return this;
    }

    public AccessioningServiceTester merge(String accessionA, String accessionB, String reason) {
        lastMethodResponse = new MethodTester(() -> accessioningService.merge(accessionA, accessionB, reason));
        return this;
    }

    public IMethodTester getLastMethodResponse() {
        return lastMethodResponse;
    }

    public AccessionWrapperCollectionTester get(List<TestModel> models) {
        return addToCollection(singleVersionResults,
                new AccessionWrapperCollectionTester(() -> accessioningService.get(models)));
    }

    public AccessionWrapperCollectionTester getAccessions(String... accessionIds) {
        return addToCollection(singleVersionResults,
                new AccessionWrapperCollectionTester(() ->
                        getLatestVersionOfAccessions(accessionIds)));
    }

    private List<AccessionWrapper<TestModel, String, String>> getLatestVersionOfAccessions(String[] accessionIds)
            throws AccessionDoesNotExistException, AccessionMergedException, AccessionDeprecatedException {
        List<AccessionWrapper<TestModel, String, String>> accessionWrappers = new ArrayList<>();
        for (String accessionId : accessionIds) {
            accessionWrappers.add(accessioningService.getByAccession(accessionId));
        }
        return accessionWrappers;
    }

    private <T extends IMethodTester> T addToCollection(Collection<T> collection, T t) {
        collection.add(t);
        lastMethodResponse = t;
        return t;
    }

    private List<TestModel> toModels(String... values) {
        return Arrays.stream(values).map(TestModel::of).collect(Collectors.toList());
    }

    public AccessioningServiceTester deprecate(String accessionId, String reason) {
        lastMethodResponse = new MethodTester(() -> accessioningService.deprecate(accessionId, reason));
        return this;
    }
}
