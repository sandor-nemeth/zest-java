package org.qi4j.runtime.injection.provider;

import java.io.Serializable;
import org.qi4j.api.entity.association.AbstractAssociation;
import org.qi4j.api.entity.association.EntityStateHolder;
import org.qi4j.api.injection.scope.State;
import org.qi4j.api.property.Property;
import org.qi4j.api.property.StateHolder;
import org.qi4j.runtime.composite.Resolution;
import org.qi4j.runtime.injection.DependencyModel;
import org.qi4j.runtime.injection.InjectionContext;
import org.qi4j.runtime.injection.InjectionProvider;
import org.qi4j.runtime.injection.InjectionProviderFactory;
import org.qi4j.spi.composite.StateDescriptor;
import org.qi4j.spi.composite.CompositeDescriptor;
import org.qi4j.spi.entity.EntityStateDescriptor;
import org.qi4j.spi.entity.EntityDescriptor;
import org.qi4j.spi.entity.association.AssociationDescriptor;
import org.qi4j.spi.property.PropertyDescriptor;

/**
 * JAVADOC
 */
public final class StateInjectionProviderFactory
    implements InjectionProviderFactory, Serializable
{
    public InjectionProvider newInjectionProvider( Resolution resolution, DependencyModel dependencyModel )
        throws InvalidInjectionException
    {
        if( StateHolder.class.isAssignableFrom( dependencyModel.rawInjectionType()))
        {
            // @State StateHolder properties;
            return new StateInjectionProvider();
        }
        else if( Property.class.isAssignableFrom( dependencyModel.rawInjectionType() ) )
        {
            // @State Property<String> name;
            StateDescriptor descriptor = ((CompositeDescriptor)resolution.object()).state();
            State annotation = (State) dependencyModel.injectionAnnotation();
            String name;
            if( annotation.value().equals( "" ) )
            {
                name = resolution.field().getName();
            }
            else
            {
                name = annotation.value();
            }

            PropertyDescriptor propertyDescriptor = descriptor.getPropertyByName( name );

            // Check if property exists
            if( propertyDescriptor == null )
            {
                return null;
            }

            return new PropertyInjectionProvider( propertyDescriptor );
        }
        else if( AbstractAssociation.class.isAssignableFrom( dependencyModel.rawInjectionType() ) )
        {
            // @State Association<MyEntity> name;
            EntityStateDescriptor descriptor = ((EntityDescriptor) resolution.object()).state();
            State annotation = (State) dependencyModel.injectionAnnotation();
            String name;
            if( annotation.value().equals( "" ) )
            {
                name = resolution.field().getName();
            }
            else
            {
                name = annotation.value();
            }
            AssociationDescriptor model = descriptor.getAssociationByName( name );

            // No such association found
            if( model == null )
            {
                return null;
            }

            return new AssociationInjectionProvider( model );
        }

        throw new InjectionProviderException( "Injected value has invalid type" );
    }

    private class PropertyInjectionProvider
        implements InjectionProvider, Serializable
    {
        private final PropertyDescriptor propertyDescriptor;

        public PropertyInjectionProvider( PropertyDescriptor propertyDescriptor )
        {
            this.propertyDescriptor = propertyDescriptor;
        }

        public Object provideInjection( InjectionContext context ) throws InjectionProviderException
        {
            Property value = context.state().getProperty( propertyDescriptor.accessor() );
            if( value != null )
            {
                return value;
            }
            else
            {
                throw new InjectionProviderException( "Non-optional property " + propertyDescriptor + " had no value" );
            }
        }
    }

    private class AssociationInjectionProvider
        implements InjectionProvider, Serializable
    {
        private final AssociationDescriptor associationDescriptor;

        public AssociationInjectionProvider( AssociationDescriptor associationDescriptor )
        {
            this.associationDescriptor = associationDescriptor;
        }

        public Object provideInjection( InjectionContext context ) throws InjectionProviderException
        {
            AbstractAssociation abstractAssociation = ((EntityStateHolder) context.state()).getAssociation( associationDescriptor.accessor() );
            if( abstractAssociation != null )
            {
                return abstractAssociation;
            }
            else
            {
                throw new InjectionProviderException( "Non-optional association " + associationDescriptor.qualifiedName() + " had no association" );
            }
        }
    }

    private class StateInjectionProvider
        implements InjectionProvider, Serializable
    {
        public Object provideInjection( InjectionContext context ) throws InjectionProviderException
        {
            return context.state();
        }
    }
}