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

package org.apache.zest.spi.service.importer;

import org.junit.Test;
import org.apache.zest.api.common.Visibility;
import org.apache.zest.api.injection.scope.Service;
import org.apache.zest.bootstrap.AssemblyException;
import org.apache.zest.bootstrap.ModuleAssembly;
import org.apache.zest.test.AbstractZestTest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test import of singleton services
 */
public class InstanceImporterTest
    extends AbstractZestTest
{
    @Service
    TestInterface service;

    public void assemble( ModuleAssembly module )
        throws AssemblyException
    {
        ModuleAssembly serviceModule = module.layer().module( "Service module" );
        serviceModule.importedServices( TestInterface.class )
            .setMetaInfo( new TestService() )
            .visibleIn( Visibility.layer );
        module.objects( InstanceImporterTest.class );
    }

    @Test
    public void givenSingletonServiceObjectWhenServicesAreInjectedThenSingletonIsFound()
    {
        assertThat( "service is injected properly", service.helloWorld(), equalTo( "Hello World" ) );
    }

    public interface TestInterface
    {
        public String helloWorld();
    }

    public static class TestService
        implements TestInterface
    {
        public String helloWorld()
        {
            return "Hello World";
        }
    }
}
