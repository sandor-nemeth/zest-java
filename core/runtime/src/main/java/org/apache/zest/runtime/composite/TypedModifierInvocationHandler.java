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
package org.apache.zest.runtime.composite;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.zest.api.composite.InvalidCompositeException;

/**
 * JAVADOC
 */
public final class TypedModifierInvocationHandler
    extends FragmentInvocationHandler
{
    @Override
    public Object invoke( Object proxy, Method method, Object[] args )
        throws Throwable
    {
        try
        {
            return this.method.invoke( fragment, args );
        }
        catch( InvocationTargetException e )
        {
            throw cleanStackTrace( e.getTargetException(), proxy, method );
        }
        catch( Throwable e )
        {
            if( fragment == null )
            {
                throw new InvalidCompositeException( "No fragment available for method " + method.getName() );
            }
            throw cleanStackTrace( e, proxy, method );
        }
    }
}