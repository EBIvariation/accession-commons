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
package uk.ac.ebi.ampt2d.accession.variant.rest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.ampt2d.accession.common.rest.BasicRestController;
import uk.ac.ebi.ampt2d.accession.variant.VariantAccessioningService;
import uk.ac.ebi.ampt2d.accession.variant.VariantModel;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/v1/variant")
@ConditionalOnProperty(name = "services", havingValue = "variant-accession")
public class VariantAccessioningRestController extends BasicRestController<VariantModel, VariantDTO, Long> {

    public VariantAccessioningRestController(VariantAccessioningService service) {
        super(service, model -> new VariantDTO(model.getAssemblyAccession(), model.getProjectAccession(), model
                .getChromosome(), model.getStart(), model.getType()));
    }

}

