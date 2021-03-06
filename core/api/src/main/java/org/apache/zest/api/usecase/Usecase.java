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

package org.apache.zest.api.usecase;

import java.io.Serializable;
import org.apache.zest.api.common.MetaInfo;
import org.apache.zest.api.structure.MetaInfoHolder;

/**
 * A Usecase. A Usecase is used as a model for UnitOfWork, and helps
 * implementations decide what to do in certain circumstances.
 */
public final class Usecase
    implements Serializable, MetaInfoHolder
{
    public static final Usecase DEFAULT = new Usecase( "Default", new MetaInfo() );

    private static final long serialVersionUID = 1L;
    private final String name;
    private final MetaInfo metaInfo;

    Usecase( String name, MetaInfo metaInfo )
    {
        this.name = name;
        this.metaInfo = metaInfo;
    }

    /**
     * Name of the usecase.
     *
     * @return the name
     */
    public String name()
    {
        return name;
    }

    /**
     * Meta-info for the usecase. This can be of any type, and is typically set when creating the usecase
     * and read during the execution of the usecase.
     *
     * @param infoType the MetaInfo type to retrieve.
     *
     * @return the previously stored metaInfo of the given type for the usecase.
     */
    @Override
    public <T> T metaInfo( Class<T> infoType )
    {
        return metaInfo.get( infoType );
    }

    @Override
    public String toString()
    {
        return name + ", meta info:" + metaInfo;
    }
}
