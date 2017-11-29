/*
 *
 * Copyright 2017 EMBL - European Bioinformatics Institute
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
package uk.ac.ebi.ampt2d.accession;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class UUIDAccessionGeneratorTest {

    public static final String TEST_NAMESPACE = "Test";

    private static Random randomGenerator;

    @BeforeClass
    public static void setUpClass() throws Exception {
        randomGenerator = new Random();
    }

    @Test
    public void differentAccessionsAreGeneratedForDifferentInputs() throws Exception {
        String hash1 = randomChecksum();
        String hash2 = randomChecksum();

        UuidAccessionGenerator<String> generator = new UuidAccessionGenerator<>(TEST_NAMESPACE);

        Map<String, UUID> accessions = generator.generateAccessions(new HashSet<>(Arrays.asList(hash1, hash2)));

        UUID accession1 = accessions.get(hash1);
        UUID accession2 = accessions.get(hash2);

        assertTrue(accession1 != null);
        assertTrue(accession2 != null);

        assertNotEquals(accession1, accession2);
    }

    @Test
    public void oneGeneratorReturnsTheSameAccessionInDifferentCallsWithTheSameInput() {
        String hash = randomChecksum();

        UuidAccessionGenerator<String> generator = new UuidAccessionGenerator<>(TEST_NAMESPACE);

        Map<String, UUID> accessions = generator.generateAccessions(Collections.singleton(hash));
        UUID accession1 = accessions.get(hash);

        accessions = generator.generateAccessions(Collections.singleton(hash));
        UUID accession2 = accessions.get(hash);

        assertEquals(accession1, accession2);
    }

    @Test
    public void twoDifferentGeneratorInstancesForSameNamespaceReturnTheSameAccessionForTheSameInput() {
        String hash = randomChecksum();

        UuidAccessionGenerator<String> generator = new UuidAccessionGenerator<>(TEST_NAMESPACE);

        Map<String, UUID> accessions = generator.generateAccessions(Collections.singleton(hash));
        UUID accession1 = accessions.get(hash);

        UuidAccessionGenerator<String> generator2 = new UuidAccessionGenerator<>(TEST_NAMESPACE);
        accessions = generator2.generateAccessions(Collections.singleton(hash));
        UUID accession2 = accessions.get(hash);

        assertEquals(accession1, accession2);
    }

    @Test
    public void twoDifferentGeneratorInstancesForDiferentNamespacesReturnDifferentAccessionsForTheSameInput() {
        String hash = randomChecksum();

        UuidAccessionGenerator<String> generator = new UuidAccessionGenerator<>(TEST_NAMESPACE);

        Map<String, UUID> accessions = generator.generateAccessions(Collections.singleton(hash));
        UUID accession1 = accessions.get(hash);

        UuidAccessionGenerator<String> generator2 = new UuidAccessionGenerator<>("SHA1");
        accessions = generator2.generateAccessions(Collections.singleton(hash));
        UUID accession2 = accessions.get(hash);

        assertNotEquals(accession1, accession2);
    }

    private String randomChecksum() {
        String hexDigits = "0123456789abcdef";

        char[] text = new char[32];
        for (int i = 0; i < text.length; i++) {
            text[i] = hexDigits.charAt(randomGenerator.nextInt(hexDigits.length()));
        }

        return new String(text);
    }
}