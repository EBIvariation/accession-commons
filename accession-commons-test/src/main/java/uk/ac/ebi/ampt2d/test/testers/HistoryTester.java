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
package uk.ac.ebi.ampt2d.test.testers;

import org.junit.Assert;
import uk.ac.ebi.ampt2d.commons.accession.core.HistoryService;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionDoesNotExistException;
import uk.ac.ebi.ampt2d.commons.accession.core.models.EventType;
import uk.ac.ebi.ampt2d.commons.accession.core.models.HistoryEvent;
import uk.ac.ebi.ampt2d.test.models.TestModel;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HistoryTester {

    private final HistoryService<TestModel, String> historyService;

    public HistoryTester(HistoryService<TestModel, String> historyService) {
        this.historyService = historyService;
    }


    public HistoryAccessionTester accession(String id) throws AccessionDoesNotExistException {
        return new HistoryAccessionTester(id);
    }

    public class HistoryAccessionTester {

        private final List<HistoryEvent<TestModel, String>> history;

        public HistoryAccessionTester(String id) throws AccessionDoesNotExistException {
            this.history = historyService.getHistory(id);
        }

        public HistoryAccessionTester assertTotalEvents(int total) {
            assertEquals(total, history.size());
            return this;
        }

        public HistoryAccessionTester assertEvent(int i, Consumer<HistoryEvent<TestModel, String>> consumer) {
            consumer.accept(history.get(i));
            return this;
        }

    }

    public static Consumer<HistoryEvent<TestModel, String>> assertEventIs(EventType eventType) {
        return event -> assertEquals(eventType, event.getEventType());
    }

    public static Consumer<HistoryEvent<TestModel, String>> assertAccession(String accession) {
        return event -> assertEquals(accession, event.getAccession());
    }

    public static Consumer<HistoryEvent<TestModel, String>> assertNullMergedInto() {
        return event -> assertNull(event.getMergedInto());
    }

    public static Consumer<HistoryEvent<TestModel, String>> assertMergedInto(String accession) {
        return event -> Assert.assertEquals(accession, event.getMergedInto());
    }

    public static Consumer<HistoryEvent<TestModel, String>> assertNullVersion() {
        return event -> assertNull(event.getVersion());
    }

    public static Consumer<HistoryEvent<TestModel, String>> assertVersion(int version) {
        return event -> Assert.assertEquals(new Integer(version), event.getVersion());
    }

    public static Consumer<HistoryEvent<TestModel, String>> assertNullData() {
        return event -> assertNull(event.getData());
    }

    public static Consumer<HistoryEvent<TestModel, String>> assertEventIsCreated() {
        return assertEventIs(EventType.CREATED).andThen(assertNullMergedInto()).andThen(assertVersion(1));
    }

    public static Consumer<HistoryEvent<TestModel, String>> assertEventIsUpdated(String value, int version) {
        return assertEventIs(EventType.UPDATED).andThen(assertNullMergedInto()).andThen(assertVersion(version))
                .andThen(assertEventContentIs(value));
    }

    private static Consumer<HistoryEvent<TestModel, String>> assertEventContentIs(String value) {
        return event -> assertEquals(value, event.getData().get(0).getValue());
    }

    public static Consumer<HistoryEvent<TestModel, String>> assertEventIsPatch(String value, int version) {
        return assertEventIs(EventType.PATCHED).andThen(assertNullMergedInto()).andThen(assertVersion(version))
                .andThen(assertEventContentIs(value));
    }

    public static Consumer<HistoryEvent<TestModel, String>> assertEventIsDeprecated() {
        return assertEventIs(EventType.DEPRECATED).andThen(assertNullMergedInto()).andThen(assertNullVersion())
                .andThen(assertNullData());
    }

    public static Consumer<HistoryEvent<TestModel, String>> assertEventIsMerged(String accession) {
        return assertEventIs(EventType.MERGED).andThen(assertMergedInto(accession)).andThen(assertNullVersion())
                .andThen(assertNullData());
    }

}
