/*
 *
 * Copyright 2017 EMBL - European Bioinformatics Institute
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
package uk.ac.ebi.ampt2d.accession.server;

import uk.ac.ebi.ampt2d.accession.file.UuidFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class FileAccessionResponse {

    private List<FileAccession> accessions;

    public FileAccessionResponse() {
    }

    public FileAccessionResponse(Map<UuidFile, UUID> accessionMap) {
        this.accessions = accessionMap.entrySet().stream()
                                      .map(entry -> new FileAccession(entry.getKey(), entry.getValue()))
                                      .collect(Collectors.toList());
    }

    public List<FileAccession> getAccessions() {
        return accessions;
    }

    public void setAccessions(List<FileAccession> accessions) {
        this.accessions = accessions;
    }

    static class FileAccession {

        private UuidFile file;

        private UUID accession;

        public FileAccession() {
        }

        FileAccession(UuidFile file, UUID accession) {
            this.file = file;
            this.accession = accession;
        }

        public UuidFile getFile() {
            return file;
        }

        public void setFile(UuidFile file) {
            this.file = file;
        }

        public UUID getAccession() {
            return accession;
        }

        public void setAccession(UUID accession) {
            this.accession = accession;
        }
    }
}
