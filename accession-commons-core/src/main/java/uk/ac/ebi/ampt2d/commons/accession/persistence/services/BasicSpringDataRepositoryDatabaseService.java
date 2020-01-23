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

import uk.ac.ebi.ampt2d.commons.accession.core.DatabaseService;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionDeprecatedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionDoesNotExistException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionMergedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.HashAlreadyExistsException;
import uk.ac.ebi.ampt2d.commons.accession.core.models.AccessionVersionsWrapper;
import uk.ac.ebi.ampt2d.commons.accession.core.models.AccessionWrapper;
import uk.ac.ebi.ampt2d.commons.accession.core.models.EventType;
import uk.ac.ebi.ampt2d.commons.accession.core.models.SaveResponse;
import uk.ac.ebi.ampt2d.commons.accession.persistence.models.IAccessionedObject;
import uk.ac.ebi.ampt2d.commons.accession.persistence.repositories.IAccessionedObjectRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Basic implementation of {@link DatabaseService} that requires a Spring Data repository that extends
 * {@link IAccessionedObjectRepository}, a function to generate the entities from the triple model/hash/accession,
 * a function to get the accession from the entity and a function to get the hashed representation of the message
 * from the entity.
 *
 * @param <MODEL> Type of the objects identified by the accessions
 * @param <ACCESSION_ENTITY> Type of entity object
 * @param <ACCESSION> Type of the accession that identifies an object of a particular model
 */
public class BasicSpringDataRepositoryDatabaseService<
        MODEL,
        ACCESSION extends Serializable,
        ACCESSION_ENTITY extends IAccessionedObject<MODEL, String, ACCESSION>>
        implements DatabaseService<MODEL, String, ACCESSION> {

    private final IAccessionedObjectRepository<ACCESSION_ENTITY, ACCESSION> repository;

    private final Function<AccessionWrapper<MODEL, String, ACCESSION>, ACCESSION_ENTITY> toEntityFunction;

    private final InactiveAccessionService<MODEL, ACCESSION, ACCESSION_ENTITY> inactiveAccessionService;

    public BasicSpringDataRepositoryDatabaseService(
            IAccessionedObjectRepository<ACCESSION_ENTITY, ACCESSION> repository,
            Function<AccessionWrapper<MODEL, String, ACCESSION>, ACCESSION_ENTITY> toEntityFunction,
            InactiveAccessionService<MODEL, ACCESSION, ACCESSION_ENTITY> inactiveAccessionService) {
        this.repository = repository;
        this.toEntityFunction = toEntityFunction;
        this.inactiveAccessionService = inactiveAccessionService;
    }

    @Override
    public List<AccessionWrapper<MODEL, String, ACCESSION>> findAllByHash(Collection<String> hashes) {
        List<AccessionWrapper<MODEL, String, ACCESSION>> wrappedAccessions = new ArrayList<>();
        repository.findAllById(hashes).iterator().forEachRemaining(
                entity -> wrappedAccessions.add(toModelWrapper(entity)));
        return wrappedAccessions;
    }

    private AccessionWrapper<MODEL, String, ACCESSION> toModelWrapper(ACCESSION_ENTITY entity) {
        return new AccessionWrapper<>(entity.getAccession(), entity.getHashedMessage(), entity.getModel(),
                entity.getVersion());
    }

    @Override
    public AccessionVersionsWrapper<MODEL, String, ACCESSION> findByAccession(ACCESSION accession)
            throws AccessionDoesNotExistException, AccessionMergedException, AccessionDeprecatedException {
        List<ACCESSION_ENTITY> entities = repository.findByAccession(accession);
        checkAccessionIsActive(entities, accession);
        return toAccessionWrapper(entities);
    }

    private void checkAccessionIsActive(List<ACCESSION_ENTITY> entities, ACCESSION accession)
            throws AccessionDoesNotExistException, AccessionMergedException, AccessionDeprecatedException {
        if (entities == null || entities.isEmpty()) {
            checkAccessionNotMergedOrDeprecated(accession);
        }
    }

    private void checkAccessionNotMergedOrDeprecated(ACCESSION accession) throws AccessionDoesNotExistException,
            AccessionMergedException, AccessionDeprecatedException {
        EventType eventType = inactiveAccessionService.getLastEventType(accession).orElseThrow(() -> new
                AccessionDoesNotExistException(accession.toString()));
        switch (eventType) {
            case MERGED:
                throw new AccessionMergedException(accession.toString(),
                        inactiveAccessionService.getLastEvent(accession).getMergedInto().toString());
            case DEPRECATED:
                throw new AccessionDeprecatedException(accession.toString());
        }
    }

    private AccessionVersionsWrapper<MODEL, String, ACCESSION> toAccessionWrapper(List<ACCESSION_ENTITY> entities) {
        final List<AccessionWrapper<MODEL, String, ACCESSION>> models = entities.stream().map(this::toModelWrapper)
                .collect(Collectors.toList());
        return new AccessionVersionsWrapper<>(models);
    }

    @Override
    public AccessionWrapper<MODEL, String, ACCESSION> findLastVersionByAccession(ACCESSION accession)
            throws AccessionDoesNotExistException, AccessionMergedException, AccessionDeprecatedException {
        final List<ACCESSION_ENTITY> acessionEntities = repository.findByAccession(accession);
        checkAccessionIsActive(acessionEntities, accession);
        return toModelWrapper(filterOldVersions(acessionEntities));
    }

    private ACCESSION_ENTITY filterOldVersions(List<ACCESSION_ENTITY> accessionedElements) {
        int maxVersion = 1;
        ACCESSION_ENTITY lastVersionEntity = null;
        for (ACCESSION_ENTITY element : accessionedElements) {
            if (element.getVersion() >= maxVersion) {
                maxVersion = element.getVersion();
                lastVersionEntity = element;
            }
        }

        return lastVersionEntity;
    }

    @Override
    public AccessionWrapper<MODEL, String, ACCESSION> findByAccessionVersion(ACCESSION accession, int version)
            throws AccessionDoesNotExistException, AccessionDeprecatedException, AccessionMergedException {
        ACCESSION_ENTITY result = doFindAccessionVersion(accession, version);
        return toModelWrapper(result);
    }

    @Override
    public SaveResponse<ACCESSION> save(List<AccessionWrapper<MODEL, String, ACCESSION>> objects) {
        return repository.insert(toEntities(objects));
    }

    private List<ACCESSION_ENTITY> toEntities(List<AccessionWrapper<MODEL, String, ACCESSION>> partitionToSave) {
        return partitionToSave.stream().map(toEntityFunction).collect(Collectors.toList());
    }

    private ACCESSION_ENTITY doFindAccessionVersion(ACCESSION accession, int version) throws
            AccessionDoesNotExistException, AccessionMergedException, AccessionDeprecatedException {
        ACCESSION_ENTITY result = repository.findByAccessionAndVersion(accession, version);
        if (result == null) {
            checkAccessionNotMergedOrDeprecated(accession);
            // Accession does exist, but the version does not.
            throw new AccessionDoesNotExistException(accession.toString(), version);
        }
        return result;
    }

    @Override
    public AccessionVersionsWrapper<MODEL, String, ACCESSION> patch(ACCESSION accession, String hash, MODEL model,
                                                                    String reason)
            throws AccessionDoesNotExistException, HashAlreadyExistsException, AccessionDeprecatedException,
            AccessionMergedException {
        List<ACCESSION_ENTITY> entities = getAccession(accession);
        checkHashDoesNotExist(hash);
        int maxVersion = 1;
        for (ACCESSION_ENTITY entity : entities) {
            if (entity.getVersion() >= maxVersion) {
                maxVersion = entity.getVersion();
            }
        }
        maxVersion = maxVersion + 1;
        checkedInsert(accession, hash, model, maxVersion);
        inactiveAccessionService.patch(accession, reason);
        return findByAccession(accession);
    }

    private void checkedInsert(ACCESSION accession, String hash, MODEL model, int maxVersion)
            throws HashAlreadyExistsException {
        try {
            repository.insert(Arrays.asList(toEntityFunction.apply(
                    new AccessionWrapper<>(accession, hash, model, maxVersion))));
        } catch (RuntimeException e) {
            checkHashDoesNotExist(hash);
            throw e;
        }
    }

    private void checkHashDoesNotExist(String hash)
            throws HashAlreadyExistsException {
        final ACCESSION_ENTITY dbAccession = repository.findById(hash).orElse(null);
        if (dbAccession != null) {
            throw new HashAlreadyExistsException(dbAccession.getHashedMessage(), dbAccession.getAccession());
        }
    }

    /**
     * @param accessionId Accession that identifies the object
     * @return All entries of an accession. It is never empty
     * @throws AccessionDoesNotExistException when the accession has never existed
     * @throws AccessionDeprecatedException when the accession exists but has been deprecated
     * @throws AccessionMergedException when the accession exists but has been merged into another accession
     */
    private List<ACCESSION_ENTITY> getAccession(ACCESSION accessionId)
            throws AccessionDoesNotExistException, AccessionDeprecatedException, AccessionMergedException {
        List<ACCESSION_ENTITY> accessionedElements = repository.findByAccession(accessionId);
        checkAccessionIsActive(accessionedElements, accessionId);
        return accessionedElements;
    }

    @Override
    public AccessionVersionsWrapper<MODEL, String, ACCESSION> update(ACCESSION accession, String hash, MODEL model, int version)
            throws AccessionDoesNotExistException, HashAlreadyExistsException, AccessionMergedException,
            AccessionDeprecatedException {
        ACCESSION_ENTITY oldVersion = doFindAccessionVersion(accession, version);
        checkHashDoesNotExist(hash);

        inactiveAccessionService.update(oldVersion, "Version update");
        repository.delete(oldVersion);
        checkedInsert(accession, hash, model, version);
        return findByAccession(accession);
    }

    @Override
    public void deprecate(ACCESSION accession, String reason) throws AccessionDoesNotExistException,
            AccessionMergedException, AccessionDeprecatedException {
        List<ACCESSION_ENTITY> accessionedElements = getAccession(accession);
        inactiveAccessionService.deprecate(accession, accessionedElements, reason);
        repository.deleteAll(accessionedElements);
    }

    @Override
    public void merge(ACCESSION accession, ACCESSION mergeInto, String reason)
            throws AccessionMergedException, AccessionDoesNotExistException, AccessionDeprecatedException {
        List<ACCESSION_ENTITY> accessionedElements = getAccession(accession);
        getAccession(mergeInto);
        inactiveAccessionService.merge(accession, mergeInto, accessionedElements, reason);
        repository.deleteAll(accessionedElements);
    }

}
