
/*
 * Repackaged from org.apache.lucene.util.OpenBitSet (Lucene).
 * svn rev. 1479633, https://svn.apache.org/repos/asf/lucene/dev/trunk
 * 
 * Minor changes in class hierarchy, removed serialization.
 */


/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.carrotsearch.hppcrt;

/**
 * A variety of high efficiency bit twiddling routines.
 */
public final class BitUtil
{

    private BitUtil() {} // no instance

    // The pop methods used to rely on bit-manipulation tricks for speed but it
    // turns out that it is faster to use the Long.bitCount method (which is an
    // intrinsic since Java 6u18) in a naive loop, see LUCENE-2221

    /** Returns the number of set bits in an array of longs. */
    public static long pop_array(final long[] arr, final int wordOffset, final int numWords) {
        long popCount = 0;
        for (int i = wordOffset, end = wordOffset + numWords; i < end; ++i) {
            popCount += Long.bitCount(arr[i]);
        }
        return popCount;
    }

    /** Returns the popcount or cardinality of the two sets after an intersection.
     *  Neither array is modified. */
    public static long pop_intersect(final long[] arr1, final long[] arr2, final int wordOffset, final int numWords) {
        long popCount = 0;
        for (int i = wordOffset, end = wordOffset + numWords; i < end; ++i) {
            popCount += Long.bitCount(arr1[i] & arr2[i]);
        }
        return popCount;
    }

    /** Returns the popcount or cardinality of the union of two sets.
     *  Neither array is modified. */
    public static long pop_union(final long[] arr1, final long[] arr2, final int wordOffset, final int numWords) {
        long popCount = 0;
        for (int i = wordOffset, end = wordOffset + numWords; i < end; ++i) {
            popCount += Long.bitCount(arr1[i] | arr2[i]);
        }
        return popCount;
    }

    /** Returns the popcount or cardinality of A & ~B.
     *  Neither array is modified. */
    public static long pop_andnot(final long[] arr1, final long[] arr2, final int wordOffset, final int numWords) {
        long popCount = 0;
        for (int i = wordOffset, end = wordOffset + numWords; i < end; ++i) {
            popCount += Long.bitCount(arr1[i] & ~arr2[i]);
        }
        return popCount;
    }

    /** Returns the popcount or cardinality of A ^ B
     * Neither array is modified. */
    public static long pop_xor(final long[] arr1, final long[] arr2, final int wordOffset, final int numWords) {
        long popCount = 0;
        for (int i = wordOffset, end = wordOffset + numWords; i < end; ++i) {
            popCount += Long.bitCount(arr1[i] ^ arr2[i]);
        }
        return popCount;
    }

    /** returns the next highest power of two, or the current value if it's already a power of two or zero*/
    public static int nextHighestPowerOfTwo(int v) {
        v--;
        v |= v >> 1;
        v |= v >> 2;
        v |= v >> 4;
        v |= v >> 8;
        v |= v >> 16;
        v++;
        return v;
    }

    /** returns the next highest power of two, or the current value if it's already a power of two or zero*/
    public static long nextHighestPowerOfTwo(long v) {
        v--;
        v |= v >> 1;
        v |= v >> 2;
        v |= v >> 4;
        v |= v >> 8;
        v |= v >> 16;
        v |= v >> 32;
        v++;
        return v;
    }
}
