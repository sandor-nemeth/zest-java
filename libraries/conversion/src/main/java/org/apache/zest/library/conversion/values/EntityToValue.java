/*
 * Copyright 2010-2012 Niclas Hedhman.
 * Copyright 2011 Rickard Öberg.
 * Copyright 2013-2015 Paul Merlin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.zest.library.conversion.values;

import java.lang.reflect.AccessibleObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.zest.api.association.Association;
import org.apache.zest.api.association.AssociationDescriptor;
import org.apache.zest.api.association.AssociationStateDescriptor;
import org.apache.zest.api.association.AssociationStateHolder;
import org.apache.zest.api.association.ManyAssociation;
import org.apache.zest.api.association.NamedAssociation;
import org.apache.zest.api.entity.EntityComposite;
import org.apache.zest.api.entity.EntityDescriptor;
import org.apache.zest.api.entity.EntityReference;
import org.apache.zest.api.entity.Identity;
import org.apache.zest.api.injection.scope.Structure;
import org.apache.zest.api.mixin.Mixins;
import org.apache.zest.api.property.PropertyDescriptor;
import org.apache.zest.api.structure.Module;
import org.apache.zest.api.value.NoSuchValueException;
import org.apache.zest.api.value.ValueBuilder;
import org.apache.zest.api.value.ValueDescriptor;
import org.apache.zest.functional.Iterables;
import org.apache.zest.spi.ZestSPI;

import static org.apache.zest.library.conversion.values.Shared.STRING_COLLECTION_TYPE_SPEC;
import static org.apache.zest.library.conversion.values.Shared.STRING_MAP_TYPE_SPEC;
import static org.apache.zest.library.conversion.values.Shared.STRING_TYPE_SPEC;

/**
 * @deprecated Please use {@link org.apache.zest.api.unitofwork.UnitOfWork#toValue(Class, Identity)} instead.
 */
@Mixins( EntityToValue.EntityToValueMixin.class )
public interface EntityToValue
{
    /**
     * Convert an entity to a value.
     *
     * @param <T> parametrized type of the value
     * @param valueType type of the value
     * @param entity the entity to convert to a value
     * @return the resulting value
     */
    <T> T convert( Class<T> valueType, Object entity );

    /**
     * Convert an entity to a value with an opportunity to customize its prototype.
     *
     * @param <T> parametrized type of the value
     * @param valueType type of the value
     * @param entity the entity to convert to a value
     * @param prototypeOpportunity a Function that will be mapped on the value prototype before instanciantion
     * @return the resulting value
     */
    <T> T convert( Class<T> valueType, Object entity, Function<T, T> prototypeOpportunity );

    /**
     * Convert an iterable of entities to an iterable of values.
     *
     * @param <T> parametrized type of the value
     * @param valueType type of the value
     * @param entities the entities to convert to values
     * @return the resulting values
     */
    <T> Iterable<T> convert( Class<T> valueType, Iterable<Object> entities );

    /**
     * Convert an iterable of entities to an iterable of values with an opportunity to customize their prototypes.
     *
     * @param <T> parametrized type of the value
     * @param valueType type of the value
     * @param entities the entities to convert to values
     * @param prototypeOpportunity a Function that will be mapped on each of the value prototypes before instanciation.
     * @return the resulting values
     */
    <T> Iterable<T> convert( Class<T> valueType, Iterable<Object> entities, Function<T, T> prototypeOpportunity );

    static class EntityToValueMixin
        implements EntityToValue
    {

        @Structure
        private ZestSPI spi;
        @Structure
        private Module module;

        @Override
        public <T> T convert( final Class<T> valueType, Object entity )
        {
            return createInstance( doConversion( valueType, entity ) );
        }

        @Override
        @SuppressWarnings( "unchecked" )
        public <T> T convert( final Class<T> valueType, Object entity, Function<T, T> prototypeOpportunity )
        {
            ValueBuilder<?> builder = doConversion( valueType, entity );
            prototypeOpportunity.apply( (T) builder.prototype() );
            return createInstance( builder );
        }

        @Override
        public <T> Iterable<T> convert( final Class<T> valueType, Iterable<Object> entities )
        {
            return Iterables.map(
                new Function<Object, T>()
                {
                    @Override
                    public T apply( Object entity )
                    {
                        return convert( valueType, entity );
                    }
                }, entities );
        }

        @Override
        public <T> Iterable<T> convert( final Class<T> valueType, Iterable<Object> entities, final Function<T, T> prototypeOpportunity )
        {
            return Iterables.map(
                new Function<Object, T>()
                {
                    @Override
                    public T apply( Object entity )
                    {
                        return convert( valueType, entity, prototypeOpportunity );
                    }
                }, entities );
        }

        private <T> ValueBuilder<?> doConversion( final Class<T> valueType, Object entity )
        {
            ValueDescriptor valueDescriptor = module.valueDescriptor( valueType.getName() );
            if( valueDescriptor == null )
            {
                throw new NoSuchValueException( valueType.getName(), module.name() );
            }
            Unqualified unqualified = valueDescriptor.metaInfo( Unqualified.class );
//            Iterable<? extends PropertyDescriptor> properties = valueDescriptor.state().properties();
            final EntityComposite composite = (EntityComposite) entity;
            final EntityDescriptor entityDescriptor = spi.entityDescriptorFor( composite );
            final AssociationStateHolder associationState = spi.stateOf( composite );
            ValueBuilder<?> builder;

            if( unqualified == null || !unqualified.value() )
            {
                // Copy state using qualified names
                builder = module.newValueBuilderWithState(
                    valueType,
                    new Function<PropertyDescriptor, Object>()
                {
                    @Override
                    public Object apply( PropertyDescriptor descriptor )
                    {
                        try
                        {
                            return associationState.propertyFor( descriptor.accessor() ).get();
                        }
                        catch( IllegalArgumentException e )
                        {
                            AssociationStateDescriptor entityState = entityDescriptor.state();
                            String associationName = descriptor.qualifiedName().name();
                            if( STRING_TYPE_SPEC.test( descriptor.valueType() ) )
                            {
                                // Find Association and convert to string
                                AssociationDescriptor associationDescriptor;
                                try
                                {
                                    associationDescriptor = entityState.getAssociationByName( associationName );
                                }
                                catch( IllegalArgumentException e1 )
                                {
                                    return null;
                                }
                                AccessibleObject associationMethod = associationDescriptor.accessor();
                                Object entity = associationState.associationFor( associationMethod ).get();
                                if( entity != null )
                                {
                                    return ( (Identity) entity ).identity().get();
                                }
                                else
                                {
                                    return null;
                                }
                            }
                            else if( STRING_COLLECTION_TYPE_SPEC.test( descriptor.valueType() ) )
                            {
                                AssociationDescriptor associationDescriptor;
                                try
                                {
                                    associationDescriptor = entityState.getManyAssociationByName( associationName );
                                }
                                catch( IllegalArgumentException e1 )
                                {
                                    return Collections.emptyList();
                                }

                                ManyAssociation<?> state = associationState.manyAssociationFor( associationDescriptor.accessor() );
                                List<String> entities = new ArrayList<>( state.count() );
                                for( Object entity : state )
                                {
                                    entities.add( ( (Identity) entity ).identity().get() );
                                }
                                return entities;
                            }
                            else if( STRING_MAP_TYPE_SPEC.test( descriptor.valueType() ) )
                            {
                                AssociationDescriptor associationDescriptor;
                                try
                                {
                                    associationDescriptor = entityState.getNamedAssociationByName( associationName );
                                }
                                catch( IllegalArgumentException e1 )
                                {
                                    return Collections.emptyMap();
                                }

                                NamedAssociation<?> state = associationState.namedAssociationFor( associationDescriptor.accessor() );
                                Map<String, String> entities = new LinkedHashMap<>( state.count() );
                                for( String name : state )
                                {
                                    entities.put( name, ( (Identity) state.get( name ) ).identity().get() );
                                }
                                return entities;
                            }

                            return null;
                        }
                    }
                    },
                    new Function<AssociationDescriptor, EntityReference>()
                    {
                        @Override
                        public EntityReference apply( AssociationDescriptor associationDescriptor )
                        {
                            return EntityReference.entityReferenceFor(
                                associationState.associationFor( associationDescriptor.accessor() ).get() );
                        }
                    },
                    new Function<AssociationDescriptor, Iterable<EntityReference>>()
                    {
                        @Override
                        public Iterable<EntityReference> apply( AssociationDescriptor associationDescriptor )
                        {
                            ManyAssociation<?> state = associationState.manyAssociationFor( associationDescriptor.accessor() );
                            List<EntityReference> refs = new ArrayList<>( state.count() );
                            for( Object entity : state )
                            {
                                refs.add( EntityReference.entityReferenceFor( entity ) );
                            }
                            return refs;
                        }
                    },
                    new Function<AssociationDescriptor, Map<String, EntityReference>>()
                    {
                        @Override
                        public Map<String, EntityReference> apply( AssociationDescriptor associationDescriptor )
                        {
                            NamedAssociation<?> assoc = associationState.namedAssociationFor( associationDescriptor.accessor() );
                            Map<String, EntityReference> refs = new LinkedHashMap<>( assoc.count() );
                            for( String name : assoc )
                            {
                                refs.put( name, EntityReference.entityReferenceFor( assoc.get( name ) ) );
                            }
                            return refs;
                        }
                    } );
            }
            else
            {
                builder = module.newValueBuilderWithState(valueType,
                    new Function<PropertyDescriptor, Object>()
                {
                    @Override
                    public Object apply( final PropertyDescriptor descriptor )
                    {
                        AssociationStateDescriptor entityState = entityDescriptor.state();
                        String propertyName = descriptor.qualifiedName().name();
                        try
                        {
                            PropertyDescriptor propertyDescriptor = entityState.findPropertyModelByName( propertyName );
                            return associationState.propertyFor( propertyDescriptor.accessor() ).get();
                        }
                        catch( IllegalArgumentException e )
                        {
                            if( STRING_TYPE_SPEC.test( descriptor.valueType() ) )
                            {
                                // Find Association and convert to string
                                AssociationDescriptor associationDescriptor;
                                try
                                {
                                    associationDescriptor = entityState.getAssociationByName( propertyName );
                                }
                                catch( IllegalArgumentException e1 )
                                {
                                    return null;
                                }

                                AccessibleObject associationMethod = associationDescriptor.accessor();
                                Object entity = associationState.associationFor( associationMethod ).get();
                                if( entity != null )
                                {
                                    return ( (Identity) entity ).identity().get();
                                }
                                else
                                {
                                    return null;
                                }
                            }
                            else if( STRING_COLLECTION_TYPE_SPEC.test( descriptor.valueType() ) )
                            {
                                AssociationDescriptor associationDescriptor;
                                try
                                {
                                    associationDescriptor = entityState.getManyAssociationByName( propertyName );
                                }
                                catch( IllegalArgumentException e1 )
                                {
                                    return null;
                                }

                                AccessibleObject associationMethod = associationDescriptor.accessor();
                                ManyAssociation<?> state = associationState.manyAssociationFor( associationMethod );
                                List<String> entities = new ArrayList<>( state.count() );
                                for( Object entity : state )
                                {
                                    entities.add( ( (Identity) entity ).identity().get() );
                                }
                                return entities;
                            }
                            else if( STRING_MAP_TYPE_SPEC.test( descriptor.valueType() ) )
                            {
                                AssociationDescriptor associationDescriptor;
                                try
                                {
                                    associationDescriptor = entityState.getNamedAssociationByName( propertyName );
                                }
                                catch( IllegalArgumentException e1 )
                                {
                                    return null;
                                }

                                AccessibleObject associationMethod = associationDescriptor.accessor();
                                NamedAssociation<?> state = associationState.namedAssociationFor( associationMethod );
                                Map<String, String> entities = new LinkedHashMap<>( state.count() );
                                for( String name : state )
                                {
                                    entities.put( name, ( (Identity) state.get( name ) ).identity().get() );
                                }
                                return entities;
                            }
                            return null;
                        }
                    }
                    },
                    new Function<AssociationDescriptor, EntityReference>()
                    {
                        @Override
                        public EntityReference apply( AssociationDescriptor descriptor )
                        {
                            AssociationDescriptor associationDescriptor;
                            try
                            {
                                associationDescriptor = entityDescriptor.state()
                                    .getAssociationByName( descriptor.qualifiedName().name() );
                            }
                            catch( IllegalArgumentException e )
                            {
                                return null;
                            }

                            AccessibleObject associationMethod = associationDescriptor.accessor();
                            Association<Object> association = associationState.associationFor( associationMethod );
                            return EntityReference.entityReferenceFor( association.get() );
                        }
                    },
                    new Function<AssociationDescriptor, Iterable<EntityReference>>()
                    {
                        @Override
                        public Iterable<EntityReference> apply( final AssociationDescriptor descriptor )
                        {
                            AssociationDescriptor associationDescriptor;
                            try
                            {
                                String associationName = descriptor.qualifiedName().name();
                                AssociationStateDescriptor entityState = entityDescriptor.state();
                                associationDescriptor = entityState.getManyAssociationByName( associationName );
                            }
                            catch( IllegalArgumentException e )
                            {
                                return Iterables.empty();
                            }

                            ManyAssociation<?> state = associationState.manyAssociationFor( associationDescriptor.accessor() );
                            List<EntityReference> refs = new ArrayList<>( state.count() );
                            for( Object entity : state )
                            {
                                refs.add( EntityReference.entityReferenceFor( entity ) );
                            }
                            return refs;
                        }
                    },
                    new Function<AssociationDescriptor, Map<String, EntityReference>>()
                    {
                        @Override
                        public Map<String, EntityReference> apply( AssociationDescriptor descriptor )
                        {
                            AssociationDescriptor associationDescriptor;
                            try
                            {
                                String associationName = descriptor.qualifiedName().name();
                                AssociationStateDescriptor entityState = entityDescriptor.state();
                                associationDescriptor = entityState.getNamedAssociationByName( associationName );
                            }
                            catch( IllegalArgumentException e )
                            {
                                return Collections.emptyMap();
                            }
                            AccessibleObject associationMethod = associationDescriptor.accessor();
                            NamedAssociation<Object> assoc = associationState.namedAssociationFor( associationMethod );
                            Map<String, EntityReference> refs = new LinkedHashMap<>( assoc.count() );
                            for( String name : assoc )
                            {
                                refs.put( name, EntityReference.entityReferenceFor( assoc.get( name ) ) );
                            }
                            return refs;
                        }
                    } );
            }
            return builder;
        }

        @SuppressWarnings( "unchecked" )
        private <T> T createInstance( ValueBuilder<?> builder )
        {
            return (T) builder.newInstance();
        }
    }

}