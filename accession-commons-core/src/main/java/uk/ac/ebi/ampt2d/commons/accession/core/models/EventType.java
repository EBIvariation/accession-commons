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
package uk.ac.ebi.ampt2d.commons.accession.core.models;

public enum EventType {

    // a new accessioned object is created
    CREATED,

    // accessioned object has changed
    UPDATED,

    // new object version with the same accession
    PATCHED,

    // accession1 merged into accession2
    MERGED,

    // new accession id created because of RS split
    RS_SPLIT,

    // accession is no longer valid
    DEPRECATED

}
