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
package org.apache.zest.runtime.constraints;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.Test;
import org.apache.zest.api.common.Optional;
import org.apache.zest.api.composite.TransientComposite;
import org.apache.zest.api.constraint.Constraint;
import org.apache.zest.api.constraint.ConstraintDeclaration;
import org.apache.zest.api.constraint.ConstraintViolation;
import org.apache.zest.api.constraint.ConstraintViolationException;
import org.apache.zest.api.constraint.Constraints;
import org.apache.zest.api.constraint.Name;
import org.apache.zest.api.mixin.Mixins;
import org.apache.zest.bootstrap.AssemblyException;
import org.apache.zest.bootstrap.ModuleAssembly;
import org.apache.zest.test.AbstractZestTest;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ConstraintsTest
    extends AbstractZestTest
{
    public void assemble( ModuleAssembly module )
        throws AssemblyException
    {
        module.transients( MyOneComposite.class );
        module.transients( MyOneComposite2.class );
    }

    @Test
    public void givenCompositeWithConstraintsWhenInstantiatedThenUseDeclarationOnComposite()
        throws Throwable
    {
        MyOne my = transientBuilderFactory.newTransient( MyOneComposite.class );
        ArrayList<String> list = new ArrayList<String>();
        list.add( "zout" );
        my.doSomething( "habba", list );
        try
        {
            my.doSomething( "niclas", new ArrayList<String>() );
            fail( "Should have thrown a ConstraintViolationException." );
        }
        catch( ConstraintViolationException e )
        {
            Collection<ConstraintViolation> violations = e.constraintViolations();
            assertEquals( 2, violations.size() );
//            assertEquals( MyOne.class.getName(), e.mixinTypeName() );
        }
    }

    @Test
    public void givenCompositeWithoutConstraintsWhenInstantiatedThenUseDeclarationOnConstraint()
        throws Throwable
    {
        MyOne my = transientBuilderFactory.newTransient( MyOneComposite2.class );
        ArrayList<String> list = new ArrayList<String>();
        list.add( "zout" );
        my.doSomething( "habba", list );
        try
        {
            my.doSomething( "niclas", new ArrayList<String>() );
            fail( "Should have thrown a ConstraintViolationException." );
        }
        catch( ConstraintViolationException e )
        {
            Collection<ConstraintViolation> violations = e.constraintViolations();
            assertEquals( 2, violations.size() );
//            assertEquals( MyOne.class.getName(), e.mixinTypeName() );
        }
    }

    @Test
    public void givenConstrainedGenericWildcardParameterWhenInvokedThenUseConstraint()
    {
        MyOne myOne = transientBuilderFactory.newTransient( MyOneComposite.class );
        ArrayList<String> list = new ArrayList<String>();
        list.add( "Foo" );
        myOne.doSomething2( list );
    }

    @Test
    public void givenCompositeConstraintWhenInvokedThenUseAllConstraints()
    {
        MyOne myOne = transientBuilderFactory.newTransient( MyOneComposite.class );
        ArrayList<String> list = new ArrayList<String>();
        list.add( "Foo" );
        myOne.doSomething3( list );
    }

    @Constraints( TestConstraintImpl.class )
    @Mixins( MyOneMixin.class )
    public interface MyOneComposite
        extends MyOne, TransientComposite
    {
    }

    @Mixins( MyOneMixin.class )
    public interface MyOneComposite2
        extends MyOne, TransientComposite
    {
    }

    public interface MyOne
    {
        void doSomething( @Optional @TestConstraint String abc, @TestConstraint List<String> collection );

        void doSomething2( @TestConstraint @NonEmptyCollection List<?> collection );

        void doSomething3( @CompositeConstraint @Name( "somecollection" ) List<?> collection );
    }

    public abstract static class MyOneMixin
        implements MyOne
    {
        public void doSomething( String abc, List<String> collection )
        {
            if( abc == null || collection == null )
            {
                throw new NullPointerException();
            }
        }

        public void doSomething2( List<?> collection )
        {
            if( collection == null )
            {
                throw new NullPointerException();
            }
        }

        public void doSomething3( List<?> collection )
        {
            if( collection == null )
            {
                throw new NullPointerException();
            }
        }
    }

    @ConstraintDeclaration
    @Retention( RUNTIME )
    @Constraints( TestConstraintImpl.class )
    public @interface TestConstraint
    {
    }

    public static class TestConstraintImpl
        implements Constraint<TestConstraint, Object>
    {
        public boolean isValid( TestConstraint annotation, Object value )
            throws NullPointerException
        {
            if( value instanceof String )
            {
                return ( (String) value ).startsWith( "habba" );
            }
            return value instanceof Collection && ( (Collection) value ).size() > 0;
        }
    }

    @ConstraintDeclaration
    @Retention( RUNTIME )
    @Constraints( { NonEmptyCollectionConstraint.class } )
    public @interface NonEmptyCollection
    {
    }

    public static class NonEmptyCollectionConstraint
        implements Constraint<NonEmptyCollection, Collection<?>>
    {
        public boolean isValid( NonEmptyCollection annotation, Collection<?> value )
            throws NullPointerException
        {
            return value.size() > 0;
        }
    }

    @ConstraintDeclaration
    @Retention( RUNTIME )
    @TestConstraint
    @NonEmptyCollection
    public @interface CompositeConstraint
    {
    }
}
