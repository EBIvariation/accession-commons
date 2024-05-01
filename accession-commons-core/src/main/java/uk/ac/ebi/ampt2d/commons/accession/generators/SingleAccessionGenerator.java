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

import uk.ac.ebi.ampt2d.commons.accession.core.models.AccessionWrapper;
import uk.ac.ebi.ampt2d.commons.accession.core.models.SaveResponse;
import uk.ac.ebi.ampt2d.commons.accession.hashing.SHA1HashingFunction;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Base class for accession generators that don't suffer performance penalty if they don't generate the accessions
 * in batches
 *
 * @param <MODEL> Type of the objects identified by the accessions
 * @param <ACCESSION> Type of the accession that identifies an object of a particular model
 */
public class SingleAccessionGenerator<MODEL, ACCESSION> implements AccessionGenerator<MODEL, ACCESSION> {

    private final Function<MODEL, ACCESSION> generateAccessionFunction;

    public SingleAccessionGenerator(Function<MODEL, ACCESSION> generateAccessionFunction) {
        this.generateAccessionFunction = generateAccessionFunction;
    }

    @Override
    public <HASH> List<AccessionWrapper<MODEL, HASH, ACCESSION>> generateAccessions(Map<HASH, MODEL> messages, String applicationInstanceId) {
        return messages.entrySet()
                .stream()
                .map(entry -> new AccessionWrapper<>(generateAccessionFunction.apply(entry.getValue()), entry.getKey(),
                        entry.getValue()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public void postSave(SaveResponse<ACCESSION> response) {
        // No action performed, as all the accessions are generated on the fly.
    }

    public static <MODEL, ACCESSION extends Serializable> SingleAccessionGenerator<MODEL, ACCESSION>
    ofHashAccessionGenerator(Function<MODEL, String> summaryFunction, Function<String, ACCESSION> hashingFunction) {
        return new SingleAccessionGenerator<>(message -> summaryFunction.andThen(hashingFunction).apply(message));
    }

    public static <MODEL> SingleAccessionGenerator<MODEL, String> ofSHA1AccessionGenerator(
            Function<MODEL, String> summaryFunction) {
        return ofHashAccessionGenerator(summaryFunction, new SHA1HashingFunction());
    }

    @Override
    public void shutDownAccessionGenerator() {
        // Do nothing - no resources to release
    }
}
