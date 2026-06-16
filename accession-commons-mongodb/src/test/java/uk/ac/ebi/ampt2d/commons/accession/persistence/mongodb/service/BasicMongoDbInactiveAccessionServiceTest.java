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
package uk.ac.ebi.ampt2d.commons.accession.persistence.mongodb.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.ac.ebi.ampt2d.commons.accession.core.models.EventType;
import uk.ac.ebi.ampt2d.commons.accession.core.models.IEvent;
import uk.ac.ebi.ampt2d.test.configuration.MongoDbTestConfiguration;
import uk.ac.ebi.ampt2d.test.persistence.document.TestDocument;
import uk.ac.ebi.ampt2d.test.persistence.document.TestEventDocument;
import uk.ac.ebi.ampt2d.test.persistence.repository.TestRepository;
import uk.ac.ebi.ampt2d.test.persistence.service.TestMongoDbInactiveAccessionService;
import uk.ac.ebi.ampt2d.utils.MongoTestContainerHelper;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.ac.ebi.ampt2d.commons.accession.core.models.EventType.DEPRECATED;
import static uk.ac.ebi.ampt2d.commons.accession.core.models.EventType.MERGED;
import static uk.ac.ebi.ampt2d.commons.accession.core.models.EventType.UPDATED;
import static uk.ac.ebi.ampt2d.test.persistence.document.TestDocument.document;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {MongoDbTestConfiguration.class})
public class BasicMongoDbInactiveAccessionServiceTest extends MongoTestContainerHelper {

    private static final String DEFAULT_REASON = "default-test-reason";

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanDb() {
        mongoTemplate.getDb().drop();
    }

    @Autowired
    private TestRepository repository;

    @Autowired
    private TestMongoDbInactiveAccessionService service;

    @Test
    public void testLastOperationDoesNotExistBehaviour() {
        new LastOperationAsserts("notExist").assertDoesNotExist();
    }

    @Test
    public void testUpdate() {
        update(document(1, "test-update-1")).assertExists().assertIsUpdate();
    }

    @Test
    public void testDeprecate() {
        deprecate(document(1, "test-deprecate-1")).assertExists().assertIsDeprecate();
    }

    @Test
    public void testMerge() {
        merge(document(1, "test-deprecate-1"), "a2").assertExists().assertIsMerge("a1", "a2", 1);
    }

    private LastOperationAsserts merge(TestDocument document, String accession) {
        service.merge(document.getAccession(), accession, Arrays.asList(document), DEFAULT_REASON);
        return new LastOperationAsserts(document.getAccession());
    }

    private LastOperationAsserts update(TestDocument document) {
        repository.insert(Arrays.asList(document));
        service.update(document, DEFAULT_REASON);
        return new LastOperationAsserts(document.getAccession());
    }

    private LastOperationAsserts deprecate(TestDocument document) {
        repository.insert(Arrays.asList(document));
        service.deprecate(document.getAccession(), Arrays.asList(document), DEFAULT_REASON);
        return new LastOperationAsserts(document.getAccession());
    }

    private class LastOperationAsserts {

        private final IEvent<?, String> lastOperation;

        public LastOperationAsserts(String accession) {
            this.lastOperation = service.getLastEvent(accession);
        }

        public LastOperationAsserts assertDoesNotExist() {
            assertNull(lastOperation);
            return this;
        }

        public LastOperationAsserts assertExists() {
            assertNotNull(lastOperation);
            return this;
        }

        public LastOperationAsserts assertIsUpdate() {
            return isOfType(UPDATED);
        }

        private LastOperationAsserts isOfType(EventType type) {
            assertEquals(type, lastOperation.getEventType());
            return this;
        }

        public LastOperationAsserts assertIsDeprecate() {
            return isOfType(DEPRECATED);
        }

        public LastOperationAsserts assertIsMerge(String origin, String mergeInto, int totalElements) {
            return isOfType(MERGED).assertOrigin(origin).assertMergeInto(mergeInto)
                    .assertTotalStoredElements(totalElements);
        }

        private LastOperationAsserts assertOrigin(String origin) {
            assertEquals(origin, lastOperation.getAccession());
            return this;
        }

        private LastOperationAsserts assertMergeInto(String destination) {
            assertEquals(destination, lastOperation.getMergedInto());
            return this;
        }

        private LastOperationAsserts assertTotalStoredElements(int totalElements) {
            assertEquals(totalElements, ((TestEventDocument) lastOperation).getInactiveObjects().size());
            return this;
        }

    }
}
