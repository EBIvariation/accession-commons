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
package uk.ac.ebi.ampt2d.commons.accession.hashing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.Function;

/**
 * Implementation of the SHA1 hashing function, in order to generate a unique hash from a given string.
 */
public class SHA1HashingFunction implements Function<String, String> {

    private static final Logger sha1UtilLogger = LoggerFactory.getLogger(SHA1HashingFunction.class);

    @Override
    public String apply(String summary) {
        return generateSha1FromBytes(summary.getBytes());
    }

    private static String generateSha1FromBytes(byte[] nameBytes) {
        return HexFormat.of().withUpperCase().formatHex(toSHA1(nameBytes));
    }

    private static byte[] toSHA1(byte[] bytes) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            sha1UtilLogger.error("No Such Algorithm - SHA-1");
        }
        return md.digest(bytes);
    }
}
