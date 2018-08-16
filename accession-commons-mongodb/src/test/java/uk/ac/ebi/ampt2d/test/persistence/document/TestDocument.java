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

import org.springframework.data.mongodb.core.mapping.Document;
import uk.ac.ebi.ampt2d.commons.accession.core.models.AccessionWrapper;
import uk.ac.ebi.ampt2d.commons.accession.persistence.mongodb.document.AccessionedDocument;
import uk.ac.ebi.ampt2d.test.models.TestModel;

@Document
public class TestDocument extends AccessionedDocument<TestModel, String> implements TestModel {

    private String value;

    private String nonIdentifyingValue;

    TestDocument() {
        super();
    }

    public TestDocument(String value, String hashedMessage, String accession) {
        super(hashedMessage, accession);
        this.value = value;
        this.nonIdentifyingValue = "";
    }

    public TestDocument(String value, String hashedMessage, String accession, int version) {
        this(value, "", hashedMessage, accession, version);
    }

    public TestDocument(String value, String nonIdentifyingValue, String hashedMessage, String accession, int version) {
        super(hashedMessage, accession, version);
        this.value = value;
        this.nonIdentifyingValue = nonIdentifyingValue;
    }

    public TestDocument(AccessionWrapper<TestModel, String, String> wrapper) {
        this(wrapper.getData().getValue(), wrapper.getData().getNonIdentifyingValue(), wrapper.getHash(),
             wrapper.getAccession(), wrapper.getVersion());
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getNonIdentifyingValue() {
        return nonIdentifyingValue;
    }

    public static TestDocument document(int num) {
        return new TestDocument("test-" + num, "h" + num, "a" + num);
    }

    public static TestDocument document(int accessionNum, int hashNum) {
        return new TestDocument("test-" + hashNum, "h" + hashNum, "a" + accessionNum);
    }

    public static TestDocument document(int accessionNum, String value) {
        return new TestDocument("test-" + value, "h" + value, "a" + accessionNum);
    }

    @Override
    public TestModel getModel() {
        return this;
    }
}
