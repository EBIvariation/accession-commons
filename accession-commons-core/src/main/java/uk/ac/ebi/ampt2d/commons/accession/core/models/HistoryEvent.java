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
package uk.ac.ebi.ampt2d.commons.accession.core.models;

import uk.ac.ebi.ampt2d.commons.accession.persistence.models.IAccessionedObject;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class HistoryEvent<MODEL, ACCESSION> implements IEvent<MODEL, ACCESSION> {

    private EventType eventType;

    private ACCESSION accession;

    private Integer version;

    private ACCESSION mergedInto;

    private ACCESSION splitInto;

    private String reason;

    private LocalDateTime createdDate;

    private List<MODEL> data;

    public HistoryEvent(EventType eventType, ACCESSION accession, Integer version, ACCESSION destinationAccession,
                        String reason, LocalDateTime createdDate, MODEL data) {
        this(eventType, accession, version, destinationAccession, reason, createdDate, Collections.singletonList(data));
    }

    public HistoryEvent(EventType eventType, ACCESSION accession, Integer version, ACCESSION destinationAccession,
                        String reason, LocalDateTime createdDate, List<MODEL> data) {
        this.eventType = eventType;
        this.accession = accession;
        this.version = version;
        if (this.eventType == EventType.MERGED) {
            this.mergedInto = destinationAccession;
        } else if (this.eventType == EventType.RS_SPLIT) {
            this.splitInto = destinationAccession;
        }
        this.reason = reason;
        this.createdDate = createdDate;
        this.data = data;
    }

    @Override
    public EventType getEventType() {
        return eventType;
    }

    @Override
    public ACCESSION getAccession() {
        return accession;
    }

    public Integer getVersion() {
        return version;
    }

    @Override
    public ACCESSION getMergedInto() {
        return mergedInto;
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

    @Override
    public List<? extends IAccessionedObject<MODEL, ?, ACCESSION>> getInactiveObjects() {
        return null;
    }

    public List<MODEL> getData() {
        return data;
    }

    public static <MODEL, ACCESSION> HistoryEvent<MODEL, ACCESSION> created(ACCESSION accession, MODEL model,
                                                                            LocalDateTime createdDate) {
        return new HistoryEvent<>(EventType.CREATED, accession, 1, null, "", createdDate, model);
    }

    public static <MODEL, ACCESSION> HistoryEvent<MODEL, ACCESSION> patch(ACCESSION accession, int version,
                                                                          MODEL model, String reason, LocalDateTime createdDate) {
        return new HistoryEvent<>(EventType.PATCHED, accession, version, null, reason, createdDate, model);
    }

    public static <MODEL, ACCESSION> HistoryEvent<MODEL, ACCESSION> deprecated(ACCESSION accession, String reason,
                                                                               LocalDateTime createdDate) {
        return new HistoryEvent<>(EventType.DEPRECATED, accession, null, null, reason, createdDate, null);
    }

    public static <MODEL, ACCESSION> HistoryEvent<MODEL, ACCESSION> merged(ACCESSION accession, ACCESSION mergedInto,
                                                                           String reason, LocalDateTime createdDate) {
        return new HistoryEvent<>(EventType.MERGED, accession, null, mergedInto, reason, createdDate, null);
    }

    public static <MODEL, ACCESSION> HistoryEvent<MODEL, ACCESSION> split(ACCESSION accession, ACCESSION splitInto,
                                                                          String reason, LocalDateTime createdDate) {
        return new HistoryEvent<>(EventType.RS_SPLIT, accession, null, splitInto, reason, createdDate, null);
    }

    public static <MODEL, ACCESSION> HistoryEvent<MODEL, ACCESSION> updated(ACCESSION accession, int version, MODEL data,
                                                                            String reason, LocalDateTime createdDate) {
        return new HistoryEvent<>(EventType.UPDATED, accession, version, null, reason, createdDate, data);
    }

}
