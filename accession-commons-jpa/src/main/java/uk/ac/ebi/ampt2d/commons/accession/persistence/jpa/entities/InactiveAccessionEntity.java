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
package uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.entities;

import uk.ac.ebi.ampt2d.commons.accession.persistence.models.IAccessionedObject;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Base class for inactive accessioned objects to be serialized as relational entities.
 * The derived classes must be annotated as Entity.
 *
 * @param <MODEL> Type of the objects identified by the accessions
 * @param <ACCESSION> Type of the accession that identifies an object of a particular model
 */
@MappedSuperclass
public abstract class InactiveAccessionEntity<MODEL, ACCESSION extends Serializable>
        implements IAccessionedObject<MODEL, String, ACCESSION> {

    @Id
    @GeneratedValue
    private Long id;

    @NotNull
    @Column(nullable = false, updatable = false)
    private String hashedMessage;

    @NotNull
    @Column(nullable = false, updatable = false)
    private ACCESSION accession;

    @Column(nullable = false, updatable = false)
    private int version;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(nullable = false, updatable = false)
    private Long historyId;

    protected InactiveAccessionEntity() {

    }

    public InactiveAccessionEntity(IAccessionedObject<MODEL, String, ACCESSION> object) {
        this.hashedMessage = object.getHashedMessage();
        this.accession = object.getAccession();
        this.version = object.getVersion();
        this.createdDate = object.getCreatedDate();
    }

    public void setHistoryId(Long historyId) {
        this.historyId = historyId;
    }

    @Override
    public String getHashedMessage() {
        return hashedMessage;
    }

    @Override
    public ACCESSION getAccession() {
        return accession;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

}
