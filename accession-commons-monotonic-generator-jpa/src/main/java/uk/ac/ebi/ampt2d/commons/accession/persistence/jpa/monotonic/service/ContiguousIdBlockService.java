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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.ampt2d.commons.accession.block.initialization.BlockParameters;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.entities.ContiguousIdBlock;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.repositories.ContiguousIdBlockRepository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * The ContiguousIdBlockService is used by AccessionGenerator to enter/update block information in DB.
 *
 * In case of multiprocessing, we need to make sure a block is used by only one AccessionGenerator at any point of time.
 * To prevent a block from being used by multiple AccessionGenerator, we mark the block as reserved (using column reserved)
 * when they are in use by an AccessionGenerator. A block, marked as reserved implies it is currently being used by an
 * AccessionGenerator and should not be picked up for use by any other AccessionGenerator.
 *
 * Whenever an AccessionGenerator asks for a block from the ContiguousIdBlockService for using the accessions in it, we
 * should reserve the block for the calling Accession Generator.
 *
 * Existing Uncompleted Blocks
 *      When an AccessionGenerator starts, it asks for Uncompleted Blocks, in order to use the remaining accessions
 *      in them. As these blocks, will be used by the calling AccessionGenerator, we need to explicitly mark them
 *      as reserved in DB.
 *      (see method @reserveUncompletedBlocksByCategoryIdAndApplicationInstanceIdOrderByEndAsc)
 * New Block
 *      When an AccessionGenerator asks for a new block, we create a new block with correct values (based on the given
 *      parameters and existing blocks) and save it in DB. A newly created block is implicitly marked as reserved.
 *      (see method @reserveNewBlock)
 *
 * Also, when saving the blocks, we need to check for the block's last committed value.
 * If its last committed value is same as last value, we should release the block in DB.
 *
 * To guarantee safe multiprocessing in PostgreSQL, all methods in ContiguousIdBlockService that access the DB must use
 * the SERIALIZABLE transaction isolation level.
 * See here for details: https://wiki.postgresql.org/wiki/Serializable#PostgreSQL_Implementation
 *
 */
public class ContiguousIdBlockService {

    private static final Logger logger = LoggerFactory.getLogger(ContiguousIdBlockService.class);

    private ContiguousIdBlockRepository repository;

    private Map<String, BlockParameters> categoryBlockInitializations;

    @PersistenceContext
    EntityManager entityManager;

    public ContiguousIdBlockService(ContiguousIdBlockRepository repository, Map<String, BlockParameters>
            categoryBlockInitializations) {
        this.repository = repository;
        this.categoryBlockInitializations = categoryBlockInitializations;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void save(Iterable<ContiguousIdBlock> blocks) {
        logger.trace("Inside blockService save (multiple)");
        // release block if full
        blocks.forEach(block -> {
            logger.trace("Block: {}", block);
            if (block.isFull()) {
                logger.trace("Releasing block");
                block.releaseReserved();
            }
        });
        repository.saveAll(blocks);
        logger.trace("All blocks saved");
        entityManager.flush();
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void save(ContiguousIdBlock block) {
        logger.trace("Inside blockService save (single)");
        logger.trace("Block: {}", block);
        // release block if full
        if (block.isFull()) {
            logger.trace("Releasing block");
            block.releaseReserved();
        }
        repository.save(block);
        logger.trace("Block saved");
        entityManager.flush();
    }

    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
    public ContiguousIdBlock reserveNewBlock(String categoryId, String instanceId) {
        logger.trace("Inside reserveNewBlock for instanceId {}", instanceId);
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
        logger.trace("Reserved new block: {}", reservedBlock);
        entityManager.flush();
        return reservedBlock;
    }

    public BlockParameters getBlockParameters(String categoryId) {
        return categoryBlockInitializations.get(categoryId);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ContiguousIdBlock reserveFirstUncompletedBlockForCategoryIdAndApplicationInstanceId(String categoryId, String applicationInstanceId) {
        logger.trace("Inside reserveUncompletedBlock for instanceId {}", applicationInstanceId);
        ContiguousIdBlock block = repository.findUncompletedAndUnreservedBlocksOrderByLastValueAsc(categoryId,
                        PageRequest.of(0, 1)).stream()
                .findFirst().orElse(null);

        if (block != null) {
            block.setApplicationInstanceId(applicationInstanceId);
            block.markAsReserved();

            save(block);
        }

        return block;
    }

    public List<ContiguousIdBlock> allBlocksForCategoryIdReservedBeforeTheGivenTimeFrame(String categoryId,
                                                                                         LocalDateTime lastUpdatedTimeStamp) {
        return repository.findByCategoryIdAndReservedIsTrueAndLastUpdatedTimestampLessThanEqualOrderByLastValueAsc(categoryId, lastUpdatedTimeStamp);
    }

}
