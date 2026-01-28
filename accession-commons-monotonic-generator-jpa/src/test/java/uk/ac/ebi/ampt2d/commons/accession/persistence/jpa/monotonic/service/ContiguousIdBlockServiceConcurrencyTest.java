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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.entities.ContiguousIdBlock;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.repositories.ContiguousIdBlockRepository;
import uk.ac.ebi.ampt2d.test.configuration.MonotonicAccessionGeneratorTestConfiguration;
import uk.ac.ebi.ampt2d.test.configuration.PostgresTestConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static uk.ac.ebi.ampt2d.commons.accession.util.ContiguousIdBlockUtil.getUnreservedContiguousIdBlock;

@RunWith(SpringRunner.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = {PostgresTestConfiguration.class, MonotonicAccessionGeneratorTestConfiguration.class})
public class ContiguousIdBlockServiceConcurrencyTest {

    private static final String CATEGORY_ID = "cat-concurrency-test";
    private static final String INSTANCE_ID = "test-instance";

    @Autowired
    private ContiguousIdBlockRepository repository;

    @Autowired
    private ContiguousIdBlockService blockService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @Before
    public void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        // Clean up any existing data before test
        transactionTemplate.execute(status -> {
            repository.deleteAll();
            return null;
        });
    }

    @After
    public void cleanUp() {
        // Clean up test data after each test
        transactionTemplate.execute(status -> {
            repository.deleteAll();
            return null;
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void testConcurrentUncompletedBlockReservation() throws Exception {
        // 1. Create an uncompleted, unreserved block in a separate transaction
        transactionTemplate.execute(status -> {
            ContiguousIdBlock uncompletedBlock = getUnreservedContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 0, 1000);
            repository.save(uncompletedBlock);
            return null;
        });

        // Verify the block was created
        Long blockCount = transactionTemplate.execute(status -> repository.count());
        assertEquals("Block should be created", 1L, blockCount.longValue());

        // 2. Use CyclicBarrier to synchronize N threads
        int numThreads = 5;
        CyclicBarrier barrier = new CyclicBarrier(numThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<ContiguousIdBlock>> futures = new ArrayList<>();

        // 3. All threads try to reserve the same block simultaneously
        for (int i = 0; i < numThreads; i++) {
            final String instanceId = "instance-" + i;
            futures.add(executor.submit(new Callable<ContiguousIdBlock>() {
                @Override
                public ContiguousIdBlock call() throws Exception {
                    barrier.await(); // Wait for all threads to be ready
                    return blockService.reserveFirstUncompletedBlockForCategoryIdAndApplicationInstanceId(
                            CATEGORY_ID, instanceId);
                }
            }));
        }

        // 4. Collect results
        Set<Long> reservedBlockIds = new HashSet<>();
        int successCount = 0;
        for (Future<ContiguousIdBlock> future : futures) {
            ContiguousIdBlock block = future.get();
            if (block != null) {
                reservedBlockIds.add(block.getId());
                successCount++;
            }
        }

        executor.shutdown();

        // 5. Assert: Only ONE thread should have reserved the block
        // With the race condition, multiple threads may successfully reserve the same block
        // After the fix with SKIP LOCKED, only one thread should get the block
        assertEquals("Only one thread should successfully reserve the block", 1, successCount);
        assertEquals("All successful reservations should be for different blocks",
                successCount, reservedBlockIds.size());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void testConcurrentReservationWithMultipleBlocks() throws Exception {
        // Create multiple uncompleted, unreserved blocks in a separate transaction
        transactionTemplate.execute(status -> {
            for (int i = 0; i < 5; i++) {
                ContiguousIdBlock block = getUnreservedContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, i * 1000, 1000);
                repository.save(block);
            }
            return null;
        });

        // Verify blocks were created
        Long blockCount = transactionTemplate.execute(status -> repository.count());
        assertEquals("5 blocks should be created", 5L, blockCount.longValue());

        // Use CyclicBarrier to synchronize N threads
        int numThreads = 5;
        CyclicBarrier barrier = new CyclicBarrier(numThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<ContiguousIdBlock>> futures = new ArrayList<>();

        // All threads try to reserve blocks simultaneously
        for (int i = 0; i < numThreads; i++) {
            final String instanceId = "instance-" + i;
            futures.add(executor.submit(new Callable<ContiguousIdBlock>() {
                @Override
                public ContiguousIdBlock call() throws Exception {
                    barrier.await(); // Wait for all threads to be ready
                    return blockService.reserveFirstUncompletedBlockForCategoryIdAndApplicationInstanceId(
                            CATEGORY_ID, instanceId);
                }
            }));
        }

        // Collect results - some threads may fail due to serialization conflicts (expected with SERIALIZABLE)
        Set<Long> reservedBlockIds = new HashSet<>();
        int successCount = 0;
        int serializationFailures = 0;
        for (Future<ContiguousIdBlock> future : futures) {
            try {
                ContiguousIdBlock block = future.get();
                if (block != null) {
                    reservedBlockIds.add(block.getId());
                    successCount++;
                }
            } catch (ExecutionException e) {
                // Serialization failures are expected with SERIALIZABLE isolation
                // In production, these would be retried
                if (isSerializationFailure(e)) {
                    serializationFailures++;
                } else {
                    throw e;
                }
            }
        }

        executor.shutdown();

        // With SKIP LOCKED, each successful thread should get a DIFFERENT block (no race condition)
        // Some threads may fail due to serialization conflicts, which is expected PostgreSQL behavior
        assertTrue("At least one thread should successfully reserve a block", successCount >= 1);
        assertEquals("All successful reservations should be for different blocks (no race condition)",
                successCount, reservedBlockIds.size());
        assertEquals("Total outcomes should equal number of threads",
                numThreads, successCount + serializationFailures);
    }

    /**
     * Checks if an ExecutionException wraps a PostgreSQL serialization failure.
     * These are expected when multiple SERIALIZABLE transactions conflict.
     */
    private boolean isSerializationFailure(ExecutionException e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause.getMessage() != null && cause.getMessage().contains("could not serialize access")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
