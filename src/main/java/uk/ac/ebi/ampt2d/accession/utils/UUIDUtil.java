/*
 *
 * Copyright 2017 EMBL - European Bioinformatics Institute
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
package uk.ac.ebi.ampt2d.accession.utils;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/* UUID Util class to generate Version 5 UUID*/
public class UUIDUtil {

    // Based on the private UUID(bytes[]) constructor
    private static UUID getUuidFromBytes(byte[] data) {
        long msb = 0;
        long lsb = 0;
        assert data.length == 16;
        for (int i = 0; i < 8; i++)
            msb = (msb << 8) | (data[i] & 0xff);
        for (int i = 8; i < 16; i++)
            lsb = (lsb << 8) | (data[i] & 0xff);
        return new UUID(msb, lsb);
    }

    public static byte[] getUuidAsBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    public static UUID getNamespaceUUIDFromBytes(byte[] namespace) {
        //Setting Root Name Space to Null for getting UUID for Namespace String
        byte[] rootNamespaceUuid = getUuidAsBytes(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        return generateVersion5Uuid(rootNamespaceUuid, namespace);
    }

    public static UUID generateVersion5Uuid(byte[] namespaceUuidBytes, byte[] nameBytes) {
        byte[] namespaceAndNameBytes = new byte[namespaceUuidBytes.length + nameBytes.length];
        System.arraycopy(namespaceUuidBytes, 0, namespaceAndNameBytes, 0, namespaceUuidBytes.length);
        System.arraycopy(nameBytes, 0, namespaceAndNameBytes, namespaceUuidBytes.length, nameBytes.length);

        byte[] resultIn20Bytes = toSHA1(namespaceAndNameBytes);
        byte[] resultTrimmedTo16Bytes = new byte[16];
        System.arraycopy(resultIn20Bytes, 0, resultTrimmedTo16Bytes, 0, 16);

        resultTrimmedTo16Bytes[6] &= 0x0F;
        resultTrimmedTo16Bytes[6] |= 0x50;
        resultTrimmedTo16Bytes[8] &= 0x3F;
        resultTrimmedTo16Bytes[8] |= 0x80;

        return getUuidFromBytes(resultTrimmedTo16Bytes);
    }

    public static byte[] toSHA1(byte[] bytes) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return md.digest(bytes);
    }
}
