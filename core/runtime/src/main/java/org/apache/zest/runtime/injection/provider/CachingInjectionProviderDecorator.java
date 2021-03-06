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

package org.apache.zest.runtime.injection.provider;

import org.apache.zest.runtime.injection.InjectionContext;
import org.apache.zest.runtime.injection.InjectionProvider;

/**
 * If a dependency resolution should be a singleton, wrap it with this
 * to provide a single instance "cache".
 */
public final class CachingInjectionProviderDecorator
    implements InjectionProvider
{
    private final InjectionProvider decoratedProvider;
    private volatile Object singletonInstance;

    public CachingInjectionProviderDecorator( InjectionProvider injectionProvider )
    {
        this.decoratedProvider = injectionProvider;
    }

    public InjectionProvider decoratedProvider()
    {
        return decoratedProvider;
    }

    @Override
    public Object provideInjection( InjectionContext context )
        throws InjectionProviderException
    {
        if( singletonInstance == null )
        {
            synchronized( this )
            {
                if( singletonInstance == null )
                {
                    singletonInstance = decoratedProvider.provideInjection( context );
                }
            }
        }

        return singletonInstance;
    }
}
