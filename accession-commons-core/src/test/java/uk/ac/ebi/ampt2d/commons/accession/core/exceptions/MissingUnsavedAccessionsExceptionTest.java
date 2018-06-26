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
package uk.ac.ebi.ampt2d.commons.accession.core.exceptions;

import org.junit.Test;
import uk.ac.ebi.ampt2d.commons.accession.core.AccessionWrapper;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MissingUnsavedAccessionsExceptionTest {

    @Test
    public void testTextGeneration() {
        List<AccessionWrapper<String, String, String>> unsavedAccessions = Arrays.asList(
                new AccessionWrapper<>("2", "HA", "A"),
                new AccessionWrapper<>("3", "HB", "B")
        );
        List<AccessionWrapper<String, String, String>> retrievedAccessions = Arrays.asList(
                new AccessionWrapper<>("1", "HA", "A")
        );
        MissingUnsavedAccessionsException exception =
                new MissingUnsavedAccessionsException(unsavedAccessions, retrievedAccessions);
        assertEquals("Unsaved objects could not be found: [AccessionWrapper{accession=3, hash=HB, version=1}]",
                exception.getMessage());
    }

}
