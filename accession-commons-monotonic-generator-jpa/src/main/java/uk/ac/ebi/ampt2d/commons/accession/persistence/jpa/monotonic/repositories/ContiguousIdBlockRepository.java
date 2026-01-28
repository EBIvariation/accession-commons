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
package uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.entities.ContiguousIdBlock;

import javax.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ContiguousIdBlockRepository extends CrudRepository<ContiguousIdBlock, Long> {
    @Query("SELECT cib FROM ContiguousIdBlock cib WHERE cib.categoryId = :categoryId AND cib.lastCommitted != cib.lastValue AND (cib.reserved IS NULL OR cib.reserved IS FALSE) ORDER BY cib.lastValue asc")
    // The pessimistic write lock ("select for update" in SQL) ensures that multiple application instances running
    // concurrently won't reserve the same incomplete blocks. This will prevent any other application from accessing
    // these rows until the transaction is either rolled back or committed (i.e., other applications using this method
    // will be blocked).
    // Note that application instances reserving the same new blocks is prevented by the uniqueness constraint in the
    // database and subsequent retry in the accession generator.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ContiguousIdBlock> findUncompletedAndUnreservedBlocksOrderByLastValueAsc(@Param("categoryId") String categoryId,
                                                                                  Pageable pageable);

    /**
     * Atomically finds and locks the first uncompleted and unreserved block for a given category.
     * Uses PostgreSQL's FOR UPDATE SKIP LOCKED to prevent race conditions where multiple concurrent
     * transactions could reserve the same block.
     *
     * SKIP LOCKED ensures that:
     * 1. The first available (unlocked) row is atomically selected and locked
     * 2. Rows already locked by other transactions are skipped rather than waited for
     * 3. Each concurrent transaction gets a different row (or null if none available)
     *
     * This solves the race condition that exists with SERIALIZABLE isolation + PESSIMISTIC_WRITE,
     * where Postgre
     * SQL's SSI evaluates WHERE clauses against snapshots rather than current data.
     *
     * @param categoryId the category ID to search for blocks
     * @return the first uncompleted and unreserved block, or null if none available
     */
    @Query(value = "SELECT * FROM contiguous_id_blocks " +
            "WHERE category_id = :categoryId " +
            "AND last_committed != last_value " +
            "AND (reserved IS NULL OR reserved = FALSE) " +
            "ORDER BY last_value ASC " +
            "LIMIT 1 " +
            "FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    ContiguousIdBlock findFirstUncompletedAndUnreservedBlockForUpdate(@Param("categoryId") String categoryId);

    ContiguousIdBlock findFirstByCategoryIdOrderByLastValueDesc(String categoryId);

    List<ContiguousIdBlock> findByCategoryIdAndReservedIsTrueAndLastUpdatedTimestampLessThanEqualOrderByLastValueAsc(
            String categoryId, LocalDateTime lastUpdatedTime);
}
