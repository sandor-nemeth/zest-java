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
package org.apache.zest.sample.dcicargo.sample_b.context.interaction.handling.parsing;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.apache.zest.api.common.Optional;
import org.apache.zest.api.injection.scope.Structure;
import org.apache.zest.api.mixin.Mixins;
import org.apache.zest.api.service.ServiceComposite;
import org.apache.zest.api.value.ValueBuilder;
import org.apache.zest.api.value.ValueBuilderFactory;
import org.apache.zest.library.constraints.annotation.NotEmpty;
import org.apache.zest.sample.dcicargo.sample_b.context.interaction.handling.parsing.dto.ParsedHandlingEventData;
import org.apache.zest.sample.dcicargo.sample_b.context.interaction.handling.parsing.exception.InvalidHandlingEventDataException;
import org.apache.zest.sample.dcicargo.sample_b.data.structure.handling.HandlingEventType;

/**
 * Parse Handling Event Data  (subfunction use case)
 *
 * First step in the ProcessHandlingEvent use case.
 *
 * Since no Data objects are playing a Role in a Context, it's implemented as a Service
 * instead of a Context. In that respect it doesn't have much to do with DCI, but it shares
 * the intend to implement a use case and we therefore have it in the context package that
 * is then given the broader semantics of the English word "context".
 *
 * Could be implemented as a web service endpoint like HandlingReportServiceImpl,
 * a file upload solution like UploadDirectoryScanner in the DDD sample - or some other
 * technical solution.
 */
@Mixins( ParseHandlingEventData.Mixin.class )
public interface ParseHandlingEventData
    extends ServiceComposite
{
    // Step 1 - Receive handling event data for a handled cargo
    // Step 2 - Verify that data is complete (with annotated constraints)

    public ParsedHandlingEventData parse( @NotEmpty String completionStr,
                                          @NotEmpty String trackingIdStr,
                                          @NotEmpty String handlingEventTypeStr,
                                          @NotEmpty String unLocodeStr,
                                          @Optional String voyageNumberStr
    )
        throws InvalidHandlingEventDataException;

    abstract class Mixin
        implements ParseHandlingEventData
    {
        @Structure
        ValueBuilderFactory vbf;

        static final String ISO_8601_FORMAT = "yyyy-MM-dd HH:mm";

        LocalDate completionDate;
        HandlingEventType handlingEventType;

        public ParsedHandlingEventData parse( String completionStr,
                                              String trackingIdStr,
                                              String handlingEventTypeStr,
                                              String unLocodeStr,
                                              String voyageNumberStr
        )
            throws InvalidHandlingEventDataException
        {
            // Step 3 - Perform basic type conversion

            try
            {
                completionDate = LocalDate.parse( completionStr.trim(), DateTimeFormatter.ISO_LOCAL_DATE );
            }
            catch( DateTimeParseException e )
            {
                throw new InvalidHandlingEventDataException(
                    "Invalid date format: '" + completionStr + "' must be on ISO 8601 format " + ISO_8601_FORMAT );
            }

            try
            {
                handlingEventType = HandlingEventType.valueOf( handlingEventTypeStr.trim() );
            }
            catch( Exception e )
            {
                throw new InvalidHandlingEventDataException( e.getMessage() );
            }

            // Step 4 - Collect parsed handling event data

            ValueBuilder<ParsedHandlingEventData> parsedData = vbf.newValueBuilder( ParsedHandlingEventData.class );
            parsedData.prototype().registrationDate().set( LocalDate.now() );
            parsedData.prototype().completionDate().set( completionDate );
            parsedData.prototype().trackingIdString().set( trackingIdStr );
            parsedData.prototype().handlingEventType().set( handlingEventType );
            parsedData.prototype().unLocodeString().set( unLocodeStr );
            parsedData.prototype().voyageNumberString().set( voyageNumberStr );

            // Step 5 - Return parsed handling event data

            return parsedData.newInstance();
        }
    }
}
