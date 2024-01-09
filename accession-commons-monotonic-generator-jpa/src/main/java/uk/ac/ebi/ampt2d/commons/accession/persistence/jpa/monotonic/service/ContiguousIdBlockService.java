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
package uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.service;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.ampt2d.commons.accession.block.initialization.BlockParameters;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.entities.ContiguousIdBlock;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.repositories.ContiguousIdBlockRepository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContiguousIdBlockService {

    private ContiguousIdBlockRepository repository;

    private Map<String, BlockParameters> categoryBlockInitializations;

    @PersistenceContext
    EntityManager entityManager;

    public ContiguousIdBlockService(ContiguousIdBlockRepository repository, Map<String, BlockParameters>
            categoryBlockInitializations) {
        this.repository = repository;
        this.categoryBlockInitializations = categoryBlockInitializations;
    }

    @Transactional
    public void save(Iterable<ContiguousIdBlock> blocks) {
        repository.saveAll(blocks);
        entityManager.flush();
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ContiguousIdBlock reserveNewBlock(String categoryId, String instanceId) {
        ContiguousIdBlock lastBlock = repository.findFirstByCategoryIdOrderByLastValueDesc(categoryId);
        BlockParameters blockParameters = getBlockParameters(categoryId);
        ContiguousIdBlock reservedBlock;
        if (lastBlock != null) {
            reservedBlock = repository.save(lastBlock.nextBlock(instanceId, blockParameters.getBlockSize(),
                                                                blockParameters.getNextBlockInterval(),
                                                                blockParameters.getBlockStartValue()));
        } else {
            ContiguousIdBlock newBlock = new ContiguousIdBlock(categoryId, instanceId,
                                                               blockParameters.getBlockStartValue(),
                                                               blockParameters.getBlockSize());
            reservedBlock = repository.save(newBlock);
        }
        entityManager.flush();
        return reservedBlock;
    }

    public BlockParameters getBlockParameters(String categoryId) {
        return categoryBlockInitializations.get(categoryId);
    }

    @Transactional(readOnly = true)
    public List<ContiguousIdBlock> getUncompletedBlocksByCategoryIdAndApplicationInstanceIdOrderByEndAsc(
            String categoryId, String applicationInstanceId) {
        try (Stream<ContiguousIdBlock> reservedBlocksOfThisInstance = repository
                .findAllByCategoryIdAndApplicationInstanceIdOrderByLastValueAsc(categoryId, applicationInstanceId)) {
            return reservedBlocksOfThisInstance.filter(ContiguousIdBlock::isNotFull).collect(Collectors.toList());
        }
    }
}
