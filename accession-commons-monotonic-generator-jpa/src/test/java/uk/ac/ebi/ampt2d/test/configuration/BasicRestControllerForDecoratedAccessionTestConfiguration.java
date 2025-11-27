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
package uk.ac.ebi.ampt2d.test.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import uk.ac.ebi.ampt2d.commons.accession.autoconfigure.EnableBasicRestControllerAdvice;
import uk.ac.ebi.ampt2d.commons.accession.autoconfigure.EnableSpringDataContiguousIdService;
import uk.ac.ebi.ampt2d.commons.accession.core.AccessionSaveMode;
import uk.ac.ebi.ampt2d.commons.accession.core.AccessioningService;
import uk.ac.ebi.ampt2d.commons.accession.core.BasicAccessioningService;
import uk.ac.ebi.ampt2d.commons.accession.core.DecoratedAccessioningService;
import uk.ac.ebi.ampt2d.commons.accession.generators.monotonic.MonotonicAccessionGenerator;
import uk.ac.ebi.ampt2d.commons.accession.hashing.SHA1HashingFunction;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.service.ContiguousIdBlockService;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.repositories.BasicJpaAccessionedObjectCustomRepositoryImpl;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.service.BasicJpaInactiveAccessionService;
import uk.ac.ebi.ampt2d.commons.accession.persistence.repositories.IAccessionedObjectCustomRepository;
import uk.ac.ebi.ampt2d.commons.accession.persistence.services.InactiveAccessionService;
import uk.ac.ebi.ampt2d.commons.accession.service.BasicSpringDataRepositoryMonotonicDatabaseService;
import uk.ac.ebi.ampt2d.test.models.TestModel;
import uk.ac.ebi.ampt2d.test.persistence.TestLongHistoryRepository;
import uk.ac.ebi.ampt2d.test.persistence.TestLongOperationEntity;
import uk.ac.ebi.ampt2d.test.persistence.TestMonotonicEntity;
import uk.ac.ebi.ampt2d.test.persistence.TestMonotonicInactiveAccessionEntity;
import uk.ac.ebi.ampt2d.test.persistence.TestMonotonicInactiveAccessionRepository;
import uk.ac.ebi.ampt2d.test.persistence.TestMonotonicRepository;
import uk.ac.ebi.ampt2d.test.rest.TestControllerForDecoratedAccession;

@Configuration
@EnableSpringDataContiguousIdService
@EnableWebMvc
@EnableBasicRestControllerAdvice
@ComponentScan(basePackageClasses = IAccessionedObjectCustomRepository.class)
@EnableJpaAuditing
@ComponentScan(basePackageClasses = BasicJpaAccessionedObjectCustomRepositoryImpl.class)
@EntityScan("uk.ac.ebi.ampt2d.test.persistence")
@EnableJpaRepositories(basePackages = {"uk.ac.ebi.ampt2d.test.persistence",
        "uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.repositories"
})
@AutoConfigureDataJpa
public class BasicRestControllerForDecoratedAccessionTestConfiguration {
    private static final String CATEGORY_ID = "category-id-monotonic-test";
    private static final String INSTANCE_ID = "instance-id-monotonic-test";
    private static final String PREFIX = "EGA";
    private static final String PAD_FORMAT = "%011d";

    @Autowired
    private TestMonotonicRepository repository;

    @Autowired
    private ContiguousIdBlockService contiguousIdBlockService;

    @Autowired
    private TestMonotonicInactiveAccessionRepository inactiveRepository;

    @Autowired
    private TestLongHistoryRepository historyRepository;

    @Bean
    public BasicSpringDataRepositoryMonotonicDatabaseService<TestModel, TestMonotonicEntity> getDatabaseService() {
        return new BasicSpringDataRepositoryMonotonicDatabaseService<>(
                repository,
                TestMonotonicEntity::new,
                inactiveService()
        );
    }

    @Bean
    public InactiveAccessionService<TestModel, Long, TestMonotonicEntity> inactiveService() {
        return new BasicJpaInactiveAccessionService<>(
                historyRepository,
                TestMonotonicInactiveAccessionEntity::new,
                inactiveRepository,
                TestLongOperationEntity::new
        );
    }

    @Bean
    public AccessioningService<TestModel, String, String> decoratedAccessioningService() {
        return DecoratedAccessioningService.buildPrefixPaddedLongAccessionService(analysisAccessionService(), PREFIX,
                PAD_FORMAT, Long::parseLong);
    }

    @Bean
    public AccessioningService<TestModel, String, Long> analysisAccessionService() {
        return new BasicAccessioningService<>(
                new MonotonicAccessionGenerator<>(CATEGORY_ID, contiguousIdBlockService,
                        getDatabaseService()),
                getDatabaseService(),
                analysis -> analysis.getValue(),
                new SHA1HashingFunction(),
                AccessionSaveMode.SAVE_ALL_THEN_RESOLVE);
    }

    @Bean
    public TestControllerForDecoratedAccession testController() {
        return new TestControllerForDecoratedAccession(decoratedAccessioningService());
    }

}