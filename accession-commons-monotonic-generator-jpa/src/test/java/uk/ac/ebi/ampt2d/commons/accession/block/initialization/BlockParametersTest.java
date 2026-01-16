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
package uk.ac.ebi.ampt2d.commons.accession.block.initialization;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class BlockParametersTest {

    private BlockParameters blockParameters;

    @Test
    public void emptyBlockParameters() {
        Map<String, String> blockInitializations = null;
        assertThrows(BlockInitializationException.class, () -> {
            blockParameters = new BlockParameters("test", blockInitializations);
        });
    }

    @Test
    public void invalidBlockSize() {
        assertThrows(BlockInitializationException.class, () -> {
            blockParameters = new BlockParameters("test", getBlockInitialization("0", "0", "0"));
        });
    }

    @Test
    public void invalidBlockStartValue() {
        assertThrows(BlockInitializationException.class, () -> {
            blockParameters = new BlockParameters("test", getBlockInitialization("1000", "-1", "0"));
        });
    }

    @Test
    public void invalidInterleaveInterval() {
        assertThrows(BlockInitializationException.class, () -> {
            blockParameters = new BlockParameters("test", getBlockInitialization("1000", "0", "1a"));
        });
    }

    @Test
    public void missingBlockParameters() {
        assertThrows(BlockInitializationException.class, () -> {
            blockParameters = new BlockParameters("test", getBlockInitialization("1000", "null", "0"));
        });
    }

    @Test
    public void validBlockParameters() {
        blockParameters = new BlockParameters("test", getBlockInitialization("1000", "0", "0"));
    }

    private Map<String, String> getBlockInitialization(String blockSize, String blockStartValue, String nextBlockInterval) {
        Map<String, String> blockInitializations = new HashMap<>();
        blockInitializations.put("blockSize", blockSize);
        blockInitializations.put("blockStartValue", blockStartValue);
        blockInitializations.put("nextBlockInterval", nextBlockInterval);
        return blockInitializations;
    }
}
