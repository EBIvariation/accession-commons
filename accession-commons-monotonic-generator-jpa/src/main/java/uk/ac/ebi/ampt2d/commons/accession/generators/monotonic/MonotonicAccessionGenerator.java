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
package uk.ac.ebi.ampt2d.commons.accession.generators.monotonic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ampt2d.commons.accession.block.initialization.BlockInitializationException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionCouldNotBeGeneratedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionGeneratorShutDownException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionIsNotPendingException;
import uk.ac.ebi.ampt2d.commons.accession.core.models.AccessionWrapper;
import uk.ac.ebi.ampt2d.commons.accession.core.models.SaveResponse;
import uk.ac.ebi.ampt2d.commons.accession.generators.AccessionGenerator;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.entities.ContiguousIdBlock;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.service.ContiguousIdBlockService;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.service.MonotonicDatabaseService;
import uk.ac.ebi.ampt2d.commons.accession.utils.ExponentialBackOff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Generates monotonically increasing ids for type of objects across multiple application instances. Each
 * application reserves blocks with a serialized transaction. Each reserved block contains an id of the application
 * instance, an id for type of object and a counter to keep track of the confirmed generated ids.
 * <p>
 * In case of application restart, the previous application state can be loaded with {@link #recoverState(long[])}
 */
public class MonotonicAccessionGenerator<MODEL> implements AccessionGenerator<MODEL, Long> {

    private static final Logger logger = LoggerFactory.getLogger(MonotonicAccessionGenerator.class);

    private final BlockManager blockManager;
    private final String categoryId;
    private final ContiguousIdBlockService blockService;
    private MonotonicDatabaseService monotonicDatabaseService;
    private boolean UNCOMPLETED_BLOCKS_AVAILABLE = true;

    private boolean SHUTDOWN = false;

    public MonotonicAccessionGenerator(String categoryId,
                                       ContiguousIdBlockService contiguousIdBlockService,
                                       MonotonicDatabaseService monotonicDatabaseService) {
        this.categoryId = categoryId;
        this.blockService = contiguousIdBlockService;
        this.monotonicDatabaseService = monotonicDatabaseService;
        assertBlockParametersAreInitialized(blockService, categoryId);
        this.blockManager = new BlockManager();
    }

    private static void assertBlockParametersAreInitialized(ContiguousIdBlockService blockService, String categoryId) {
        if (blockService.getBlockParameters(categoryId) == null) {
            throw new BlockInitializationException("BlockParameters not initialized for category '" + categoryId + "'");
        }
    }

    private boolean recoverAndReserveUncompletedBlock(String applicationInstanceId) {
        logger.trace("Reserving incomplete block");
        if (monotonicDatabaseService != null) {
            ContiguousIdBlock uncompletedBlock = blockService
                    .reserveFirstUncompletedBlockForCategoryIdAndApplicationInstanceId(categoryId, applicationInstanceId);
            if (uncompletedBlock != null) {
                //Insert as available ranges
                blockManager.addBlock(uncompletedBlock);

                recoverStateForElements(monotonicDatabaseService.getAccessionsInRanges(
                        Collections.singletonList(new MonotonicRange(uncompletedBlock.getLastCommitted() + 1,
                                uncompletedBlock.getLastValue()))));

                return true;
            }
        }

        return false;
    }

    /**
     * This function will recover the internal state of committed elements and will remove them from the available
     * ranges.
     *
     * @param committedElements
     * @throws AccessionIsNotPendingException
     */
    private void recoverStateForElements(long[] committedElements) throws AccessionIsNotPendingException {
        blockService.save(blockManager.recoverState(committedElements));
    }

    public synchronized long[] generateAccessions(int numAccessionsToGenerate, String applicationInstanceId)
            throws AccessionCouldNotBeGeneratedException {
        checkAccessionGeneratorNotShutDown();
        logger.trace("Generating {} accessions for application ID {}", numAccessionsToGenerate, applicationInstanceId);
        long[] accessions = new long[numAccessionsToGenerate];
        reserveBlocksUntilSizeIs(numAccessionsToGenerate, applicationInstanceId);

        int i = 0;
        while (i < numAccessionsToGenerate) {
            int remainingAccessionsToGenerate = numAccessionsToGenerate - i;
            long[] ids = blockManager.pollNext(remainingAccessionsToGenerate);
            System.arraycopy(ids, 0, accessions, i, ids.length);
            i += ids.length;
        }
        assert (i == numAccessionsToGenerate);

        return accessions;
    }

    /**
     * Ensures that the available ranges queue hold @param totalAccessionsToGenerate or more elements
     *
     * @param totalAccessionsToGenerate
     * @param applicationInstanceId     - The id of the application(instance) that is trying to reserve the block
     */
    private synchronized void reserveBlocksUntilSizeIs(int totalAccessionsToGenerate, String applicationInstanceId) {
        while (!blockManager.hasAvailableAccessions(totalAccessionsToGenerate)) {
            ExponentialBackOff.execute(() -> reserveBlock(categoryId, applicationInstanceId), 10, 30);
        }
    }

    private synchronized void reserveBlock(String categoryId, String instanceId) {
        logger.trace("Inside reserveBlock");
        if (UNCOMPLETED_BLOCKS_AVAILABLE) {
            boolean reservedUncompleted = recoverAndReserveUncompletedBlock(instanceId);
            if (!reservedUncompleted) {
                UNCOMPLETED_BLOCKS_AVAILABLE = false;
            }
        } else {
            reserveNewBlock(categoryId, instanceId);
        }
    }


    private synchronized void reserveNewBlock(String categoryId, String instanceId) {
        logger.trace("Reserving new block");
        blockManager.addBlock(blockService.reserveNewBlock(categoryId, instanceId));
    }

    public synchronized void commit(long... accessions) throws AccessionIsNotPendingException {
        checkAccessionGeneratorNotShutDown();
        blockService.save(blockManager.commit(accessions));
    }

    public synchronized void release(long... accessions) throws AccessionIsNotPendingException {
        checkAccessionGeneratorNotShutDown();
        blockManager.release(accessions);
    }

    public synchronized MonotonicRangePriorityQueue getAvailableRanges() {
        checkAccessionGeneratorNotShutDown();
        return blockManager.getAvailableRanges();
    }

    @Override
    public <HASH> List<AccessionWrapper<MODEL, HASH, Long>> generateAccessions(Map<HASH, MODEL> messages, String applicationInstanceId)
            throws AccessionCouldNotBeGeneratedException {
        checkAccessionGeneratorNotShutDown();
        long[] accessions = generateAccessions(messages.size(), applicationInstanceId);
        int i = 0;
        List<AccessionWrapper<MODEL, HASH, Long>> accessionedModels = new ArrayList<>();
        for (Map.Entry<HASH, ? extends MODEL> entry : messages.entrySet()) {
            accessionedModels.add(new AccessionWrapper<>(accessions[i], entry.getKey(), entry.getValue()));
            i++;
        }

        return accessionedModels;
    }

    @Override
    public synchronized void postSave(SaveResponse<Long> response) {
        checkAccessionGeneratorNotShutDown();
        commit(response.getSavedAccessions().stream().mapToLong(l -> l).toArray());
        release(response.getSaveFailedAccessions().stream().mapToLong(l -> l).toArray());
    }

    public void shutDownAccessionGenerator() {
        List<ContiguousIdBlock> blockList = blockManager.getAssignedBlocks();
        blockList.stream().forEach(block -> block.releaseReserved());
        blockService.save(blockList);
        blockManager.shutDownBlockManager();
        SHUTDOWN = true;
    }

    /**
     * Before doing any operation on Accession Generator, we need to make sure it has not been shut down.
     * We should make the check by calling this method as the first thing in all public methods of this class
     */
    private void checkAccessionGeneratorNotShutDown() {
        if (SHUTDOWN) {
            throw new AccessionGeneratorShutDownException("Accession Generator has been shut down and is no longer available");
        }
    }

}
