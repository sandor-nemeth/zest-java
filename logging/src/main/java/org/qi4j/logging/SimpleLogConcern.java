/*
 * Copyright 2006 Niclas Hedhman.
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
package org.qi4j.logging;

import org.qi4j.composite.Composite;
import org.qi4j.composite.scope.Service;
import org.qi4j.composite.scope.ThisCompositeAs;
import org.qi4j.logging.logtypes.ErrorType;
import org.qi4j.logging.logtypes.InfoType;
import org.qi4j.logging.logtypes.WarningType;

public final class SimpleLogConcern
    implements SimpleLog
{
    @Service private LogService logService;
    @ThisCompositeAs Composite composite;

    public void info( String message )
    {
        logService.log( InfoType.INSTANCE, composite.dereference(), "", message );
    }

    public void info( String message, Object param1 )
    {
        logService.log( InfoType.INSTANCE, composite.dereference(), "", message, param1 );
    }

    public void info( String message, Object param1, Object param2 )
    {
        logService.log( InfoType.INSTANCE, composite.dereference(), "", message, param1, param2 );
    }

    public void info( String message, Object... params )
    {
        logService.log( InfoType.INSTANCE, composite.dereference(), "", message, params );
    }

    public void warning( String message )
    {
        logService.log( WarningType.INSTANCE, composite.dereference(), "", message );
    }

    public void warning( String message, Object param1 )
    {
        logService.log( WarningType.INSTANCE, composite.dereference(), "", message, param1 );
    }

    public void warning( String message, Object param1, Object param2 )
    {
        logService.log( WarningType.INSTANCE, composite.dereference(), "", message, param1, param2 );
    }

    public void warning( String message, Object... params )
    {
        logService.log( WarningType.INSTANCE, composite.dereference(), "", message, params );
    }

    public void error( String message )
    {
        logService.log( ErrorType.INSTANCE, composite.dereference(), "", message );
    }

    public void error( String message, Object param1 )
    {
        logService.log( ErrorType.INSTANCE, composite.dereference(), "", message, param1 );
    }

    public void error( String message, Object param1, Object param2 )
    {
        logService.log( ErrorType.INSTANCE, composite.dereference(), "", message, param1, param2 );
    }

    public void error( String message, Object... params )
    {
        logService.log( ErrorType.INSTANCE, composite.dereference(), "", message, params );
    }
}
