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
package org.apache.zest.sample.dcicargo.sample_b.context.rolemap;

import org.apache.zest.api.entity.EntityComposite;
import org.apache.zest.sample.dcicargo.sample_b.context.interaction.booking.routing.AssignCargoToRoute;
import org.apache.zest.sample.dcicargo.sample_b.context.interaction.booking.routing.RegisterNewDestination;
import org.apache.zest.sample.dcicargo.sample_b.context.interaction.booking.specification.DeriveUpdatedRouteSpecification;
import org.apache.zest.sample.dcicargo.sample_b.context.interaction.handling.inspection.InspectCargoDeliveryStatus;
import org.apache.zest.sample.dcicargo.sample_b.context.interaction.handling.inspection.event.InspectArrivedCargo;
import org.apache.zest.sample.dcicargo.sample_b.context.interaction.handling.inspection.event.InspectCargoInCustoms;
import org.apache.zest.sample.dcicargo.sample_b.context.interaction.handling.inspection.event.InspectClaimedCargo;
import org.apache.zest.sample.dcicargo.sample_b.context.interaction.handling.inspection.event.InspectLoadedCargo;
import org.apache.zest.sample.dcicargo.sample_b.context.interaction.handling.inspection.event.InspectReceivedCargo;
import org.apache.zest.sample.dcicargo.sample_b.context.interaction.handling.inspection.event.InspectUnhandledCargo;
import org.apache.zest.sample.dcicargo.sample_b.context.interaction.handling.inspection.event.InspectUnloadedCargo;
import org.apache.zest.sample.dcicargo.sample_b.data.structure.cargo.Cargo;

/**
 * Cargo Role Map
 *
 * Shows all the Roles that a CargoEntity can play.
 *
 * Note that the CargoEntity knows nothing about this map (and that Cargo (Data) knows nothing about CargoEntity).
 */
public interface CargoRoleMap
    extends

    RegisterNewDestination.CargoInspectorRole,
    AssignCargoToRoute.CargoInspectorRole,
    DeriveUpdatedRouteSpecification.CargoInspectorRole,

    InspectCargoDeliveryStatus.DeliveryInspectorRole,

    InspectUnhandledCargo.DeliveryInspectorRole,
    InspectReceivedCargo.DeliveryInspectorRole,
    InspectLoadedCargo.DeliveryInspectorRole,
    InspectUnloadedCargo.DeliveryInspectorRole,
    InspectArrivedCargo.DeliveryInspectorRole,
    InspectCargoInCustoms.DeliveryInspectorRole,
    InspectClaimedCargo.DeliveryInspectorRole, EntityComposite, Cargo
{
}
