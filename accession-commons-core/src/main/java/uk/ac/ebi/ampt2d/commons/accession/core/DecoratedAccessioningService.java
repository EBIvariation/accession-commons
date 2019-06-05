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

import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionCouldNotBeGeneratedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionDeprecatedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionDoesNotExistException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionMergedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.HashAlreadyExistsException;
import uk.ac.ebi.ampt2d.commons.accession.core.models.AccessionVersionsWrapper;
import uk.ac.ebi.ampt2d.commons.accession.core.models.AccessionWrapper;
import uk.ac.ebi.ampt2d.commons.accession.core.models.GetOrCreateAccessionWrapper;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for creation, retrieval and modifications of object accessions, that decorates accessions usually with a
 * prefix and/or suffix.
 *
 * @param <MODEL>        Type of the objects identified by the accessions
 * @param <HASH>         Type of the hash calculated based on the fields that uniquely identify an accessioned object
 * @param <DB_ACCESSION> Type of the actual accession stored in database
 * @param <ACCESSION>    Type of the accession that identifies an object of a particular model
 */

public class DecoratedAccessioningService<MODEL, HASH, DB_ACCESSION, ACCESSION>
        implements AccessioningService<MODEL, HASH, ACCESSION> {

    private final AccessioningService<MODEL, HASH, DB_ACCESSION> service;
    private final Function<DB_ACCESSION, ACCESSION> decoratingFunction;
    private final Function<ACCESSION, DB_ACCESSION> parsingFunction;

    public DecoratedAccessioningService(AccessioningService<MODEL, HASH, DB_ACCESSION> service,
                                        Function<DB_ACCESSION, ACCESSION> decoratingFunction,
                                        Function<ACCESSION, DB_ACCESSION> parsingFunction) {
        this.service = service;
        this.decoratingFunction = decoratingFunction;
        this.parsingFunction = parsingFunction;
    }

    @Override
    public List<GetOrCreateAccessionWrapper<MODEL, HASH, ACCESSION>> getOrCreate(List<? extends MODEL> messages)
            throws AccessionCouldNotBeGeneratedException {
        return getOrCreateDecorate(service.getOrCreate(messages));
    }

    private List<GetOrCreateAccessionWrapper<MODEL, HASH, ACCESSION>> getOrCreateDecorate(
            List<GetOrCreateAccessionWrapper<MODEL, HASH, DB_ACCESSION>> accessionWrappers) {
        return accessionWrappers.stream().map(this::decorate).collect(Collectors.toList());
    }

    private GetOrCreateAccessionWrapper<MODEL, HASH, ACCESSION> decorate(
            GetOrCreateAccessionWrapper<MODEL, HASH, DB_ACCESSION> wrapper) {
        return new GetOrCreateAccessionWrapper<>(decoratingFunction.apply(wrapper.getAccession()), wrapper.getHash(),
                wrapper.getData(), wrapper.getVersion(), wrapper.isNewAccession());
    }

    private List<AccessionWrapper<MODEL, HASH, ACCESSION>> decorate(
            List<AccessionWrapper<MODEL, HASH, DB_ACCESSION>> accessionWrappers) {
        return accessionWrappers.stream().map(this::decorate).collect(Collectors.toList());
    }

    private AccessionWrapper<MODEL, HASH, ACCESSION> decorate(AccessionWrapper<MODEL, HASH, DB_ACCESSION> wrapper) {
        return new AccessionWrapper<>(decoratingFunction.apply(wrapper.getAccession()), wrapper.getHash(),
                wrapper.getData(), wrapper.getVersion());
    }

    @Override
    public List<AccessionWrapper<MODEL, HASH, ACCESSION>> get(List<? extends MODEL> accessionedObjects) {
        return decorate(service.get(accessionedObjects));
    }

    @Override
    public AccessionWrapper<MODEL, HASH, ACCESSION> getByAccession(ACCESSION accession)
            throws AccessionDoesNotExistException, AccessionMergedException, AccessionDeprecatedException {
        try {
            return decorate(service.getByAccession(parse(accession)));
        } catch (AccessionMergedException accessionMergedException) {
            throw AccessionMergedExceptionWithDecoratedAccessions(accessionMergedException);
        }
    }

    private List<DB_ACCESSION> parse(List<ACCESSION> accessions) {
        return accessions.stream().map(parsingFunction).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public AccessionWrapper<MODEL, HASH, ACCESSION> getByAccessionAndVersion(ACCESSION accession, int version)
            throws AccessionDoesNotExistException, AccessionMergedException, AccessionDeprecatedException {
        try {
            return decorate(service.getByAccessionAndVersion(parse(accession), version));
        } catch (AccessionMergedException accessionMergedException) {
            throw AccessionMergedExceptionWithDecoratedAccessions(accessionMergedException);
        }
    }

    private DB_ACCESSION parse(ACCESSION accession) throws AccessionDoesNotExistException {
        DB_ACCESSION dbAccession = parsingFunction.apply(accession);
        if (dbAccession == null) {
            throw new AccessionDoesNotExistException(accession);
        }
        return dbAccession;

    }

    @Override
    public AccessionVersionsWrapper<MODEL, HASH, ACCESSION> update(ACCESSION accession, int version, MODEL message)
            throws AccessionDoesNotExistException, HashAlreadyExistsException, AccessionDeprecatedException,
            AccessionMergedException {
        try {
            return decorate(service.update(parse(accession), version, message));
        } catch (AccessionMergedException accessionMergedException) {
            throw AccessionMergedExceptionWithDecoratedAccessions(accessionMergedException);
        }
    }

    private AccessionVersionsWrapper<MODEL, HASH, ACCESSION> decorate(
            AccessionVersionsWrapper<MODEL, HASH, DB_ACCESSION> accessionVersions) {
        return new AccessionVersionsWrapper<>(decorate(accessionVersions.getModelWrappers()));
    }

    @Override
    public AccessionVersionsWrapper<MODEL, HASH, ACCESSION> patch(ACCESSION accession, MODEL message)
            throws AccessionDoesNotExistException, HashAlreadyExistsException, AccessionDeprecatedException,
            AccessionMergedException {
        try {
            return decorate(service.patch(parse(accession), message));
        } catch (AccessionMergedException accessionMergedException) {
            throw AccessionMergedExceptionWithDecoratedAccessions(accessionMergedException);
        }
    }

    @Override
    public void deprecate(ACCESSION accession, String reason) throws AccessionMergedException,
            AccessionDoesNotExistException, AccessionDeprecatedException {
        try {
            service.deprecate(parse(accession), reason);
        } catch (AccessionMergedException accessionMergedException) {
            throw AccessionMergedExceptionWithDecoratedAccessions(accessionMergedException);
        }
    }

    @Override
    public void merge(ACCESSION accessionOrigin, ACCESSION mergeInto, String reason) throws AccessionMergedException,
            AccessionDoesNotExistException, AccessionDeprecatedException {
        try {
            service.merge(parse(accessionOrigin), parse(mergeInto), reason);
        } catch (AccessionMergedException accessionMergedException) {
            throw AccessionMergedExceptionWithDecoratedAccessions(accessionMergedException);
        }
    }

    public static <MODEL, HASH, DB_ACCESSION> DecoratedAccessioningService<MODEL, HASH, DB_ACCESSION, String>
    buildPrefixAccessionService(AccessioningService<MODEL, HASH, DB_ACCESSION> service, String prefix,
                                Function<String, DB_ACCESSION> parseFunction) {
        return new DecoratedAccessioningService<>(service, accession -> prefix + accession,
                s -> {
                    if (s.length() <= prefix.length() || !Objects.equals(s.substring(0, prefix.length()), prefix)) {
                        return null;
                    }
                    return parseFunction.apply(s.substring(prefix.length()));
                });
    }

    public static <MODEL, HASH> DecoratedAccessioningService<MODEL, HASH, Long, String>
    buildPrefixPaddedLongAccessionService(AccessioningService<MODEL, HASH, Long> service, String prefix,
                                          String padFormat, Function<String, Long> parseFunction) {
        return new DecoratedAccessioningService<>(service, accession -> prefix + String.format(padFormat, accession),
                s -> {
                    if (s.length() <= prefix.length() || !Objects.equals(s.substring(0, prefix.length()), prefix)) {
                        return null;
                    }
                    return parseFunction.apply(s.substring(prefix.length()));
                });
    }

    public AccessionMergedException AccessionMergedExceptionWithDecoratedAccessions(AccessionMergedException accessionMergedException) {
        if (decoratingFunction != null) {
            return new AccessionMergedException(
                    decoratingFunction.apply((DB_ACCESSION)
                            Long.valueOf(accessionMergedException.getOriginAccessionId())).toString(),
                    decoratingFunction.apply((DB_ACCESSION)
                            Long.valueOf(accessionMergedException.getDestinationAccessionId())).toString());
        }
        return accessionMergedException;
    }

}