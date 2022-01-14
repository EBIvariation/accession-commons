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
package uk.ac.ebi.ampt2d.commons.accession.rest.dto;

import uk.ac.ebi.ampt2d.commons.accession.core.models.EventType;
import uk.ac.ebi.ampt2d.commons.accession.core.models.HistoryEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

public class HistoryEventDTO<ACCESSION, DTO> {

    private EventType type;

    private ACCESSION accession;

    private Integer version;

    private ACCESSION splitInto;

    private ACCESSION mergedInto;

    private String reason;

    private LocalDateTime createdDate;

    private List<DTO> data;

    public HistoryEventDTO() {

    }

    public <MODEL> HistoryEventDTO(HistoryEvent<MODEL, ACCESSION> event, Function<List<MODEL>, List<DTO>> modelToDTO) {
        this.type = event.getEventType();
        this.accession = event.getAccession();
        if (event.getEventType() == EventType.MERGED) {
            this.mergedInto = event.getMergedInto();
        } else if (event.getEventType() == EventType.RS_SPLIT) {
            this.splitInto = event.getSplitInto();
        }
        this.mergedInto = event.getMergedInto();
        this.reason = event.getReason();
        this.createdDate = event.getCreatedDate();
        this.data = modelToDTO.apply(event.getData());
    }

    public EventType getType() {
        return type;
    }

    public ACCESSION getAccession() {
        return accession;
    }

    public Integer getVersion() {
        return version;
    }

    public ACCESSION getMergedInto() {
        return mergedInto;
    }

    public ACCESSION getSplitInto() {
        return splitInto;
    }

    public ACCESSION getDestinationAccession() {
        if (this.type == EventType.MERGED) {
            return mergedInto;
        } else if (this.type == EventType.RS_SPLIT) {
            return splitInto;
        } else {
            return null;
        }
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public List<DTO> getData() {
        return data;
    }
}
