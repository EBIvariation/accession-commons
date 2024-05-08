package uk.ac.ebi.ampt2d.commons.accession.generators.monotonic;

import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.entities.ContiguousIdBlock;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.service.ContiguousIdBlockService;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.service.MonotonicDatabaseService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MonotonicAccessionRecoveryAgent {
    private final ContiguousIdBlockService blockService;
    private final MonotonicDatabaseService monotonicDatabaseService;

    public MonotonicAccessionRecoveryAgent(ContiguousIdBlockService blockService,
                                           MonotonicDatabaseService monotonicDatabaseService) {
        this.blockService = blockService;
        this.monotonicDatabaseService = monotonicDatabaseService;
    }

    public void runRecovery(String categoryId, String applicationInstanceId, LocalDateTime lastUpdatedTime) {
        List<ContiguousIdBlock> blocksToRecover = blockService.allBlocksForCategoryIdReservedBeforeTheGivenTimeFrame(categoryId, lastUpdatedTime);
        for (ContiguousIdBlock block : blocksToRecover) {
            // if block is already complete, there is nothing to recover, just release the block
            if (block.getLastCommitted() == block.getLastValue()) {
                setAppInstanceIdAndReleaseBlock(applicationInstanceId, block);
                continue;
            }

            // run recover state for a block using BlockManager's recover state method
            Set<ContiguousIdBlock> blockSet = recoverStateForBlock(block);

            if (blockSet.isEmpty()) {
                // if block's last committed is correctly set, BlockManager's recover method will return an empty set
                // in this case, we just need to release the block
                setAppInstanceIdAndReleaseBlock(applicationInstanceId, block);
            } else {
                ContiguousIdBlock blockToUpdate = blockSet.iterator().next();
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
