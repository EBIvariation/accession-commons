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
package uk.ac.ebi.ampt2d.commons.accession.rest.controllers;

import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ebi.ampt2d.commons.accession.core.AccessioningService;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionCouldNotBeGeneratedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionDeprecatedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionDoesNotExistException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionMergedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.HashAlreadyExistsException;
import uk.ac.ebi.ampt2d.commons.accession.rest.dto.AccessionResponseDTO;
import uk.ac.ebi.ampt2d.commons.accession.rest.dto.AccessionVersionsResponseDTO;
import uk.ac.ebi.ampt2d.commons.accession.rest.dto.GetOrCreateAccessionResponseDTO;

import jakarta.validation.Valid;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BasicRestController<DTO extends MODEL, MODEL, HASH, ACCESSION> {

    private AccessioningService<MODEL, HASH, ACCESSION> service;
    private Function<MODEL, DTO> modelToDTO;

    public BasicRestController(AccessioningService<MODEL, HASH, ACCESSION> service,
                               Function<MODEL, DTO> modelToDTO) {
        this.service = service;
        this.modelToDTO = modelToDTO;
    }

    protected AccessioningService<MODEL, HASH, ACCESSION> getService() {
        return service;
    }

    protected Function<MODEL, DTO> getModelToDTO() {
        return modelToDTO;
    }

    @RequestMapping(value = "/{applicationInstanceId}", method = RequestMethod.POST, produces = "application/json",
            consumes = "application/json")
    public List<GetOrCreateAccessionResponseDTO<DTO, MODEL, HASH, ACCESSION>> generateAccessions(
            @PathVariable String applicationInstanceId, @RequestBody @Valid List<DTO> dtos)
            throws AccessionCouldNotBeGeneratedException {
        return service.getOrCreate(dtos, applicationInstanceId).stream()
                .map(accessionModel -> new GetOrCreateAccessionResponseDTO<>(accessionModel, modelToDTO))
                .collect(Collectors.toList());
    }

    @RequestMapping(value = "/{accession}", method = RequestMethod.GET, produces = "application/json")
    public AccessionResponseDTO<DTO, MODEL, HASH, ACCESSION> get(@PathVariable ACCESSION accession)
            throws AccessionDoesNotExistException, AccessionMergedException, AccessionDeprecatedException {
        return new AccessionResponseDTO(service.getByAccession(accession), modelToDTO);
    }

    @RequestMapping(value = "/{accession}", method = RequestMethod.PATCH, produces = "application/json",
            consumes = "application/json")
    public AccessionVersionsResponseDTO<DTO, MODEL, HASH, ACCESSION> patch(@PathVariable ACCESSION accession,
                                                                           @RequestBody @Valid DTO dto)
            throws AccessionDoesNotExistException, HashAlreadyExistsException, AccessionMergedException,
            AccessionDeprecatedException {
        return new AccessionVersionsResponseDTO<>(service.patch(accession, dto), modelToDTO);
    }

    @RequestMapping(value = "/{accession}/{version}", method = RequestMethod.POST, produces = "application/json",
            consumes = "application/json")
    public AccessionVersionsResponseDTO<DTO, MODEL, HASH, ACCESSION> update(@PathVariable ACCESSION accession,
                                                                            @PathVariable int version,
                                                                            @RequestBody @Valid DTO dto)
            throws AccessionDoesNotExistException, HashAlreadyExistsException, AccessionMergedException,
            AccessionDeprecatedException {
        return new AccessionVersionsResponseDTO<>(service.update(accession, version, dto), modelToDTO);
    }

    @RequestMapping(value = "/{accession}/{version}", method = RequestMethod.GET, produces = "application/json")
    public AccessionResponseDTO<DTO, MODEL, HASH, ACCESSION> getVersion(@PathVariable ACCESSION accession,
                                                                        @PathVariable int version)
            throws AccessionDoesNotExistException, AccessionDeprecatedException, AccessionMergedException {
        return new AccessionResponseDTO<>(service.getByAccessionAndVersion(accession, version), modelToDTO);
    }

    @RequestMapping(value = "/{accession}", method = RequestMethod.DELETE, produces = "application/json")
    public void getVersion(@PathVariable ACCESSION accession,
                           @RequestParam(required = false, defaultValue = "Deprecated") String reason)
            throws AccessionDoesNotExistException, AccessionDeprecatedException, AccessionMergedException {
        service.deprecate(accession, reason);
    }

    @RequestMapping(value = "/{accession}/merge", method = RequestMethod.POST, produces = "application/json")
    public void merge(@PathVariable("accession") ACCESSION accessionOrigin,
                      @RequestParam("mergeInto") ACCESSION mergeInto,
                      @RequestParam(name = "reason", required = false, defaultValue = "Merge") String reason)
            throws AccessionDoesNotExistException, AccessionDeprecatedException, AccessionMergedException, IllegalArgumentException {
        Assert.isTrue(!accessionOrigin.toString().equals(mergeInto), "Accessions cannot be self merged");
        service.merge(accessionOrigin, mergeInto, reason);
    }

}