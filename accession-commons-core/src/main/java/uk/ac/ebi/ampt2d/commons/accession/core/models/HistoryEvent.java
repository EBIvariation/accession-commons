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

import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import uk.ac.ebi.ampt2d.commons.accession.persistence.models.IAccessionedObject;

import java.time.LocalDateTime;
import java.util.List;

public class HistoryEvent<MODEL, ACCESSION> implements IEvent<MODEL, ACCESSION> {

    private EventType eventType;

    private ACCESSION accession;

    private Integer version;

    private ACCESSION mergedInto;

    private LocalDateTime localDateTime;

    private MODEL data;

    public HistoryEvent(EventType eventType, ACCESSION accession, Integer version, ACCESSION mergedInto,
                        LocalDateTime localDateTime, MODEL data) {
        this.eventType = eventType;
        this.accession = accession;
        this.version = version;
        this.mergedInto = mergedInto;
        this.localDateTime = localDateTime;
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

    public ACCESSION getMergedInto() {
        return mergedInto;
    }

    @Override
    public ACCESSION getSplitInto() {
        throw new NotImplementedException();
    }

    @Override
    public String getReason() {
        return null;
    }

    @Override
    public LocalDateTime getCreatedDate() {
        return null;
    }

    @Override
    public List<? extends IAccessionedObject<MODEL, ?, ACCESSION>> getInactiveObjects() {
        return null;
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public MODEL getData() {
        return data;
    }

    public static <MODEL, ACCESSION> HistoryEvent<MODEL, ACCESSION> created(ACCESSION accession, MODEL model,
                                                                            LocalDateTime localDateTime) {
        return new HistoryEvent<>(EventType.CREATED, accession, 1, null, localDateTime, model);
    }

    public static <MODEL, ACCESSION> HistoryEvent<MODEL, ACCESSION> patch(ACCESSION accession, int version,
                                                                          MODEL model, LocalDateTime localDateTime) {
        return new HistoryEvent<>(EventType.PATCHED, accession, version, null, localDateTime, model);
    }

    public static <MODEL, ACCESSION> HistoryEvent<MODEL, ACCESSION> deprecated(ACCESSION accession,
                                                                               LocalDateTime localDateTime) {
        return new HistoryEvent<>(EventType.DEPRECATED, accession, null, null, localDateTime, null);
    }

    public static <MODEL, ACCESSION> HistoryEvent<MODEL, ACCESSION> merged(ACCESSION accession, ACCESSION mergedInto,
                                                                           LocalDateTime localDateTime) {
        return new HistoryEvent<>(EventType.MERGED, accession, null, mergedInto, localDateTime, null);
    }

    public static <MODEL, ACCESSION> HistoryEvent<MODEL, ACCESSION> updated(ACCESSION accession, int version,
                                                                            MODEL data, LocalDateTime localDateTime) {
        return new HistoryEvent<>(EventType.UPDATED, accession, version, null, localDateTime, data);
    }

}
