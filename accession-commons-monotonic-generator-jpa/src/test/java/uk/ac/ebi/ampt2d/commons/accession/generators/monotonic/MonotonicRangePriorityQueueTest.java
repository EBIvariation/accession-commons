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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MonotonicRangePriorityQueueTest {

    @Test
    public void testEmptyCollection() {
        MonotonicRangePriorityQueue queue = new MonotonicRangePriorityQueue();
        assertEquals(0, queue.size());
        assertEquals(0, queue.getNumOfValuesInQueue());
    }

    @Test
    public void testAddElement() {
        MonotonicRangePriorityQueue queue = new MonotonicRangePriorityQueue();
        queue.add(new MonotonicRange(2,5));
        assertEquals(1, queue.size());
        assertEquals(4, queue.getNumOfValuesInQueue());
    }

    @Test
    public void testOfferElement() {
        MonotonicRangePriorityQueue queue = new MonotonicRangePriorityQueue();
        queue.offer(new MonotonicRange(2,5));
        assertEquals(1, queue.size());
        assertEquals(4, queue.getNumOfValuesInQueue());
    }

    @Test
    public void testRemoveElement() {
        MonotonicRangePriorityQueue queue = new MonotonicRangePriorityQueue();
        MonotonicRange range = new MonotonicRange(2,5);
        queue.add(range);
        queue.remove(range);
        assertEquals(0, queue.size());
        assertEquals(0, queue.getNumOfValuesInQueue());
    }

    @Test
    public void testPeekElement() {
        MonotonicRangePriorityQueue queue = new MonotonicRangePriorityQueue();
        MonotonicRange range = new MonotonicRange(2,5);
        queue.add(range);
        assertEquals(range, queue.peek());
        assertEquals(1, queue.size());
        assertEquals(4, queue.getNumOfValuesInQueue());
    }

    @Test
    public void testPollElement() {
        MonotonicRangePriorityQueue queue = new MonotonicRangePriorityQueue();
        MonotonicRange range = new MonotonicRange(2,5);
        queue.add(range);
        assertEquals(range, queue.poll());
        assertEquals(0, queue.size());
        assertEquals(0, queue.getNumOfValuesInQueue());
    }

    @Test
    public void testClear(){
        MonotonicRangePriorityQueue queue = new MonotonicRangePriorityQueue();
        MonotonicRange range = new MonotonicRange(2,5);
        queue.clear();
        assertEquals(0, queue.size());
        assertEquals(0, queue.getNumOfValuesInQueue());
    }

}
