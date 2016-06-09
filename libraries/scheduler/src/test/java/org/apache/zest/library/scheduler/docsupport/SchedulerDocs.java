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
package org.apache.zest.library.scheduler.docsupport;

import org.apache.zest.api.association.Association;
import org.apache.zest.api.injection.scope.Service;
import org.apache.zest.api.injection.scope.This;
import org.apache.zest.api.property.Property;
import org.apache.zest.api.unitofwork.concern.UnitOfWorkDiscardOn;
import org.apache.zest.api.unitofwork.concern.UnitOfWorkPropagation;
import org.apache.zest.api.unitofwork.concern.UnitOfWorkRetry;
import org.apache.zest.library.scheduler.Scheduler;
import org.apache.zest.library.scheduler.Task;
import org.apache.zest.library.scheduler.Schedule;
import org.apache.zest.library.scheduler.timeline.Timeline;

public class SchedulerDocs
{

    // START SNIPPET: timeline
    @Service
    Timeline timeline;
// END SNIPPET: timeline

    // START SNIPPET: 2
    @Service
    Scheduler scheduler;

    public void method()
    {
        MyTaskEntity myTask = todo();
        Schedule schedule = scheduler.scheduleOnce( myTask, 10 );
        // myTask will be run in 10 seconds from now
    }

    // END SNIPPET: 2
    MyTaskEntity todo()
    {
        return null;
    }

    // START SNIPPET: 1
    interface MyTaskEntity extends Task
    {
        Property<String> myTaskState();

        Association<AnotherEntity> anotherEntity();
    }

    class MyTaskMixin implements Runnable
    {
        @This
        MyTaskEntity me;

        @Override
        public void run()
        {
            me.myTaskState().set( me.anotherEntity().get().doSomeStuff( me.myTaskState().get() ) );
        }
    }

    // END SNIPPET: 1
    interface AnotherEntity
    {
        String doSomeStuff( String p );
    }

    public class MyTask implements Runnable
    {

        // START SNIPPET: strategy
        @Override
        @UnitOfWorkRetry( retries = 3 )
        @UnitOfWorkDiscardOn( IllegalArgumentException.class )
        @UnitOfWorkPropagation( value = UnitOfWorkPropagation.Propagation.REQUIRES_NEW, usecase = "Load Data" )
        public void run()
        {
            // END SNIPPET: strategy

        }
    }
}