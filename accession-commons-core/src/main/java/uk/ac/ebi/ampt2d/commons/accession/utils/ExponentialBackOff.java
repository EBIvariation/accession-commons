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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ampt2d.commons.accession.utils.exceptions.ExponentialBackOffMaxRetriesRuntimeException;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Lambda functor to execute a function with a exponential backoff
 */
public abstract class ExponentialBackOff {

    static Logger logger = LoggerFactory.getLogger(ExponentialBackOff.class);

    static int DEFAULT_TOTAL_ATTEMPTS = 7;
    private static int DEFAULT_TIME_BASE = 1000;
    private static int MAX_JITTER = 1000;
    private static int MIN_JITTER = 100;

    public static void execute(Runnable function) {
        execute(function, DEFAULT_TOTAL_ATTEMPTS, DEFAULT_TIME_BASE);
    }

    public static void execute(Runnable function, int totalAttempts, int timeBase) {
        int previousValue = 0;
        int currentValue = 1;
        for (int attempt = 0; attempt < totalAttempts; attempt++) {
            try {
                function.run();
                return;
            } catch (Exception e) {
                logger.trace(e.getMessage());
                doWait(currentValue, timeBase);
                int nextValue = previousValue + currentValue;
                previousValue = currentValue;
                currentValue = nextValue;
            }
        }
        throw new ExponentialBackOffMaxRetriesRuntimeException();
    }

    public static <T> T execute(Supplier<T> function) {
        return execute(function, DEFAULT_TOTAL_ATTEMPTS, DEFAULT_TIME_BASE);
    }

    public static <T> T execute(Supplier<T> function, int totalAttempts, int timeBase) {
        int previousValue = 0;
        int currentValue = 1;
        for (int attempt = 0; attempt < totalAttempts; attempt++) {
            try {
                return function.get();
            } catch (Exception e) {
                logger.trace(e.getMessage());
                doWait(currentValue, timeBase);
                int nextValue = previousValue + currentValue;
                previousValue = currentValue;
                currentValue = nextValue;
            }
        }
        throw new ExponentialBackOffMaxRetriesRuntimeException();
    }

    private static void doWait(int valueInTheSeries, int timeBase) {
        try {
            Thread.sleep(valueInTheSeries * timeBase +
                                 ThreadLocalRandom.current().nextInt(MIN_JITTER, MAX_JITTER));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
