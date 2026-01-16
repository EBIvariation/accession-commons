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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.ac.ebi.ampt2d.commons.accession.core.AccessioningService;
import uk.ac.ebi.ampt2d.commons.accession.core.HistoryService;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionDoesNotExistException;
import uk.ac.ebi.ampt2d.test.configuration.MongoDbTestConfiguration;
import uk.ac.ebi.ampt2d.test.models.TestModel;
import uk.ac.ebi.ampt2d.test.testers.AccessioningServiceTester;
import uk.ac.ebi.ampt2d.test.testers.HistoryTester;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.ac.ebi.ampt2d.test.testers.HistoryTester.assertEventIsCreated;
import static uk.ac.ebi.ampt2d.test.testers.HistoryTester.assertEventIsDeprecated;
import static uk.ac.ebi.ampt2d.test.testers.HistoryTester.assertEventIsMerged;
import static uk.ac.ebi.ampt2d.test.testers.HistoryTester.assertEventIsPatch;
import static uk.ac.ebi.ampt2d.test.testers.HistoryTester.assertEventIsUpdated;

@SpringBootTest(classes = {MongoDbTestConfiguration.class})
@Testcontainers
public class BasicMongoDbHistoryServiceTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private AccessioningService<TestModel, String, String> accessioningService;

    @Autowired
    private HistoryService<TestModel, String> historyService;

    @BeforeEach
    public void setUp() {
        mongoTemplate.getCollectionNames().forEach(name -> mongoTemplate.dropCollection(name));
    }

    @Test
    public void testNoHistory() throws AccessionDoesNotExistException {
        assertThrows(AccessionDoesNotExistException.class, () -> {
            getHistoryTester("DoesNotExist");
        });
    }

    @Test
    public void testHistoryNoOperations() throws AccessionDoesNotExistException {
        getAccessionTester()
                .getOrCreate("test-1");
        getHistoryTester("id-test-1")
                .assertTotalEvents(1)
                .assertEvent(0, assertEventIsCreated());
    }

    @Test
    public void testHistoryUpdate() throws AccessionDoesNotExistException {
        getAccessionTester()
                .getOrCreate("test-2")
                .update("id-test-2", 1, "test-2-update-1");
        getHistoryTester("id-test-2").assertTotalEvents(2)
                .assertEvent(0, assertEventIsCreated())
                .assertEvent(1, assertEventIsUpdated("test-2-update-1", 1));
    }

    @Test
    public void testHistoryPatch() throws AccessionDoesNotExistException {
        getAccessionTester()
                .getOrCreate("test-3")
                .patch("id-test-3", "test-3-patch-2");
        getHistoryTester("id-test-3")
                .assertTotalEvents(2)
                .assertEvent(0, assertEventIsCreated())
                .assertEvent(1, assertEventIsPatch("test-3-patch-2", 2));
    }

    @Test
    public void testHistoryMultiplePatch() throws AccessionDoesNotExistException {
        getAccessionTester()
                .getOrCreate("test-3")
                .patch("id-test-3", "test-3-patch-2")
                .patch("id-test-3", "test-3-patch-3");
        getHistoryTester("id-test-3")
                .assertTotalEvents(3)
                .assertEvent(0, assertEventIsCreated())
                .assertEvent(1, assertEventIsPatch("test-3-patch-2", 2))
                .assertEvent(2, assertEventIsPatch("test-3-patch-3", 3));
    }

    @Test
    public void testHistoryPatchAndUpdate() throws AccessionDoesNotExistException {
        getAccessionTester()
                .getOrCreate("test-4")
                .patch("id-test-4", "test-4-patch-2")
                .update("id-test-4", 2, "test-4-update-patch-2");

        getHistoryTester("id-test-4")
                .assertTotalEvents(3)
                .assertEvent(0, assertEventIsCreated())
                .assertEvent(1, assertEventIsPatch("test-4-patch-2", 2))
                .assertEvent(2, assertEventIsUpdated("test-4-update-patch-2", 2));
    }

    @Test
    public void testDeprecate() throws AccessionDoesNotExistException {
        getAccessionTester()
                .getOrCreate("test-5")
                .deprecate("id-test-5", "reason")
                .getLastMethodResponse().assertNoException();
        getHistoryTester("id-test-5")
                .assertTotalEvents(2)
                .assertEvent(0, assertEventIsCreated())
                .assertEvent(1, assertEventIsDeprecated());
    }

    @Test
    public void testMerge() throws AccessionDoesNotExistException {
        getAccessionTester()
                .getOrCreate("test-merge-1")
                .getOrCreate("test-6")
                .merge("id-test-6", "id-test-merge-1", "reason")
                .getLastMethodResponse().assertNoException();
        getHistoryTester("id-test-6")
                .assertTotalEvents(2)
                .assertEvent(0, assertEventIsCreated())
                .assertEvent(1, assertEventIsMerged("id-test-merge-1"));
    }

    @Test
    public void testComplexCase() throws AccessionDoesNotExistException {
        getAccessionTester()
                .getOrCreate("test-7")
                .update("id-test-7", 1, "test-7-update-1")
                .patch("id-test-7", "test-7-patch-2")
                .update("id-test-7", 2, "test-7-update-patch-2")
                .update("id-test-7", 2, "test-7-update-b-patch-2")
                .deprecate("id-test-7", "reason");

        getHistoryTester("id-test-7")
                .assertTotalEvents(6)
                .assertEvent(0, assertEventIsCreated())
                .assertEvent(1, assertEventIsUpdated("test-7-update-1", 1))
                .assertEvent(2, assertEventIsPatch("test-7-patch-2", 2))
                .assertEvent(3, assertEventIsUpdated("test-7-update-patch-2", 2))
                .assertEvent(4, assertEventIsUpdated("test-7-update-b-patch-2", 2))
                .assertEvent(5, assertEventIsDeprecated());
    }

    private HistoryTester.HistoryAccessionTester getHistoryTester(String id) throws AccessionDoesNotExistException {
        return new HistoryTester(historyService).accession(id);
    }

    public AccessioningServiceTester getAccessionTester() {
        return new AccessioningServiceTester(accessioningService);
    }

}
