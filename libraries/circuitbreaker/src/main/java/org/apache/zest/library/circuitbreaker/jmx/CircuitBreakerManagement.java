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
package org.apache.zest.library.circuitbreaker.jmx;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.apache.zest.api.activation.ActivatorAdapter;
import org.apache.zest.api.activation.Activators;
import org.apache.zest.api.injection.scope.Service;
import org.apache.zest.api.injection.scope.Structure;
import org.apache.zest.api.mixin.Mixins;
import org.apache.zest.api.service.ServiceComposite;
import org.apache.zest.api.service.ServiceReference;
import org.apache.zest.api.structure.Application;
import org.apache.zest.library.circuitbreaker.CircuitBreaker;
import org.apache.zest.library.circuitbreaker.service.ServiceCircuitBreaker;
import org.apache.zest.library.jmx.ZestMBeans;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JMX service that exposes ServiceCircuitBreakers as MBeans.
 * Logs exposed CircuitBreakers state changes.
 */
@Mixins( CircuitBreakerManagement.Mixin.class )
@Activators( CircuitBreakerManagement.Activator.class )
public interface CircuitBreakerManagement
        extends ServiceComposite
{

    /**
     * Expose all visible CircuitBreakers in JMX.
     */
    void registerCircuitBreakers()
            throws JMException;

    /**
     * Unregister all exposed CircuitBreakers.
     */
    void unregisterCircuitBreakers()
            throws JMException;

    class Activator
            extends ActivatorAdapter<ServiceReference<CircuitBreakerManagement>>
    {

        @Override
        public void afterActivation( ServiceReference<CircuitBreakerManagement> activated )
                throws Exception
        {
            activated.get().registerCircuitBreakers();
        }

        @Override
        public void beforePassivation( ServiceReference<CircuitBreakerManagement> passivating )
                throws Exception
        {
            passivating.get().unregisterCircuitBreakers();
        }

    }

    abstract class Mixin
            implements CircuitBreakerManagement
    {

        private static final Logger LOGGER = LoggerFactory.getLogger( CircuitBreakerManagement.class );

        private final Map<CircuitBreaker, ObjectName> registeredCircuitBreakers = new HashMap<CircuitBreaker, ObjectName>();

        @Structure
        private Application application;

        @Service
        private MBeanServer server;

        @Service
        Iterable<ServiceReference<ServiceCircuitBreaker>> circuitBreakers;

        @Override
        public void registerCircuitBreakers()
                throws JMException
        {
            for ( ServiceReference<ServiceCircuitBreaker> circuitBreaker : circuitBreakers )
            {
                registerCircuitBreaker( circuitBreaker.get().circuitBreaker(), circuitBreaker.identity() );
            }
        }

        @Override
        public void unregisterCircuitBreakers()
                throws JMException
        {
            for ( ObjectName objectName : registeredCircuitBreakers.values() )
            {
                server.unregisterMBean( objectName );
            }
            registeredCircuitBreakers.clear();
        }

        private void registerCircuitBreaker( final CircuitBreaker circuitBreaker, final String name )
                throws JMException
        {
            ObjectName mbeanObjectName = null;

            ObjectName serviceName = ZestMBeans.findServiceName( server, application.name(), name );
            if ( serviceName != null )
            {
                mbeanObjectName = new ObjectName( serviceName.toString() + ",name=Circuit breaker" );
            }
            else
            {
                try
                {
                    mbeanObjectName = new ObjectName( "CircuitBreaker:name=" + name );
                }
                catch ( MalformedObjectNameException e )
                {
                    throw new IllegalArgumentException( "Illegal name:" + name );
                }
            }

            CircuitBreakerJMX bean = new CircuitBreakerJMX( circuitBreaker, mbeanObjectName );

            server.registerMBean( bean, mbeanObjectName );
            registeredCircuitBreakers.put( circuitBreaker, mbeanObjectName );

            // Add logger
            circuitBreaker.addPropertyChangeListener( new PropertyChangeListener()
            {

                @Override
                public void propertyChange( PropertyChangeEvent evt )
                {
                    if ( "status".equals( evt.getPropertyName() ) )
                    {
                        if ( CircuitBreaker.Status.on.equals( evt.getNewValue() ) )
                        {
                            LOGGER.info( "Circuit breaker " + name + " is now on" );
                        }
                        else
                        {
                            LOGGER.error( "Circuit breaker " + name + " is now off" );
                        }
                    }
                }

            } );
        }

    }

}
