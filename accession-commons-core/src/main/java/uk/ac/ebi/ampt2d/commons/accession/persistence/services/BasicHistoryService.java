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
package uk.ac.ebi.ampt2d.commons.accession.persistence.services;

import uk.ac.ebi.ampt2d.commons.accession.core.HistoryService;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionDoesNotExistException;
import uk.ac.ebi.ampt2d.commons.accession.core.models.HistoryEvent;
import uk.ac.ebi.ampt2d.commons.accession.core.models.IEvent;
import uk.ac.ebi.ampt2d.commons.accession.persistence.models.IAccessionedObject;
import uk.ac.ebi.ampt2d.commons.accession.persistence.repositories.IAccessionedObjectRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BasicHistoryService<
        MODEL,
        ACCESSION extends Serializable,
        ACCESSION_ENTITY extends IAccessionedObject<MODEL, String, ACCESSION>>
        implements HistoryService<MODEL, ACCESSION> {

    private IAccessionedObjectRepository<ACCESSION_ENTITY, ACCESSION> accessionRepository;

    private InactiveAccessionService<MODEL, ACCESSION, ACCESSION_ENTITY> inactiveAccessionService;

    public BasicHistoryService(
            IAccessionedObjectRepository<ACCESSION_ENTITY, ACCESSION> accessionRepository,
            InactiveAccessionService<MODEL, ACCESSION, ACCESSION_ENTITY> inactiveAccessionService) {
        super();
        this.accessionRepository = accessionRepository;
        this.inactiveAccessionService = inactiveAccessionService;
    }

    @Override
    public List<HistoryEvent<MODEL, ACCESSION>> getHistory(ACCESSION accession) throws AccessionDoesNotExistException {
        final List<ACCESSION_ENTITY> current = accessionRepository.findByAccession(accession);
        final List<? extends IEvent<MODEL, ACCESSION>> operations =
                inactiveAccessionService.getEvents(accession);
        if (current.isEmpty() && operations.isEmpty()) {
            throw new AccessionDoesNotExistException(accession.toString());
        }
        return generateHistory(current, operations);
    }

    protected List<HistoryEvent<MODEL, ACCESSION>> generateHistory(
            List<? extends IAccessionedObject<MODEL, ?, ACCESSION>> current,
            List<? extends IEvent<MODEL, ACCESSION>> history) {
        List<HistoryEvent<MODEL, ACCESSION>> events = new ArrayList<>();
        Map<Integer, IAccessionedObject<MODEL, ?, ACCESSION>> versionMap = mapVersions(current);
        sortOperationsNewToOld(history);

        for (IEvent<MODEL, ACCESSION> operation : history) {
            HistoryEvent<MODEL, ACCESSION> newEvent;
            switch (operation.getEventType()) {
                case DEPRECATED:
                    versionMap = mapVersions(operation.getInactiveObjects());
                    newEvent = HistoryEvent.deprecated(operation.getAccession(), operation.getCreatedDate());
                    break;
                case MERGED:
                    versionMap = mapVersions(operation.getInactiveObjects());
                    newEvent = HistoryEvent.merged(operation.getAccession(),
                            operation.getMergedInto(), operation.getCreatedDate());
                    break;
                case RS_SPLIT:
                    versionMap = mapVersions(operation.getInactiveObjects());
                    newEvent = HistoryEvent.split(operation.getAccession(), operation.getSplitInto(),
                            operation.getCreatedDate());
                    break;
                case UPDATED:
                    IAccessionedObject<MODEL, ?, ACCESSION> dataBeforeUpdate = operation.getInactiveObjects().get(0);
                    int version = dataBeforeUpdate.getVersion();
                    MODEL updateData = versionMap.get(version).getModel();
                    newEvent = HistoryEvent.updated(operation.getAccession(), version, updateData,
                            operation.getCreatedDate());
                    versionMap.put(version, dataBeforeUpdate);
                    break;
                case PATCHED:
                    int lastVersion = versionMap.keySet().size();
                    newEvent = HistoryEvent.patch(operation.getAccession(), lastVersion,
                            versionMap.get(lastVersion).getModel(), operation.getCreatedDate());
                    versionMap.remove(lastVersion);
                    if (lastVersion == 0) {
                        throw new RuntimeException("Error parsing accession history");
                    }
                    break;
                default:
                    throw new RuntimeException("Unrecognized operation type");
            }
            events.add(newEvent);
        }
        IAccessionedObject<MODEL, ?, ACCESSION> accessionedObject = versionMap.get(1);
        events.add(HistoryEvent.created(accessionedObject.getAccession(), versionMap.get(1).getModel(),
                accessionedObject.getCreatedDate()));
        sortEventsOldToNew(events);
        return events;
    }

    private void sortEventsOldToNew(List<HistoryEvent<MODEL, ACCESSION>> events) {
        events.sort(Comparator.comparing(HistoryEvent::getLocalDateTime));
    }

    private void sortOperationsNewToOld(List<? extends IEvent<MODEL, ACCESSION>> operations) {
        operations.sort(Comparator.comparing(IEvent::getCreatedDate, Comparator.reverseOrder()));

    }

    private Map<Integer, IAccessionedObject<MODEL, ?, ACCESSION>> mapVersions(List<? extends
            IAccessionedObject<MODEL, ?, ACCESSION>> current) {
        return current.stream().collect(Collectors.toMap(o -> o.getVersion(), o -> o));
    }

}
