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
package uk.ac.ebi.ampt2d.test.rest;

import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionCouldNotBeGeneratedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionIsNotPendingException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.MissingUnsavedAccessionsException;
import uk.ac.ebi.ampt2d.commons.accession.core.models.AccessionWrapper;
import uk.ac.ebi.ampt2d.commons.accession.core.models.SaveResponse;
import uk.ac.ebi.ampt2d.commons.accession.generators.AccessionGenerator;
import uk.ac.ebi.ampt2d.test.models.TestModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockTestAccessionGenerator implements AccessionGenerator<TestModel, String> {

    private HashMap<Object, String> map;

    public MockTestAccessionGenerator() {
        map = new HashMap<>();
    }

    @Override
    public <HASH> List<AccessionWrapper<TestModel, HASH, String>> generateAccessions(Map<HASH, TestModel> messages)
            throws AccessionCouldNotBeGeneratedException {
        List<AccessionWrapper<TestModel, HASH, String>> accessions = new ArrayList<>();
        for (Map.Entry<HASH, TestModel> entry : messages.entrySet()) {
            accessions.add(generateAccession(entry));
        }
        return accessions;
    }

    private <HASH> AccessionWrapper<TestModel, HASH, String> generateAccession(Map.Entry<HASH, TestModel> entry)
            throws AccessionCouldNotBeGeneratedException {
        String something = entry.getValue().getValue();
        if (something.contains("MissingUnsavedAccessionsException")) {
            throw new MissingUnsavedAccessionsException(new ArrayList<>(), new ArrayList<>());
        }
        if (something.contains("AccessionIsNotPendingException")) {
            throw new AccessionIsNotPendingException(-1);
        }
        if (something.contains("AccessionCouldNotBeGeneratedException")) {
            throw new AccessionCouldNotBeGeneratedException("Test");
        }

        if (!map.containsKey(entry.getKey())) {
            map.put(entry.getKey(), "id-" + map.size());
        }
        return new AccessionWrapper<>(map.get(entry.getKey()), entry.getKey(), entry.getValue());
    }

    @Override
    public void postSave(SaveResponse<String> response) {
        // Do nothing
    }

}