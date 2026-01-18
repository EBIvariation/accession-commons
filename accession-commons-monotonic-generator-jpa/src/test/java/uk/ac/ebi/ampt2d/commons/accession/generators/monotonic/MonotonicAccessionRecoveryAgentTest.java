package uk.ac.ebi.ampt2d.commons.accession.generators.monotonic;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import uk.ac.ebi.ampt2d.commons.accession.core.models.AccessionWrapper;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.entities.ContiguousIdBlock;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.repositories.ContiguousIdBlockRepository;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.service.ContiguousIdBlockService;

import jakarta.persistence.EntityManager;
import uk.ac.ebi.ampt2d.commons.accession.service.BasicSpringDataRepositoryMonotonicDatabaseService;
import uk.ac.ebi.ampt2d.test.configuration.MonotonicAccessionGeneratorTestConfiguration;
import uk.ac.ebi.ampt2d.test.configuration.TestMonotonicDatabaseServiceTestConfiguration;
import uk.ac.ebi.ampt2d.test.models.TestModel;
import uk.ac.ebi.ampt2d.test.persistence.TestMonotonicEntity;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ContextConfiguration(classes = {MonotonicAccessionGeneratorTestConfiguration.class, TestMonotonicDatabaseServiceTestConfiguration.class})
public class MonotonicAccessionRecoveryAgentTest {
    private static final String TEST_CATEGORY = "TEST_CATEGORY";
    private static final String TEST_APP_INSTANCE_ID = "TEST_APP_INSTANCE_ID";
    private static final String TEST_RECOVERY_AGENT_APP_INSTANCE_ID = "TEST_RECOVERY_AGENT_APP_INSTANCE_ID";

    @Autowired
    private BasicSpringDataRepositoryMonotonicDatabaseService<TestModel, TestMonotonicEntity> monotonicDBService;
    @Autowired
    private ContiguousIdBlockRepository repository;
    @Autowired
    private ContiguousIdBlockService service;
    @Autowired
    private EntityManager entityManager;

    @Test
    public void testRunRecovery() throws InterruptedException {
        // block1 does not have any accessions used
        ContiguousIdBlock block1 = new ContiguousIdBlock(TEST_CATEGORY, TEST_APP_INSTANCE_ID, 0, 100);
        repository.save(block1);

        // block2 is full and has all accessions used
        ContiguousIdBlock block2 = new ContiguousIdBlock(TEST_CATEGORY, TEST_APP_INSTANCE_ID, 100, 100);
        block2.setLastCommitted(199);
        repository.save(block2);

        // block3 has some of the accessions used but not captured in the block's table
        ContiguousIdBlock block3 = new ContiguousIdBlock(TEST_CATEGORY, TEST_APP_INSTANCE_ID, 200, 100);
        repository.save(block3);
        // save some accessions in db that are not captured in block3
        List<AccessionWrapper<TestModel, String, Long>> accessionsSet = LongStream.range(200l, 225l)
                .boxed()
                .map(longAcc -> new AccessionWrapper<>(longAcc, "hash-1" + longAcc, TestModel.of("test-obj-1-" + longAcc)))
                .collect(Collectors.toList());
        monotonicDBService.save(accessionsSet);

        // block4 should not be recovered as it is after the recover cut off time
        Thread.sleep(2000);
        ContiguousIdBlock block4 = new ContiguousIdBlock(TEST_CATEGORY, TEST_APP_INSTANCE_ID, 300, 100);
        repository.save(block4);

        // run recovery through recovery agent
        LocalDateTime recoverCutOffTime = block3.getLastUpdatedTimestamp();
        MonotonicAccessionRecoveryAgent recoveryAgent = new MonotonicAccessionRecoveryAgent(service, monotonicDBService);
        recoveryAgent.runRecovery(TEST_CATEGORY, TEST_RECOVERY_AGENT_APP_INSTANCE_ID, recoverCutOffTime);

        // Clear the persistence context to ensure we fetch fresh data from the database
        entityManager.flush();
        entityManager.clear();

        List<ContiguousIdBlock> blockList = StreamSupport.stream(repository.findAll().spliterator(), false)
                .sorted(Comparator.comparing(ContiguousIdBlock::getFirstValue))
                .collect(Collectors.toList());
        assertEquals(4, blockList.size());

        assertEquals(TEST_RECOVERY_AGENT_APP_INSTANCE_ID, blockList.get(0).getApplicationInstanceId());
        assertEquals(0, blockList.get(0).getFirstValue());
        assertEquals(-1, blockList.get(0).getLastCommitted());
        assertTrue(blockList.get(0).isNotReserved());

        assertEquals(TEST_RECOVERY_AGENT_APP_INSTANCE_ID, blockList.get(1).getApplicationInstanceId());
        assertEquals(100, blockList.get(1).getFirstValue());
        assertEquals(199, blockList.get(1).getLastCommitted());
        assertTrue(blockList.get(1).isNotReserved());

        assertEquals(TEST_RECOVERY_AGENT_APP_INSTANCE_ID, blockList.get(2).getApplicationInstanceId());
        assertEquals(200, blockList.get(2).getFirstValue());
        assertEquals(224, blockList.get(2).getLastCommitted());
        assertTrue(blockList.get(2).isNotReserved());

        assertEquals(TEST_APP_INSTANCE_ID, blockList.get(3).getApplicationInstanceId());
        assertEquals(300, blockList.get(3).getFirstValue());
        assertEquals(299, blockList.get(3).getLastCommitted());
        assertTrue(blockList.get(3).isReserved());
    }

}
