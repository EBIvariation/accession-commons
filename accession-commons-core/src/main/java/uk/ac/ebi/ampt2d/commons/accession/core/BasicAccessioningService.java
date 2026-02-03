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
package uk.ac.ebi.ampt2d.commons.accession.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionCouldNotBeGeneratedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionDeprecatedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionDoesNotExistException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionMergedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.HashAlreadyExistsException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.MissingUnsavedAccessionsException;
import uk.ac.ebi.ampt2d.commons.accession.core.models.AccessionVersionsWrapper;
import uk.ac.ebi.ampt2d.commons.accession.core.models.AccessionWrapper;
import uk.ac.ebi.ampt2d.commons.accession.core.models.GetOrCreateAccessionWrapper;
import uk.ac.ebi.ampt2d.commons.accession.core.models.SaveResponse;
import uk.ac.ebi.ampt2d.commons.accession.generators.AccessionGenerator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Basic implementation of a service for creation, retrieval and modifications of object accessions.
 *
 * @param <MODEL>     Type of the objects identified by the accessions
 * @param <HASH>      Type of the hash calculated based on the fields that uniquely identify an accessioned object
 * @param <ACCESSION> Type of the accession that identifies an object of a particular model
 */
public class BasicAccessioningService<MODEL, HASH, ACCESSION extends Serializable>
        implements AccessioningService<MODEL, HASH, ACCESSION> {

    private final static Logger logger = LoggerFactory.getLogger(BasicAccessioningService.class);

    private static final String PATCH_DEFAULT_REASON = "patch";

    private AccessionGenerator<MODEL, ACCESSION> accessionGenerator;

    private DatabaseService<MODEL, HASH, ACCESSION> dbService;

    private final Function<MODEL, HASH> hashingFunction;

    private final AccessionSaveMode accessionSaveMode;

    public BasicAccessioningService(AccessionGenerator<MODEL, ACCESSION> accessionGenerator,
                                    DatabaseService<MODEL, HASH, ACCESSION> dbService,
                                    Function<MODEL, String> summaryFunction,
                                    Function<String, HASH> hashingFunction,
                                    AccessionSaveMode accessionSaveMode) {
        this.accessionGenerator = accessionGenerator;
        this.dbService = dbService;
        this.hashingFunction = summaryFunction.andThen(hashingFunction);
        this.accessionSaveMode = accessionSaveMode != null ? accessionSaveMode : AccessionSaveMode.SAVE_ALL_THEN_RESOLVE;
    }

    @Override
    public List<GetOrCreateAccessionWrapper<MODEL, HASH, ACCESSION>> getOrCreate(List<? extends MODEL> messages,
                                                                                 String applicationInstanceId)
            throws AccessionCouldNotBeGeneratedException {
        return saveAccessions(accessionGenerator.generateAccessions(mapHashOfMessages(messages), applicationInstanceId));
    }

    /**
     * Digests messages using a hash function. If two messages have the same hash, keeps the first one.
     */
    private Map<HASH, MODEL> mapHashOfMessages(List<? extends MODEL> messages) {
        return messages.stream().collect(Collectors.toMap(hashingFunction, e -> e, (r, o) -> r));
    }

    /**
     * Execute {@link DatabaseService#save(List)}  This operation will generate two lists on {@link SaveResponse}
     * saved elements and not saved elements. Not saved elements are elements that could not be stored on database
     * due to constraint exceptions. This should only happen when elements have been already stored by another
     * application instance / thread with a different id.
     * See {@link #getPreexistingAccessions(List)} } for more details.
     */
    private List<GetOrCreateAccessionWrapper<MODEL, HASH, ACCESSION>> saveAccessions(List<AccessionWrapper<MODEL, HASH, ACCESSION>> accessions) {
        logger.trace("Accessions to save: {}", accessions.stream().map(AccessionWrapper::getAccession).collect(
                Collectors.toList()));
        switch (this.accessionSaveMode) {
            case PREFILTER_EXISTING:
                return saveAccessionsPrefilteringExisting(accessions);
            case SAVE_ALL_THEN_RESOLVE:
            default:
                return saveAllAccessionsThenResolve(accessions);
        }
    }

    private List<GetOrCreateAccessionWrapper<MODEL, HASH, ACCESSION>> saveAllAccessionsThenResolve(
            List<AccessionWrapper<MODEL, HASH, ACCESSION>> accessions) {
        SaveResponse<ACCESSION> response = dbService.save(accessions);
        accessionGenerator.postSave(response);

        final List<GetOrCreateAccessionWrapper<MODEL, HASH, ACCESSION>> savedAccessions = new ArrayList<>();
        final List<AccessionWrapper<MODEL, HASH, ACCESSION>> unsavedAccessions = new ArrayList<>();
        accessions.stream().forEach(accessionModel -> {
            if (response.isSavedAccession(accessionModel.getAccession())) {
                savedAccessions.add(GetOrCreateAccessionWrapper.newAccession(accessionModel));
            } else {
                unsavedAccessions.add(accessionModel);
            }
        });
        if (!unsavedAccessions.isEmpty()) {
            getPreexistingAccessions(unsavedAccessions).stream().map(GetOrCreateAccessionWrapper::oldAccession)
                    .forEach(savedAccessions::add);
        }
        return savedAccessions;
    }

    private List<GetOrCreateAccessionWrapper<MODEL, HASH, ACCESSION>> saveAccessionsPrefilteringExisting(
            List<AccessionWrapper<MODEL, HASH, ACCESSION>> accessions) {
        Set<HASH> allHashes = accessions.stream().map(AccessionWrapper::getHash).collect(Collectors.toSet());
        List<AccessionWrapper<MODEL, HASH, ACCESSION>> preexistingAccessions = dbService.findAllByHash(allHashes);
        Set<HASH> preexistingHashes = preexistingAccessions.stream().map(AccessionWrapper::getHash).collect(Collectors.toSet());

        // release accessions associated with pre-existing hashes
        Set<ACCESSION> accessionsToRelease = accessions
                .stream()
                .filter(accession -> preexistingHashes.contains(accession.getHash()))
                .map(AccessionWrapper::getAccession)
                .collect(Collectors.toSet());
        SaveResponse<ACCESSION> response = new SaveResponse<>(Collections.emptySet(), accessionsToRelease);
        accessionGenerator.postSave(response);

        // save rest of the accessions
        List<AccessionWrapper<MODEL, HASH, ACCESSION>> accessionsToSave = accessions.stream()
                .filter(accession -> !preexistingHashes.contains(accession.getHash()))
                .collect(Collectors.toList());

        final List<GetOrCreateAccessionWrapper<MODEL, HASH, ACCESSION>> result = new ArrayList<>();

        if (!accessionsToSave.isEmpty()) {
            result.addAll(saveAllAccessionsThenResolve(accessionsToSave));
        }

        // add pre-existing back to result
        preexistingAccessions.stream().map(GetOrCreateAccessionWrapper::oldAccession).forEach(result::add);

        return result;
    }

    /**
     * We try to recover all elements that could not be saved to return their accession to the user. This is only
     * expected when another application instance or thread has saved that element already with a different id. If
     * any element can't be retrieved from the database we throw a {@link MissingUnsavedAccessionsException} to alert
     * the system.
     */
    private List<AccessionWrapper<MODEL, HASH, ACCESSION>> getPreexistingAccessions(
            List<AccessionWrapper<MODEL, HASH, ACCESSION>> saveFailedAccessions) {

        Set<HASH> unsavedHashes = saveFailedAccessions.stream().map(AccessionWrapper::getHash)
                .collect(Collectors.toSet());
        List<AccessionWrapper<MODEL, HASH, ACCESSION>> dbAccessions = dbService.findAllByHash(unsavedHashes);
        if (dbAccessions.size() != unsavedHashes.size()) {
            logger.error("Lists of unsaved hashes and pre-existing accessions differ in size");
            logger.error("Failed hashes: '" + unsavedHashes.toString() + "'");
            logger.error("Accessions retrieved from database: '" + dbAccessions + "'");
            throw new MissingUnsavedAccessionsException(saveFailedAccessions, dbAccessions);
        }
        return dbAccessions;
    }

    @Override
    public List<AccessionWrapper<MODEL, HASH, ACCESSION>> get(List<? extends MODEL> accessionedObjects) {
        return dbService.findAllByHash(getHashes(accessionedObjects));
    }

    private List<HASH> getHashes(List<? extends MODEL> accessionObjects) {
        return accessionObjects.stream().map(hashingFunction).collect(Collectors.toList());
    }

    @Override
    public AccessionWrapper<MODEL, HASH, ACCESSION> getByAccession(ACCESSION accession)
            throws AccessionDoesNotExistException, AccessionMergedException, AccessionDeprecatedException {
        return dbService.findLastVersionByAccession(accession);
    }

    @Override
    public AccessionVersionsWrapper<MODEL, HASH, ACCESSION> update(ACCESSION accession, int version, MODEL message)
            throws AccessionDoesNotExistException, HashAlreadyExistsException, AccessionDeprecatedException,
            AccessionMergedException {
        return dbService.update(accession, hashingFunction.apply(message), message, version);
    }

    @Override
    public AccessionVersionsWrapper<MODEL, HASH, ACCESSION> patch(ACCESSION accession, MODEL message)
            throws AccessionDoesNotExistException, HashAlreadyExistsException, AccessionDeprecatedException,
            AccessionMergedException {
        return dbService.patch(accession, hashingFunction.apply(message), message, PATCH_DEFAULT_REASON);
    }

    @Override
    public AccessionWrapper<MODEL, HASH, ACCESSION> getByAccessionAndVersion(ACCESSION accession, int version)
            throws AccessionDoesNotExistException, AccessionMergedException, AccessionDeprecatedException {
        return dbService.findByAccessionVersion(accession, version);
    }

    @Override
    public void deprecate(ACCESSION accession, String reason) throws AccessionMergedException,
            AccessionDoesNotExistException, AccessionDeprecatedException {
        dbService.deprecate(accession, reason);
    }

    @Override
    public void merge(ACCESSION accessionOrigin, ACCESSION mergeInto, String reason)
            throws AccessionMergedException, AccessionDoesNotExistException, AccessionDeprecatedException {
        dbService.merge(accessionOrigin, mergeInto, reason);
    }

    public void shutDownAccessioning() {
        accessionGenerator.shutDownAccessionGenerator();
    }

    protected AccessionGenerator<MODEL, ACCESSION> getAccessionGenerator() {
        return accessionGenerator;
    }

    protected DatabaseService<MODEL, HASH, ACCESSION> getDbService() {
        return dbService;
    }

}
