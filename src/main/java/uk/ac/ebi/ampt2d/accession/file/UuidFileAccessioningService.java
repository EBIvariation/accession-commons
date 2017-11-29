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
package uk.ac.ebi.ampt2d.accession.file;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.ac.ebi.ampt2d.accession.AccessioningProperties;
import uk.ac.ebi.ampt2d.accession.AccessioningService;
import uk.ac.ebi.ampt2d.accession.UuidAccessionGenerator;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Profile("file-uuid")
public class  UuidFileAccessioningService extends AccessioningService<UuidFile, UUID> {

    @Autowired
    private UuidFileAccessionRepository fileRepository;

    public UuidFileAccessioningService(AccessioningProperties properties) {
        super(new UuidAccessionGenerator<>(properties.getNamespace()));
    }

    @Override
    public Map<UuidFile, UUID> get(List<UuidFile> objects) {
        List<String> checksums = objects.stream().map(UuidFile::getHash).collect(Collectors.toList());
        Collection<UuidFile> filesInRepository = fileRepository.findByHashIn(checksums);
        return filesInRepository.stream().collect(Collectors.toMap(Function.identity(), UuidFile::getAccession));
    }

    @Override
    public void add(Map<UuidFile, UUID> accessions) {
        for (UuidFile file : accessions.keySet()) {
            file.setAccession(accessions.get(file));
        }
        fileRepository.save(accessions.keySet());
    }
}
