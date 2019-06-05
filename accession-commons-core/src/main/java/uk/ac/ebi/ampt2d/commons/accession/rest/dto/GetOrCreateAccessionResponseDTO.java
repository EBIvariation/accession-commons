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
package uk.ac.ebi.ampt2d.commons.accession.rest.dto;

import uk.ac.ebi.ampt2d.commons.accession.core.models.GetOrCreateAccessionWrapper;

import java.util.function.Function;

public class GetOrCreateAccessionResponseDTO<DTO, MODEL, HASH, ACCESSION> extends
        AccessionResponseDTO<DTO, MODEL, HASH, ACCESSION> {

    public boolean newAccession;

    public GetOrCreateAccessionResponseDTO(GetOrCreateAccessionWrapper<MODEL, HASH, ACCESSION> accessionWrapper,
                                           Function<MODEL, DTO> modelToDto) {
        super(accessionWrapper, modelToDto);
        this.newAccession = accessionWrapper.isNewAccession();
    }

    public boolean isNewAccession() {
        return newAccession;
    }

}
