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

package org.apache.zest.library.eventsourcing.domain.source.helper;

import org.apache.zest.api.configuration.Configuration;
import org.apache.zest.io.Output;
import org.apache.zest.io.Transforms;
import org.apache.zest.library.eventsourcing.domain.api.UnitOfWorkDomainEventsValue;
import org.apache.zest.library.eventsourcing.domain.source.EventSource;
import org.apache.zest.library.eventsourcing.domain.source.EventStream;
import org.apache.zest.library.eventsourcing.domain.source.UnitOfWorkEventsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper that enables a service to easily track transactions.
 * <p>
 * Upon startup
 * the tracker will get all the transactions from the store since the last
 * check, and delegate them to the given Output. It will also register itself
 * with the store so that it can get continuous updates.
 * </p>
 * <p>
 * Then, as transactions come in from the store, they will be processed in real-time.
 * If a transaction is successfully handled the configuration of the service, which must
 * extend DomainEventTrackerConfiguration, will update the marker for the last successfully handled transaction.
 * </p>
 */
public class DomainEventTracker
        implements Runnable, UnitOfWorkEventsListener
{
    private Configuration<? extends DomainEventTrackerConfiguration> configuration;
    private final Output<UnitOfWorkDomainEventsValue, ? extends Throwable> output;
    private EventStream stream;
    private EventSource source;
    private boolean started = false;
    private Logger logger;

    public DomainEventTracker( EventStream stream, EventSource source,
                               Configuration<? extends DomainEventTrackerConfiguration> configuration,
                               Output<UnitOfWorkDomainEventsValue, ? extends Throwable> output )
    {
        this.stream = stream;
        this.configuration = configuration;
        this.output = output;
        this.source = source;

        logger = LoggerFactory.getLogger( configuration.get().identity().get() );
    }

    public synchronized void start()
    {
        if (!started)
        {
            started = true;

            run();

            stream.registerListener( this );
        }
    }

    public synchronized void stop()
    {
        if (started)
        {
            started = false;
            stream.unregisterListener( this );
        }
    }

    @Override
    public synchronized void run()
    {
        // TODO This should optionally use a CircuitBreaker
        if (started && configuration.get().enabled().get())
        {
            Transforms.Counter counter = new Transforms.Counter();
            try
            {
                long currentOffset = configuration.get().lastOffset().get();
                source.events( currentOffset, Long.MAX_VALUE ).transferTo( Transforms.map( counter, output ) );

                // Save new offset, to be used in next round
                configuration.get().lastOffset().set( currentOffset+counter.count() );
                configuration.save();
            } catch (Throwable throwable)
            {
                logger.warn( "Event handling failed", throwable );
            }
        }
    }

    @Override
    public void notifyTransactions( Iterable<UnitOfWorkDomainEventsValue> transactions )
    {
        run();
    }
}
