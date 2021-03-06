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

package org.apache.zest.library.eventsourcing.application.api;

import java.time.Instant;
import org.apache.zest.api.entity.Identity;
import org.apache.zest.api.property.Property;
import org.apache.zest.api.value.ValueComposite;

/**
 * Representation of an application-event.
 * <p>
 * An application event is triggered by calling a method
 * that is of the form:
 * </p>
 * <pre><code>
 * void someName(ApplicationEvent event, SomeParam param);
 * </code></pre>
 * <p>
 * The "event" argument should be invoked with null, as it will be created during
 * the method call. If it is not null, then the method call is a replay of previously
 * created events.
 * </p>
 */
public interface ApplicationEvent
        extends ValueComposite, Identity
{
    // Usecase
    Property<String> usecase();

    // Name of method/event
    Property<String> name();

    // When the event was created
    Property<Instant> on();

    // Method parameters as JSON
    Property<String> parameters();

    // Version of the application that created this event
    Property<String> version();
}
