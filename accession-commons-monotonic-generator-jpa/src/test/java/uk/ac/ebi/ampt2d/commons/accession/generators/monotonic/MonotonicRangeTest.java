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

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MonotonicRangeTest {

    @Test
    public void assertNoRangesGeneratedWhenNoValuesProvided() {
        assertTrue(MonotonicRange.convertToMonotonicRanges().isEmpty());
    }

    @Test
    public void assertArrayOfOneRange() {
        List<MonotonicRange> ranges = MonotonicRange.convertToMonotonicRanges(1, 2);
        assertEquals(Arrays.asList(new MonotonicRange(1, 2)), ranges);
    }

    @Test
    public void assertArrayOfTwoRanges() {
        List<MonotonicRange> ranges = MonotonicRange.convertToMonotonicRanges(1, 2, 4, 5);
        assertEquals(Arrays.asList(new MonotonicRange(1, 2), new MonotonicRange(4, 5)), ranges);
    }

    @Test
    public void assertArrayOfThreeRangesAndDifferentSizes() {
        List<MonotonicRange> ranges = MonotonicRange.convertToMonotonicRanges(1, 2, 4, 5, 6, 9, 10, 12);
        assertEquals(Arrays.asList(new MonotonicRange(1, 2), new MonotonicRange(4, 6), new MonotonicRange(9, 10),
                new MonotonicRange(12, 12)), ranges);
    }

    @Test
    public void assertNotIntersectsLeft() {
        MonotonicRange range = new MonotonicRange(20, 29);
        assertFalse(range.intersects(new MonotonicRange(10, 19)));
    }

    @Test
    public void assertNotIntersectsRight() {
        MonotonicRange range = new MonotonicRange(20, 29);
        assertFalse(range.intersects(new MonotonicRange(30, 39)));
    }

    @Test
    public void assertIntersectsLeft() {
        MonotonicRange range = new MonotonicRange(20, 29);
        assertTrue(range.intersects(new MonotonicRange(10, 20)));
    }

    @Test
    public void assertIntersectsRight() {
        MonotonicRange range = new MonotonicRange(20, 29);
        assertTrue(range.intersects(new MonotonicRange(29, 39)));
    }

    @Test
    public void assertIntersectsLarger() {
        MonotonicRange range = new MonotonicRange(20, 29);
        assertTrue(range.intersects(new MonotonicRange(9, 39)));
    }

    @Test
    public void assertIntersectsShorter() {
        MonotonicRange range = new MonotonicRange(20, 29);
        assertTrue(range.intersects(new MonotonicRange(25, 26)));
    }

    @Test
    public void assertExcludeSimpleCases() {
        MonotonicRange range = new MonotonicRange(0, 10);
        assertEquals(Arrays.asList(new MonotonicRange(2, 10)),
                range.excludeIntersections(Arrays.asList(new MonotonicRange(0, 1))));
        assertEquals(Arrays.asList(new MonotonicRange(2, 10)),
                range.excludeIntersections(Arrays.asList(new MonotonicRange(-1, 1))));
        assertEquals(Arrays.asList(new MonotonicRange(0, 0), new MonotonicRange(3, 10)),
                range.excludeIntersections(Arrays.asList(new MonotonicRange(1, 2))));
        assertEquals(Arrays.asList(new MonotonicRange(0, 2)),
                range.excludeIntersections(Arrays.asList(new MonotonicRange(3, 11))));
    }

    @Test
    public void assertExcludeComplexCases() {
        MonotonicRange range = new MonotonicRange(0, 10);
        assertEquals(Arrays.asList(new MonotonicRange(2, 2), new MonotonicRange(7, 10)),
                range.excludeIntersections(Arrays.asList(new MonotonicRange(0, 1), new MonotonicRange(3, 6))));
        assertEquals(Arrays.asList(new MonotonicRange(0, 0), new MonotonicRange(7, 10)),
                range.excludeIntersections(Arrays.asList(new MonotonicRange(1, 2), new MonotonicRange(3, 6))));
        assertEquals(Arrays.asList(new MonotonicRange(0, 0), new MonotonicRange(3, 3), new MonotonicRange(8, 10)),
                range.excludeIntersections(Arrays.asList(new MonotonicRange(1, 2), new MonotonicRange(4, 7))));
        assertEquals(Arrays.asList(new MonotonicRange(0, 0), new MonotonicRange(8, 10)),
                range.excludeIntersections(Arrays.asList(new MonotonicRange(1, 3), new MonotonicRange(2, 7))));
        assertEquals(Arrays.asList(new MonotonicRange(0, 0), new MonotonicRange(3, 4), new MonotonicRange(8, 9)),
                range.excludeIntersections(Arrays.asList(new MonotonicRange(1, 2), new MonotonicRange(5, 7), new MonotonicRange(10, 12))));
    }

    @Test
    public void assertExcludeAllButFirst() {
        MonotonicRange range = new MonotonicRange(0, 5);
        assertEquals(Arrays.asList(new MonotonicRange(1, 5)),
                range.excludeIntersections(Arrays.asList(new MonotonicRange(0, 0))));
    }

    @Test
    public void assertExcludeAllButLast() {
        MonotonicRange range = new MonotonicRange(0, 5);
        assertEquals(Arrays.asList(new MonotonicRange(0, 4)),
                range.excludeIntersections(Arrays.asList(new MonotonicRange(5, 5))));
    }

    @Test
    public void assertExcludeNestedRanges() {
        MonotonicRange range = new MonotonicRange(0, 5);
        assertEquals(Arrays.asList(new MonotonicRange(0, 0), new MonotonicRange(5, 5)),
                range.excludeIntersections(Arrays.asList(new MonotonicRange(1, 4), new MonotonicRange(2, 3))));

        range = new MonotonicRange(0, 10);
        assertEquals(Arrays.asList(new MonotonicRange(0, 0), new MonotonicRange(7, 10)),
                range.excludeIntersections(Arrays.asList(new MonotonicRange(1, 4),
                        new MonotonicRange(2, 3),
                        new MonotonicRange(4, 6))));
    }

    @Test
    public void assertNoIntersectingRegions() {
        MonotonicRange range = new MonotonicRange(0, 10);
        assertEquals(Arrays.asList(new MonotonicRange(0, 10)),
                range.excludeIntersections(Arrays.asList(new MonotonicRange(12, 20))));
    }

    @Test
    public void assertExludeIntersectionNoIntersection() {
        MonotonicRange range = new MonotonicRange(0, 10);
        assertEquals(Arrays.asList(new MonotonicRange(0, 10)),
                range.excludeIntersection(new MonotonicRange(13, 15)));
    }

    @Test
    public void assertExludeIntersectionAll() {
        MonotonicRange range = new MonotonicRange(0, 10);
        assertEquals(Arrays.asList(),range.excludeIntersection(new MonotonicRange(0, 10)));
        assertEquals(Arrays.asList(),range.excludeIntersection(new MonotonicRange(0, 11)));
        assertEquals(Arrays.asList(),range.excludeIntersection(new MonotonicRange(-1, 10)));
        assertEquals(Arrays.asList(),range.excludeIntersection(new MonotonicRange(-1, 11)));
    }

    @Test
    public void assertExludeIntersectionLeft() {
        MonotonicRange range = new MonotonicRange(0, 10);
        assertEquals(Arrays.asList(new MonotonicRange(10, 10)),
                range.excludeIntersection(new MonotonicRange(0, 9)));
        assertEquals(Arrays.asList(new MonotonicRange(9, 10)),
                range.excludeIntersection(new MonotonicRange(0, 8)));
        assertEquals(Arrays.asList(new MonotonicRange(9, 10)),
                range.excludeIntersection(new MonotonicRange(-5, 8)));
    }

    @Test
    public void assertExludeIntersectionRight() {
        MonotonicRange range = new MonotonicRange(0, 10);
        assertEquals(Arrays.asList(new MonotonicRange(0, 0)),
                range.excludeIntersection(new MonotonicRange(1, 10)));
        assertEquals(Arrays.asList(new MonotonicRange(0, 1)),
                range.excludeIntersection(new MonotonicRange(2, 10)));
        assertEquals(Arrays.asList(new MonotonicRange(0, 1)),
                range.excludeIntersection(new MonotonicRange(2, 15)));
    }

    @Test
    public void assertExludeIntersectionMiddle() {
        MonotonicRange range = new MonotonicRange(0, 10);
        assertEquals(Arrays.asList(new MonotonicRange(0, 1),new MonotonicRange(9, 10)),
                range.excludeIntersection(new MonotonicRange(2, 8)));
        assertEquals(Arrays.asList(new MonotonicRange(0, 0),new MonotonicRange(10, 10)),
                range.excludeIntersection(new MonotonicRange(1, 9)));
    }

}
