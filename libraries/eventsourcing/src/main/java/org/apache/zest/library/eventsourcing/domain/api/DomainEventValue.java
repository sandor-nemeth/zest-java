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

package org.apache.zest.library.eventsourcing.domain.api;

import org.apache.zest.api.property.Property;
import org.apache.zest.api.value.ValueComposite;

/**
 * Representation of a domain-event.
 * <p>An event is triggered by calling a method that is of the form:
 * </p>
 * <pre><code>
 * &#64;DomainEvent
 * void someName(SomeParam param, AnotherParam param2);
 * </code></pre>
 *
 */
public interface DomainEventValue
        extends ValueComposite
{
    // Type of the entity being invoked
    Property<String> entityType();

    // Id of the entity that generated the event
    Property<String> entityId();

    // Name of method/event
    Property<String> name();

    // Method parameters as JSON
    Property<String> parameters();
}
