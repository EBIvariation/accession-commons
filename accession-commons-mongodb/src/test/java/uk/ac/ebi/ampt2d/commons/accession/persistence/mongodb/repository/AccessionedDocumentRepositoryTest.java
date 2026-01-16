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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.ac.ebi.ampt2d.commons.accession.core.models.SaveResponse;
import uk.ac.ebi.ampt2d.test.configuration.MongoDbTestConfiguration;
import uk.ac.ebi.ampt2d.test.persistence.document.TestDocument;
import uk.ac.ebi.ampt2d.test.persistence.repository.TestRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.ac.ebi.ampt2d.test.persistence.document.TestDocument.document;

@SpringBootTest(classes = {MongoDbTestConfiguration.class})
@Testcontainers
public class AccessionedDocumentRepositoryTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private TestRepository repository;

    @BeforeEach
    public void setUp() {
        repository.deleteAll();
    }

    private class TestInsert {

        private final long dbDocumentsBeforeInsert;
        private final int totalDocuments;
        private final SaveResponse<String> saveResponse;

        public TestInsert(List<TestDocument> documents) {
            this.dbDocumentsBeforeInsert = repository.count();
            this.totalDocuments = documents.size();
            this.saveResponse = repository.insert(documents);
        }

        public TestInsert assertInsertOk() {
            return assertTotalDbDocuments(dbDocumentsBeforeInsert + totalDocuments)
                    .assertSavedDocuments(totalDocuments)
                    .assertSaveFailedDocuments(0);
        }

        public TestInsert assertTotalDbDocuments(long totalDbDocuments) {
            assertEquals(totalDbDocuments, repository.count());
            return this;
        }

        public TestInsert assertSavedDocuments(int totalSavedDocuments) {
            assertEquals(totalSavedDocuments, saveResponse.getSavedAccessions().size());
            return this;
        }

        public TestInsert assertSaveFailedDocuments(int totalSaveFailedDocuments) {
            assertEquals(totalSaveFailedDocuments, saveResponse.getSaveFailedAccessions().size());
            return this;
        }

        public TestInsert assertAccessionHasNotBeenSaved(String accession) {
            assertTrue(saveResponse.getSaveFailedAccessions().contains(accession));
            return this;
        }
    }

    private TestInsert insertDocuments(int totalDocuments) {
        final List<TestDocument> documents = new ArrayList<>();
        for (int i = 0; i < totalDocuments; i++) {
            documents.add(document(i));
        }
        return new TestInsert(documents);
    }

    @Test
    public void testInsert() {
        assertEquals(0, repository.count());
        insertDocuments(1).assertInsertOk();
    }

    @Test
    public void testInsertMultiple() {
        assertEquals(0, repository.count());
        insertDocuments(2).assertInsertOk();
    }

    @Test
    public void testInsertDuplicatedHashInBatch() {
        assertEquals(0, repository.count());
        assertThrows(RuntimeException.class, () -> {
            insertDocuments(document(1), document(2), document(1));
        });
    }

    private TestInsert insertDocuments(TestDocument... documents) {
        return new TestInsert(Arrays.asList(documents));
    }

    @Test
    public void testInsertDuplicatedHashDifferentBatchesBeginning() {
        assertEquals(0, repository.count());
        // Insert first batch
        insertDocuments(2).assertInsertOk();

        // Insert second batch
        insertDocuments(
                document(1),
                document(2),
                document(3))
                .assertTotalDbDocuments(4)
                .assertSavedDocuments(2)
                .assertSaveFailedDocuments(1)
                .assertAccessionHasNotBeenSaved("a1");
    }

    @Test
    public void testInsertDuplicatedHashDifferentBatchesMiddle() {
        assertEquals(0, repository.count());
        // Insert first batch
        insertDocuments(2).assertInsertOk();

        // Insert second batch
        insertDocuments(
                document(2),
                document(1),
                document(3))
                .assertTotalDbDocuments(4)
                .assertSavedDocuments(2)
                .assertSaveFailedDocuments(1)
                .assertAccessionHasNotBeenSaved("a1");
    }

    @Test
    public void testInsertDuplicatedHashDifferentBatchesEnd() {
        assertEquals(0, repository.count());
        // Insert first batch
        insertDocuments(2).assertInsertOk();

        // Insert second batch
        insertDocuments(
                document(2),
                document(3),
                document(0))
                .assertTotalDbDocuments(4)
                .assertSavedDocuments(2)
                .assertSaveFailedDocuments(1)
                .assertAccessionHasNotBeenSaved("a0");
    }

    @Test
    public void testInsertDuplicatedHashDifferentAccession() {
        assertEquals(0, repository.count());
        // Insert first batch
        insertDocuments(2).assertInsertOk();

        // Insert second batch
        insertDocuments(
                document(2),
                document(3),
                document(7, 1))
                .assertTotalDbDocuments(4)
                .assertSavedDocuments(2)
                .assertSaveFailedDocuments(1)
                .assertAccessionHasNotBeenSaved("a7");
    }

    @Test
    public void testInsertMultipleDuplicatedHash() {
        assertEquals(0, repository.count());
        // Insert first batch
        insertDocuments(10).assertInsertOk();

        // Insert second batch
        insertDocuments(
                document(10),
                document(1),
                document(11),
                document(12),
                document(0),
                document(9),
                document(13),
                document(3),
                document(4))
                .assertTotalDbDocuments(14)
                .assertSavedDocuments(4)
                .assertSaveFailedDocuments(5)
                .assertAccessionHasNotBeenSaved("a0")
                .assertAccessionHasNotBeenSaved("a1")
                .assertAccessionHasNotBeenSaved("a3")
                .assertAccessionHasNotBeenSaved("a4")
                .assertAccessionHasNotBeenSaved("a9");
    }

    @Test
    public void testInsertFindById() {
        insertDocuments(1);
        Optional<TestDocument> document = repository.findById("h0");
        assertTrue(document.isPresent());
        assertEquals("h0", document.get().getHashedMessage());
        assertEquals("a0", document.get().getAccession());
        assertEquals("test-0", document.get().getValue());
    }

}
