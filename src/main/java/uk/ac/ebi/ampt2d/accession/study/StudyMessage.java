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

import com.fasterxml.jackson.annotation.JsonIgnore;
import uk.ac.ebi.ampt2d.accession.HashableMessage;
import uk.ac.ebi.ampt2d.accession.Message;

public class StudyMessage implements HashableMessage<String>, Message {

    private String studyTitle;

    private String submitterEmail;

    private String studyType;

    public StudyMessage() {
    }

    public StudyMessage(String studyTitle, String submitterEmail, String studyType) {
        this.studyTitle = studyTitle;
        this.submitterEmail = submitterEmail;
        this.studyType = studyType;
    }

    @Override
    @JsonIgnore
    public String getHashableMessage() {
        return getStudyTitle() + getStudyType() + getSubmitterEmail();
    }

    @Override
    @JsonIgnore
    public String getMessage() {
        return getHashableMessage();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StudyMessage that = (StudyMessage) o;

        return getHashableMessage() != null ? getHashableMessage().equals(that.getHashableMessage()) : that
                .getHashableMessage() == null;
    }

    @Override
    public int hashCode() {
        return getHashableMessage() != null ? getHashableMessage().hashCode() : 0;
    }

    @Override
    public String toString() {
        return "StudyMessage{" +
                "studyTitle='" + studyTitle + '\'' +
                ", submitterEmail='" + submitterEmail + '\'' +
                ", studyType='" + studyType + '\'' +
                '}';
    }
}
