/*
 *
 * Copyright 2018 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this sample except in compliance with the License.
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
package uk.ac.ebi.ampt2d.accession.sample;

import uk.ac.ebi.ampt2d.accession.GenericDatabaseService;

import java.util.Map;

public class SampleAccessioningDatabaseService extends GenericDatabaseService<SampleMessage, SampleEntity> {

    @Override
    public SampleMessage toMessage(SampleEntity sampleEntity) {
        return new SampleMessage(sampleEntity.getSampleAlias(), sampleEntity.getStudyAccession());
    }

    @Override
    public SampleEntity toEntity(Map.Entry<SampleMessage, String> entry) {
        return new SampleEntity(entry.getKey().getSampleAlias(), entry.getKey().getStudyAccession(),
                hashMessage(entry.getKey().getHashableMessage()), entry.getValue());
    }

    @Override
    public String hashMessage(String message) {
        return message;
    }
}
