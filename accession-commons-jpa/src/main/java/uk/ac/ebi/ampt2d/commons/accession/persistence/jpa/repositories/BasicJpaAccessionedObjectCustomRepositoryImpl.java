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
package uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.repositories;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import uk.ac.ebi.ampt2d.commons.accession.core.models.SaveResponse;
import uk.ac.ebi.ampt2d.commons.accession.persistence.repositories.IAccessionedObjectCustomRepository;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.entities.AccessionedEntity;

import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.util.List;
import java.util.Stack;

public abstract class BasicJpaAccessionedObjectCustomRepositoryImpl<
        ACCESSION extends Serializable,
        ENTITY extends AccessionedEntity<?, ACCESSION>>
        implements IAccessionedObjectCustomRepository<ACCESSION, ENTITY> {

    private class Partition {

        private int start;
        private int end;

        public Partition(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    private JpaEntityInformation<ENTITY, ?> entityInformation;

    private PlatformTransactionManager platformTransactionManager;

    private EntityManager entityManager;

    public BasicJpaAccessionedObjectCustomRepositoryImpl(Class<ENTITY> entityClass,
                                                         PlatformTransactionManager platformTransactionManager,
                                                         EntityManager entityManager) {
        entityInformation = JpaEntityInformationSupport.getEntityInformation(entityClass, entityManager);
        this.platformTransactionManager = platformTransactionManager;
        this.entityManager = entityManager;
    }

    @Override
    public SaveResponse<ACCESSION> insert(List<ENTITY> entities) {
        Stack<Partition> partitions = new Stack<>();
        partitions.add(new Partition(0, entities.size()));
        SaveResponse<ACCESSION> saveResponse = new SaveResponse<>();

        while (!partitions.isEmpty()) {
            Partition partition = partitions.pop();
            final List<ENTITY> partitionToSave = entities.subList(partition.start, partition.end);
            try {
                doTransactionalInsert(partitionToSave);
                partitionToSave.stream().map(AccessionedEntity::getAccession).forEach(saveResponse::addSavedAccession);
            } catch (DataIntegrityViolationException e) {
                if (partitionToSave.size() != 1) {
                    int start = partition.start;
                    int middle = (partition.end + partition.start) / 2;
                    int end = partition.end;
                    partitions.add(new Partition(start, middle));
                    partitions.add(new Partition(middle, end));
                } else {
                    saveResponse.addSaveFailedAccession(partitionToSave.get(0).getAccession());
                }
            }
        }
        return saveResponse;
    }

    private void doTransactionalInsert(List<ENTITY> entities) {
        TransactionTemplate template = new TransactionTemplate(platformTransactionManager);
        template.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                for (ENTITY entity : entities) {
                    entityManager.persist(entity);
                }
            }
        });
    }

}
