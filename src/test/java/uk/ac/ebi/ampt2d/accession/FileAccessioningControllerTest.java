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
package uk.ac.ebi.ampt2d.accession;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.ampt2d.accession.file.File;
import uk.ac.ebi.ampt2d.accession.file.UuidFile;
import uk.ac.ebi.ampt2d.accession.file.UuidFileAccessionRepository;

import java.util.Arrays;
import java.util.Set;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("file-uuid")
public class FileAccessioningControllerTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private UuidFileAccessionRepository uuidFileAccessionRepository;

    @Test
    public void testRestApi() {
        File fileA = new UuidFile("checksumA");
        File fileB = new UuidFile("checksumB");
        File fileC = new UuidFile("checksumC");

        String url = "/v1/accession/file";
        HttpEntity<Object> requestEntity = new HttpEntity<>(Arrays.asList(fileA, fileB, fileC));

        ResponseEntity<Set<UuidFile>> response = testRestTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<Set<UuidFile>>() {
        });

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3, response.getBody().size());
    }

    @Test
    public void requestPostTwiceAndWeGetSameAccessions() {
        File fileA = new UuidFile("checksumA");
        File fileB = new UuidFile("checksumB");
        File fileC = new UuidFile("checksumC");

        String url = "/v1/accession/file";
        HttpEntity<Object> requestEntity = new HttpEntity<>(Arrays.asList(fileA, fileB, fileC));

        ResponseEntity<Set<UuidFile>> response = testRestTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<Set<UuidFile>>() {
        });

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3, response.getBody().size());
        assertEquals(3, uuidFileAccessionRepository.count());

        //Accessing Post Request again with same files
        response = testRestTemplate.exchange(url, HttpMethod.POST, requestEntity, new ParameterizedTypeReference<Set<UuidFile>>() {
        });

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3, response.getBody().size());
        assertEquals(3, uuidFileAccessionRepository.count());
    }

}
