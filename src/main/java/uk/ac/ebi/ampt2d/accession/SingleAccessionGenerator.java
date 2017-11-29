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

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Base class for accession generators that don't have performance penalty if they don't generate the accessions
 * in batches
 *
 * @param <T> Object class
 */
public abstract class SingleAccessionGenerator<T, U> implements AccessionGenerator<T, U> {

    /**
     * Loops through all the objects to accession and delegates into the 'generateAccession' method the
     * responsibility of generating an accession for each of them.
     *
     * @param objects Set of objects to accession
     * @return A map of objects to unique accessions
     */
    @Override
    public Map<T, U> generateAccessions(Set<T> objects) {
        Map<T, U> accessions = objects.stream().collect(
                Collectors.toMap(Function.identity(), this::generateAccession));

        return accessions;
    }

    /**
     * Generates an accession for a given object.
     *
     * @param object Object to accession
     * @return Accession
     */
    protected abstract U generateAccession(T object);
}
