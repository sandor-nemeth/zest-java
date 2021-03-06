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
package org.apache.zest.bootstrap;

import org.apache.zest.bootstrap.unitofwork.DefaultUnitOfWorkAssembler;
import org.junit.Assert;
import org.junit.Test;
import org.apache.zest.api.activation.ActivationException;
import org.apache.zest.bootstrap.somepackage.Test2Value;
import org.apache.zest.functional.Iterables;

import static org.apache.zest.bootstrap.ClassScanner.findClasses;
import static org.apache.zest.bootstrap.ClassScanner.matches;
import static org.apache.zest.functional.Iterables.filter;

/**
 * Test and showcase of the ClassScanner assembly utility.
 */
public class ClassScannerTest
{
    @Test
    public void testClassScannerFiles()
        throws ActivationException, AssemblyException
    {
        SingletonAssembler singleton = new SingletonAssembler()
        {
            @Override
            public void assemble( ModuleAssembly module )
                throws AssemblyException
            {
                // Find all classes starting from TestValue, but include only the ones that are named *Value

                for( Class aClass : filter( matches( ".*Value" ), findClasses( TestValue.class ) ) )
                {
                    module.values( aClass );
                    new DefaultUnitOfWorkAssembler().assemble( module );
                }
            }
        };

        singleton.module().newValueBuilder( TestValue.class );
        singleton.module().newValueBuilder( Test2Value.class );
    }

    @Test
    public void testClassScannerJar()
    {
        Assert.assertEquals( 185, Iterables.count( findClasses( Test.class ) ) );
    }
}
