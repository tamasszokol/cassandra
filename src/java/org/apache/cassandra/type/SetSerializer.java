/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cassandra.type;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.*;

public class SetSerializer<T> extends CollectionSerializer<Set<T>>
{
    // interning instances
    private static final Map<AbstractSerializer<?>, SetSerializer> instances = new HashMap<AbstractSerializer<?>, SetSerializer>();

    public final AbstractSerializer<T> elements;

    public static synchronized <T> SetSerializer<T> getInstance(AbstractSerializer<T> elements)
    {
        SetSerializer<T> t = instances.get(elements);
        if (t == null)
        {
            t = new SetSerializer<T>(elements);
            instances.put(elements, t);
        }
        return t;
    }

    private SetSerializer(AbstractSerializer<T> elements)
    {
        this.elements = elements;
    }

    public Set<T> serialize(ByteBuffer bytes)
    {
        try
        {
            ByteBuffer input = bytes.duplicate();
            int n = getUnsignedShort(input);
            Set<T> l = new LinkedHashSet<T>(n);
            for (int i = 0; i < n; i++)
            {
                int s = getUnsignedShort(input);
                byte[] data = new byte[s];
                input.get(data);
                ByteBuffer databb = ByteBuffer.wrap(data);
                elements.validate(databb);
                l.add(elements.serialize(databb));
            }
            return l;
        }
        catch (BufferUnderflowException e)
        {
            throw new MarshalException("Not enough bytes to read a list");
        }
    }

    /**
     * Layout is: {@code <n><s_1><b_1>...<s_n><b_n> }
     * where:
     *   n is the number of elements
     *   s_i is the number of bytes composing the ith element
     *   b_i is the s_i bytes composing the ith element
     */
    public ByteBuffer deserialize(Set<T> value)
    {
        List<ByteBuffer> bbs = new ArrayList<ByteBuffer>(value.size());
        int size = 0;
        for (T elt : value)
        {
            ByteBuffer bb = elements.deserialize(elt);
            bbs.add(bb);
            size += 2 + bb.remaining();
        }
        return pack(bbs, value.size(), size);
    }

    public String toString(Set<T> value)
    {
        StringBuffer sb = new StringBuffer();
        boolean isFirst = true;
        for (T element : value)
        {
            if (isFirst)
            {
                isFirst = false;
            }
            else
            {
                sb.append("; ");
            }
            sb.append(elements.toString(element));
        }
        return sb.toString();
    }

    public Class<Set<T>> getType()
    {
        return (Class) Set.class;
    }
}