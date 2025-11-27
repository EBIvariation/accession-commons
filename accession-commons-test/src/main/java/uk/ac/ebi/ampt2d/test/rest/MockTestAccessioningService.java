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
package uk.ac.ebi.ampt2d.test.rest;

import uk.ac.ebi.ampt2d.commons.accession.core.AccessionSaveMode;
import uk.ac.ebi.ampt2d.commons.accession.core.BasicAccessioningService;
import uk.ac.ebi.ampt2d.commons.accession.core.DatabaseService;
import uk.ac.ebi.ampt2d.test.models.TestModel;

/**
 * Mock service, generates accessions using in-memory data structures
 */
public class MockTestAccessioningService extends BasicAccessioningService<TestModel, String, String> {


    public MockTestAccessioningService(DatabaseService<TestModel, String, String> databaseService) {
        super(
                new MockTestAccessionGenerator(),
                databaseService,
                testModel -> testModel.getValue(),
                s -> "hash-" + s,
                AccessionSaveMode.SAVE_ALL_THEN_RESOLVE
        );
    }

}