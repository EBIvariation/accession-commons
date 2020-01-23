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

import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.MongoDbConfigurationBuilder;
import com.lordofthejars.nosqlunit.mongodb.MongoDbRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.ampt2d.commons.accession.core.models.SaveResponse;
import uk.ac.ebi.ampt2d.test.configuration.MongoDbTestConfiguration;
import uk.ac.ebi.ampt2d.test.persistence.document.TestDocument;
import uk.ac.ebi.ampt2d.test.persistence.repository.TestRepository;
import uk.ac.ebi.ampt2d.test.rule.FixSpringMongoDbRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static uk.ac.ebi.ampt2d.test.persistence.document.TestDocument.document;

;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {MongoDbTestConfiguration.class})
public class AccessionedDocumentRepositoryTest {

    @Autowired
    private TestRepository repository;

    @Rule
    public MongoDbRule mongoDbRule = new FixSpringMongoDbRule(MongoDbConfigurationBuilder.mongoDb()
            .databaseName("accession-test").build());

    //Required for nosql unit
    @Autowired
    private ApplicationContext applicationContext;

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

    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    @Test
    public void testInsert() {
        assertEquals(0, repository.count());
        insertDocuments(1).assertInsertOk();
    }

    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    @Test
    public void testInsertMultiple() {
        assertEquals(0, repository.count());
        insertDocuments(2).assertInsertOk();
    }

    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    @Test(expected = RuntimeException.class)
    public void testInsertDuplicatedHashInBatch() {
        assertEquals(0, repository.count());
        insertDocuments(document(1), document(2), document(1));
    }

    private TestInsert insertDocuments(TestDocument... documents) {
        return new TestInsert(Arrays.asList(documents));
    }

    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
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

    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
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

    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
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

    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
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

    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
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

    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    @Test
    public void testInsertFindById() {
        insertDocuments(1);
        TestDocument document = repository.findById("h0").orElse(null);
        assertEquals("h0", document.getHashedMessage());
        assertEquals("a0", document.getAccession());
        assertEquals("test-0", document.getValue());
    }

}
