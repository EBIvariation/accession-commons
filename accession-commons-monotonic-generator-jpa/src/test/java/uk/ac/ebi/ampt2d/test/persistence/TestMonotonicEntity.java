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

import jakarta.persistence.Entity;

@Entity
public class TestMonotonicEntity extends AccessionedEntity<TestModel, Long> implements TestModel {

    private String something;

    TestMonotonicEntity() {
        super(null, null, 1);
    }

    public TestMonotonicEntity(AccessionWrapper<TestModel, String, Long> wrapper) {
        this(wrapper.getAccession(), wrapper.getHash(), wrapper.getVersion(),
                wrapper.getData().getValue());
    }

    public TestMonotonicEntity(Long accession, String hashedMessage, int version, String something) {
        super(hashedMessage, accession, version);
        this.something = something;
    }

    @Override
    public String getValue() {
        return something;
    }

    @Override
    public TestModel getModel() {
        return this;
    }
}
