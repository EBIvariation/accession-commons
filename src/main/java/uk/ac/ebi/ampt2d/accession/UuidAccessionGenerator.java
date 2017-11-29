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
package uk.ac.ebi.ampt2d.accession;

import uk.ac.ebi.ampt2d.accession.utils.UUIDUtil;

import java.nio.ByteBuffer;
import java.util.UUID;


public class UuidAccessionGenerator<T> extends SingleAccessionGenerator<T, UUID> {

    private byte[] namespaceUuidBytes;

    public UuidAccessionGenerator(String namespace) {
        namespaceUuidBytes = getNamespaceUUIDBytes(namespace);
    }

    private byte[] getNamespaceUUIDBytes(String namespace) {
        UUID namespaceUuid = UUIDUtil.getNamespaceUUIDFromBytes(namespace.getBytes());
        return UUIDUtil.getUuidAsBytes(namespaceUuid);
    }

    @Override
    protected UUID generateAccession(T object) {
        byte[] hashBytes = ByteBuffer.allocate(4).putInt(object.hashCode()).array();
        UUID accession = UUIDUtil.generateVersion5Uuid(namespaceUuidBytes,hashBytes);
        return accession;
    }

}
