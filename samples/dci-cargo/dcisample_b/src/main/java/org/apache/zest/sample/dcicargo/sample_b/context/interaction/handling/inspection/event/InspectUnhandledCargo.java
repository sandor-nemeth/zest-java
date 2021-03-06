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
package org.apache.zest.sample.dcicargo.sample_b.context.interaction.handling.inspection.event;

import java.time.Instant;
import org.apache.zest.api.injection.scope.This;
import org.apache.zest.api.mixin.Mixins;
import org.apache.zest.api.value.ValueBuilder;
import org.apache.zest.sample.dcicargo.sample_b.context.interaction.handling.inspection.exception.InspectionException;
import org.apache.zest.sample.dcicargo.sample_b.context.interaction.handling.inspection.exception.InspectionFailedException;
import org.apache.zest.sample.dcicargo.sample_b.data.structure.cargo.Cargo;
import org.apache.zest.sample.dcicargo.sample_b.data.structure.cargo.RouteSpecification;
import org.apache.zest.sample.dcicargo.sample_b.data.structure.delivery.Delivery;
import org.apache.zest.sample.dcicargo.sample_b.data.structure.delivery.NextHandlingEvent;
import org.apache.zest.sample.dcicargo.sample_b.data.structure.handling.HandlingEvent;
import org.apache.zest.sample.dcicargo.sample_b.data.structure.itinerary.Itinerary;
import org.apache.zest.sample.dcicargo.sample_b.infrastructure.dci.Context;
import org.apache.zest.sample.dcicargo.sample_b.infrastructure.dci.RoleMixin;

import static org.apache.zest.sample.dcicargo.sample_b.data.structure.delivery.RoutingStatus.MISROUTED;
import static org.apache.zest.sample.dcicargo.sample_b.data.structure.delivery.RoutingStatus.NOT_ROUTED;
import static org.apache.zest.sample.dcicargo.sample_b.data.structure.delivery.RoutingStatus.ROUTED;
import static org.apache.zest.sample.dcicargo.sample_b.data.structure.delivery.TransportStatus.NOT_RECEIVED;
import static org.apache.zest.sample.dcicargo.sample_b.data.structure.handling.HandlingEventType.RECEIVE;

/**
 * Inspect Unhandled Cargo (subfunction use case)
 *
 * This is one the variations of the {@link org.apache.zest.sample.dcicargo.sample_b.context.interaction.handling.inspection.InspectCargoDeliveryStatus} use case.
 *
 * Here we can check a cargo that hasn't been received in origin yet.
 */
public class InspectUnhandledCargo extends Context
{
    private DeliveryInspectorRole deliveryInspector;

    private HandlingEvent noEvent;

    private RouteSpecification routeSpecification;

    private Itinerary itinerary;

    public InspectUnhandledCargo( Cargo cargo )
    {
        deliveryInspector = rolePlayer( DeliveryInspectorRole.class, cargo );

        noEvent = cargo.delivery().get().lastHandlingEvent().get();

        routeSpecification = cargo.routeSpecification().get();
        itinerary = cargo.itinerary().get();
    }

    public void inspect()
        throws InspectionException
    {
        // Pre-conditions
        if( noEvent != null )
        {
            throw new InspectionFailedException( "Can only inspect unhandled cargo." );
        }

        deliveryInspector.inspectUnhandledCargo();
    }

    @Mixins( DeliveryInspectorRole.Mixin.class )
    public interface DeliveryInspectorRole
    {
        void setContext( InspectUnhandledCargo context );

        void inspectUnhandledCargo()
            throws InspectionException;

        class Mixin
            extends RoleMixin<InspectUnhandledCargo>
            implements DeliveryInspectorRole
        {
            @This
            Cargo cargo;

            Delivery newDelivery;

            public void inspectUnhandledCargo()
                throws InspectionException
            {
                // Step 1 - Collect known delivery data

                ValueBuilder<Delivery> newDeliveryBuilder = vbf.newValueBuilder( Delivery.class );
                newDelivery = newDeliveryBuilder.prototype();
                newDelivery.timestamp().set( Instant.now() );
                newDelivery.lastHandlingEvent().set( null );
                newDelivery.transportStatus().set( NOT_RECEIVED );
                newDelivery.isUnloadedAtDestination().set( false );
                newDelivery.itineraryProgressIndex().set( 0 );

                // Can't be misdirected before being handled
                newDelivery.isMisdirected().set( false );

                // Step 2 - Determine if cargo is routed

                if( c.itinerary == null )
                {
                    newDelivery.routingStatus().set( NOT_ROUTED );
                    newDelivery.eta().set( null );
                }
                else if( !c.routeSpecification.isSatisfiedBy( c.itinerary ) )
                {
                    newDelivery.routingStatus().set( MISROUTED );
                    newDelivery.eta().set( null );
                }
                else
                {
                    newDelivery.routingStatus().set( ROUTED );
                    newDelivery.eta().set( c.itinerary.eta() );
                }

                // Step 3 - Expect receipt in origin location

                ValueBuilder<NextHandlingEvent> nextHandlingEvent = vbf.newValueBuilder( NextHandlingEvent.class );
                nextHandlingEvent.prototype().handlingEventType().set( RECEIVE );
                nextHandlingEvent.prototype().location().set( cargo.origin().get() );
                nextHandlingEvent.prototype().date().set( null );
                nextHandlingEvent.prototype().voyage().set( null );
                newDelivery.nextHandlingEvent().set( nextHandlingEvent.newInstance() );

                // Step 4 - Save cargo delivery snapshot

                cargo.delivery().set( newDeliveryBuilder.newInstance() );
            }
        }
    }
}