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
package uk.ac.ebi.ampt2d.commons.accession.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import uk.ac.ebi.ampt2d.commons.accession.generators.monotonic.MonotonicRange;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.service.MonotonicDatabaseService;
import uk.ac.ebi.ampt2d.test.configuration.TestMonotonicDatabaseServiceTestConfiguration;
import uk.ac.ebi.ampt2d.test.models.TestModel;
import uk.ac.ebi.ampt2d.test.persistence.TestMonotonicEntity;
import uk.ac.ebi.ampt2d.test.persistence.TestMonotonicRepository;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ContextConfiguration(classes = {TestMonotonicDatabaseServiceTestConfiguration.class})
public class BasicSpringDataRepositoryMonotonicDatabaseServiceTest {

    private static final long ACCESSION_1 = 10L;

    private static final long ACCESSION_2 = 20L;

    private static final long ACCESSION_3 = 120L;

    private static final long ACCESSION_4 = 400000L;

    private static final long ACCESSION_5 = 990000L;

    private static final long ACCESSION_6 = 1000001L;

    private static final long START_RANGE_1 = 0L;

    private static final long END_RANGE_1 = 99L;

    private static final long START_RANGE_2 = 100L;

    private static final long END_RANGE_2 = 199L;

    private static final long START_BIG_RANGE_3 = 200L;

    private static final long END_BIG_RANGE_3 = 1000000L;

    @Autowired
    private TestMonotonicRepository repository;

    @Autowired
    private MonotonicDatabaseService service;

    @BeforeEach
    public void setUp() throws Exception {
        repository.deleteAll();
    }

    @AfterEach
    public void tearDown() throws Exception {
        repository.deleteAll();
    }

    @Test
    public void recoverUnconfirmedAccessions() {
        repository.insert(Collections.singletonList(new TestMonotonicEntity(ACCESSION_1, "message", 1, "value")));
        long[] accessionsInRanges = service.getAccessionsInRanges(
                Collections.singletonList(new MonotonicRange(START_RANGE_1, END_RANGE_1)));
        assertEquals(1, accessionsInRanges.length);
        assertEquals(ACCESSION_1, accessionsInRanges[0]);
    }

    @Test
    public void recoverUnconfirmedAccessionsSeveralEntities() {
        repository.insert(Arrays.asList(
                new TestMonotonicEntity(ACCESSION_1, "message 1", 1, "value 1"),
                new TestMonotonicEntity(ACCESSION_2, "message 2", 1, "value 2")
        ));
        long[] accessionsInRanges = service.getAccessionsInRanges(
                Collections.singletonList(new MonotonicRange(START_RANGE_1, END_RANGE_1)));
        assertEquals(2, accessionsInRanges.length);
        assertEquals(ACCESSION_1, accessionsInRanges[0]);
        assertEquals(ACCESSION_2, accessionsInRanges[1]);
    }

    @Test
    public void recoverUnconfirmedAccessionsSeveralBlocks() {
        repository.insert(Arrays.asList(
                new TestMonotonicEntity(ACCESSION_1, "message 1", 1, "value 1"),
                new TestMonotonicEntity(ACCESSION_3, "message 2", 1, "value 2")
        ));
        long[] accessionsInRanges = service.getAccessionsInRanges(
                Arrays.asList(
                        new MonotonicRange(START_RANGE_1, END_RANGE_1),
                        new MonotonicRange(START_RANGE_2, END_RANGE_2)
                ));
        assertEquals(2, accessionsInRanges.length);
        assertEquals(ACCESSION_1, accessionsInRanges[0]);
        assertEquals(ACCESSION_3, accessionsInRanges[1]);
    }

    @Test
    public void recoverUnconfirmedAccessionsSeveralBigBlocks() {
        repository.insert(Arrays.asList(
                new TestMonotonicEntity(ACCESSION_1, "message 1", 1, "value 1"),
                new TestMonotonicEntity(ACCESSION_4, "message 2", 1, "value 2"),
                new TestMonotonicEntity(ACCESSION_5, "message 3", 1, "value 3"),

                // note this accession falls out of every range
                new TestMonotonicEntity(ACCESSION_6, "message 4", 1, "value 4")
        ));
        long[] accessionsInRanges = service.getAccessionsInRanges(
                Arrays.asList(
                        new MonotonicRange(START_RANGE_1, END_RANGE_1),
                        new MonotonicRange(START_RANGE_2, END_RANGE_2),
                        new MonotonicRange(START_BIG_RANGE_3, END_BIG_RANGE_3)
                ));
        assertEquals(3, accessionsInRanges.length);
        assertEquals(ACCESSION_1, accessionsInRanges[0]);
        assertEquals(ACCESSION_4, accessionsInRanges[1]);
        assertEquals(ACCESSION_5, accessionsInRanges[2]);
    }
}