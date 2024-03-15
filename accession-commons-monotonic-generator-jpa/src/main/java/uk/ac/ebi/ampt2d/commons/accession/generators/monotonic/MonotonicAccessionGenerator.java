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

import uk.ac.ebi.ampt2d.commons.accession.block.initialization.BlockInitializationException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionCouldNotBeGeneratedException;
import uk.ac.ebi.ampt2d.commons.accession.exception.AccessionGeneratorShutDownException;
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

    private final BlockManager blockManager;
    private final String categoryId;
    private final String applicationInstanceId;
    private final ContiguousIdBlockService blockService;

    private Boolean SHUTDOWN = Boolean.FALSE;

    public MonotonicAccessionGenerator(String categoryId,
                                       String applicationInstanceId,
                                       ContiguousIdBlockService contiguousIdBlockService,
                                       MonotonicDatabaseService databaseService) {
        this(categoryId, applicationInstanceId, contiguousIdBlockService);
        // As we are going through the available ranges and at the same time we are also going to manipulate/update them
        // Need to make a copy of the original for iteration to avoid ConcurrentModificationException
        MonotonicRangePriorityQueue copyOfAvailableRanges = new MonotonicRangePriorityQueue();
        for (MonotonicRange range : getAvailableRanges()) {
            copyOfAvailableRanges.offer(range);
        }
        for (MonotonicRange monotonicRange : copyOfAvailableRanges) {
            recoverState(databaseService.getAccessionsInRanges(Collections.singletonList(monotonicRange)));
        }
    }

    public MonotonicAccessionGenerator(String categoryId,
                                       String applicationInstanceId,
                                       ContiguousIdBlockService contiguousIdBlockService,
                                       long[] initializedAccessions) {
        this(categoryId, applicationInstanceId, contiguousIdBlockService);
        if (initializedAccessions != null) {
            recoverState(initializedAccessions);
        }
    }

    //Package protected for testing without initialized Accessions
    MonotonicAccessionGenerator(String categoryId,
                                String applicationInstanceId,
                                ContiguousIdBlockService contiguousIdBlockService) {
        this.categoryId = categoryId;
        this.applicationInstanceId = applicationInstanceId;
        this.blockService = contiguousIdBlockService;
        this.blockManager = initializeBlockManager(blockService, categoryId, applicationInstanceId);
    }

    private static BlockManager initializeBlockManager(ContiguousIdBlockService blockService, String categoryId,
                                                       String applicationInstanceId) {
        assertBlockParametersAreInitialized(blockService, categoryId);
        BlockManager blockManager = new BlockManager();
        List<ContiguousIdBlock> uncompletedBlocks = blockService
                .reserveUncompletedBlocksByCategoryIdAndApplicationInstanceIdOrderByEndAsc(categoryId,
                        applicationInstanceId);
        //Insert as available ranges
        for (ContiguousIdBlock block : uncompletedBlocks) {
            blockManager.addBlock(block);
        }
        return blockManager;
    }

    private static void assertBlockParametersAreInitialized(ContiguousIdBlockService blockService, String categoryId) {
        if (blockService.getBlockParameters(categoryId) == null) {
            throw new BlockInitializationException("BlockParameters not initialized for category '" + categoryId + "'");
        }
    }

    /**
     * This function will recover the internal state of committed elements and will remove them from the available
     * ranges.
     *
     * @param committedElements
     * @throws AccessionIsNotPendingException
     */
    private void recoverState(long[] committedElements) throws AccessionIsNotPendingException {
        blockService.save(blockManager.recoverState(committedElements));
    }

    public synchronized long[] generateAccessions(int numAccessionsToGenerate)
            throws AccessionCouldNotBeGeneratedException {
        checkAccessionGeneratorNotShutDown();
        long[] accessions = new long[numAccessionsToGenerate];
        reserveNewBlocksUntilSizeIs(numAccessionsToGenerate);

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
     */
    private synchronized void reserveNewBlocksUntilSizeIs(int totalAccessionsToGenerate) {
        while (!blockManager.hasAvailableAccessions(totalAccessionsToGenerate)) {
            ExponentialBackOff.execute(() -> reserveNewBlock(categoryId, applicationInstanceId), 10, 30);
        }
    }

    private synchronized void reserveNewBlock(String categoryId, String instanceId) {
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
    public <HASH> List<AccessionWrapper<MODEL, HASH, Long>> generateAccessions(Map<HASH, MODEL> messages)
            throws AccessionCouldNotBeGeneratedException {
        checkAccessionGeneratorNotShutDown();
        long[] accessions = generateAccessions(messages.size());
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

    public void shutDownAccessionGenerator(){
        blockService.save(blockManager.shutDownBlockManager());
        SHUTDOWN = Boolean.TRUE;
    }

    private void checkAccessionGeneratorNotShutDown(){
        if(SHUTDOWN){
            throw new AccessionGeneratorShutDownException("Accession Generator has been shut down and is no longer available");
        }
    }

}
