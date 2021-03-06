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
package org.apache.zest.sample.forum.domainevent;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.function.Function;
import org.apache.zest.api.concern.Concerns;
import org.apache.zest.api.concern.GenericConcern;
import org.apache.zest.api.injection.scope.Structure;
import org.apache.zest.api.structure.Application;
import org.apache.zest.api.unitofwork.UnitOfWork;
import org.apache.zest.api.unitofwork.UnitOfWorkCallback;
import org.apache.zest.api.unitofwork.UnitOfWorkCompletionException;
import org.apache.zest.api.unitofwork.UnitOfWorkFactory;
import org.apache.zest.api.value.ValueBuilder;
import org.apache.zest.api.value.ValueBuilderFactory;
import org.apache.zest.functional.Iterables;
import org.apache.zest.library.rest.server.api.ObjectSelection;
import org.restlet.Request;

/**
 * TODO
 */
@Concerns( DomainEvent.DomainEventConcern.class )
@Retention( RetentionPolicy.RUNTIME )
public @interface DomainEvent
{
    class DomainEventConcern
        extends GenericConcern
    {
        @Structure
        ValueBuilderFactory vbf;

        @Structure
        UnitOfWorkFactory uowf;

        @Structure
        Application application;

        @Override
        public Object invoke( Object proxy, Method method, Object[] args )
            throws Throwable
        {
            Object result = next.invoke( proxy, method, args );

            UnitOfWork unitOfWork = uowf.currentUnitOfWork();

            ValueBuilder<DomainEventValue> builder = vbf.newValueBuilder( DomainEventValue.class );
            DomainEventValue prototype = builder.prototype();
            prototype.version().set( application.version() );
            prototype.timestamp().set( unitOfWork.currentTime() );
            prototype.context().set( proxy.getClass().getSuperclass().getName().split( "\\$" )[ 0 ] );
            prototype.name().set( method.getName() );

            int idx = 0;
            for( Object arg : args )
            {
                idx++;
                String name = "param" + idx;
                ValueBuilder<ParameterValue> parameterBuilder = vbf.newValueBuilder( ParameterValue.class );
                parameterBuilder.prototype().name().set( name );
                parameterBuilder.prototype().value().set( arg );
                prototype.parameters().get().add( parameterBuilder.newInstance() );
            }

            Iterables.addAll( prototype.selection().get(), Iterables.map( new Function<Object, String>()
            {
                @Override
                public String apply( Object o )
                {
                    return o.toString();
                }
            }, ObjectSelection.current().selection() ) );

            final DomainEventValue domainEvent = builder.newInstance();

            unitOfWork.addUnitOfWorkCallback( new UnitOfWorkCallback()
            {
                @Override
                public void beforeCompletion()
                    throws UnitOfWorkCompletionException
                {
                }

                @Override
                public void afterCompletion( UnitOfWorkStatus status )
                {
                    if( status.equals( UnitOfWorkStatus.COMPLETED ) )
                    {
                        Request.getCurrent().getAttributes().put( "event", domainEvent );
                    }
                }
            } );

            return result;
        }
    }
}
