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
package uk.ac.ebi.ampt2d.commons.accession.persistence.mongodb.document;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.mongodb.core.index.Indexed;
import uk.ac.ebi.ampt2d.commons.accession.persistence.models.IAccessionedObject;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Base class for accessioned objects to be serialized as MongoDB documents.
 * The derived classes must be annotated as Document.
 *
 * @param <MODEL>
 * @param <ACCESSION>
 */
public abstract class AccessionedDocument<MODEL, ACCESSION extends Serializable>
        implements IAccessionedObject<MODEL, String, ACCESSION>, Persistable<String> {

    @Id
    private String hashedMessage;

    @Indexed(background = true)
    private ACCESSION accession;

    private int version;

    @CreatedDate
    private LocalDateTime createdDate;

    protected AccessionedDocument() {
    }

    public AccessionedDocument(String hashedMessage, ACCESSION accession) {
        this(hashedMessage, accession, 1);
    }

    public AccessionedDocument(String hashedMessage, ACCESSION accession, int version) {
        this.hashedMessage = hashedMessage;
        this.accession = accession;
        this.version = version;
    }

    @Override
    public String getId() {
        return hashedMessage;
    }

    @Override
    public boolean isNew() {
        return true;
    }

    @Override
    public ACCESSION getAccession() {
        return accession;
    }

    @Override
    public String getHashedMessage() {
        return hashedMessage;
    }

    @Override
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public int getVersion() {
        return version;
    }

}
