/*
 *
 * Copyright 2019 EMBL - European Bioinformatics Institute
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
package uk.ac.ebi.ampt2d.commons.accession.hashing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Function;

/**
 * Implementation of the MD5 hashing function, in order to generate a unique hash from a given string.
 */
public class MD5HashingFunction implements Function<String, String> {

    private static final Logger MD5_UTIL_LOGGER = LoggerFactory.getLogger(MD5HashingFunction.class);

    @Override
    public String apply(String summary) {
        return generateMD5FromBytes(summary.getBytes());
    }

    private static String generateMD5FromBytes(byte[] nameBytes) {
        return DatatypeConverter.printHexBinary(toMD5(nameBytes));
    }

    private static byte[] toMD5(byte[] bytes) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            MD5_UTIL_LOGGER.error("No Such Algorithm - MD5");
        }
        return md.digest(bytes);
    }
}
