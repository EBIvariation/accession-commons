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
package uk.ac.ebi.ampt2d.commons.accession.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import uk.ac.ebi.ampt2d.commons.accession.rest.dto.AccessionResponseDTO;
import uk.ac.ebi.ampt2d.test.configuration.BasicRestControllerTestConfiguration;
import uk.ac.ebi.ampt2d.test.rest.BasicRestModel;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJsonTesters
@AutoConfigureTestDatabase
@ContextConfiguration(classes = {BasicRestControllerTestConfiguration.class})
public class BasicRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JacksonTester<BasicRestModel> jsonModel;

    @Autowired
    private JacksonTester<List<BasicRestModel>> jsonModelList;

    @Autowired
    private JacksonTester<List<AccessionResponseDTO<BasicRestModel, BasicRestModel, String, String>>> jsonAccessions;

    @Test
    public void testNoContentIfAccessioningDoesNotExist() throws Exception {
        doGet("notExistingId", status().isNotFound());
    }

    private ResultActions doGet(String accession) throws Exception {
        return doGet(accession, status().isOk());
    }

    private ResultActions doGet(String accession, ResultMatcher resultMatcher) throws Exception {
        return mockMvc.perform(get("/v1/test/" + accession).contentType(MediaType.APPLICATION_JSON))
                .andExpect(resultMatcher);
    }

    @Test
    public void testAccessionOk() throws Exception {
        doAccession("simpleTest");
    }

    private ResultActions doAccession(ResultMatcher resultMatcher, String... values) throws Exception {
        final BasicRestModel[] restModels = Arrays.stream(values).map(s -> new BasicRestModel(s))
                .toArray(size -> new BasicRestModel[size]);
        return mockMvc.perform(post("/v1/test/" + "applicationInstanceId")
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .content(jsonModelList.write(Arrays.asList(restModels)).getJson()))
                .andExpect(resultMatcher);
    }

    private MvcResult doAccession(String... values) throws Exception {
        return doAccession(status().isOk(), values)
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(values.length)))
                .andExpect(jsonPath("$[*].data.value", containsInAnyOrder(values)))
                .andReturn();
    }

    @Test
    public void testMultipleAccessionOk() throws Exception {
        doAccession("simpleTest2", "simpleTest3");
    }

    @Test
    public void testThrowExceptions() throws Exception {
        doAccession(status().isInternalServerError(), "MissingUnsavedAccessionsException")
                .andExpect(jsonPath("$.exception")
                        .value("uk.ac.ebi.ampt2d.commons.accession.core.exceptions.MissingUnsavedAccessionsException"));
        doAccession(status().isInternalServerError(), "AccessionIsNotPendingException")
                .andExpect(jsonPath("$.exception")
                        .value("uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionIsNotPendingException"));
        doAccession(status().isInternalServerError(), "AccessionCouldNotBeGeneratedException")
                .andExpect(jsonPath("$.exception")
                        .value("uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionCouldNotBeGeneratedException"));
    }

    @Test
    public void testUpdate() throws Exception {
        String accession = extractAccession(doAccession("update-test-1"));
        doUpdate(accession, 1, "update-test-1b");
    }

    private String extractAccession(MvcResult mvcResult) throws java.io.IOException {
        return jsonAccessions.parseObject(mvcResult.getResponse().getContentAsString()).get(0).getAccession();
    }

    private ResultActions doUpdate(String accession, int version, String value,
                                   ResultMatcher resultMatcher) throws Exception {
        return mockMvc.perform(post("/v1/test/" + accession + "/" + version)
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .content(jsonModel.write(new BasicRestModel(value)).getJson()))
                .andExpect(resultMatcher);
    }

    private void doUpdate(String accession, int version, String value) throws Exception {
        doUpdate(accession, version, value, status().isOk())
                .andExpect(jsonPath("$.versions." + version + ".value").value(value));
    }

    @Test
    public void testUpdateAccessionDoesNotExist() throws Exception {
        doUpdate("doesnotexist", 1, "update-accession-does-not-exist", status().isNotFound());
    }

    @Test
    public void testHashCollisionUpdate() throws Exception {
        final MvcResult mvcResult = doAccession("update-test-2", "update-test-3");

        final AccessionResponseDTO<BasicRestModel, BasicRestModel, String, String> response =
                jsonAccessions.parseObject(mvcResult.getResponse().getContentAsString()).get(1);

        doUpdate(response.getAccession(), response.getVersion(), "update-test-2",
                status().isConflict());
    }

    @Test
    public void patchOperation() throws Exception {
        String accession = extractAccession(doAccession("patch-test-1"));
        doPatch(accession, "patch-test-1b");
    }

    private void doPatch(String accession, String value) throws Exception {
        doPatch(accession, value, status().isOk())
                .andExpect(jsonPath("$.versions.*.value", hasSize(greaterThan(1))));
    }

    private ResultActions doPatch(String accession, String value, ResultMatcher resultMatcher)
            throws Exception {
        return mockMvc.perform(patch("/v1/test/" + accession)
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .content(jsonModel.write(new BasicRestModel(value)).getJson()))
                .andExpect(resultMatcher);
    }

    @Test
    public void testPatchAccessionDoesNotExist() throws Exception {
        doPatch("doesnotexist", "patch-accession-does-not-exist", status().isNotFound());
    }

    @Test
    public void testGetAccessionVersion() throws Exception {
        String accession = extractAccession(doAccession("get-accession-version-test-1"));
        doPatch(accession, "get-accession-version-test-1b");

        doGetVersion(accession, 1)
                .andExpect(jsonPath("$.data.value").value("get-accession-version-test-1"));
        doGetVersion(accession, 2)
                .andExpect(jsonPath("$.data.value").value("get-accession-version-test-1b"));
    }

    private ResultActions doGetVersion(String accession, int version) throws Exception {
        return doGet(accession + "/" + version);
    }

    private ResultActions doGetVersion(String accession, int version, ResultMatcher resultMatcher) throws Exception {
        return doGet(accession + "/" + version, resultMatcher);
    }

    @Test
    public void testGetAccessionVersionNotExists() throws Exception {
        String accession = extractAccession(doAccession("get-accession-version-test-2"));
        doUpdate(accession, 1, "get-accession-version-test-2b");

        doGetVersion("notexists", 1, status().isNotFound());
        doGetVersion(accession, 3, status().isNotFound());
    }

    @Test
    public void testGetAccession() throws Exception {
        final MvcResult mvcResult = doAccession("get-accession-test-1");
        String accession = extractAccession(mvcResult);
        doUpdate(accession, 1, "get-accession-test-1b");

        doGet(accession)
                .andExpect(jsonPath("$.data.value").value("get-accession-test-1b"));
    }

    @Test
    public void testCollectionOfDTOValidation() throws Exception {
        doAccession(status().is4xxClientError(), null, null)
                .andExpect(jsonPath("$.exception")
                        .value("jakarta.validation.ValidationException"))
                .andExpect(jsonPath("$.message")
                        .value("basicRestModelList[0] : Please provide a value\n" +
                                "basicRestModelList[1] : Please provide a value\n"));
    }

    @Test
    public void testDTOValidation() throws Exception {
        String accession = extractAccession(doAccession("dto-validation"));
        doUpdate(accession, 1, null, status().is4xxClientError())
                .andExpect(jsonPath("$.exception")
                        .value("org.springframework.web.bind.MethodArgumentNotValidException"))
                .andExpect(jsonPath("$.message").value("Please provide a value"));
    }

    @Test
    public void testDeprecate() throws Exception {
        String accession = extractAccession(doAccession("deprecate-test-1"));
        doDeprecate(accession);
        doGet(accession, status().isGone());
        doGetVersion(accession, 1, status().isGone());
    }

    @Test
    public void testDeprecateAndUpdate() throws Exception {
        String accession = extractAccession(doAccession("deprecate-update-test-1"));
        doDeprecate(accession);
        doUpdate(accession, 1, "deprecate-update-test-1b", status().isGone());
    }

    @Test
    public void testDeprecateAndPatch() throws Exception {
        String accession = extractAccession(doAccession("deprecate-patch-test-1"));
        doDeprecate(accession);
        doPatch(accession, "deprecate-patch-test-1b", status().isGone());
    }

    private void doDeprecate(String accession) throws Exception {
        mockMvc.perform(delete("/v1/test/" + accession).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testMergeWithSelf() throws Exception {
        mockMvc.perform(post("/v1/test/{accession}/merge", "accession").param("mergeInto", "accession"))
                .andExpect(status().is4xxClientError()).andExpect(jsonPath("$.message").value("Accessions cannot be" +
                " self merged"));
    }

    @Test
    public void testMerge() throws Exception {
        String accession1 = extractAccession(doAccession("merge-test-1"));
        String accession2 = extractAccession(doAccession("merge-test-2"));
        doMerge(accession1, accession2).andExpect(status().isOk());
        doGet(accession1, status().is3xxRedirection()).andExpect(
                redirectedUrlPattern("**/v1/test/" + accession2));
        doMerge(accession1, accession2).andExpect(status().isNotFound()).
                andExpect(jsonPath("$.message").value(accession1 + " has been already merged into " + accession2));
        doMerge(accession2, accession1).andExpect(status().isNotFound()).
                andExpect(jsonPath("$.message").value(accession1 + " has been already merged into " + accession2));
    }

    @Test
    public void testDeprecateAfterMerge() throws Exception {
        String accession1 = extractAccession(doAccession("merge-test-1"));
        String accession2 = extractAccession(doAccession("merge-test-2"));
        doMerge(accession1, accession2).andExpect(status().isOk());
        mockMvc.perform(delete("/v1/test/" + accession1).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message")
                .value(accession1 + " has been already merged into " + accession2));
    }

    private ResultActions doMerge(String accessionOrigin, String mergeInto) throws Exception {
        return mockMvc.perform(post("/v1/test/{accession}/merge", accessionOrigin).param("mergeInto",
                mergeInto));
    }

}
