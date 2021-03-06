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
package org.apache.zest.sample.dcicargo.sample_a.communication.query;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.model.IModel;
import org.apache.zest.api.composite.TransientComposite;
import org.apache.zest.api.injection.scope.Service;
import org.apache.zest.api.injection.scope.Structure;
import org.apache.zest.api.mixin.Mixins;
import org.apache.zest.api.unitofwork.UnitOfWorkFactory;
import org.apache.zest.sample.dcicargo.sample_a.context.support.FoundNoRoutesException;
import org.apache.zest.sample.dcicargo.sample_a.context.support.RoutingService;
import org.apache.zest.sample.dcicargo.sample_a.data.shipping.cargo.Cargo;
import org.apache.zest.sample.dcicargo.sample_a.data.shipping.itinerary.Itinerary;
import org.apache.zest.sample.dcicargo.sample_a.infrastructure.model.JSONModel;

/**
 * Booking queries
 *
 * This is in a Zest composite since we can then conveniently get the routing service injected.
 * We could choose to implement all query classes like this too.
 *
 * Used by the communication layer only. Can change according to ui needs.
 */
@Mixins( BookingQueries.Mixin.class )
public interface BookingQueries
    extends TransientComposite
{
    List<IModel<Itinerary>> routeCandidates( String trackingIdString )
        throws FoundNoRoutesException;

    abstract class Mixin
        implements BookingQueries
    {
        @Structure
        UnitOfWorkFactory uowf;

        @Service
        RoutingService routingService;

        public List<IModel<Itinerary>> routeCandidates( final String trackingIdString )
            throws FoundNoRoutesException
        {
            Cargo cargo = uowf.currentUnitOfWork().get( Cargo.class, trackingIdString );
            List<Itinerary> routes = routingService.fetchRoutesForSpecification( cargo.routeSpecification().get() );

            List<IModel<Itinerary>> modelList = new ArrayList<IModel<Itinerary>>();
            for( Itinerary itinerary : routes )
            {
                modelList.add( JSONModel.of( itinerary ) );
            }
            return modelList;
        }
    }
}
