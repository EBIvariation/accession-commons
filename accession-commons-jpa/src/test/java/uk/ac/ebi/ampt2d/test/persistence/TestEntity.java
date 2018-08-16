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
package uk.ac.ebi.ampt2d.test.persistence;

import uk.ac.ebi.ampt2d.commons.accession.core.models.AccessionWrapper;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.entities.AccessionedEntity;
import uk.ac.ebi.ampt2d.test.models.TestModel;

import javax.persistence.Entity;

@Entity
public class TestEntity extends AccessionedEntity<TestModel, String> implements TestModel {

    private String something;

    private String nonIdentifyingValue;

    TestEntity() {
        super(null, null, 1);
    }

    public TestEntity(AccessionWrapper<TestModel, String, String> model) {
        this(model.getAccession(), model.getHash(), model.getVersion(), model.getData().getValue(),
             model.getData().getNonIdentifyingValue());
    }

    public TestEntity(String accession, String hashedMessage, int version, String something,
                      String nonIdentifyingValue) {
        super(hashedMessage, accession, version);
        this.something = something;
        this.nonIdentifyingValue = nonIdentifyingValue;
    }

    @Override
    public String getValue() {
        return something;
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
