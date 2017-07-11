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

import org.junit.*;
import org.junit.rules.ExpectedException;

public class Long2LongHashIndexLargeBlockSizeTest
{
    static final long MISSING_VALUE = -2;

    Long2LongHashIndex index;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void createIndex() throws Exception
    {
        index = new Long2LongHashIndex(16, 3);
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
        assertThat(index.get(0, MISSING_VALUE) == MISSING_VALUE);
    }

    @Test
    public void shouldReturnMissingValueForNonExistingKey()
    {
        // given
        index.put(1, 1);

        // then
        assertThat(index.get(0, MISSING_VALUE) == MISSING_VALUE);
    }

    @Test
    public void shouldReturnLongValueForKey()
    {
        // given
        index.put(1, 1);

        // if then
        assertThat(index.get(1, MISSING_VALUE)).isEqualTo(1);
    }

    @Test
    public void shouldRemoveValueForKey()
    {
        // given
        index.put(1, 1);

        // if
        final long removeResult = index.remove(1, -1);

        //then
        assertThat(removeResult).isEqualTo(1);
        assertThat(index.get(1, -1)).isEqualTo(-1);
    }

    @Test
    public void shouldNotRemoveValueForDifferentKey()
    {
        // given
        index.put(1, 1);
        index.put(2, 2);

        // if
        final long removeResult = index.remove(1, -1);

        //then
        assertThat(removeResult).isEqualTo(1);
        assertThat(index.get(1, -1)).isEqualTo(-1);
        assertThat(index.get(2, -1)).isEqualTo(2);
    }

    @Test
    public void shouldNotSplitBeforeBlockIsFull()
    {
        // given
        index.put(0, 0);

        // if
        index.put(1, 1);

        // then
        assertThat(index.blockCount()).isEqualTo(1);
        assertThat(index.get(0, MISSING_VALUE)).isEqualTo(0);
        assertThat(index.get(1, MISSING_VALUE)).isEqualTo(1);
    }

    @Test
    public void shouldSplitWhenBlockIsFull()
    {
        // given
        index.put(1, 1);
        index.put(2, 2);
        index.put(3, 3);
        assertThat(index.blockCount()).isEqualTo(1);

        // if
        index.put(4, 4);

        // then
        assertThat(index.blockCount()).isEqualTo(2);
        assertThat(index.get(1, MISSING_VALUE)).isEqualTo(1);
        assertThat(index.get(2, MISSING_VALUE)).isEqualTo(2);
        assertThat(index.get(3, MISSING_VALUE)).isEqualTo(3);
        assertThat(index.get(4, MISSING_VALUE)).isEqualTo(4);
    }

    @Test
    public void shouldSplitMultipleTimesWhenBlockIsFull()
    {
        // given
        index.put(1, 1);
        index.put(3, 3);
        index.put(5, 5);
        assertThat(index.blockCount()).isEqualTo(1);

        // if
        index.put(7, 7);

        // then
        assertThat(index.blockCount()).isEqualTo(3);
        assertThat(index.get(1, MISSING_VALUE)).isEqualTo(1);
        assertThat(index.get(3, MISSING_VALUE)).isEqualTo(3);
        assertThat(index.get(5, MISSING_VALUE)).isEqualTo(5);
        assertThat(index.get(7, MISSING_VALUE)).isEqualTo(7);
    }

    @Test
    public void shouldPutMultipleValues()
    {
        for (int i = 0; i < 16; i += 2)
        {
            index.put(i, i);
        }

        for (int i = 1; i < 16; i += 2)
        {
            index.put(i, i);
        }

        for (int i = 0; i < 16; i++)
        {
            assertThat(index.get(i, MISSING_VALUE)).isEqualTo(i);
        }

        assertThat(index.blockCount()).isEqualTo(8);
    }

    @Test
    public void shouldPutMultipleValuesInOrder()
    {
        for (int i = 0; i < 16; i++)
        {
            index.put(i, i);
        }

        for (int i = 0; i < 16; i++)
        {
            assertThat(index.get(i, MISSING_VALUE)).isEqualTo(i);
        }

        assertThat(index.blockCount()).isEqualTo(8);
    }

    @Test
    public void shouldReplaceMultipleValuesInOrder()
    {
        for (int i = 0; i < 16; i++)
        {
            index.put(i, i);
        }

        for (int i = 0; i < 16; i++)
        {
            assertThat(index.put(i, i)).isTrue();
        }

        assertThat(index.blockCount()).isEqualTo(8);
    }

}