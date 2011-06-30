/*
 * Copyright 2009 Niclas Hedhman.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.qi4j.test.gae;

import org.qi4j.bootstrap.AssemblyException;
import org.qi4j.bootstrap.ModuleAssembly;
import org.qi4j.core.testsupport.AbstractEntityStoreTest;
import org.qi4j.entitystore.gae.GaeEntityStoreService;
import org.qi4j.entitystore.gae.GaeIdGeneratorService;

public class UnitTests extends AbstractEntityStoreTest
{
    public void assemble( ModuleAssembly module )
        throws AssemblyException
    {
        super.assemble( module );
        System.out.println( "Registering GAE services." );
        module.services( GaeEntityStoreService.class, GaeIdGeneratorService.class );
    }
}