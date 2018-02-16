/*
 *
 * Copyright 2018 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this variant except in compliance with the License.
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
package uk.ac.ebi.ampt2d.accession.variant;

import uk.ac.ebi.ampt2d.accession.GenericDatabaseService;

import java.util.Map;

public class VariantAccessioningDatabaseService extends GenericDatabaseService<VariantMessage, VariantEntity> {

    @Override
    public VariantMessage toMessage(VariantEntity variantEntity) {
        return new VariantMessage(variantEntity.getAssemblyAccession(), variantEntity
                .getProjectAccession(), variantEntity.getChromosome(), variantEntity.getStart(), variantEntity.getType());
    }

    @Override
    public VariantEntity toEntity(Map.Entry<VariantMessage, String> entry) {
        VariantEntity variantEntity = new VariantEntity(entry.getKey().getAssemblyAccession(), entry.getKey()
                .getProjectAccession(),
                entry.getKey()
                        .getChromosome(), entry.getKey().getStart(), entry.getKey().getType());
        variantEntity.setAccession(entry.getValue());
        variantEntity.setHashedMessage(hashMessage(entry.getKey().getHashableMessage()));

        return variantEntity;
    }

    @Override
    public String hashMessage(String message) {
        return message;
    }
}
