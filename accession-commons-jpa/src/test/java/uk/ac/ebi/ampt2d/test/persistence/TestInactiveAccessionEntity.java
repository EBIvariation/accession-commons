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
import java.time.LocalDateTime;

@Entity
public class TestInactiveAccessionEntity extends InactiveAccessionEntity<TestModel, String> implements TestModel {

    private String something;

    TestInactiveAccessionEntity() {
        super();
    }

    public TestInactiveAccessionEntity(TestEntity testEntity) {
        super(testEntity);
        this.something = testEntity.getValue();
    }

    public String getValue() {
        return something;
    }

    @Override
    public String getHashedMessage() {
        return null;
    }

    @Override
    public LocalDateTime getCreatedDate() {
        return null;
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public TestModel getModel() {
        return this;
    }
}
