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
package uk.ac.ebi.ampt2d.accessioning.commons.generators;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.ampt2d.accessioning.commons.core.SaveResponse;
import uk.ac.ebi.ampt2d.accessioning.commons.hashing.SHA1HashingFunction;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

@RunWith(BlockJUnit4ClassRunner.class)
public class SingleAccessionGeneratorTest {

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

        List<ModelHashAccession<TestUser, String, String>> accessions = generator.generateAccessions(hashToModel);
        assertEquals(3, accessions.size());
        assertAccession(0, accessions, null);
    }

    private void assertAccession(int i, List<ModelHashAccession<TestUser, String, String>> accessions,
                                 Function<String, String> accessionHashingFunction) {
        assertEquals("hash" + i, accessions.get(i).hash());
        String accessionText = "test_name" + i + "_test_surname" + i;
        if (accessionHashingFunction != null) {
            accessionText = accessionHashingFunction.apply(accessionText);
        }
        assertEquals(accessionText, accessions.get(i).accession());
        assertEquals("test_name" + i, accessions.get(i).model().name);
        assertEquals("test_surname" + i, accessions.get(i).model().surname);
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

        List<ModelHashAccession<TestUser, String, String>> accessions = generator.generateAccessions(hashToModel);
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

        List<ModelHashAccession<TestUser, String, String>> accessions = generator.generateAccessions(hashToModel);
        assertEquals(3, accessions.size());
        assertAccession(0, accessions, new SHA1HashingFunction());
    }

    @Test
    public void testPostSaveAction(){
        SingleAccessionGenerator<TestUser, String> generator = SingleAccessionGenerator.ofSHA1AccessionGenerator(
                testUser -> testUser.name + "_" + testUser.surname
        );

        HashMap<String, TestUser> hashToModel = new LinkedHashMap<>();
        hashToModel.put("hash0", new TestUser("test_name0", "test_surname0"));
        hashToModel.put("hash1", new TestUser("test_name1", "test_surname1"));
        hashToModel.put("hash2", new TestUser("test_name2", "test_surname2"));

        List<ModelHashAccession<TestUser, String, String>> accessions = generator.generateAccessions(hashToModel);
        generator.postSave(new SaveResponse<>(hashToModel, new HashMap<>()));
    }

}