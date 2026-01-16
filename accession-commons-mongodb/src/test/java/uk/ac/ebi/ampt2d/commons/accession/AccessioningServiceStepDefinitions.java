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
package uk.ac.ebi.ampt2d.commons.accession;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionDoesNotExistException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionMergedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.HashAlreadyExistsException;
import uk.ac.ebi.ampt2d.test.models.TestModel;
import uk.ac.ebi.ampt2d.test.testers.AccessioningServiceTester;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AccessioningServiceStepDefinitions {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private AccessioningServiceTester tester;

    @Before
    public void setUp() {
        // Clean up all collections before each scenario
        mongoTemplate.getCollectionNames().forEach(name -> mongoTemplate.dropCollection(name));
    }

    @Given("^already accessioned ([\\w-,]+)$")
    @When("^user submits ([\\w-,]+) to accessioning service$")
    public void submitObjectsToAccessioningService(String objects) {
        tester.getOrCreate(toModels(objects));
    }

    private List<TestModel> toModels(String values) {
        return Arrays.stream(values.split(",")).map(TestModel::of).collect(Collectors.toList());
    }

    @Then("^user received accessions: ([\\w-,]+)$")
    public void iShouldReceiveAccessions(String objects) {
        final String[] accessions = objects.split(",");
        tester.getSingleVersionResults().assertAccessions(accessions);
    }

    @When("^user sends patch (\\w+) for accession ([\\w-]+)$")
    public void userSendsPatchInputForAccession(String patchData, String accession) {
        tester.patch(accession, TestModel.of(patchData));
    }

    @Then("^user should receive (\\d+) (?:patches|patch) for accession ([\\w-]+)")
    public void userShouldReceivePatchesForAccession(int totalPatches, String accession) {
        tester.getLastMultipleVersionResult().assertAccession(accession).assertTotalVersions(totalPatches);
    }

    @And("^patch must have versions increased$")
    public void patchMustHaveVersionsIncreased() {
        tester.getLastMultipleVersionResult().assertVersionsAreIncreased();
    }

    @Then("^user should receive 'accession does not exist'$")
    public void userReceivesAccessionDoesNotExistError() {
        tester.getLastMethodResponse().assertThrow(AccessionDoesNotExistException.class);
    }

    @When("^user updates ([\\w-]+) patch (\\d+) with ([\\w-]+)$")
    public void userUpdatesAccessionWithInput(String accession, int patch, String newData) {
        tester.update(accession, patch, newData);
    }


    @And("^hash of patch (\\d+) should be ([\\w-]+)$")
    public void hashOfPatchShouldBeHash(int version, String hash) {
        tester.getLastMultipleVersionResult().assertHash(version, hash);
    }

    @When("^user merges ([\\w-]+) into ([\\w-]+) reason: ([\\w ]+)$")
    public void userMergesAWithB(String accessionA, String accessionB, String reason) {
        tester.merge(accessionA, accessionB, reason);
    }

    @Then("^user should receive 'hash already exists exception'$")
    public void weShouldHaveAUnknownError() {
        tester.getLastMethodResponse().assertThrow(HashAlreadyExistsException.class);
    }

    @Then("^operation finished correctly$")
    public void lastProcessFinishedOk() {
        tester.getLastMethodResponse().assertNoException();
    }

    @Then("^user should receive 'accession already merged exception'$")
    public void weShouldHaveAAccessionAlreadyMerged() {
        tester.getLastMethodResponse().assertThrow(AccessionMergedException.class);
    }

    @When("^user retrieves objects: ([\\w-,]+)$")
    public void userRetrievesObjectA(String values) {
        tester.get(toModels(values));
    }

    @Then("^user receives (\\d+) elements$")
    public void userReceivesANumberOfElements(int numberOfElements) {
        tester.getSingleVersionResults().assertSize(numberOfElements);
    }

    @And("^user received a response with values: ([\\w-,]+)$")
    public void userReceivedAResponseWithObject(String values) {
        tester.getSingleVersionResults().assertAccessionValues(values.split(","));
    }

    @When("^user retrieves accessions: ([\\w-,]+)$")
    public void userRetrievesAccessionsIdServiceA(String accessionIds) {
        tester.getAccessions(accessionIds.split(","));
    }

    @Then("^user receives no data$")
    public void userReceivesNoData() {
        assertTrue(tester.getSingleVersionResults().getData().isEmpty(), "User received data");
    }
}
