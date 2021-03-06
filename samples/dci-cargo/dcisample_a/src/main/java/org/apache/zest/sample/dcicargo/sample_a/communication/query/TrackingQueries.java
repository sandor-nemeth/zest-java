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
import org.apache.zest.api.query.Query;
import org.apache.zest.api.query.QueryBuilder;
import org.apache.zest.api.query.QueryExpressions;
import org.apache.zest.sample.dcicargo.sample_a.data.shipping.cargo.Cargo;
import org.apache.zest.sample.dcicargo.sample_a.data.shipping.handling.HandlingEvent;
import org.apache.zest.sample.dcicargo.sample_a.infrastructure.model.Queries;
import org.apache.zest.sample.dcicargo.sample_a.infrastructure.model.QueryModel;

import static org.apache.zest.api.query.QueryExpressions.*;

/**
 * Tracking queries
 *
 * Used by the communication layer only. Can change according to ui needs.
 */
public class TrackingQueries extends Queries
{
    public List<String> routedCargos()
    {
        Cargo cargoEntity = templateFor( Cargo.class );

        QueryBuilder<Cargo> qb = qbf.newQueryBuilder( Cargo.class )
            .where( isNotNull( cargoEntity.itinerary() ) );
        Query<Cargo> cargos = uowf.currentUnitOfWork().newQuery( qb )
            .orderBy( orderBy( cargoEntity.trackingId().get().id() ) );

        List<String> cargoList = new ArrayList<String>();
        for( Cargo cargo : cargos )
        {
            cargoList.add( cargo.trackingId().get().id().get() );
        }

        return cargoList;
    }

    public IModel<List<HandlingEvent>> events( final String trackingIdString )
    {
        return new QueryModel<HandlingEvent>( HandlingEvent.class )
        {
            public Query<HandlingEvent> getQuery()
            {
                HandlingEvent eventTemplate = templateFor( HandlingEvent.class );

                QueryBuilder<HandlingEvent> qb = qbf.newQueryBuilder( HandlingEvent.class )
                    .where( QueryExpressions.eq( eventTemplate.trackingId().get().id(), trackingIdString ) );
                return uowf
                    .currentUnitOfWork()
                    .newQuery( qb )
                    .orderBy( orderBy( eventTemplate.completionDate() ) );
            }
        };
    }
}