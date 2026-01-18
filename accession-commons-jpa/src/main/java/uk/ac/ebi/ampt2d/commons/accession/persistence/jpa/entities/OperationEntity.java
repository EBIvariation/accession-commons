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

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import uk.ac.ebi.ampt2d.commons.accession.core.models.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;

/**
 * Entity that represents an operation that changes the state of an accessioned object. The derived classes must
 * be annotated as Entity.
 *
 * @param <ACCESSION> Type of the accession that identifies an object of a particular model
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class OperationEntity<ACCESSION> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private ACCESSION accession;

    @Column
    private ACCESSION mergeInto;

    @Column
    private ACCESSION splitInto;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;

    @Column(nullable = false, length = 2000)
    private String reason;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdDate;

    public Long getId() {
        return id;
    }

    public ACCESSION getAccession() {
        return accession;
    }

    public ACCESSION getMergeInto() {
        return mergeInto;
    }

    public ACCESSION getSplitInto() {
        return splitInto;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void fill(EventType type, ACCESSION origin, ACCESSION destination, String reason) {
        this.eventType = type;
        this.accession = origin;
        if (type == EventType.MERGED) {
            this.mergeInto = destination;
        } else if (type == EventType.RS_SPLIT) {
            this.splitInto = destination;
        }
        this.reason = reason;
    }
}