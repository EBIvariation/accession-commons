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

import uk.ac.ebi.ampt2d.test.models.TestModel;

import jakarta.validation.constraints.NotNull;

public class BasicRestModel implements TestModel {

    @NotNull(message = "Please provide a value")
    private String value;

    public BasicRestModel() {
    }

    public BasicRestModel(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

}
