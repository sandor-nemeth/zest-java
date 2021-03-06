/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.zest.library.appbrowser.json;

import org.json.JSONException;
import org.json.JSONWriter;
import org.apache.zest.library.appbrowser.Formatter;

public abstract class AbstractJsonFormatter<NODE,LEAF>
    implements Formatter<NODE, LEAF>
{
    private final JSONWriter writer;

    public AbstractJsonFormatter( JSONWriter writer )
    {
        this.writer = writer;
    }

    protected void field( String name, String value )
        throws JSONException
    {
        writer.key( name ).value(value);
    }

    protected void field( String name, boolean value )
        throws JSONException
    {
        writer.key( name ).value(value);
    }

    protected void array( String name )
        throws JSONException
    {
        writer.key(name);
        writer.array();
    }

    protected void endArray()
        throws JSONException
    {
        writer.endArray();
    }

    protected void object()
        throws JSONException
    {
        writer.object();
    }

    protected void object(String name)
        throws JSONException
    {
        writer.key(name);
        writer.object();
    }

    protected void endObject()
        throws JSONException
    {
        writer.endObject();
    }

    protected void value( Object value )
        throws JSONException
    {
        writer.value( value );
    }

}
