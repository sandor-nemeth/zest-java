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
package org.apache.zest.runtime.activation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.zest.api.activation.Activator;
import org.apache.zest.api.activation.PassivationException;
import org.apache.zest.functional.Iterables;

/**
 * Instance of a Zest Activators of one Activation target. Contains ordered
 * Activators and roll the Activation on the target.
 *
 * @param <ActivateeType> Type of the activation target
 */
public class ActivatorsInstance<ActivateeType>
    implements Activator<ActivateeType>
{
    @SuppressWarnings( {"raw", "unchecked"} )
    public static final ActivatorsInstance EMPTY = new ActivatorsInstance( Collections.emptyList() );

    private final Iterable<Activator<ActivateeType>> activators;

    public ActivatorsInstance( Iterable<Activator<ActivateeType>> activators )
    {
        this.activators = activators;
    }

    @Override
    public void beforeActivation( ActivateeType activating )
        throws Exception
    {
        for( Activator<ActivateeType> activator : activators )
        {
            activator.beforeActivation( activating );
        }
    }

    @Override
    public void afterActivation( ActivateeType activated )
        throws Exception
    {
        for( Activator<ActivateeType> activator : activators )
        {
            activator.afterActivation( activated );
        }
    }

    @Override
    public void beforePassivation( ActivateeType passivating )
        throws Exception
    {
        Set<Exception> exceptions = new LinkedHashSet<>();
        for( Activator<ActivateeType> activator : Iterables.reverse( activators ) )
        {
            try
            {
                activator.beforePassivation( passivating );
            }
            catch( Exception ex )
            {
                exceptions.add( ex );
            }
        }
        if( !exceptions.isEmpty() )
        {
            throw new PassivationException( exceptions );
        }
    }

    @Override
    public void afterPassivation( ActivateeType passivated )
        throws Exception
    {
        Set<Exception> exceptions = new LinkedHashSet<>();
        for( Activator<ActivateeType> activator : Iterables.reverse( activators ) )
        {
            try
            {
                activator.afterPassivation( passivated );
            }
            catch( Exception ex )
            {
                exceptions.add( ex );
            }
        }
        if( !exceptions.isEmpty() )
        {
            throw new PassivationException( exceptions );
        }
    }

}
