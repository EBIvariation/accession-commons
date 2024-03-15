package uk.ac.ebi.ampt2d.commons.accession.util;

import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.entities.ContiguousIdBlock;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.repositories.ContiguousIdBlockRepository;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static List<ContiguousIdBlock> getUncompletedBlocksByCategoryIdAndApplicationInstanceIdOrderByEndAsc(ContiguousIdBlockRepository repository,
            String categoryId, String applicationInstanceId) {
        try (Stream<ContiguousIdBlock> reservedBlocksOfThisInstance = repository
                .findAllByCategoryIdAndApplicationInstanceIdOrderByLastValueAsc(categoryId, applicationInstanceId)) {
            List<ContiguousIdBlock> blocksList = reservedBlocksOfThisInstance.filter(block -> block.isNotFull())
                    .collect(Collectors.toList());

            return blocksList;
        }
    }

}
