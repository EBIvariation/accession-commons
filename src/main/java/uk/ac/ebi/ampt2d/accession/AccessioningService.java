/*
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
 */
package uk.ac.ebi.ampt2d.accession;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A service that provides accessions for objects
 *
 * @param <MESSAGE_TYPE>
 * @param <ACCESSION_TYPE>
 */
public class AccessioningService<MESSAGE_TYPE, ACCESSION_TYPE> {

    private AccessionGenerator<MESSAGE_TYPE, ACCESSION_TYPE> accessionGenerator;

    private DatabaseService dbService;

    /**
     * Constructs a service that will retrieve existing accessions, and create new ones if necessary.
     *
     * @param accessionGenerator Generator that creates new accessions for not accessioned objects
     */
    public AccessioningService(AccessionGenerator<MESSAGE_TYPE, ACCESSION_TYPE> accessionGenerator, DatabaseService
            dbService) {
        this.accessionGenerator = accessionGenerator;
        this.dbService = dbService;
    }

    /**
     * Get accessions for a list of objects. It looks for the object's accessions in a repository, and it they don't
     * exist, generate new ones, storing them in the repository
     *
     * @param objects List of objects to accession
     * @return Objects to accessions map
     */
    public Map<MESSAGE_TYPE, ACCESSION_TYPE> getAccessions(List<MESSAGE_TYPE> objects) {
        // look for accessions for those objects in the repository
        Map<MESSAGE_TYPE, ACCESSION_TYPE> storedAccessions = this.get(objects);

        // get all objects that are not in the repository
        Set<MESSAGE_TYPE> objectsNotInRepository = objects.stream()
                .filter(object -> !storedAccessions.containsKey(object)).collect(Collectors.toSet());

        if (!objectsNotInRepository.isEmpty()) {
            // generate accessions for all the new objects, adding them to the repository
            Map<MESSAGE_TYPE, ACCESSION_TYPE> newAccessions = accessionGenerator.generateAccessions
                    (objectsNotInRepository);
            this.add(newAccessions);
            storedAccessions.putAll(newAccessions);
        }

        return storedAccessions;
    }

    public Map<MESSAGE_TYPE, ACCESSION_TYPE> get(List<MESSAGE_TYPE> accessionObjects) {
        return dbService.findObjectsInDB(accessionObjects);
    }

    public void add(Map<MESSAGE_TYPE, ACCESSION_TYPE> accessions) {
        dbService.save(accessions);
    }

    public Map<ACCESSION_TYPE, MESSAGE_TYPE> getObjectsFromAccessions(List<ACCESSION_TYPE> accessions) {
        return dbService.getEntitiesFromAccessions(accessions);
    }
}
