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
package uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.service;

import uk.ac.ebi.ampt2d.commons.accession.core.models.EventType;
import uk.ac.ebi.ampt2d.commons.accession.persistence.services.BasicInactiveAccessionService;
import uk.ac.ebi.ampt2d.commons.accession.persistence.models.IAccessionedObject;
import uk.ac.ebi.ampt2d.commons.accession.persistence.repositories.IHistoryRepository;
import uk.ac.ebi.ampt2d.commons.accession.core.models.IEvent;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.repositories.InactiveAccessionRepository;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.entities.InactiveAccessionEntity;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.entities.OperationEntity;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.models.JpaEvent;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class BasicJpaInactiveAccessionService<
        MODEL,
        ACCESSION extends Serializable,
        ACCESSION_ENTITY extends IAccessionedObject<MODEL, ?, ACCESSION>,
        ACCESSION_INACTIVE_ENTITY extends InactiveAccessionEntity<MODEL, ACCESSION>,
        OPERATION_ENTITY extends OperationEntity<ACCESSION>>
        extends
        BasicInactiveAccessionService<MODEL, ACCESSION, ACCESSION_ENTITY, ACCESSION_INACTIVE_ENTITY> {

    private final IHistoryRepository<ACCESSION, OPERATION_ENTITY, ?> historyRepository;

    private final InactiveAccessionRepository<ACCESSION_INACTIVE_ENTITY> inactiveAccessionRepository;

    private final Supplier<OPERATION_ENTITY> historyEntitySupplier;


    public BasicJpaInactiveAccessionService(IHistoryRepository<ACCESSION, OPERATION_ENTITY, ?> historyRepository,
                                            Function<ACCESSION_ENTITY, ACCESSION_INACTIVE_ENTITY> toInactiveEntity,
                                            InactiveAccessionRepository<ACCESSION_INACTIVE_ENTITY>
                                                    inactiveAccessionRepository,
                                            Supplier<OPERATION_ENTITY> historyEntitySupplier) {
        super(toInactiveEntity);
        this.historyRepository = historyRepository;
        this.inactiveAccessionRepository = inactiveAccessionRepository;
        this.historyEntitySupplier = historyEntitySupplier;
    }

    @Override
    protected void saveHistory(EventType type, ACCESSION accession, ACCESSION mergeInto, String reason,
                               List<ACCESSION_INACTIVE_ENTITY> accessionInactiveEntities) {
        OPERATION_ENTITY operation = historyEntitySupplier.get();
        operation.fill(type, accession, mergeInto, reason);
        final OPERATION_ENTITY savedOperation = historyRepository.save(operation);
        if(accessionInactiveEntities!=null) {
            accessionInactiveEntities.forEach(entity -> entity.setHistoryId(savedOperation.getId()));
            inactiveAccessionRepository.saveAll(accessionInactiveEntities);
        }

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
        OperationEntity<ACCESSION> lastOperation = historyRepository.findTopByAccessionOrderByCreatedDateDesc(accession);
        return toJpaOperation(lastOperation);
    }

    private IEvent<MODEL, ACCESSION> toJpaOperation(OperationEntity<ACCESSION> lastOperation) {
        return new JpaEvent(lastOperation, inactiveAccessionRepository.findAllByHistoryId(lastOperation.getId()));
    }

    @Override
    public List<IEvent<MODEL, ACCESSION>> getEvents(ACCESSION accession) {
        final List<OPERATION_ENTITY> operations = historyRepository.findAllByAccession(accession);
        return operations.stream().map(this::toJpaOperation).collect(Collectors.toList());
    }

}
