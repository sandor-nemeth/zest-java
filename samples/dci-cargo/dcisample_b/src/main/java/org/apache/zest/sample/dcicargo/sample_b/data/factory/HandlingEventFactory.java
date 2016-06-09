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
package org.apache.zest.sample.dcicargo.sample_b.data.factory;

import java.util.Date;
import org.apache.zest.api.common.Optional;
import org.apache.zest.api.entity.EntityBuilder;
import org.apache.zest.api.injection.scope.Structure;
import org.apache.zest.api.mixin.Mixins;
import org.apache.zest.api.unitofwork.UnitOfWork;
import org.apache.zest.api.unitofwork.UnitOfWorkFactory;
import org.apache.zest.sample.dcicargo.sample_b.data.factory.exception.CannotCreateHandlingEventException;
import org.apache.zest.sample.dcicargo.sample_b.data.structure.handling.HandlingEvent;
import org.apache.zest.sample.dcicargo.sample_b.data.structure.handling.HandlingEventType;
import org.apache.zest.sample.dcicargo.sample_b.data.structure.location.Location;
import org.apache.zest.sample.dcicargo.sample_b.data.structure.tracking.TrackingId;
import org.apache.zest.sample.dcicargo.sample_b.data.structure.voyage.Voyage;

/**
 * HandlingEventFactory
 *
 * Creates a valid handling event
 *
 * - Verifies if a voyage is mandatory or prohibited for the handling event type
 * - Verifies if a load/unload location is expected by the voyage
 *
 * Validations of TrackingId, Location and Voyage are considered out of this scope.
 */
@Mixins( HandlingEventFactory.Mixin.class )
public interface HandlingEventFactory
{
    HandlingEvent createHandlingEvent( Date registrationTime,
                                       Date completionTime,
                                       TrackingId trackingId,
                                       HandlingEventType handlingEventType,
                                       Location location,
                                       @Optional Voyage voyage
    )
        throws CannotCreateHandlingEventException;

    public abstract class Mixin
        implements HandlingEventFactory
    {
        @Structure
        UnitOfWorkFactory uowf;

        public HandlingEvent createHandlingEvent( Date registrationTime,
                                                  Date completionTime,
                                                  TrackingId trackingId,
                                                  HandlingEventType handlingEventType,
                                                  Location location,
                                                  Voyage voyage
        )
            throws CannotCreateHandlingEventException
        {
            if( voyage == null && handlingEventType.requiresVoyage() )
            {
                throw new CannotCreateHandlingEventException( "Voyage is required for handling event type " + handlingEventType );
            }

            else if( voyage != null && handlingEventType.prohibitsVoyage() )
            {
                throw new CannotCreateHandlingEventException( "Voyage is not allowed with handling event type " + handlingEventType );
            }

            UnitOfWork uow = uowf.currentUnitOfWork();
            EntityBuilder<HandlingEvent> handlingEventBuilder = uow.newEntityBuilder( HandlingEvent.class );
            handlingEventBuilder.instance().registrationTime().set( registrationTime );
            handlingEventBuilder.instance().completionTime().set( completionTime );
            handlingEventBuilder.instance().trackingId().set( trackingId );
            handlingEventBuilder.instance().handlingEventType().set( handlingEventType );
            handlingEventBuilder.instance().location().set( location );
            handlingEventBuilder.instance().voyage().set( voyage );

            // Save and return
            return handlingEventBuilder.newInstance();
        }
    }
}