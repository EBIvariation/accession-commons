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

import com.mongodb.bulk.BulkWriteError;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.bulk.BulkWriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.BulkOperationException;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import uk.ac.ebi.ampt2d.commons.accession.core.models.SaveResponse;
import uk.ac.ebi.ampt2d.commons.accession.persistence.repositories.IAccessionedObjectCustomRepository;
import uk.ac.ebi.ampt2d.commons.accession.persistence.mongodb.document.AccessionedDocument;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BasicMongoDbAccessionedCustomRepositoryImpl<
        ACCESSION extends Serializable,
        DOCUMENT extends AccessionedDocument<?, ACCESSION>>
        implements IAccessionedObjectCustomRepository<ACCESSION, DOCUMENT> {

    private final static Logger logger = LoggerFactory.getLogger(BasicMongoDbAccessionedCustomRepositoryImpl.class);

    private final Class<DOCUMENT> clazz;
    private final MongoTemplate mongoTemplate;

    public BasicMongoDbAccessionedCustomRepositoryImpl(Class<DOCUMENT> clazz, MongoTemplate mongoTemplate) {
        this.clazz = clazz;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public SaveResponse<ACCESSION> insert(List<DOCUMENT> documents) {
        checkHashUniqueness(documents);
        setAuditCreatedDate(documents);
        final BulkOperations insert = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, clazz).insert(documents);
        final Set<String> erroneousIds = new HashSet<>();

        try {
            insert.execute();
        } catch (DuplicateKeyException e) {
            MongoBulkWriteException bulkWriteException = ((MongoBulkWriteException) e.getCause());
            bulkWriteException.getWriteErrors().forEach(error -> {
                String errorId = reportBulkOperationException(error).orElseThrow(() -> e);
                erroneousIds.add(errorId);
            });
        } catch (RuntimeException e) {
            logger.error("Unexpected runtime exception in MongoDB bulk insert", e);
            throw e;
        }
        return generateSaveResponse(documents, erroneousIds);
    }

    private void checkHashUniqueness(Collection<DOCUMENT> documents) {
        final Set<String> duplicatedHash = new HashSet<>();
        documents.forEach(document -> {
            if (duplicatedHash.contains(document.getHashedMessage())) {
                throw new RuntimeException("Duplicated hash in MongoDB bulk insert.");
            }
            duplicatedHash.add(document.getHashedMessage());
        });
    }

    /**
     * Set this manually when using a bulk operation.
     *
     * @param documents MongoDB documents to have their creation date set
     */
    private void setAuditCreatedDate(Iterable<DOCUMENT> documents) {
        LocalDateTime createdDate = LocalDateTime.now();
        for (DOCUMENT document: documents) {
            document.setCreatedDate(createdDate);
        }
    }

    private Optional<String> reportBulkOperationException(BulkWriteError error) {
        if (11000 == error.getCode()) {
            final String message = error.getMessage();
            Pattern pattern = Pattern.compile("_id_ dup key:.\\{.:.\"(.*)\".\\}");
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                return Optional.of(matcher.group(1));
            } else {
                logger.error("Error parsing BulkWriteError in BulkOperationException. Code '" + error.getCode()
                        + "' Message: '" + error.getMessage() + "'");
            }
        } else {
            logger.error("Unexpected BulkWriteError in BulkOperationException. Code: '" + error.getCode()
                    + "'. Message: '" + error.getMessage() + "'");
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
