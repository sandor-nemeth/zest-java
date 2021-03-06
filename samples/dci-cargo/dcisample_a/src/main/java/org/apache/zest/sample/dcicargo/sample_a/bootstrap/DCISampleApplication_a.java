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
package org.apache.zest.sample.dcicargo.sample_a.bootstrap;

import org.apache.wicket.ConverterLocator;
import org.apache.wicket.Page;
import org.apache.wicket.datetime.PatternDateConverter;
import org.apache.wicket.devutils.stateless.StatelessChecker;
import org.apache.zest.sample.dcicargo.sample_a.communication.web.booking.BookNewCargoPage;
import org.apache.zest.sample.dcicargo.sample_a.communication.web.booking.CargoDetailsPage;
import org.apache.zest.sample.dcicargo.sample_a.communication.web.booking.CargoListPage;
import org.apache.zest.sample.dcicargo.sample_a.communication.web.booking.ChangeDestinationPage;
import org.apache.zest.sample.dcicargo.sample_a.communication.web.booking.RouteCargoPage;
import org.apache.zest.sample.dcicargo.sample_a.communication.web.handling.RegisterHandlingEventPage;
import org.apache.zest.sample.dcicargo.sample_a.communication.web.tracking.TrackCargoPage;
import org.apache.zest.sample.dcicargo.sample_a.infrastructure.WicketZestApplication;
import org.apache.zest.sample.dcicargo.sample_a.infrastructure.wicket.tabs.TabsPanel;

/**
 * DCI Sample application instance
 *
 * A Wicket application backed by Zest.
 */
public class DCISampleApplication_a
    extends WicketZestApplication
{
    public void wicketInit()
    {
        // Tabs and SEO urls.
        mountPages();

        // Show/hide Ajax debugging.
        getDebugSettings().setDevelopmentUtilitiesEnabled( true );

        // Check that components are stateless when required.
        getComponentPostOnBeforeRenderListeners().add( new StatelessChecker() );

        // Show/hide wicket tags in html code.
        getMarkupSettings().setStripWicketTags( true );

        // Default date format (we don't care for now about the hour of the day)
        ( (ConverterLocator) getConverterLocator() ).set( java.util.Date.class,
                                                          new PatternDateConverter( "yyyy-MM-dd", true ) );
    }

    private void mountPages()
    {
        TabsPanel.registerTab( this, CargoListPage.class, "booking", "Booking and Routing" );
        TabsPanel.registerTab( this, TrackCargoPage.class, "tracking", "Tracking" );
        TabsPanel.registerTab( this, RegisterHandlingEventPage.class, "handling", "Handling" );

        mountPage( "/booking", CargoListPage.class );
        mountPage( "/booking/book-new-cargo", BookNewCargoPage.class );
        mountPage( "/booking/cargo", CargoDetailsPage.class );
        mountPage( "/booking/change-destination", ChangeDestinationPage.class );
        mountPage( "/booking/route-cargo", RouteCargoPage.class );

        mountPage( "/handling", RegisterHandlingEventPage.class );
        mountPage( "/tracking", TrackCargoPage.class );
    }

    public Class<? extends Page> getHomePage()
    {
        return CargoListPage.class;
    }
}