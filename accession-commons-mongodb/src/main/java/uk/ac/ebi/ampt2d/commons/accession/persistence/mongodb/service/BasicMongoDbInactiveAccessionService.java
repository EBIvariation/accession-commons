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
package uk.ac.ebi.ampt2d.commons.accession.persistence.mongodb.service;

import uk.ac.ebi.ampt2d.commons.accession.core.models.EventType;
import uk.ac.ebi.ampt2d.commons.accession.persistence.services.BasicInactiveAccessionService;
import uk.ac.ebi.ampt2d.commons.accession.persistence.models.IAccessionedObject;
import uk.ac.ebi.ampt2d.commons.accession.persistence.repositories.IHistoryRepository;
import uk.ac.ebi.ampt2d.commons.accession.core.models.IEvent;
import uk.ac.ebi.ampt2d.commons.accession.persistence.mongodb.document.InactiveSubDocument;
import uk.ac.ebi.ampt2d.commons.accession.persistence.mongodb.document.EventDocument;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class BasicMongoDbInactiveAccessionService<
        MODEL,
        ACCESSION extends Serializable,
        ACCESSION_ENTITY extends IAccessionedObject<MODEL, ?, ACCESSION>,
        ACCESSION_INACTIVE_ENTITY extends InactiveSubDocument<MODEL, ACCESSION>,
        OPERATION_ENTITY extends EventDocument<MODEL, ACCESSION, ACCESSION_INACTIVE_ENTITY>>
        extends BasicInactiveAccessionService<MODEL, ACCESSION, ACCESSION_ENTITY, ACCESSION_INACTIVE_ENTITY> {

    private IHistoryRepository<ACCESSION, OPERATION_ENTITY, String> historyRepository;

    private Supplier<OPERATION_ENTITY> supplier;

    public BasicMongoDbInactiveAccessionService(
            IHistoryRepository<ACCESSION, OPERATION_ENTITY, String> historyRepository,
            Function<ACCESSION_ENTITY, ACCESSION_INACTIVE_ENTITY> toInactiveEntity,
            Supplier<OPERATION_ENTITY> supplier) {
        super(toInactiveEntity);
        this.historyRepository = historyRepository;
        this.supplier = supplier;
    }

    @Override
    protected void saveHistory(EventType type, ACCESSION accession, ACCESSION mergeInto, String reason,
                               List<ACCESSION_INACTIVE_ENTITY> entities) {
        OPERATION_ENTITY operation = supplier.get();
        operation.fill(type, accession, mergeInto, reason, entities);
        historyRepository.save(operation);
    }

    @Override
    public Optional<EventType> getLastEventType(ACCESSION accession) {
        final OPERATION_ENTITY lastEvent = historyRepository.findTopByAccessionOrderByCreatedDateDesc(accession);
        if (lastEvent != null) {
            return Optional.of(lastEvent.getEventType());
        }
        return Optional.empty();
    }

    @Override
    public IEvent<MODEL, ACCESSION> getLastEvent(ACCESSION accession) {
        return historyRepository.findTopByAccessionOrderByCreatedDateDesc(accession);
    }

    @Override
    public List<? extends IEvent<MODEL, ACCESSION>> getEvents(ACCESSION accession) {
        return historyRepository.findAllByAccession(accession);
    }

    @Override
    public List<? extends IEvent<MODEL, ACCESSION>> getAllEventsInvolvedIn(ACCESSION accession) {
        return historyRepository.findAllInvolvedIn(accession);
    }
}
