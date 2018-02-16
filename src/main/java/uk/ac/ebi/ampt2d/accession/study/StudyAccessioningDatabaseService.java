/*
 *
 * Copyright 2018 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this study except in compliance with the License.
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
package uk.ac.ebi.ampt2d.accession.study;

import uk.ac.ebi.ampt2d.accession.GenericDatabaseService;

import java.util.Map;

public class StudyAccessioningDatabaseService extends GenericDatabaseService<StudyMessage, StudyEntity> {

    @Override
    public StudyMessage toMessage(StudyEntity studyEntity) {
        return new StudyMessage(studyEntity.getStudyTitle(), studyEntity.getStudyType(), studyEntity.getSubmitterEmail());
    }

    @Override
    public StudyEntity toEntity(Map.Entry<StudyMessage, String> entry) {
        return new StudyEntity(entry.getKey().getStudyTitle(), entry.getKey().getStudyType(), entry.getKey()
                .getSubmitterEmail(), entry.getValue(), hashMessage(entry.getKey().getHashableMessage()));
    }

    @Override
    public String hashMessage(String message) {
        return message;
    }
}
