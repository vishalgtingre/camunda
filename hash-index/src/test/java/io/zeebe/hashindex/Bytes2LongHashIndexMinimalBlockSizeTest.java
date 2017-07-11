/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zeebe.hashindex;

import static org.assertj.core.api.Assertions.assertThat;

import org.agrona.concurrent.UnsafeBuffer;
import org.junit.*;

public class Bytes2LongHashIndexMinimalBlockSizeTest
{
    static final long MISSING_VALUE = -2;

    byte[][] keys = new byte[16][64];

    Bytes2LongHashIndex index;

    @Before
    public void createIndex()
    {
        final int indexSize = 32;

        index = new Bytes2LongHashIndex(indexSize, 1, 64);

        // generate keys
        for (int i = 0; i < keys.length; i++)
        {
            final byte[] val = String.valueOf(i).getBytes();

            for (int j = 0; j < val.length; j++)
            {
                keys[i][j] = val[j];
            }
        }
    }

    @After
    public void close()
    {
        index.close();
    }

    @Test
    public void shouldReturnMissingValueForEmptyMap()
    {
        // given that the map is empty
        assertThat(index.get(keys[0], MISSING_VALUE) == MISSING_VALUE);
    }

    @Test
    public void shouldReturnMissingValueForNonExistingKey()
    {
        // given
        index.put(keys[1], 1);

        // then
        assertThat(index.get(keys[0], MISSING_VALUE) == MISSING_VALUE);
    }

    @Test
    public void shouldReturnLongValueForKey()
    {
        // given
        index.put(keys[1], 1);

        // if then
        assertThat(index.get(keys[1], MISSING_VALUE)).isEqualTo(1);
    }

    @Test
    public void shouldReturnLongValueForKeyFromBuffer()
    {
        // given
        index.put(keys[1], 1);

        // if then
        final UnsafeBuffer keyBuffer = new UnsafeBuffer(keys[1]);
        assertThat(index.get(keyBuffer, 0, keyBuffer.capacity(), MISSING_VALUE)).isEqualTo(1);
    }

    @Test
    public void shouldSplit()
    {
        // given
        index.put(keys[0], 0);

        // if
        index.put(keys[1], 1);

        // then
        assertThat(index.blockCount()).isEqualTo(2);
        assertThat(index.get(keys[0], MISSING_VALUE)).isEqualTo(0);
        assertThat(index.get(keys[1], MISSING_VALUE)).isEqualTo(1);
    }


    @Test
    public void shouldPutMultipleValues()
    {
        for (int i = 0; i < 16; i += 2)
        {
            index.put(keys[i], i);
        }

        for (int i = 1; i < 16; i += 2)
        {
            index.put(keys[i], i);
        }

        for (int i = 0; i < 16; i++)
        {
            assertThat(index.get(keys[i], MISSING_VALUE) == i);
        }
    }

    @Test
    public void shouldPutValueFromBuffer()
    {
        // when
        final UnsafeBuffer keyBuffer = new UnsafeBuffer(keys[1]);
        index.put(keyBuffer, 0, keyBuffer.capacity(), 1);

        // then
        assertThat(index.get(keys[1], -1L)).isEqualTo(1L);
    }

    @Test
    public void shouldPutMultipleValuesInOrder()
    {
        for (int i = 0; i < 16; i++)
        {
            index.put(keys[i], i);
        }

        for (int i = 0; i < 16; i++)
        {
            assertThat(index.get(keys[i], MISSING_VALUE) == i);
        }

        assertThat(index.blockCount()).isEqualTo(16);
    }

    @Test
    public void shouldReplaceMultipleValuesInOrder()
    {
        for (int i = 0; i < 16; i++)
        {
            index.put(keys[i], i);
        }

        for (int i = 0; i < 16; i++)
        {
            assertThat(index.put(keys[i], i)).isTrue();
        }

        assertThat(index.blockCount()).isEqualTo(16);
    }


    @Test
    public void shouldRemoveValueForKey()
    {
        // given
        index.put(keys[1], 1);

        // if
        final long removeResult = index.remove(keys[1], -1);

        //then
        assertThat(removeResult).isEqualTo(1);
        assertThat(index.get(keys[1], -1)).isEqualTo(-1);
    }

    @Test
    public void shouldRemoveValueForKeyFromBuffer()
    {
        // given
        index.put(keys[1], 1);

        // if
        final UnsafeBuffer keyBuffer = new UnsafeBuffer(keys[1]);
        final long removeResult = index.remove(keyBuffer, 0, keyBuffer.capacity(), -1);

        //then
        assertThat(removeResult).isEqualTo(1);
        assertThat(index.get(keys[1], -1)).isEqualTo(-1);
    }

    @Test
    public void shouldNotRemoveValueForDifferentKey()
    {
        // given
        index.put(keys[1], 1);
        index.put(keys[2], 2);

        // if
        final long removeResult = index.remove(keys[1], -1);

        //then
        assertThat(removeResult).isEqualTo(1);
        assertThat(index.get(keys[1], -1)).isEqualTo(-1);
        assertThat(index.get(keys[2], -1)).isEqualTo(2);
    }

}