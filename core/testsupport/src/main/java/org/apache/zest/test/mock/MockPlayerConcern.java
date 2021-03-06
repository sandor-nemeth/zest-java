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
package org.apache.zest.test.mock;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import org.apache.zest.api.concern.ConcernOf;

public class MockPlayerConcern
    extends ConcernOf<InvocationHandler>
    implements InvocationHandler
{
    /**
     * @see java.lang.reflect.InvocationHandler#invoke(Object, java.lang.reflect.Method, Object[])
     */
    @Override
    public Object invoke( final Object proxy, final Method method, final Object[] args )
        throws Throwable
    {
        // TODO implementation
        throw new UnsupportedOperationException( "MockResolver player concern is not yet implemented" );
    }
}
