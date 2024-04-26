/*
 *
 * Copyright 2019 EMBL - European Bioinformatics Institute
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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import uk.ac.ebi.ampt2d.commons.accession.rest.dto.AccessionResponseDTO;
import uk.ac.ebi.ampt2d.test.configuration.BasicRestControllerForDecoratedAccessionTestConfiguration;
import uk.ac.ebi.ampt2d.test.rest.BasicRestModel;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJsonTesters
@AutoConfigureTestDatabase
@ContextConfiguration(classes = {BasicRestControllerForDecoratedAccessionTestConfiguration.class})
public class DecoratedAccessionMergedExceptionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JacksonTester<BasicRestModel> jsonModel;

    @Autowired
    private JacksonTester<List<BasicRestModel>> jsonModelList;

    @Autowired
    private JacksonTester<List<AccessionResponseDTO<BasicRestModel, BasicRestModel, String, String>>> jsonAccessions;

    @Test
    public void testAllOperationsAfterMerge() throws Exception {
        String accession1 = extractAccession(doAccession("merge-test-1"));
        String accession2 = extractAccession(doAccession("merge-test-2"));
        String accession3 = extractAccession(doAccession("merge-test-3"));
        Assert.assertEquals("EGA00000000100", accession1);
        Assert.assertEquals("EGA00000000101", accession2);
        Assert.assertEquals("EGA00000000102", accession3);
        doMerge(accession2, accession3).andExpect(status().isOk());
        doMerge(accession1, accession2).andExpect(status().isNotFound()).andExpect(jsonPath("$.message")
                .value(accession2 + " has been already merged into " + accession3));
        doMerge(accession1, accession3).andExpect(status().isOk());
        doMerge(accession1, accession3).andExpect(status().isNotFound()).andExpect(jsonPath("$.message")
                .value(accession1 + " has been already merged into " + accession3));

        //test deprecate after merge
        mockMvc.perform(delete("/v1/test/" + accession1).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message")
                .value(accession1 + " has been already merged into " + accession3));

        //test patch after merge
        mockMvc.perform(patch("/v1/test/" + accession1)
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .content(jsonModel.write(new BasicRestModel("patch-test-3")).getJson()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message")
                .value(accession1 + " has been already merged into " + accession3));

        //test update after merge
        mockMvc.perform(post("/v1/test/" + accession2 + "/1")
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                .content(jsonModel.write(new BasicRestModel("patch-test-3")).getJson()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message")
                .value(accession2 + " has been already merged into " + accession3));

        //test get after merge
        mockMvc.perform(get("/v1/test/" + accession2))
                .andExpect(status().isMovedPermanently())
                .andExpect(redirectedUrl("http://localhost/v1/test/"+accession3));
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

    private String extractAccession(MvcResult mvcResult) throws java.io.IOException {
        return jsonAccessions.parseObject(mvcResult.getResponse().getContentAsString()).get(0).getAccession();
    }

    private ResultActions doMerge(String accessionOrigin, String mergeInto) throws Exception {
        return mockMvc.perform(post("/v1/test/{accession}/merge", accessionOrigin).param("mergeInto",
                mergeInto));
    }

}