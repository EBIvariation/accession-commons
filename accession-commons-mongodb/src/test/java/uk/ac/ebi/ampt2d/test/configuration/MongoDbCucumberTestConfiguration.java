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
package uk.ac.ebi.ampt2d.test.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import uk.ac.ebi.ampt2d.commons.accession.core.AccessionSaveMode;
import uk.ac.ebi.ampt2d.commons.accession.core.AccessioningService;
import uk.ac.ebi.ampt2d.commons.accession.core.BasicAccessioningService;
import uk.ac.ebi.ampt2d.commons.accession.core.DatabaseService;
import uk.ac.ebi.ampt2d.commons.accession.core.HistoryService;
import uk.ac.ebi.ampt2d.commons.accession.generators.SingleAccessionGenerator;
import uk.ac.ebi.ampt2d.commons.accession.hashing.SHA1HashingFunction;
import uk.ac.ebi.ampt2d.commons.accession.persistence.services.BasicHistoryService;
import uk.ac.ebi.ampt2d.commons.accession.persistence.services.BasicSpringDataRepositoryDatabaseService;
import uk.ac.ebi.ampt2d.test.models.TestModel;
import uk.ac.ebi.ampt2d.test.persistence.document.TestDocument;
import uk.ac.ebi.ampt2d.test.persistence.document.TestEventDocument;
import uk.ac.ebi.ampt2d.test.persistence.document.TestInactiveSubDocument;
import uk.ac.ebi.ampt2d.test.persistence.repository.TestOperationRepository;
import uk.ac.ebi.ampt2d.test.persistence.repository.TestRepository;
import uk.ac.ebi.ampt2d.test.persistence.service.TestMongoDbInactiveAccessionService;
import uk.ac.ebi.ampt2d.test.testers.AccessioningServiceTester;

@Configuration
@EntityScan(basePackages = {"uk.ac.ebi.ampt2d.test.persistence.document"})
@EnableMongoRepositories(basePackages = {"uk.ac.ebi.ampt2d.test.persistence.repository",
        "uk.ac.ebi.ampt2d.commons.accession.persistence.mongodb.repository"})
@EnableMongoAuditing
@AutoConfigureDataMongo
public class MongoDbCucumberTestConfiguration {

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private TestOperationRepository testOperationRepository;

    @Bean
    public TestMongoDbInactiveAccessionService testMongoDbInactiveAccessionService() {
        return new TestMongoDbInactiveAccessionService(
                testOperationRepository,
                TestInactiveSubDocument::new,
                TestEventDocument::new
        );
    }

    @Bean
    public DatabaseService<TestModel, String, String> testMongoDbService() {
        return new BasicSpringDataRepositoryDatabaseService<>(
                testRepository,
                TestDocument::new,
                testMongoDbInactiveAccessionService()
        );
    }

    @Bean
    public HistoryService<TestModel, String> testMongoDbHistoryService() {
        return new BasicHistoryService<>(testRepository, testMongoDbInactiveAccessionService());
    }

    @Bean
    public AccessioningService<TestModel, String, String> testMongoDbAccessioningService() {
        return new BasicAccessioningService<>(
                new SingleAccessionGenerator<>(o ->
                        "id-service-" + o.getValue()),
                testMongoDbService(),
                TestModel::getValue,
                new SHA1HashingFunction(),
                AccessionSaveMode.SAVE_ALL_THEN_RESOLVE
        );
    }

    @Bean
    @Scope("cucumber-glue")
    public AccessioningServiceTester accessioningServiceTester() {
        return new AccessioningServiceTester(testMongoDbAccessioningService());
    }

}
