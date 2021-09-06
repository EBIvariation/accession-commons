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
import java.util.List;

/**
 * Definition of an event triggered when an operation is performed over an accessioned object.
 * Some operations could make some objects become inactive.
 *
 * @param <MODEL> Type of the objects identified by the accessions
 * @param <ACCESSION> Type of the accession that identifies an object of a particular model
 */
public interface IEvent<MODEL, ACCESSION> {

    /**
     * @return Accession of the original object
     */
    ACCESSION getAccession();

    /**
     * @return Accession of the target object into the original object accession has been merged
     */
    ACCESSION getMergedInto();

    /**
     * @return Accession of the target object into which the original object accession has split
     */
    ACCESSION getSplitInto();

    /**
     * @return Type of the event like creation, update, etc, executed on the accessioned object
     */
    EventType getEventType();

    /**
     * @return Reason why the object is going through the event
     */
    String getReason();

    /**
     * @return The time at which the event occurred
     */
    LocalDateTime getCreatedDate();

    /**
     * @return List of the objects which are in inactive state as a result of this event
     */
    List<? extends IAccessionedObject<MODEL, ?, ACCESSION>> getInactiveObjects();

}
