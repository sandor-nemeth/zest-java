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
package org.apache.zest.sample.dcicargo.sample_b.context.interaction.booking.exception;

import org.apache.zest.sample.dcicargo.sample_b.data.structure.cargo.RouteSpecification;
import org.apache.zest.sample.dcicargo.sample_b.data.structure.itinerary.Itinerary;

/**
 * Javadoc
 */
public class UnsatisfyingRouteException extends Exception
{
    private RouteSpecification routeSpec;
    private Itinerary itinerary;

    public UnsatisfyingRouteException( RouteSpecification routeSpec, Itinerary itinerary )
    {
        this.routeSpec = routeSpec;
        this.itinerary = itinerary;
    }

    @Override
    public String getMessage()
    {
//        return "Route specification was not satisfied with itinerary.";

        // When testing:
        return "Route specification was not satisfied with itinerary:\n" + routeSpec.print() + "\n" + itinerary.print();
    }
}