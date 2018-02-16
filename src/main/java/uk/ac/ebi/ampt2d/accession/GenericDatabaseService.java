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

import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class GenericDatabaseService<MESSAGE_TYPE extends Message, ENTITY_TYPE extends AccessionableEntity>
        implements DatabaseService<MESSAGE_TYPE, ENTITY_TYPE> {

    @Autowired
    private AccessioningRepository<ENTITY_TYPE, String> repository;

    @Override
    public Map<MESSAGE_TYPE, String> findObjectsInDB(List<MESSAGE_TYPE> accessionObjects) {
        List<String> hashes = accessionObjects.stream().map(obj ->
                hashMessage(obj.getMessage())).collect(Collectors.toList());
        Collection<ENTITY_TYPE> entities = repository.findByHashedMessageIn(hashes);
        return entities.stream().collect(Collectors.toMap(this::toMessage, AccessionableEntity::getAccession));
    }

    @Override
    public Map<String, MESSAGE_TYPE> getEntitiesFromAccessions(List<String> accessions) {
        Collection<ENTITY_TYPE> entities = repository.findByAccessionIn(accessions);
        return entities.stream().collect(Collectors.toMap(AccessionableEntity::getAccession, this::toMessage));
    }

    @Override
    public void save(Map<MESSAGE_TYPE, String> accessioningObjects) {
        Set<ENTITY_TYPE> entitySet = accessioningObjects.entrySet().stream()
                .map(this::toEntity).collect(Collectors.toSet());
        repository.save(entitySet);
    }
}
