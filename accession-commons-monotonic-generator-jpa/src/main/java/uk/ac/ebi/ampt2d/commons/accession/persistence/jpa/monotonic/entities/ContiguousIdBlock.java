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
package uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * This class represents a block allocated by an application instance, in a monotonic sequence associated with a
 * category.
 * <p>
 * It is defined by the first and last values of the monotonic sequence, and the last committed value to the database.
 * <p>
 * The last committed value of the block is initialized as the first value of the block minus one, to simplify the
 * application logic that implements the calculations.
 */
@Entity
@Table(
        name = "contiguous_id_blocks",
        indexes = {
                @Index(name = "CATEGORY_INDEX", columnList = "categoryId")
        },
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"categoryId", "firstValue"})
        }
)
public class ContiguousIdBlock implements Comparable<ContiguousIdBlock> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(nullable = false, length = 255)
    private String categoryId;

    @Column(nullable = false, length = 255)
    private String applicationInstanceId;

    private long firstValue;

    private long lastValue;

    private long lastCommitted;

    // Create / update dates

    ContiguousIdBlock() {
        //Hibernate default constructor
    }

    public ContiguousIdBlock(String categoryId, String applicationInstanceId, long firstValue, long size) {
        this.categoryId = categoryId;
        this.applicationInstanceId = applicationInstanceId;
        this.firstValue = firstValue;
        this.lastValue = firstValue + size - 1;
        this.lastCommitted = firstValue + size - 1;
//        this.lastCommitted = firstValue - 1;
    }

    /**
     * This method creates a new block based on the previous block and nextBlockInterval.
     * In case of nextBlockInterval is zero, the next block can start right after the last one, otherwise
     * the interleavingPoint is calculated by finding the range in which the lastvalue (previous block's endValue)
     * falls.
     * <p>
     * Below are the series of cases
     * <p>
     * if earliestBlockStartValue = 0 , nextBlockInterval = 50 , lastValue = 40 then,
     * interleavingPoint = 50 and remainingSizeInRangeOfBlocks=9 so the nextBlockStartValue will be 41
     * if earliestBlockStartValue = 0 , nextBlockInterval = 50 , lastValue = 49 then,
     * interleavingPoint = 50 and remainingSizeInRangeOfBlocks=0 so the nextBlockStartValue will be 100
     * <p>
     * if earliestBlockStartValue = 0 , nextBlockInterval = 50 , lastValue = 120 then,
     * interleavingPoint = 150 and remainingSizeInRangeOfBlocks= 29 so the nextBlockStartValue will be 121
     * if earliestBlockStartValue = 0 , nextBlockInterval = 50 , lastValue = 130 and size is 25 then,
     * interleavingPoint = 150 and remainingSizeInRangeOfBlocks= 19 so the nextBlockStartValue equals 131 and size = 19
     *
     * @param instanceId              instance id for the machine
     * @param size                    size of the block to be reserved
     * @param nextBlockInterval       specifies when to interleave and interval to interleave
     * @param earliestBlockStartValue refers to blockStartValue in properties file
     * @return
     */
    public ContiguousIdBlock nextBlock(String instanceId, long size, long nextBlockInterval,
                                       long earliestBlockStartValue) {
        long nextBlockStartValue = lastValue + 1;
        if (nextBlockInterval == 0) {
            return new ContiguousIdBlock(categoryId, instanceId, nextBlockStartValue, size);
        }
        long interleavingPoint = ((lastValue - earliestBlockStartValue) / nextBlockInterval + 1) *
                nextBlockInterval + earliestBlockStartValue;
        long remainingSize = interleavingPoint - nextBlockStartValue;
        if (remainingSize <= 0) {
            nextBlockStartValue = nextBlockStartValue + nextBlockInterval;
            remainingSize = nextBlockInterval;
        }
        if (size > remainingSize) {
            /* To make sure we allocate only available size in case we have reserved different sizes
            for different instances */
            size = remainingSize;
        }

        return new ContiguousIdBlock(categoryId, instanceId, nextBlockStartValue, size);
    }

    public long getId() {
        return id;
    }

    public long getLastCommitted() {
        return lastCommitted;
    }

    public void setLastCommitted(long lastCommitted) {
        this.lastCommitted = lastCommitted;
    }

    public long getFirstValue() {
        return firstValue;
    }

    public long getLastValue() {
        return lastValue;
    }

    public boolean isNotFull() {
        return lastCommitted != lastValue;
    }

    @Override
    public int compareTo(ContiguousIdBlock contiguousIdBlock) {
        return Long.compare(firstValue, contiguousIdBlock.getFirstValue());
    }
}
