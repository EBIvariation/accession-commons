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
package uk.ac.ebi.ampt2d.commons.accession.utils;

import org.junit.jupiter.api.Test;
import uk.ac.ebi.ampt2d.commons.accession.utils.exceptions.ExponentialBackOffMaxRetriesRuntimeException;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExponentialBackOffTest {

    private class ExtendedRunnable implements Runnable, Supplier<String> {

        private int numRun = 0;

        @Override
        public void run() {
            numRun++;
            throw new RuntimeException();
        }

        @Override
        public String get() {
            numRun++;
            throw new RuntimeException();
        }
    }

    @Test
    public void testRunnableDefaultTotalAttempts() {
        ExtendedRunnable runnable = new ExtendedRunnable();
        assertThrows(ExponentialBackOffMaxRetriesRuntimeException.class, () -> {
            ExponentialBackOff.execute((Runnable) runnable);
        });
        assertEquals(ExponentialBackOff.DEFAULT_TOTAL_ATTEMPTS, runnable.numRun);
    }

    @Test
    public void testFunctionDefaultTotalAttempts() {
        ExtendedRunnable runnable = new ExtendedRunnable();
        assertThrows(ExponentialBackOffMaxRetriesRuntimeException.class, () -> {
            ExponentialBackOff.execute((Supplier) runnable);
        });
        assertEquals(ExponentialBackOff.DEFAULT_TOTAL_ATTEMPTS, runnable.numRun);
    }

    @Test
    public void testRunnableTotalAttempts() {
        ExtendedRunnable runnable = new ExtendedRunnable();
        assertThrows(ExponentialBackOffMaxRetriesRuntimeException.class, () -> {
            ExponentialBackOff.execute((Runnable) runnable, 3, 1000);
        });
        assertEquals(3, runnable.numRun);
    }

    @Test
    public void testFunctionTotalAttempts() {
        ExtendedRunnable runnable = new ExtendedRunnable();
        assertThrows(ExponentialBackOffMaxRetriesRuntimeException.class, () -> {
            ExponentialBackOff.execute((Supplier) runnable, 3, 1000);
        });
        assertEquals(3, runnable.numRun);
    }

    @Test
    public void testRunnable() {
        ExponentialBackOff.execute(() -> {
            //DO NOTHING
        });
    }

    @Test
    public void testFunction() {
        assertTrue(ExponentialBackOff.execute(() -> true));
    }

}
