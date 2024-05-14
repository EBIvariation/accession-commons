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

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.entities.ContiguousIdBlock;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ContiguousIdBlockRepository extends CrudRepository<ContiguousIdBlock, Long> {
    @Query("SELECT cib FROM ContiguousIdBlock cib WHERE cib.categoryId = :categoryId AND cib.lastCommitted != cib.lastValue AND (cib.reserved IS NULL OR cib.reserved IS FALSE) ORDER BY cib.lastValue asc")
    List<ContiguousIdBlock> findUncompletedAndUnreservedBlocksOrderByLastValueAsc(@Param("categoryId") String categoryId);

    ContiguousIdBlock findFirstByCategoryIdOrderByLastValueDesc(String categoryId);

    List<ContiguousIdBlock> findByCategoryIdAndReservedIsTrueAndLastUpdatedTimestampLessThanEqualOrderByLastValueAsc(
            String categoryId, LocalDateTime lastUpdatedTime);
}
