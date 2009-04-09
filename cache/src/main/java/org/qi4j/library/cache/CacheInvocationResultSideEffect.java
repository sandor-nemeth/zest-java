package org.qi4j.library.cache;

import org.qi4j.api.common.AppliesTo;
import org.qi4j.api.injection.scope.Invocation;
import org.qi4j.api.injection.scope.This;
import org.qi4j.api.sideeffect.GenericSideEffect;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Cache result of @Cached method calls.
 */
@AppliesTo( Cached.class )
public class CacheInvocationResultSideEffect extends GenericSideEffect
{
    @This private InvocationCache cache;
    @Invocation private Method method;

    public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable
    {
        // Get value
        // if an exception is thrown, don't do anything
        Object result = next.invoke( proxy, method, args );
        if( result == null )
        {
            result = Void.TYPE;
        }

        String cacheName = method.getName();
        if( args != null )
        {
            cacheName += Arrays.asList( args );
        }
        Object oldResult = cache.getCachedValue( cacheName );
        if( oldResult == null || !oldResult.equals( result ) )
        {
            cache.setCachedValue( cacheName, result );
        }
        return result;
    }
}
