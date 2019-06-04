/*
 *
 * Copyright 2019 EMBL - European Bioinformatics Institute
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
package uk.ac.ebi.ampt2d.commons.accession.core.models;

import java.io.Serializable;

public class GetOrCreateAccessionWrapper<MODEL, HASH, ACCESSION> extends AccessionWrapper<MODEL, HASH, ACCESSION> {

    private boolean alreadyCreated;

    public GetOrCreateAccessionWrapper(ACCESSION accession, HASH hash, MODEL data, boolean alreadyCreated) {
        super(accession, hash, data);
        this.alreadyCreated = alreadyCreated;
    }

    public GetOrCreateAccessionWrapper(ACCESSION accession, HASH hash, MODEL data, int version, boolean alreadyCreated) {
        super(accession, hash, data, version);
        this.alreadyCreated = alreadyCreated;
    }

    public static <MODEL, HASH, ACCESSION extends Serializable>
    GetOrCreateAccessionWrapper<MODEL, HASH, ACCESSION> newAccession(
            AccessionWrapper<MODEL, HASH, ACCESSION> wrapper) {
        return new GetOrCreateAccessionWrapper<>(wrapper.getAccession(), wrapper.getHash(), wrapper.getData(),
                wrapper.getVersion(), false);
    }

    public static <MODEL, HASH, ACCESSION extends Serializable>
    GetOrCreateAccessionWrapper<MODEL, HASH, ACCESSION> oldAccession(
            AccessionWrapper<MODEL, HASH, ACCESSION> wrapper) {
        return new GetOrCreateAccessionWrapper<>(wrapper.getAccession(), wrapper.getHash(), wrapper.getData(),
                wrapper.getVersion(), true);
    }

    public boolean isAlreadyCreated() {
        return alreadyCreated;
    }

}
