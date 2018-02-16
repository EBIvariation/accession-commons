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
 */
package uk.ac.ebi.ampt2d.accession;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

@NoRepositoryBean
public interface AccessioningRepository<ENTITY_TYPE extends AccessionableEntity, ID_TYPE extends Serializable>
        extends Repository<ENTITY_TYPE, ID_TYPE> {

    Collection<ENTITY_TYPE> findByHashedMessageIn(List<ID_TYPE> hashes);

    <S extends ENTITY_TYPE> Iterable<S> save(Iterable<S> var1);

    long count();

    Collection<ENTITY_TYPE> findByAccessionIn(List<ID_TYPE> accessions);
}
