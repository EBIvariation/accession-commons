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
package uk.ac.ebi.ampt2d.commons.accession.hashing;

import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class SHA1HashingFunctionTest {

    @Test
    public void testGenerateAccession() {
        String object1 = "Object1";
        String object2 = "Object2";
        Function<String, String> hashingFunction = new SHA1HashingFunction();
        String object1Accession = hashingFunction.apply(object1);
        String object2Accession = hashingFunction.apply(object2);
        String object3Accession = hashingFunction.apply(object2);
        assertNotEquals(object1Accession, object2Accession);
        assertEquals(object2Accession, object3Accession);
        assertEquals(40, object1Accession.length());
    }
}
