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
package org.apache.zest.runtime.unitofwork;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.zest.api.entity.EntityReference;
import org.apache.zest.spi.entity.NamedAssociationState;

/**
 * Default implementation of NamedAssociationState that also
 * keeps a list of changes that can be extracted at any time.
 */
public final class BuilderNamedAssociationState
    implements NamedAssociationState
{
    private final Map<String, EntityReference> references;

    public BuilderNamedAssociationState()
    {
        references = new HashMap<>();
    }

    @Override
    public int count()
    {
        return references.size();
    }

    @Override
    public boolean containsName( String name )
    {
        return references.containsKey( name );
    }

    @Override
    public boolean put( String name, EntityReference entityReference )
    {
        return references.put( name, entityReference ) != null;
    }

    @Override
    public boolean remove( String name )
    {
        return references.remove( name ) != null;
    }

    @Override
    public EntityReference get( String name )
    {
        return references.get( name );
    }

    @Override
    public String nameOf( EntityReference entityReference )
    {
        for( Map.Entry<String, EntityReference> entry : references.entrySet() )
        {
            if( entry.getValue().equals( entityReference ) )
            {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public Iterator<String> iterator()
    {
        return references.keySet().iterator();
    }
}
