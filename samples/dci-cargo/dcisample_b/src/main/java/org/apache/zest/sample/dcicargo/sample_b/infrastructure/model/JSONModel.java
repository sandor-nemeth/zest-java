/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package org.apache.zest.sample.dcicargo.sample_b.infrastructure.model;

import org.apache.zest.api.value.ValueComposite;
import org.apache.zest.api.value.ValueSerializer;

/**
 * JSONModel
 *
 * Model that can serialize/de-serialize an object to/from a JSON string.
 */
public class JSONModel<T, U extends ValueComposite>
    extends ReadOnlyModel<T>
{
    private Class<U> valueCompositeClass;
    private String json;
    private transient T valueComposite;

    @SuppressWarnings( "unchecked" )
    public JSONModel( T valueComposite, Class<U> valueCompositeClass )
    {
        json = serviceFinder.findService( ValueSerializer.class ).get().serialize( (U) valueComposite );;
        this.valueCompositeClass = valueCompositeClass;
    }

    @SuppressWarnings( "unchecked" )
    public static <T, U extends ValueComposite> JSONModel<T, U> of( T value )
    {
        if( !( value instanceof ValueComposite ) )
        {
            throw new RuntimeException( value + " has to be an instance of a ValueComposite." );
        }

        // Get ValueComposite interface
        Class<U> valueCompositeClass = (Class<U>) api.valueDescriptorFor( value ).valueType().mainType();

        return new JSONModel<T, U>( value, valueCompositeClass );
    }

    @SuppressWarnings( "unchecked" )
    public T getObject()
    {
        if( valueComposite == null && json != null )
        {
            // De-serialize
            valueComposite = (T) vbf.newValueFromSerializedState( valueCompositeClass, json ); // Unchecked cast
        }
        return valueComposite;
    }

    public void detach()
    {
        valueComposite = null;
    }
}
