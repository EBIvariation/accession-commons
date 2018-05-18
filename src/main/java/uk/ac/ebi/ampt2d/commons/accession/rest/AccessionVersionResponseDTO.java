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
package uk.ac.ebi.ampt2d.commons.accession.rest;

import uk.ac.ebi.ampt2d.commons.accession.core.ModelWrapper;

import java.util.function.Function;

/**
 * Response containing the object that has been accessioned, as well as additional information like the accession or a
 * flag indicating whether the accession is active. To be used at the REST API layer.
 *
 * @param <DTO>
 * @param <MODEL>
 * @param <HASH>
 * @param <ACCESSION>
 */
public class AccessionVersionResponseDTO<DTO, MODEL, HASH, ACCESSION> {

    private ACCESSION accession;

    private int version;

    private DTO data;

    AccessionVersionResponseDTO() {
    }

    public AccessionVersionResponseDTO(ModelWrapper<MODEL, HASH, ACCESSION> modelWrapper, Function<MODEL, DTO> modelToDto) {
        this.accession = modelWrapper.getAccession();
        this.version = modelWrapper.getVersion();
        this.data = modelToDto.apply(modelWrapper.getData());
    }

    public ACCESSION getAccession() {
        return accession;
    }

    public int getVersion() {
        return version;
    }

    public DTO getData() {
        return data;
    }
}