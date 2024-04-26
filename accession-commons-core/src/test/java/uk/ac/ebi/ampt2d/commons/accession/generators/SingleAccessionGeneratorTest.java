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
package uk.ac.ebi.ampt2d.commons.accession.generators;

import org.junit.Test;
import uk.ac.ebi.ampt2d.commons.accession.core.models.AccessionWrapper;
import uk.ac.ebi.ampt2d.commons.accession.core.models.SaveResponse;
import uk.ac.ebi.ampt2d.commons.accession.hashing.SHA1HashingFunction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class SingleAccessionGeneratorTest {
    private static String APPLICATION_INSTANCE_ID = "TEST_APPPLICATION_INSTANCE_ID";

    private class TestUser {

        private String name;

        private String surname;

        public TestUser(String name, String surname) {
            this.name = name;
            this.surname = surname;
        }

    }

    @Test
    public void testSingleAccessionGenerator() {
        SingleAccessionGenerator<TestUser, String> generator = new SingleAccessionGenerator<>(
                testUser -> testUser.name + "_" + testUser.surname);
        HashMap<String, TestUser> hashToModel = new LinkedHashMap<>();
        hashToModel.put("hash0", new TestUser("test_name0", "test_surname0"));
        hashToModel.put("hash1", new TestUser("test_name1", "test_surname1"));
        hashToModel.put("hash2", new TestUser("test_name2", "test_surname2"));

        List<AccessionWrapper<TestUser, String, String>> accessions = generator.generateAccessions(hashToModel, APPLICATION_INSTANCE_ID);
        assertEquals(3, accessions.size());
        assertAccession(0, accessions, null);
    }

    private void assertAccession(int i, List<AccessionWrapper<TestUser, String, String>> accessions,
                                 Function<String, String> accessionHashingFunction) {
        assertEquals("hash" + i, accessions.get(i).getHash());
        String accessionText = "test_name" + i + "_test_surname" + i;
        if (accessionHashingFunction != null) {
            accessionText = accessionHashingFunction.apply(accessionText);
        }
        assertEquals(accessionText, accessions.get(i).getAccession());
        assertEquals("test_name" + i, accessions.get(i).getData().name);
        assertEquals("test_surname" + i, accessions.get(i).getData().surname);
    }

    @Test
    public void testOfHashAccession() {
        SingleAccessionGenerator<TestUser, String> generator = SingleAccessionGenerator.ofHashAccessionGenerator(
                testUser -> testUser.name + "_" + testUser.surname,
                new SHA1HashingFunction()
        );

        HashMap<String, TestUser> hashToModel = new LinkedHashMap<>();
        hashToModel.put("hash0", new TestUser("test_name0", "test_surname0"));
        hashToModel.put("hash1", new TestUser("test_name1", "test_surname1"));
        hashToModel.put("hash2", new TestUser("test_name2", "test_surname2"));

        List<AccessionWrapper<TestUser, String, String>> accessions = generator.generateAccessions(hashToModel, APPLICATION_INSTANCE_ID);
        assertEquals(3, accessions.size());
        assertAccession(0, accessions, new SHA1HashingFunction());
    }

    @Test
    public void testOfSHA1HashAccession() {
        SingleAccessionGenerator<TestUser, String> generator = SingleAccessionGenerator.ofSHA1AccessionGenerator(
                testUser -> testUser.name + "_" + testUser.surname
        );

        HashMap<String, TestUser> hashToModel = new LinkedHashMap<>();
        hashToModel.put("hash0", new TestUser("test_name0", "test_surname0"));
        hashToModel.put("hash1", new TestUser("test_name1", "test_surname1"));
        hashToModel.put("hash2", new TestUser("test_name2", "test_surname2"));

        List<AccessionWrapper<TestUser, String, String>> accessions = generator.generateAccessions(hashToModel, APPLICATION_INSTANCE_ID);
        assertEquals(3, accessions.size());
        assertAccession(0, accessions, new SHA1HashingFunction());
    }

    @Test
    public void testPostSaveAction() {
        SingleAccessionGenerator<TestUser, String> generator = SingleAccessionGenerator.ofSHA1AccessionGenerator(
                testUser -> testUser.name + "_" + testUser.surname
        );

        HashMap<String, TestUser> hashToModel = new LinkedHashMap<>();
        hashToModel.put("hash0", new TestUser("test_name0", "test_surname0"));
        hashToModel.put("hash1", new TestUser("test_name1", "test_surname1"));
        hashToModel.put("hash2", new TestUser("test_name2", "test_surname2"));

        List<AccessionWrapper<TestUser, String, String>> modelAccessions = generator.generateAccessions(hashToModel, APPLICATION_INSTANCE_ID);
        Set<String> accessions = modelAccessions.stream().map(AccessionWrapper::getAccession).collect(Collectors.toSet());
        generator.postSave(new SaveResponse<>(accessions, new HashSet<>()));
    }

}
