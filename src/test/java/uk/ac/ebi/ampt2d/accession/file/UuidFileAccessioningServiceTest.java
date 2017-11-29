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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = FileConfiguration.class)
@ActiveProfiles("file-uuid")
public class UuidFileAccessioningServiceTest {

    @Autowired
    private UuidFileAccessioningService service;

    @Test
    public void sameAccessionsAreReturnedForIdenticalFiles() throws Exception {
        String checksumA = "checksumA";
        String checksumB = "checksumB";
        UuidFile fileA = new UuidFile(checksumA);
        UuidFile fileB = new UuidFile(checksumB);

        Map<UuidFile, UUID> generatedAccessions = service.getAccessions(Arrays.asList(fileA, fileB));

        fileA = new UuidFile(checksumA);
        fileB = new UuidFile(checksumB);

        Map<UuidFile, UUID> retrievedAccessions = service.getAccessions(Arrays.asList(fileA, fileB));

        assertEquals(generatedAccessions.get(fileA), retrievedAccessions.get(fileA));
        assertEquals(generatedAccessions.get(fileB), retrievedAccessions.get(fileB));
    }

    @Test
    public void everyNewObjectReceiveOneAccession() throws Exception {
        List<UuidFile> newObjects = Arrays.asList(new UuidFile("checksumA"), new UuidFile("checksumB"), new UuidFile("checksumC"));
        Map<UuidFile, UUID> accessions = service.getAccessions(newObjects);

        for (UuidFile object : newObjects) {
            assertNotNull(accessions.get(object));
        }
    }

    @Test
    public void sameObjectsGetSameAccession() throws Exception {

        UuidFile fileA = new UuidFile("checksumA");
        UuidFile fileB = new UuidFile("checksumA");

        List<UuidFile> newObjects = Arrays.asList(fileA, fileB);
        Map<UuidFile, UUID> accessions = service.getAccessions(newObjects);

        UUID accession1 = accessions.get(fileA);
        UUID anotherAccession1 = accessions.get(fileB);

        assertEquals(accession1, anotherAccession1);
    }

    @Test
    public void differentObjectsGetDifferentAccessions() throws Exception {
        UuidFile fileA = new UuidFile("checksumA");
        UuidFile fileB = new UuidFile("checksumB");

        List<UuidFile> newObjects = Arrays.asList(fileA, fileB);
        Map<UuidFile, UUID> accessions = service.getAccessions(newObjects);

        UUID accession1 = accessions.get(fileA);
        UUID anotherAccession1 = accessions.get(fileB);

        assertNotEquals(accession1, anotherAccession1);
    }

    @Test
    public void differentCallsToTheServiceUsingSameObjectsWillReturnSameAccessions() throws Exception {
        UuidFile fileA = new UuidFile("checksumA");
        UuidFile fileB = new UuidFile("checksumB");

        List<UuidFile> newObjects = Arrays.asList(fileA, fileB);
        Map<UuidFile, UUID> accessions = service.getAccessions(newObjects);

        UUID accession1 = accessions.get(fileA);
        UUID anotherAccession1 = accessions.get(fileB);

        Map<UuidFile, UUID> accessionsFromSecondServiceCall = service.getAccessions(Arrays.asList(fileA, fileB));

        assertEquals(accession1, accessionsFromSecondServiceCall.get(fileA));
        assertEquals(anotherAccession1, accessionsFromSecondServiceCall.get(fileB));
    }

    @Test
    public void mixingAlreadyAccessionedAndNewObjectsIsAllowed() throws Exception {
        UuidFile fileA = new UuidFile("checksumA");
        UuidFile fileB = new UuidFile("checksumB");

        Map<UuidFile, UUID> accessions = service.getAccessions(Arrays.asList(fileA, fileB));

        UUID accession1 = accessions.get(fileA);
        UUID accession2 = accessions.get(fileB);

        UuidFile fileC = new UuidFile("checksumC");
        UuidFile fileD = new UuidFile("checksumD");

        List<UuidFile> objectsToAccession = Arrays.asList(fileA, fileB, fileC, fileD);
        Map<UuidFile, UUID> accessionsFromSecondServiceCall = service
                .getAccessions(objectsToAccession);

        assertEquals(accession1, accessionsFromSecondServiceCall.get(fileA));
        assertEquals(accession2, accessionsFromSecondServiceCall.get(fileB));
        assertNotNull(accessionsFromSecondServiceCall.get(fileC));
        assertNotNull(accessionsFromSecondServiceCall.get(fileD));

        assertEquals(objectsToAccession.size(),
                new HashSet<>(accessionsFromSecondServiceCall.values()).size());
    }

}