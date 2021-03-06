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
package org.apache.zest.sample.dcicargo.sample_a.data.shipping.cargo;

import java.util.UUID;
import org.apache.zest.api.common.Optional;
import org.apache.zest.api.entity.EntityBuilder;
import org.apache.zest.api.injection.scope.Structure;
import org.apache.zest.api.mixin.Mixins;
import org.apache.zest.api.unitofwork.UnitOfWork;
import org.apache.zest.api.unitofwork.UnitOfWorkFactory;
import org.apache.zest.api.value.ValueBuilder;
import org.apache.zest.api.value.ValueBuilderFactory;
import org.apache.zest.sample.dcicargo.sample_a.data.shipping.delivery.Delivery;

/**
 * Cargo "collection" - could have had a many-association to cargos if it was part of the domain model.
 */
@Mixins( Cargos.Mixin.class )
public interface Cargos
{
    String CARGOS_ID = "Cargos_id";

    Cargo createCargo( RouteSpecification routeSpecification, Delivery delivery, @Optional String id );

    class Mixin
        implements Cargos
    {
        @Structure
        UnitOfWorkFactory uowf;

        @Structure
        ValueBuilderFactory vbf;

        public Cargo createCargo( RouteSpecification routeSpecification, Delivery delivery, String id )
        {
            TrackingId trackingId = buildTrackingId( id );

            UnitOfWork uow = uowf.currentUnitOfWork();
            EntityBuilder<Cargo> cargoBuilder = uow.newEntityBuilder( Cargo.class, trackingId.id().get() );
            cargoBuilder.instance().trackingId().set( trackingId );
            cargoBuilder.instance().origin().set( routeSpecification.origin().get() );
            cargoBuilder.instance().routeSpecification().set( routeSpecification );
            cargoBuilder.instance().delivery().set( delivery );

            return cargoBuilder.newInstance();
        }

        private TrackingId buildTrackingId( String id )
        {
            if( id == null || id.trim().equals( "" ) )
            {
                // Build random tracking id
                final String uuid = UUID.randomUUID().toString().toUpperCase();
                id = uuid.substring( 0, uuid.indexOf( "-" ) );
            }

            ValueBuilder<TrackingId> trackingIdBuilder = vbf.newValueBuilder( TrackingId.class );
            trackingIdBuilder.prototype().id().set( id );
            return trackingIdBuilder.newInstance();
        }
    }
}
