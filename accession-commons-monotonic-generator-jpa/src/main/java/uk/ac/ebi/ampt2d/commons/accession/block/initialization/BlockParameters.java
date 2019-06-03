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

import java.util.Map;

/**
 * Initialization parameters for blocks of monotonic accession
 */
public class BlockParameters {

    private static final String BLOCK_SIZE = "blockSize";
    private static final String BLOCK_START_VALUE = "blockStartValue";
    private static final String NEXT_BLOCK_INTERVAL = "nextBlockInterval";

    private long blockStartValue;
    private long blockSize;
    private long nextBlockInterval;

    public BlockParameters(String categoryId, Map<String, String> blockInitializations) {
        StringBuilder errorBuffer = new StringBuilder();
        blockStartValue = parseVariable(errorBuffer, BLOCK_START_VALUE, blockInitializations, 0);
        blockSize = parseVariable(errorBuffer, BLOCK_SIZE, blockInitializations, 1);
        nextBlockInterval = parseVariable(errorBuffer, NEXT_BLOCK_INTERVAL, blockInitializations, 0);

        if (errorBuffer.length() > 0) {
            throw new BlockInitializationException("Error while parsing parameters for category '" + categoryId + "' " +
                    "error: '" + errorBuffer.toString() + "'");
        }
    }

    private long parseVariable(StringBuilder errorBuffer, String variable,
                               Map<String, String> variables, int minValue) {
        if (variables == null){
            errorBuffer.append("No parameters found");
            return -1;
        }
        if (!variables.containsKey(variable)) {
            errorBuffer.append(" Variable '" + variable + "' is missing.");
            return -1;
        } else {
            try {
                Long value = Long.parseLong(variables.get(variable));
                if (value < minValue) {
                    errorBuffer.append(" Variable '" + variable + "' value should be greater than or equal to '" + minValue + "'");
                    return -1;
                }
                return value;
            } catch (NumberFormatException e) {
                errorBuffer.append(" Variable '" + variable + "' could not be parsed correctly.");
                return -1;
            }
        }
    }

    public long getBlockStartValue() {
        return blockStartValue;
    }

    public long getBlockSize() {
        return blockSize;
    }

    public long getNextBlockInterval() {
        return nextBlockInterval;
    }

    @Override
    public String toString() {
        return "BlockParameters{" +
                "blockStartValue=" + blockStartValue +
                ", blockSize=" + blockSize +
                ", nextBlockInterval=" + nextBlockInterval +
                '}';
    }
}
