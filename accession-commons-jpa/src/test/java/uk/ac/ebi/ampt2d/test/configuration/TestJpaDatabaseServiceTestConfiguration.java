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
import org.springframework.boot.test.autoconfigure.core.AutoConfigureCache;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import uk.ac.ebi.ampt2d.commons.accession.core.AccessionSaveMode;
import uk.ac.ebi.ampt2d.commons.accession.core.AccessioningService;
import uk.ac.ebi.ampt2d.commons.accession.core.BasicAccessioningService;
import uk.ac.ebi.ampt2d.commons.accession.generators.SingleAccessionGenerator;
import uk.ac.ebi.ampt2d.commons.accession.hashing.SHA1HashingFunction;
import uk.ac.ebi.ampt2d.commons.accession.persistence.services.BasicSpringDataRepositoryDatabaseService;
import uk.ac.ebi.ampt2d.commons.accession.core.DatabaseService;
import uk.ac.ebi.ampt2d.commons.accession.persistence.repositories.IAccessionedObjectCustomRepository;
import uk.ac.ebi.ampt2d.commons.accession.persistence.services.InactiveAccessionService;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.repositories.BasicJpaAccessionedObjectCustomRepositoryImpl;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.service.BasicJpaInactiveAccessionService;
import uk.ac.ebi.ampt2d.test.models.TestModel;
import uk.ac.ebi.ampt2d.test.persistence.TestEntity;
import uk.ac.ebi.ampt2d.test.persistence.TestInactiveAccessionEntity;
import uk.ac.ebi.ampt2d.test.persistence.TestInactiveAccessionRepository;
import uk.ac.ebi.ampt2d.test.persistence.TestRepository;
import uk.ac.ebi.ampt2d.test.persistence.TestStringHistoryRepository;
import uk.ac.ebi.ampt2d.test.persistence.TestStringOperationEntity;

@Configuration
@ComponentScan(basePackageClasses = IAccessionedObjectCustomRepository.class)
@EnableJpaAuditing
@ComponentScan(basePackageClasses = BasicJpaAccessionedObjectCustomRepositoryImpl.class)
@EntityScan("uk.ac.ebi.ampt2d.test.persistence")
@EnableJpaRepositories(basePackages = {"uk.ac.ebi.ampt2d.test.persistence",
        "uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.repositories"
})
@AutoConfigureCache
@AutoConfigureDataJpa
@AutoConfigureTestEntityManager
public class TestJpaDatabaseServiceTestConfiguration {

    @Autowired
    private TestRepository repository;

    @Autowired
    private TestStringHistoryRepository historyRepository;

    @Autowired
    private TestInactiveAccessionRepository testInactiveAccessionRepository;

    @Bean
    public DatabaseService<TestModel, String, String> databaseService() {
        return new BasicSpringDataRepositoryDatabaseService<>(
                repository,
                TestEntity::new,
                inactiveService()
        );
    }

    @Bean
    public InactiveAccessionService<TestModel, String, TestEntity> inactiveService() {
        return new BasicJpaInactiveAccessionService<>(
                historyRepository,
                TestInactiveAccessionEntity::new,
                testInactiveAccessionRepository,
                TestStringOperationEntity::new
        );
    }

    @Bean
    public AccessioningService<TestModel, String, String> accessioningService() {
        return new BasicAccessioningService<>(
                SingleAccessionGenerator.ofHashAccessionGenerator(
                        TestModel::getValue,
                        s -> "id-service-" + s
                ),
                databaseService(),
                TestModel::getValue,
                new SHA1HashingFunction(),
                AccessionSaveMode.SAVE_ALL_THEN_RESOLVE
        );
    }

}
