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
package uk.ac.ebi.ampt2d.commons.accession.generators.monotonic;

import org.junit.Test;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionCouldNotBeGeneratedException;
import uk.ac.ebi.ampt2d.commons.accession.core.exceptions.AccessionIsNotPendingException;
import uk.ac.ebi.ampt2d.commons.accession.persistence.jpa.monotonic.entities.ContiguousIdBlock;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BlockManagerTest {

    private static final String CATEGORY_ID = "category-id";
    private static final String INSTANCE_ID = "instance-id";

    @Test
    public void noAvailableAccessionsIfNoBlocks() {
        BlockManager manager = new BlockManager();
        assertFalse(manager.hasAvailableAccessions(10));
    }

    @Test
    public void availableAccessionWhenBlockHashBeenAdded() {
        BlockManager manager = new BlockManager();
        manager.addBlock(new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 0, 100));
        assertTrue(manager.hasAvailableAccessions(10));
        assertFalse(manager.hasAvailableAccessions(101));
    }

    @Test(expected = AccessionIsNotPendingException.class)
    public void commitAccessionsThatHaveNotBeenGenerated() {
        BlockManager manager = new BlockManager();
        manager.commit(new long[]{1, 3, 5});
    }

    @Test(expected = AccessionIsNotPendingException.class)
    public void releaseAccessionsThatHaveNotBeenGenerated() {
        BlockManager manager = new BlockManager();
        manager.release(new long[]{1, 3, 5});
    }

    @Test(expected = AccessionCouldNotBeGeneratedException.class)
    public void pollNextWhenNoValues() throws AccessionCouldNotBeGeneratedException {
        BlockManager manager = new BlockManager();
        manager.pollNext(4);
    }

    @Test
    public void generateAccessions() throws AccessionCouldNotBeGeneratedException {
        BlockManager manager = new BlockManager();
        manager.addBlock(new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 0, 100));
        long[] accessions = manager.pollNext(10);
        assertEquals(10, accessions.length);
        assertArrayEquals(new long[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, accessions);
    }

    @Test
    public void generateAccessionsAndRelease() throws AccessionCouldNotBeGeneratedException {
        BlockManager manager = new BlockManager();
        manager.addBlock(new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 0, 100));
        long[] accessions = manager.pollNext(10);
        assertEquals(10, accessions.length);
        assertArrayEquals(new long[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, accessions);
        manager.release(new long[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
        accessions = manager.pollNext(10);
        assertEquals(10, accessions.length);
        assertArrayEquals(new long[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, accessions);
    }

    @Test
    public void generateAccessionsAndReleaseSome() throws AccessionCouldNotBeGeneratedException {
        BlockManager manager = new BlockManager();
        manager.addBlock(new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 0, 100));
        long[] accessions = manager.pollNext(10);
        assertEquals(10, accessions.length);
        assertArrayEquals(new long[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, accessions);
        manager.release(new long[]{0, 1, 2, 6, 7, 8, 9});
        accessions = manager.pollNext(10);
        assertEquals(3, accessions.length);
        assertArrayEquals(new long[]{0, 1, 2}, accessions);
        accessions = manager.pollNext(10);
        assertEquals(4, accessions.length);
        assertArrayEquals(new long[]{6, 7, 8, 9}, accessions);
    }

    @Test
    public void generateAccessionsAndConfirmSome() throws AccessionCouldNotBeGeneratedException {
        BlockManager manager = new BlockManager();
        manager.addBlock(new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 0, 100));
        long[] accessions = manager.pollNext(10);
        assertEquals(10, accessions.length);
        assertArrayEquals(new long[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}, accessions);
        manager.commit(new long[]{0, 1, 2, 6, 7, 8, 9});
        accessions = manager.pollNext(10);
        assertEquals(10, accessions.length);
        assertArrayEquals(new long[]{10, 11, 12, 13, 14, 15, 16, 17, 18, 19}, accessions);
    }

    @Test
    public void recoverState() throws AccessionCouldNotBeGeneratedException {
        BlockManager manager = new BlockManager();
        manager.addBlock(new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 0, 100));
        manager.recoverState(new long[]{0, 1, 2, 6, 7, 8, 9});
        long[] accessions = manager.pollNext(10);
        assertEquals(3, accessions.length);
        assertArrayEquals(new long[]{3, 4, 5}, accessions);
        accessions = manager.pollNext(10);
        assertEquals(10, accessions.length);
        assertArrayEquals(new long[]{10, 11, 12, 13, 14, 15, 16, 17, 18, 19}, accessions);
    }

    @Test
    public void multipleContinuousBlocks() throws AccessionCouldNotBeGeneratedException {
        BlockManager manager = new BlockManager();
        manager.addBlock(new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 0, 10));
        manager.addBlock(new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 10, 10));
        manager.recoverState(new long[]{0, 1, 2, 6, 7, 8, 9});
        long[] accessions = manager.pollNext(10);
        assertEquals(3, accessions.length);
        assertArrayEquals(new long[]{3, 4, 5}, accessions);
        accessions = manager.pollNext(10);
        assertEquals(10, accessions.length);
        assertArrayEquals(new long[]{10, 11, 12, 13, 14, 15, 16, 17, 18, 19}, accessions);
    }

    @Test
    public void commitAllValuesOnBlockManager() throws AccessionCouldNotBeGeneratedException {
        BlockManager manager = new BlockManager();
        manager.addBlock(new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 0, 10));
        manager.addBlock(new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 10, 10));
        long[] accessions1 = manager.pollNext(10);
        long[] accessions2 = manager.pollNext(10);
        manager.commit(accessions1);
        manager.commit(accessions2);
    }

    @Test
    public void testGetAssignedBlocks(){
        BlockManager manager = new BlockManager();
        manager.addBlock(new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 0, 10));
        manager.addBlock(new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 10, 10));

        assertEquals(2, manager.getAssignedBlocks().size());
    }

    @Test
    public void testShutDownBlockManager(){
        BlockManager manager = new BlockManager();
        manager.addBlock(new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 0, 10));
        manager.addBlock(new ContiguousIdBlock(CATEGORY_ID, INSTANCE_ID, 10, 10));
        manager.shutDownBlockManager();

        assertEquals(0, manager.getAssignedBlocks().size());
        assertEquals(0, manager.getAvailableRanges().size());

    }

}
