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

import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.ampt2d.commons.accession.core.models.EventType;
import uk.ac.ebi.ampt2d.commons.accession.core.models.IEvent;
import uk.ac.ebi.ampt2d.commons.accession.persistence.models.IAccessionedObject;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InactiveAccessionService<
        MODEL,
        ACCESSION extends Serializable,
        ACCESSION_ENTITY extends IAccessionedObject<MODEL, ?, ACCESSION>> {

    @Transactional
    void update(ACCESSION_ENTITY entity, String reason);

    @Transactional
    void patch(ACCESSION accession, String reason);

    @Transactional
    void deprecate(ACCESSION accession, Collection<ACCESSION_ENTITY> entities, String reason);

    @Transactional
    void merge(ACCESSION accessionOrigin, ACCESSION accession, List<ACCESSION_ENTITY> entities, String reason);

    Optional<EventType> getLastEventType(ACCESSION accession);

    IEvent<MODEL, ACCESSION> getLastEvent(ACCESSION accession);

    List<? extends IEvent<MODEL, ACCESSION>> getEvents(ACCESSION accession);

    List<? extends IEvent<MODEL, ACCESSION>> getAllEventsInvolvedIn(ACCESSION accession);
}
