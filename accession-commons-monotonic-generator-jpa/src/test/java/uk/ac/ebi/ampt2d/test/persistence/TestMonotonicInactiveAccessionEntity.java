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

import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.entities.InactiveAccessionEntity;
import uk.ac.ebi.ampt2d.test.models.TestModel;

import jakarta.persistence.Entity;

@Entity
public class TestMonotonicInactiveAccessionEntity extends InactiveAccessionEntity<TestModel, Long> implements TestModel {

    private String something;

    TestMonotonicInactiveAccessionEntity() {
        super();
    }

    public TestMonotonicInactiveAccessionEntity(TestMonotonicEntity testEntity) {
        super(testEntity);
        this.something = testEntity.getValue();
    }

    @Override
    public TestModel getModel() {
        return this;
    }

    @Override
    public String getValue() {
        return something;
    }
}
