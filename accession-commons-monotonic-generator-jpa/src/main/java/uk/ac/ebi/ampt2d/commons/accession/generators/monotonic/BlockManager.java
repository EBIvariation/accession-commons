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

import org.springframework.data.util.Pair;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionCouldNotBeGeneratedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionIsNotPendingException;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.entities.ContiguousIdBlock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * This class holds the state of the monotonic id blocks used at this moment on the application.
 * NOTE: This class is not thread safe.
 */
class BlockManager {

    private final PriorityQueue<ContiguousIdBlock> assignedBlocks;

    private final MonotonicRangePriorityQueue availableRanges;

    private final HashSet<Long> generatedAccessions;

    private final PriorityQueue<Long> committedAccessions;

    public BlockManager() {
        this.assignedBlocks = new PriorityQueue<>(ContiguousIdBlock::compareTo);
        this.availableRanges = new MonotonicRangePriorityQueue();
        this.generatedAccessions = new HashSet<>();
        this.committedAccessions = new PriorityQueue<>(Long::compareTo);
    }

    public void addBlock(ContiguousIdBlock block) {
        assignedBlocks.add(block);
        availableRanges.add(new MonotonicRange(block.getLastCommitted() + 1, block.getLastValue()));
    }

    public MonotonicRangePriorityQueue getAvailableRanges() {
        return availableRanges;
    }

    /**
     * Polls the next continuous array of monotonic values.
     *
     * @param maxValues Max array size returned by the function
     * @return Array of monotonically increasing IDs
     */
    public long[] pollNext(int maxValues) throws AccessionCouldNotBeGeneratedException {
        if (!hasAvailableAccessions(maxValues)) {
            throw new AccessionCouldNotBeGeneratedException("Block manager doesn't have " + maxValues + " values available.");
        }
        MonotonicRange monotonicRange = pollNextMonotonicRange(maxValues);
        long[] ids = monotonicRange.getIds();
        generatedAccessions.addAll(LongStream.of(ids).boxed().collect(Collectors.toList()));
        return ids;
    }

    /**
     * Polls the next monotonic range to use.
     *
     * @param maxSize Max size of returned {@link MonotonicRange}
     * @return Next available range, if larger than maxSize, then the range is split and only the left part is returned.
     */
    private MonotonicRange pollNextMonotonicRange(int maxSize) {
        MonotonicRange monotonicRange = availableRanges.poll();
        if (monotonicRange.getTotalOfValues() > maxSize) {
            Pair<MonotonicRange, MonotonicRange> splitResult = monotonicRange.split(maxSize);
            monotonicRange = splitResult.getFirst();
            availableRanges.add(splitResult.getSecond());
        }

        return monotonicRange;
    }

    public boolean hasAvailableAccessions(int accessionsNeeded) {
        return availableRanges.getNumOfValuesInQueue() >= accessionsNeeded;
    }

    public Set<ContiguousIdBlock> commit(long[] accessions) throws AccessionIsNotPendingException {
        assertAccessionsArePending(accessions);
        return doCommit(accessions);
    }

    private void assertAccessionsArePending(long[] accessions) throws AccessionIsNotPendingException {
        for (long accession : accessions) {
            if (!generatedAccessions.contains(accession)) {
                throw new AccessionIsNotPendingException(accession);
            }
        }
    }

    private Set<ContiguousIdBlock> doCommit(long[] accessions) {
        Set<ContiguousIdBlock> blocksToUpdate = new HashSet<>();
        if (accessions == null || accessions.length == 0) {
            return blocksToUpdate;
        }

        addToCommitted(accessions);

        ContiguousIdBlock block = assignedBlocks.peek();
        while (block != null && committedAccessions.peek() != null &&
                committedAccessions.peek() == block.getLastCommitted() + 1) {
            //Next value continues sequence, change last committed value
            block.setLastCommitted(committedAccessions.poll());
            blocksToUpdate.add(block);
            if (!block.isNotFull()) {
                assignedBlocks.poll();
                block = assignedBlocks.peek();
            }
        }

        return blocksToUpdate;
    }

    private void addToCommitted(long[] accessions) {
        for (long accession : accessions) {
            committedAccessions.add(accession);
            generatedAccessions.remove(accession);
        }
    }

    public void release(long[] accessions) throws AccessionIsNotPendingException {
        assertAccessionsArePending(accessions);
        doRelease(accessions);
    }

    private void doRelease(long[] accessions) {
        availableRanges.addAll(MonotonicRange.convertToMonotonicRanges(accessions));
        generatedAccessions.removeAll(LongStream.of(accessions).boxed().collect(Collectors.toList()));
    }

    /**
     * This function will recover the internal state of committed elements and will remove them from the available
     * ranges.
     *
     * @param committedElements Accessions that have already been committed
     * @throws AccessionIsNotPendingException When the generated accession does not match with the accession to commit
     */
    public Set<ContiguousIdBlock> recoverState(long[] committedElements) throws AccessionIsNotPendingException {
        List<MonotonicRange> ranges = MonotonicRange.convertToMonotonicRanges(committedElements);
        List<MonotonicRange> newAvailableRanges = new ArrayList<>();
        for (MonotonicRange monotonicRange : this.availableRanges) {
            newAvailableRanges.addAll(monotonicRange.excludeIntersections(ranges));
        }

        this.availableRanges.clear();
        this.availableRanges.addAll(newAvailableRanges);
        return doCommit(committedElements);
    }

    public List<ContiguousIdBlock> shutDownBlockManager() {
        List<ContiguousIdBlock> blockList = assignedBlocks.stream().collect(Collectors.toList());
        blockList.stream().forEach(block -> block.releaseReserved());
        assignedBlocks.clear();
        availableRanges.clear();
        generatedAccessions.clear();
        return blockList;
    }
}
