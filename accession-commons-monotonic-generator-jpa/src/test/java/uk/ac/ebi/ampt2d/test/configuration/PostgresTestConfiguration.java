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

import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
public class PostgresTestConfiguration {

    private static final PostgreSQLContainer<?> postgresContainer;

    static {
        // Disable Ryuk container reaper for environments where Docker socket mounting may not work
        // (e.g., Rancher Desktop). The container will be cleaned up when the JVM exits.
        System.setProperty("testcontainers.ryuk.disabled", "true");

        postgresContainer = new PostgreSQLContainer<>("postgres:14")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test");
        postgresContainer.start();

        // Register shutdown hook to stop container when JVM exits
        Runtime.getRuntime().addShutdownHook(new Thread(postgresContainer::stop));
    }

    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .url(postgresContainer.getJdbcUrl())
                .username(postgresContainer.getUsername())
                .password(postgresContainer.getPassword())
                .driverClassName("org.postgresql.Driver")
                .build();
    }

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return (Map<String, Object> hibernateProperties) -> {
            hibernateProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQL95Dialect");
            hibernateProperties.put("hibernate.hbm2ddl.auto", "create-drop");
        };
    }
}
