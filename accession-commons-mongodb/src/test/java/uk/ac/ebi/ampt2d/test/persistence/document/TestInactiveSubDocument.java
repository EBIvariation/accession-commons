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
package uk.ac.ebi.ampt2d.test.persistence.document;

import uk.ac.ebi.ampt2d.commons.accession.persistence.mongodb.document.InactiveSubDocument;
import uk.ac.ebi.ampt2d.test.models.TestModel;

public class TestInactiveSubDocument extends InactiveSubDocument<TestModel, String> implements TestModel {

    private String value;

    private String nonIdentifyingValue;

    TestInactiveSubDocument() {
        super();
    }

    public TestInactiveSubDocument(TestDocument testDocument) {
        super(testDocument);
        this.value = testDocument.getValue();
        this.nonIdentifyingValue = testDocument.getNonIdentifyingValue();
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getNonIdentifyingValue() {
        return nonIdentifyingValue;
    }

    @Override
    public TestModel getModel() {
        return this;
    }
}
