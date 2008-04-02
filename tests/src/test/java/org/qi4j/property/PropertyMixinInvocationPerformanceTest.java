/*
 * Copyright (c) 2008, Rickard Öberg. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.qi4j.property;

import java.text.NumberFormat;
import org.junit.Test;
import org.qi4j.bootstrap.AssemblyException;
import org.qi4j.bootstrap.ModuleAssembly;
import org.qi4j.composite.Composite;
import org.qi4j.composite.CompositeBuilder;
import org.qi4j.composite.Mixins;
import org.qi4j.composite.scope.PropertyField;
import org.qi4j.library.framework.entity.PropertyMixin;
import org.qi4j.test.AbstractQi4jTest;

/**
 * PropertyMixin invocation performance test. Don't forget to add VM value "-server"
 * before running this test!
 */
public class PropertyMixinInvocationPerformanceTest
    extends AbstractQi4jTest
{
    public void assemble( ModuleAssembly module )
        throws AssemblyException
    {
        module.addComposites( SimpleComposite.class );
        module.addComposites( SimpleComposite2.class );
    }

    @Test
    public void testNewInstance()
    {
        {
            CompositeBuilder<SimpleComposite> builder = compositeBuilderFactory.newCompositeBuilder( SimpleComposite.class );
            SimpleComposite simple = builder.newInstance();

            int rounds = 1;
            for( int i = 0; i < rounds; i++ )
            {
                performanceCheck( simple );
            }
        }

        {
            CompositeBuilder<SimpleComposite> builder = compositeBuilderFactory.newCompositeBuilder( SimpleComposite.class );
            SimpleComposite simple = builder.newInstance();

            int rounds = 1;
            for( int i = 0; i < rounds; i++ )
            {
                performanceCheck( simple );
            }
        }
    }

    private void performanceCheck( SimpleComposite simple )
    {
        long count = 10000000L;

        {
            long start = System.currentTimeMillis();
            for( long i = 0; i < count; i++ )
            {
                simple.test();
            }
            long end = System.currentTimeMillis();
            long time = end - start;
            long callsPerSecond = ( count / time ) * 1000;
            System.out.println( "Accesses per second: " + NumberFormat.getIntegerInstance().format( callsPerSecond ) );
        }

        {
            long start = System.currentTimeMillis();
            for( long i = 0; i < count; i++ )
            {
                simple.test().get();
            }
            long end = System.currentTimeMillis();
            long time = end - start;
            long callsPerSecond = ( count / time ) * 1000;
            System.out.println( "Gets per second: " + NumberFormat.getIntegerInstance().format( callsPerSecond ) );
        }
    }

    @Mixins( PropertyMixin.class )
    public interface SimpleComposite
        extends Composite
    {
        public Property<String> test();
    }

    @Mixins( SimpleMixin.class )
    public interface SimpleComposite2
        extends SimpleComposite
    {
    }

    public abstract static class SimpleMixin
        implements SimpleComposite2
    {
        @PropertyField Property<String> test;

        public Property<String> test()
        {
            return test;
        }
    }
}