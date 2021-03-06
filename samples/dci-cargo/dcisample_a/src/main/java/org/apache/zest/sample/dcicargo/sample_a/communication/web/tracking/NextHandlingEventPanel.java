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
package org.apache.zest.sample.dcicargo.sample_a.communication.web.tracking;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.devutils.stateless.StatelessComponent;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.ValueMap;
import org.apache.zest.sample.dcicargo.sample_a.data.shipping.cargo.Cargo;
import org.apache.zest.sample.dcicargo.sample_a.data.shipping.delivery.ExpectedHandlingEvent;
import org.apache.zest.sample.dcicargo.sample_a.data.shipping.handling.HandlingEvent;
import org.apache.zest.sample.dcicargo.sample_a.data.shipping.handling.HandlingEventType;
import org.apache.zest.sample.dcicargo.sample_a.data.shipping.location.Location;

/**
 * Next expected handling event
 *
 * Quite some logic to render 1 line of information!
 */
@StatelessComponent
public class NextHandlingEventPanel extends Panel
{
    public NextHandlingEventPanel( String id, IModel<Cargo> cargoModel )
    {
        super( id );

        ValueMap map = new ValueMap();
        Label label = new Label( "text", new StringResourceModel(
            "expectedEvent.${expectedEvent}", this, new Model<>( map ) ) );
        add( label );

        Cargo cargo = cargoModel.getObject();
        Location destination = cargo.routeSpecification().get().destination().get();

        if( cargo.itinerary().get() == null )
        {
            map.put( "expectedEvent", "ROUTE" );
            return;
        }

        HandlingEvent previousEvent = cargo.delivery().get().lastHandlingEvent().get();
        if( previousEvent == null )
        {
            map.put( "expectedEvent", "RECEIVE" );
            map.put( "location", cargo.routeSpecification().get().origin().get().getString() );
            return;
        }

        Location lastLocation = previousEvent.location().get();
        if( previousEvent.handlingEventType().get() == HandlingEventType.CLAIM && lastLocation == destination )
        {
            map.put( "expectedEvent", "END_OF_CYCLE" );
            map.put( "location", destination.getString() );
            label.add( new AttributeModifier( "class", "correctColor" ) );
            return;
        }

        ExpectedHandlingEvent nextEvent = cargo.delivery().get().nextExpectedHandlingEvent().get();
        if( nextEvent == null )
        {
            map.put( "expectedEvent", "UNKNOWN" );
            label.add( new AttributeModifier( "class", "errorColor" ) );
            return;
        }

        map.put( "expectedEvent", nextEvent.handlingEventType().get().name() );
        map.put( "location", nextEvent.location().get().getString() );

        if( nextEvent.date() != null )
        {
            map.put( "time", nextEvent.date().get().toString() );
        }

        if( nextEvent.voyage().get() != null )
        {
            map.put( "voyage", nextEvent.voyage().get().voyageNumber().get().number().get() );
        }
    }
}
