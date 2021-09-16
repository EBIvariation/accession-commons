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
import org.springframework.data.domain.Persistable;
import org.springframework.data.mongodb.core.index.Indexed;
import uk.ac.ebi.ampt2d.commons.accession.core.models.EventType;
import uk.ac.ebi.ampt2d.commons.accession.core.models.IEvent;

import javax.persistence.Id;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Mongo document that represents an operation that changes the state of an accessioned object. The derived classes must
 * be annotated as Entity.
 *
 * @param <MODEL> Type of the objects identified by the accessions
 * @param <ACCESSION> Type of the accession that identifies an object of a particular model
 * @param <INACTIVE_DOCUMENT> Currently inactive accessioned object
 */
public abstract class EventDocument<
        MODEL,
        ACCESSION extends Serializable,
        INACTIVE_DOCUMENT extends InactiveSubDocument<MODEL, ACCESSION>>
        implements IEvent<MODEL, ACCESSION>, Persistable<String> {

    @Id
    private String id;

    private EventType eventType;

    @Indexed(background = true)
    private ACCESSION accession;

    private ACCESSION mergeInto;

    private ACCESSION splitInto;

    private String reason;

    private List<INACTIVE_DOCUMENT> inactiveObjects;

    @CreatedDate
    private LocalDateTime createdDate;

    protected EventDocument() {
    }

    public void fill(EventType eventType, ACCESSION accessionIdOrigin, ACCESSION accessionIdDestiny,
                     String reason, List<INACTIVE_DOCUMENT> inactiveObjects) {
        this.eventType = eventType;
        this.accession = accessionIdOrigin;
        if(eventType==EventType.RS_SPLIT){
            this.splitInto = accessionIdDestiny;
        }else if(eventType==EventType.MERGED){
            this.mergeInto = accessionIdDestiny;
        }
        this.reason = reason;
        this.inactiveObjects = new ArrayList<>();
        if (inactiveObjects != null && !inactiveObjects.isEmpty()) {
            this.inactiveObjects.addAll(inactiveObjects);
        }
    }

    public void fill(EventType eventType, ACCESSION accessionIdOrigin,
                     String reason, List<INACTIVE_DOCUMENT> inactiveObjects) {
       fill(eventType, accessionIdOrigin, null, reason, inactiveObjects);
    }

    public void fill(EventType eventType, String reason, List<INACTIVE_DOCUMENT> inactiveObjects) {
        fill(eventType, null, reason, inactiveObjects);
    }

    @Override
    public EventType getEventType() {
        return eventType;
    }

    @Override
    public ACCESSION getAccession() {
        return accession;
    }

    @Override
    public ACCESSION getMergedInto() {
        return mergeInto;
    }

    @Override
    public ACCESSION getSplitInto() {
        return splitInto;
    }

    @Override
    public String getReason() {
        return reason;
    }

    @Override
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    /**
     * When performing bulk operations the createdDate will not be filled automatically, so the setter is needed
     * in order to assign the date manually
     */
    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return true;
    }

    @Override
    public List<INACTIVE_DOCUMENT> getInactiveObjects() {
        return inactiveObjects;
    }
}
