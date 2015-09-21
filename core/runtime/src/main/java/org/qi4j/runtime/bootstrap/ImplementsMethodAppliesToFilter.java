/*
 * Copyright (c) 2008, Rickard Öberg. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.qi4j.runtime.bootstrap;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.qi4j.api.common.AppliesToFilter;

/**
 * JAVADOC
 */
final class ImplementsMethodAppliesToFilter
    implements AppliesToFilter
{
    @Override
    public boolean appliesTo( Method method, Class<?> mixin, Class<?> compositeType, Class<?> fragmentClass )
    {
        try
        {
            return !Modifier.isAbstract( fragmentClass.getMethod( method.getName(), method.getParameterTypes() )
                                             .getModifiers() );
        }
        catch( NoSuchMethodException e )
        {
            return false;
        }
    }
}