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

    ContiguousIdBlock findFirstByCategoryIdOrderByLastValueDesc(String categoryId);

    List<ContiguousIdBlock> findByCategoryIdAndReservedIsTrueAndLastUpdatedTimestampLessThanEqualOrderByLastValueAsc(
            String categoryId, LocalDateTime lastUpdatedTime);
}
