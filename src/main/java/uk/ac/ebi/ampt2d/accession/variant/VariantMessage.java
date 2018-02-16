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

import com.fasterxml.jackson.annotation.JsonIgnore;
import uk.ac.ebi.ampt2d.accession.HashableMessage;
import uk.ac.ebi.ampt2d.accession.Message;

public class VariantMessage implements HashableMessage<String>, Message {

    private String assemblyAccession;

    private String projectAccession;

    private String chromosome;

    private long start;

    private VariantType type;

    public VariantMessage() {
    }

    public VariantMessage(String assemblyAccession, String projectAccession, String chromosome, long start, VariantType type) {
        this.assemblyAccession = assemblyAccession;
        this.projectAccession = projectAccession;
        this.chromosome = chromosome;
        this.start = start;
        this.type = type;
    }

    @Override
    @JsonIgnore
    public String getHashableMessage() {
        return getAssemblyAccession() + getChromosome() + getProjectAccession() + getStart() + getType();
    }

    @Override
    @JsonIgnore
    public String getMessage() {
        return getHashableMessage();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VariantMessage that = (VariantMessage) o;

        return getHashableMessage() != null ? getHashableMessage().equals(that.getHashableMessage()) : that
                .getHashableMessage() == null;
    }

    @Override
    public int hashCode() {
        return getHashableMessage() != null ? getHashableMessage().hashCode() : 0;
    }

    @Override
    public String toString() {
        return "VariantMessage{" +
                "assemblyAccession='" + assemblyAccession + '\'' +
                ", projectAccession='" + projectAccession + '\'' +
                ", chromosome='" + chromosome + '\'' +
                ", start=" + start +
                ", type=" + type +
                '}';
    }
}
