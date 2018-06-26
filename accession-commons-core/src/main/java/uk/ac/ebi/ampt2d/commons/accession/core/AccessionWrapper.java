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
package uk.ac.ebi.ampt2d.commons.accession.core;

/**
 * Wrapper containing the object that has been accessioned, as well as additional information like the accession or a
 * flag indicating whether the accession is active. To be used in the service layer.
 *
 * @param <MODEL>
 * @param <HASH>
 * @param <ACCESSION>
 */
public class AccessionWrapper<MODEL, HASH, ACCESSION> {

    private ACCESSION accession;

    private HASH hash;

    private MODEL data;

    private int version;

    public AccessionWrapper(ACCESSION accession, HASH hash, MODEL data) {
        this(accession, hash, data, 1);
    }

    public AccessionWrapper(ACCESSION accession, HASH hash, MODEL data, int version) {
        this.accession = accession;
        this.hash = hash;
        this.data = data;
        this.version = version;
    }

    public ACCESSION getAccession() {
        return accession;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public HASH getHash() {
        return hash;
    }

    public MODEL getData() {
        return data;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccessionWrapper)) return false;

        AccessionWrapper<?, ?, ?> that = (AccessionWrapper<?, ?, ?>) o;

        if (version != that.version) return false;
        if (!accession.equals(that.accession)) return false;
        return hash.equals(that.hash);
    }

    @Override
    public int hashCode() {
        int result = accession.hashCode();
        result = 31 * result + hash.hashCode();
        result = 31 * result + version;
        return result;
    }

    @Override
    public String toString() {
        return "AccessionWrapper{" +
                "accession=" + accession +
                ", hash=" + hash +
                ", version=" + version +
                '}';
    }
}
