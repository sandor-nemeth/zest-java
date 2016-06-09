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
package org.apache.zest.sample.dcicargo.sample_b.context.interaction.handling.parsing.dto;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.zest.api.common.Optional;
import org.apache.zest.api.mixin.Mixins;
import org.apache.zest.api.property.Immutable;
import org.apache.zest.api.property.Property;
import org.apache.zest.sample.dcicargo.sample_b.data.structure.handling.HandlingEventType;
import org.apache.zest.sample.dcicargo.sample_b.infrastructure.conversion.DTO;

/**
 * The ParsedHandlingEventData simply helps move submitted event registration data around.
 */
@Immutable
@Mixins( ParsedHandlingEventData.Mixin.class )
public interface ParsedHandlingEventData extends DTO
{
    Property<Date> registrationTime();

    Property<Date> completionTime();

    Property<String> trackingIdString();

    Property<HandlingEventType> handlingEventType();

    Property<String> unLocodeString();

    @Optional
    Property<String> voyageNumberString();

    public String print();

    abstract class Mixin
        implements ParsedHandlingEventData
    {
        public String print()
        {
            String voyage = "";
            if( voyageNumberString().get() != null )
            {
                voyage = voyageNumberString().get();
            }

            SimpleDateFormat date = new SimpleDateFormat( "yyyy-MM-dd" );

            StringBuilder builder = new StringBuilder( "\nPARSED HANDLING EVENT DATA -----------------" ).
                append( "\n  Tracking id string           " ).append( trackingIdString().get() ).
                append( "\n  Handling Event Type string   " ).append( handlingEventType().get().name() ).
                append( "\n  UnLocode string              " ).append( unLocodeString().get() ).
                append( "\n  Completed string             " ).append( date.format( completionTime().get() ) ).
                append( "\n  Registered string            " ).append( date.format( registrationTime().get() ) ).
                append( "\n  Voyage string                " ).append( voyage ).
                append( "\n--------------------------------------------\n" );

            return builder.toString();
        }
    }
}