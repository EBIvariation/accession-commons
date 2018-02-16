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
package uk.ac.ebi.ampt2d.accession.study;

import uk.ac.ebi.ampt2d.accession.AccessionableEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.Size;

@Entity
public class StudyEntity implements AccessionableEntity {

    @Id
    @Column(nullable = false, unique = true, updatable = false)
    @Size(max = 230, min = 0)
    private String accession;

    @Column(nullable = false)
    private String studyTitle;

    @Column(nullable = false)
    private String submitterEmail;

    @Column(nullable = false)
    private String studyType;

    @Column(nullable = false, unique = true)
    private String hashedMessage;

    StudyEntity() {
    }

    public StudyEntity(String studyTitle, String submitterEmail, String studyType, String accession, String hashedMessage) {
        this.studyTitle = studyTitle;
        this.submitterEmail = submitterEmail;
        this.studyType = studyType;
        this.accession = accession;
        this.hashedMessage = hashedMessage;
    }

    public String getAccession() {
        return this.accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public String getStudyTitle() {
        return studyTitle;
    }

    public String getSubmitterEmail() {
        return submitterEmail;
    }

    public String getStudyType() {
        return studyType;
    }
}
