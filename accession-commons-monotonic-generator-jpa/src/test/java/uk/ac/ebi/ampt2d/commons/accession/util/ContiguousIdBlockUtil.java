package uk.ac.ebi.ampt2d.commons.accession.util;

import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.entities.ContiguousIdBlock;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.repositories.ContiguousIdBlockRepository;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ContiguousIdBlockUtil {

    public static ContiguousIdBlock getUnreservedContiguousIdBlock(ContiguousIdBlock block) {
        block.releaseReserved();
        return block;
    }

    public static ContiguousIdBlock getUnreservedContiguousIdBlock(String categoryId, String instanceId, long firstValue, long size) {
        ContiguousIdBlock block = new ContiguousIdBlock(categoryId, instanceId, firstValue, size);
        block.releaseReserved();
        return block;
    }

    public static List<ContiguousIdBlock> getAllBlocksInDB(ContiguousIdBlockRepository repository) {
        return StreamSupport.stream(repository.findAll().spliterator(), false)
                .sorted(Comparator.comparing(ContiguousIdBlock::getFirstValue))
                .collect(Collectors.toList());
    }

    public static List<ContiguousIdBlock> getAllBlocksForCategoryId(ContiguousIdBlockRepository repository, String categoryId) {
        return StreamSupport.stream(repository.findAll().spliterator(), false)
                .filter(block -> block.getCategoryId().equals(categoryId))
                .sorted(Comparator.comparing(ContiguousIdBlock::getFirstValue))
                .collect(Collectors.toList());
    }

    public static List<ContiguousIdBlock> getAllUncompletedAndUnReservedBlocksForCategoryId(ContiguousIdBlockRepository repository,
                                                                                            String categoryId) {
        return StreamSupport.stream(repository.findAll().spliterator(), false)
                .filter(block -> block.getCategoryId().equals(categoryId))
                .filter(block -> block.isNotFull())
                .filter(block -> block.isNotReserved())
                .sorted(Comparator.comparing(ContiguousIdBlock::getFirstValue))
                .collect(Collectors.toList());
    }

    public static List<ContiguousIdBlock> getAllUncompletedBlocksForCategoryId(ContiguousIdBlockRepository repository,
                                                                               String categoryId) {
        return StreamSupport.stream(repository.findAll().spliterator(), false)
                .filter(block -> block.getCategoryId().equals(categoryId))
                .filter(block -> block.isNotFull())
                .sorted(Comparator.comparing(ContiguousIdBlock::getFirstValue))
                .collect(Collectors.toList());
    }

    public static List<ContiguousIdBlock> getAllCompletedBlocksForCategoryId(ContiguousIdBlockRepository repository,
                                                                             String categoryId) {
        return StreamSupport.stream(repository.findAll().spliterator(), false)
                .filter(block -> block.getCategoryId().equals(categoryId))
                .filter(block -> block.isFull())
                .sorted(Comparator.comparing(ContiguousIdBlock::getFirstValue))
                .collect(Collectors.toList());
    }

    public static List<ContiguousIdBlock> getAllUnreservedBlocksForCategoryId(ContiguousIdBlockRepository repository,
                                                                              String categoryId) {
        return StreamSupport.stream(repository.findAll().spliterator(), false)
                .filter(block -> block.getCategoryId().equals(categoryId))
                .filter(block -> block.isNotReserved())
                .sorted(Comparator.comparing(ContiguousIdBlock::getFirstValue))
                .collect(Collectors.toList());
    }

    public static List<ContiguousIdBlock> getAllReservedBlocksForCategoryId(ContiguousIdBlockRepository repository,
                                                                            String categoryId) {
        return StreamSupport.stream(repository.findAll().spliterator(), false)
                .filter(block -> block.getCategoryId().equals(categoryId))
                .filter(block -> block.isReserved())
                .sorted(Comparator.comparing(ContiguousIdBlock::getFirstValue))
                .collect(Collectors.toList());
    }

    public static List<ContiguousIdBlock> getAllCompletedAndReservedBlocksForCategoryId(ContiguousIdBlockRepository repository,
                                                                                        String categoryId) {
        return StreamSupport.stream(repository.findAll().spliterator(), false)
                .filter(block -> block.getCategoryId().equals(categoryId))
                .filter(block -> block.isFull())
                .filter(block -> block.isReserved())
                .sorted(Comparator.comparing(ContiguousIdBlock::getFirstValue))
                .collect(Collectors.toList());
    }

}
