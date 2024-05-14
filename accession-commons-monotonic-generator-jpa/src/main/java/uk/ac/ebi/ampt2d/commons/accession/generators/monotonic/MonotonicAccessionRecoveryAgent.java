package uk.ac.ebi.ampt2d.commons.accession.generators.monotonic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.entities.ContiguousIdBlock;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.service.ContiguousIdBlockService;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.service.MonotonicDatabaseService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MonotonicAccessionRecoveryAgent {
    private final static Logger logger = LoggerFactory.getLogger(MonotonicAccessionRecoveryAgent.class);

    private final ContiguousIdBlockService blockService;
    private final MonotonicDatabaseService monotonicDatabaseService;

    public MonotonicAccessionRecoveryAgent(ContiguousIdBlockService blockService,
                                           MonotonicDatabaseService monotonicDatabaseService) {
        this.blockService = blockService;
        this.monotonicDatabaseService = monotonicDatabaseService;
    }

    public void runRecovery(String categoryId, String applicationInstanceId, LocalDateTime lastUpdatedTime) {
        logger.info("Starting recovering of blocks for category " + categoryId);
        List<ContiguousIdBlock> blocksToRecover = blockService.allBlocksForCategoryIdReservedBeforeTheGivenTimeFrame(categoryId, lastUpdatedTime);
        logger.info("List of block ids to recover : " + blocksToRecover.stream().map(b -> Long.toString(b.getId()))
                .collect(Collectors.joining(",")));
        for (ContiguousIdBlock block : blocksToRecover) {
            logger.info("Recovering Block: " + block);
            if (block.getLastCommitted() == block.getLastValue()) {
                logger.info("Block is already completely used, not need to run recovery. Releasing the block.");
                setAppInstanceIdAndReleaseBlock(applicationInstanceId, block);
                continue;
            }

            // run recover state for a block using BlockManager's recover state method
            Set<ContiguousIdBlock> blockSet = recoverStateForBlock(block);

            if (blockSet.isEmpty()) {
                // if block's last committed is correctly set, BlockManager's recover method will return an empty set
                logger.info("Block's last committed is correct. No updates to last_committed. Releasing the block.");
                setAppInstanceIdAndReleaseBlock(applicationInstanceId, block);
            } else {
                ContiguousIdBlock blockToUpdate = blockSet.iterator().next();
                logger.info("Recovery ran successfully for block. Last committed updated to " + block.getLastCommitted()
                        + ". Saving and releasing the block.");
                setAppInstanceIdAndReleaseBlock(applicationInstanceId, blockToUpdate);
            }
        }
    }

    private Set<ContiguousIdBlock> recoverStateForBlock(ContiguousIdBlock block) {
        BlockManager blockManager = new BlockManager();
        blockManager.addBlock(block);
        MonotonicRange monotonicRange = blockManager.getAvailableRanges().poll();
        long[] committedElements = monotonicDatabaseService.getAccessionsInRanges(Collections.singletonList(monotonicRange));
        return blockManager.recoverState(committedElements);
    }

    private void setAppInstanceIdAndReleaseBlock(String applicationInstanceId, ContiguousIdBlock block) {
        block.setApplicationInstanceId(applicationInstanceId);
        block.releaseReserved();
        blockService.save(block);
    }
}
