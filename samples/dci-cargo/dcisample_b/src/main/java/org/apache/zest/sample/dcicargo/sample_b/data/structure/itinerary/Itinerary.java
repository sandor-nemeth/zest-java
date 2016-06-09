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
package org.apache.zest.sample.dcicargo.sample_b.data.structure.itinerary;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.apache.zest.api.mixin.Mixins;
import org.apache.zest.api.property.Property;
import org.apache.zest.api.value.ValueComposite;
import org.apache.zest.library.constraints.annotation.NotEmpty;

/**
 * Itinerary
 *
 * Describes a planned route for a Cargo.
 *
 * The Itinerary has a list of Legs each describing expected loading onto/ unloading from
 * voyages at different locations. The list of legs is mandatory, immutable and can't be empty.
 */
@Mixins( Itinerary.Mixin.class )
public interface Itinerary
    extends ValueComposite
{
    @NotEmpty
    Property<List<Leg>> legs();

    // Side-effects free and UI agnostic convenience methods
    Leg firstLeg();

    Leg leg( Integer current );

    Leg lastLeg();

    Date eta();

    int days();

    String print();

    public abstract class Mixin
        implements Itinerary
    {
        public Leg firstLeg()
        {
            return legs().get().get( 0 );
        }

        public Leg leg( Integer index )
        {
            if( index < 0 || index + 1 > legs().get().size() )
            {
                return null;
            }

            return legs().get().get( index );
        }

        public Leg lastLeg()
        {
            return legs().get().get( legs().get().size() - 1 );
        }

        public Date eta()
        {
            return lastLeg().unloadTime().get();
        }

        public int days()
        {
            Date dep = firstLeg().loadTime().get();
            Date arr = lastLeg().unloadTime().get();
            return Days.daysBetween( new LocalDate( dep ), new LocalDate( arr ) ).getDays();
        }

        public String print()
        {
            StringBuilder sb = new StringBuilder( "\nITINERARY -----------------------------------------------------" );
            for( int i = 0; i < legs().get().size(); i++ )
            {
                printLeg( i, sb, legs().get().get( i ) );
            }
            return sb.append( "\n---------------------------------------------------------------\n" ).toString();
        }

        private void printLeg( int i, StringBuilder sb, Leg leg )
        {
            sb.append( "\n  Leg " ).append( i );
            sb.append( "  Load " );
            sb.append( new SimpleDateFormat( "yyyy-MM-dd" ).format( leg.loadTime().get() ) );
            sb.append( " " ).append( leg.loadLocation().get() );
            sb.append( "   " ).append( leg.voyage().get() );
            sb.append( "   Unload " );
            sb.append( new SimpleDateFormat( "yyyy-MM-dd" ).format( leg.unloadTime().get() ) );
            sb.append( " " ).append( leg.unloadLocation().get() );
        }
    }
}