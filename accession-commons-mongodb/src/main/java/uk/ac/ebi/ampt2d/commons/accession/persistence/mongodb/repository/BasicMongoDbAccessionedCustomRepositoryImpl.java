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
package uk.ac.ebi.ampt2d.commons.accession.persistence.mongodb.repository;

import com.mongodb.BulkWriteError;
import org.springframework.data.mongodb.BulkOperationException;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import uk.ac.ebi.ampt2d.commons.accession.core.SaveResponse;
import uk.ac.ebi.ampt2d.commons.accession.persistence.IAccessionedObjectCustomRepository;
import uk.ac.ebi.ampt2d.commons.accession.persistence.mongodb.document.AccessionedDocument;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BasicMongoDbAccessionedCustomRepositoryImpl<
        ACCESSION extends Serializable,
        DOCUMENT extends AccessionedDocument<ACCESSION>>
        implements IAccessionedObjectCustomRepository<ACCESSION, DOCUMENT> {

    private final Class<DOCUMENT> clazz;
    private final MongoTemplate mongoTemplate;

    public BasicMongoDbAccessionedCustomRepositoryImpl(Class<DOCUMENT> clazz, MongoTemplate mongoTemplate) {
        this.clazz = clazz;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public SaveResponse<ACCESSION> insert(List<DOCUMENT> documents) {
        checkHashUniqueness(documents);
        putAuditSaveDate(documents);
        final BulkOperations insert = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, clazz)
                .insert(new ArrayList<>(documents));
        final Set<String> duplicatedHash = new HashSet<>();

        try {
            insert.execute();
        } catch (BulkOperationException e) {
            e.getErrors().forEach(error -> {
                String duplicatedId = parseIdDuplicateKey(error).orElseThrow(() -> e);
                duplicatedHash.add(duplicatedId);
            });
        }
        return generateSaveResponse(documents, duplicatedHash);
    }

    private void checkHashUniqueness(Collection<DOCUMENT> documents) {
        final Set<String> duplicatedHash = new HashSet<>();
        documents.forEach(document -> {
            if (duplicatedHash.contains(document.getHashedMessage())) {
                throw new RuntimeException("Duplicated hash in mongodb insert batch");
            }
            duplicatedHash.add(document.getHashedMessage());
        });
    }

    /**
     * Unfortunately we need to set this manually when using a bulk operation.
     * @param documents
     */
    private void putAuditSaveDate(Iterable<DOCUMENT> documents) {
        LocalDateTime createdDate = LocalDateTime.now();
        for(DOCUMENT document: documents){
            document.setCreatedDate(createdDate);
        }
    }

    private Optional<String> parseIdDuplicateKey(BulkWriteError error) {
        if (11000 == error.getCode()) {
            final String message = error.getMessage();
            Pattern pattern = Pattern.compile("_id_ dup key:.\\{.:.\"(.*)\".\\}");
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                return Optional.of(matcher.group(1));
            }
        }
        return Optional.empty();
    }

    private SaveResponse<ACCESSION> generateSaveResponse(Collection<DOCUMENT> documents, Set<String> duplicatedHash) {
        final Set<ACCESSION> savedAccessions = new HashSet<>();
        final Set<ACCESSION> saveFailedAccessions = new HashSet<>();

        documents.forEach(document -> {
            if (!duplicatedHash.contains(document.getHashedMessage())) {
                savedAccessions.add(document.getAccession());
            } else {
                saveFailedAccessions.add(document.getAccession());
            }
        });

        return new SaveResponse<>(savedAccessions, saveFailedAccessions);
    }

}
