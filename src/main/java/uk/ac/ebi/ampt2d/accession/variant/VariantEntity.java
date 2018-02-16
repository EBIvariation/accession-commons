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
package uk.ac.ebi.ampt2d.accession.variant;

import uk.ac.ebi.ampt2d.accession.AccessionableEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.Size;

@Entity
public class VariantEntity implements AccessionableEntity {

    @Id
    @Column(nullable = false, unique = true, updatable = false)
    @Size(max = 230, min = 0)
    private String accession;

    @Column(nullable = false)
    private String assemblyAccession;

    @Column(nullable = false)
    private String projectAccession;

    @Column(nullable = false)
    private String chromosome;

    @Column(nullable = false)
    private long start;

    @Column(nullable = false)
    private VariantType type;

    @Column(nullable = false, unique = true)
    private String hashedMessage;

    VariantEntity() {
    }

    public VariantEntity(String assemblyAccession, String projectAccession, String chromosome, long start, VariantType
            type) {
        this.assemblyAccession = assemblyAccession;
        this.projectAccession = projectAccession;
        this.chromosome = chromosome;
        this.start = start;
        this.type = type;
    }

    public String getAccession() {
        return this.accession;
    }

    public void setAccession(String accession) {
        this.accession = accession;
    }

    public void setHashedMessage(String hashedMessage) {
        this.hashedMessage = hashedMessage;
    }

    public String getAssemblyAccession() {
        return assemblyAccession;
    }

    public String getProjectAccession() {
        return projectAccession;
    }

    public String getChromosome() {
        return chromosome;
    }

    public long getStart() {
        return start;
    }

    public VariantType getType() {
        return type;
    }
}
