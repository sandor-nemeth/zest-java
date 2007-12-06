/*
 * Copyright (c) 2007, Rickard �berg. All Rights Reserved.
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

package org.qi4j.spi.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import junit.framework.TestCase;
import org.qi4j.annotation.Concerns;
import org.qi4j.annotation.Mixins;
import org.qi4j.annotation.scope.ConcernFor;
import org.qi4j.bootstrap.AssemblyException;
import org.qi4j.bootstrap.ModuleAssembly;
import org.qi4j.bootstrap.SingletonAssembly;
import org.qi4j.composite.Composite;

/**
 * TODO
 */
public class CompositeSerializationMappingTest
    extends TestCase
{
    public void testCompositeSerializationMapping()
        throws Exception
    {
        // Create "server" application
        SingletonAssembly server = new SingletonAssembly()
        {
            public void configure( ModuleAssembly module ) throws AssemblyException
            {
                module.addComposite( ServerChairComposite.class );
            }
        };

        // Create "client" application
        SingletonAssembly client = new SingletonAssembly()
        {
            public void configure( ModuleAssembly module ) throws AssemblyException
            {
                module.addComposite( ClientChairComposite.class );
            }
        };

        // Create server chair
        ChairComposite serverChair = server.getCompositeBuilderFactory().newCompositeBuilder( ChairComposite.class ).newInstance();

        assertEquals( "Sitting on the server", serverChair.sit() );

        // Serialize it
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        CompositeOutputStream cout = new CompositeOutputStream( bout, server.getRuntime() );
        cout.writeObject( serverChair );

        // Deserialize it
        ByteArrayInputStream bin = new ByteArrayInputStream( bout.toByteArray() );
        CompositeInputStream cin = new CompositeInputStream( bin, null, client.getCompositeBuilderFactory(), client.getRuntime() );
        ClientChairComposite clientChair = (ClientChairComposite) cin.readObject();

        assertEquals( "Sitting on the client", clientChair.sit() );
        assertEquals( 2, clientChair.getSitCount() ); // Ensure that serialized state is ok
    }


    @Mixins( ChairMixin.class )
    public interface ChairComposite
        extends Chair, Composite
    {
    }

    public interface Chair
    {
        String sit();

        int getSitCount();
    }

    @Concerns( ServerSitting.class )
    public interface ServerChairComposite
        extends ChairComposite
    {
    }

    public static abstract class ServerSitting
        implements Chair
    {
        @ConcernFor Chair next;

        public String sit()
        {
            return next.sit() + " on the server";
        }
    }

    public static abstract class ClientSitting
        implements Chair
    {
        @ConcernFor Chair next;

        public String sit()
        {
            return next.sit() + " on the client";
        }
    }

    @Concerns( ClientSitting.class )
    public interface ClientChairComposite
        extends ChairComposite
    {
    }

    public static class ChairMixin
        implements Chair, Serializable
    {
        int sitCount;

        public String sit()
        {
            sitCount++;
            return "Sitting";
        }

        public int getSitCount()
        {
            return sitCount;
        }
    }
}
