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
package uk.ac.ebi.ampt2d.test.testers;

import uk.ac.ebi.ampt2d.commons.accession.core.AccessioningService;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionCouldNotBeGeneratedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionDeprecatedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionDoesNotExistException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionMergedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.HashAlreadyExistsException;
import uk.ac.ebi.ampt2d.commons.accession.core.models.AccessionWrapper;
import uk.ac.ebi.ampt2d.test.models.TestModel;

import java.util.Arrays;

public class AccessionTester {
    private static String APPLICATION_INSTANCE_ID = "TEST_APPPLICATION_INSTANCE_ID";

    private AccessioningService<TestModel, String, String> accessioningService;

    public AccessionTester(AccessioningService<TestModel, String, String> accessioningService) {
        this.accessioningService = accessioningService;
    }

    public SingleAccessionTester accession(String value) throws AccessionCouldNotBeGeneratedException {
        return new SingleAccessionTester(accessioningService.getOrCreate(Arrays.asList(TestModel.of(value)), APPLICATION_INSTANCE_ID).get(0));
    }

    public class SingleAccessionTester {

        private static final String DEFAULT_REASON = "reason";

        private String accession;

        public SingleAccessionTester(AccessionWrapper<TestModel, String, String> wrappedAccession) {
            this.accession = wrappedAccession.getAccession();
        }


        public SingleAccessionTester update(int version, String value) throws AccessionDeprecatedException,
                AccessionDoesNotExistException, AccessionMergedException, HashAlreadyExistsException {
            accessioningService.update(accession, version, TestModel.of(value));
            return this;
        }

        public SingleAccessionTester patch(String value) throws AccessionDeprecatedException,
                AccessionDoesNotExistException, AccessionMergedException, HashAlreadyExistsException {
            accessioningService.patch(accession, TestModel.of(value));
            return this;
        }

        public void deprecate() throws AccessionMergedException, AccessionDoesNotExistException,
                AccessionDeprecatedException {
            accessioningService.deprecate(accession, DEFAULT_REASON);
        }

        public void merge(String accession) throws AccessionMergedException, AccessionDoesNotExistException,
                AccessionDeprecatedException {
            accessioningService.merge(this.accession, accession, DEFAULT_REASON);
        }
    }

}
