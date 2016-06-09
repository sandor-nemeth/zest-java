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

package org.apache.zest.library.scheduler.defaults;

import java.util.concurrent.atomic.AtomicInteger;
import org.apache.zest.api.injection.scope.This;
import org.apache.zest.library.scheduler.internal.Execution;
import org.apache.zest.library.scheduler.SchedulerService;

public class DefaultThreadFactory
    implements java.util.concurrent.ThreadFactory
{
    private static final AtomicInteger POOL_NUMBER = new AtomicInteger( 1 );
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger( 1 );
    private final String namePrefix;

    protected DefaultThreadFactory( @This SchedulerService me )
    {
        SecurityManager sm = System.getSecurityManager();
        group = ( sm != null ) ? sm.getThreadGroup() : Execution.ExecutionMixin.TG;
        namePrefix = me.identity().get() + "-P" + POOL_NUMBER.getAndIncrement() + "W";
    }

    @Override
    public Thread newThread( Runnable runnable )
    {
        Thread thread = new Thread( group, runnable, namePrefix + threadNumber.getAndIncrement(), 0 );
        if( thread.isDaemon() )
        {
            thread.setDaemon( false );
        }
        if( thread.getPriority() != Thread.NORM_PRIORITY )
        {
            thread.setPriority( Thread.NORM_PRIORITY );
        }
        return thread;
    }
}