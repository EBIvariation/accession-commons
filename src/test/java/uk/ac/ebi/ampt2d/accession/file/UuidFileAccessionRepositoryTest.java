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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.ampt2d.accession.AccessionGenerator;
import uk.ac.ebi.ampt2d.accession.UuidAccessionGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("file-uuid")
public class UuidFileAccessionRepositoryTest {

    @Autowired
    private UuidFileAccessionRepository accessionRepository;

    private AccessionGenerator<UuidFile, UUID> generator;

    private AccessionGenerator<UuidFile, UUID> alternativeGenerator;

    @Before
    public void setUp() throws Exception {
        generator = new UuidAccessionGenerator<>("ACC");
        alternativeGenerator = new UuidAccessionGenerator<>("ALT");

    }

    @Test
    public void testFilesAreStoredInTheRepository() throws Exception {
        List<UuidFile> files = Arrays.asList(new UuidFile("checksumA"), new UuidFile("checksumB"));
        Map<UuidFile, UUID> accessionedFiles = generator.generateAccessions(new HashSet<>(files));
        for (Map.Entry<UuidFile, UUID> entry : accessionedFiles.entrySet()) {
            entry.getKey().setAccession(entry.getValue());
        }
        accessionRepository.save(accessionedFiles.keySet());

        assertEquals(2, accessionRepository.count());

        Collection<String> checksums = new ArrayList<>();
        checksums.add("checksumA");
        checksums.add("checksumB");
        Collection<UuidFile> accessionsFromRepository = accessionRepository.findByHashIn(checksums);
        assertEquals(new ArrayList<>(accessionedFiles.keySet()), accessionsFromRepository);
    }

    @Test
    public void addingTheSameFilesWillReplaceTheAccessionsInTheRepository() throws Exception {
        List<UuidFile> files = Arrays.asList(new UuidFile("checksumA"), new UuidFile("checksumB"));
        HashSet<UuidFile> fileSet = new HashSet<>(files);

        // Store the files with the initial accessions
        Map<UuidFile, UUID> accessionedFiles = generator.generateAccessions(fileSet);
        for (Map.Entry<UuidFile, UUID> entry : accessionedFiles.entrySet()) {
            entry.getKey().setAccession(entry.getValue());
        }
        accessionRepository.save(accessionedFiles.keySet());
        assertEquals(2, accessionRepository.count());

        // Storing again the same files with new accessions overwrites the existing ones
        Map<UuidFile, UUID> alternativeAccesionedFiles = alternativeGenerator.generateAccessions(fileSet);
        for (Map.Entry<UuidFile, UUID> entry : alternativeAccesionedFiles.entrySet()) {
            entry.getKey().setAccession(entry.getValue());
        }
        accessionRepository.save(alternativeAccesionedFiles.keySet());
        assertEquals(2, accessionRepository.count());
    }

    @Test(expected = org.springframework.dao.DataIntegrityViolationException.class)
    public void cantStoreFileWithoutAccession() {
        UuidFile file = new UuidFile("cantStoreFileWithoutAccession");
        accessionRepository.save(file);
    }

    @Test(expected = org.springframework.dao.DataIntegrityViolationException.class)
    public void cantStoreMultipleFilesWithSameHash() {
        UuidFile originalFile = new UuidFile("cantStoreMultipleFilesWithSameHash");
        originalFile.setAccession(UUID.randomUUID());
        accessionRepository.save(originalFile);
        assertEquals(1, accessionRepository.count());

        UuidFile newFile = new UuidFile(originalFile.getHash());
        newFile.setAccession(UUID.randomUUID());
        accessionRepository.save(newFile);
    }

    @Test(expected = org.springframework.dao.DataIntegrityViolationException.class)
    public void cantStoreMultipleFilesWithSameAccession() {
        UuidFile originalFile = new UuidFile("cantStoreMultipleFilesWithSameAccession1");
        originalFile.setAccession(UUID.randomUUID());
        accessionRepository.save(originalFile);
        assertEquals(1, accessionRepository.count());

        UuidFile newFile = new UuidFile("cantStoreMultipleFilesWithSameAccession2");
        newFile.setAccession(originalFile.getAccession());
        accessionRepository.save(newFile);
    }
}