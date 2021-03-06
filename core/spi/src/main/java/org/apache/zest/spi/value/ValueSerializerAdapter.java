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
package org.apache.zest.spi.value;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.apache.zest.api.ZestAPI;
import org.apache.zest.api.association.Association;
import org.apache.zest.api.association.AssociationStateHolder;
import org.apache.zest.api.association.ManyAssociation;
import org.apache.zest.api.association.NamedAssociation;
import org.apache.zest.api.composite.CompositeInstance;
import org.apache.zest.api.entity.EntityComposite;
import org.apache.zest.api.entity.EntityReference;
import org.apache.zest.api.property.Property;
import org.apache.zest.api.value.ValueComposite;
import org.apache.zest.api.value.ValueDescriptor;
import org.apache.zest.api.value.ValueSerializationException;
import org.apache.zest.api.value.ValueSerializer;

/**
 * Adapter for pull-parsing capable ValueSerializers.
 *
 * <p>
 * Among Plain values (see {@link ValueSerializer}) some are considered primitives to underlying serialization
 * mechanisms and by so handed/come without conversion to/from implementations. Primitive values can be one of:
 * </p>
 * <ul>
 * <li>String,</li>
 * <li>Character or char,</li>
 * <li>Boolean or boolean,</li>
 * <li>Integer or int,</li>
 * <li>Long or long,</li>
 * <li>Short or short,</li>
 * <li>Byte or byte,</li>
 * <li>Float or float,</li>
 * <li>Double or double.</li>
 * </ul>
 * <p>
 * Some other Plain values are transformed before being handed to implementations:
 * </p>
 * <ul>
 * <li>BigInteger and BigDecimal depends on ValueSerializer.{@link org.apache.zest.api.value.ValueSerializer.Options};</li>
 * <li>Date as a ISO-8601 UTC String;</li>
 * <li>DateTime (JodaTime) as a ISO-8601 String with timezone offset or Z for UTC;</li>
 * <li>LocalDateTime (JodaTime) as a ISO-8601 String with no timezone offset;</li>
 * <li>LocalDate (JodaTime) as a ISO-8601 String with no time info;</li>
 * </ul>
 *
 * @param <OutputType> Implementor output type
 */
public abstract class ValueSerializerAdapter<OutputType>
    implements ValueSerializer
{

    public interface ComplexSerializer<T, OutputType>
    {
        void serialize( Options options, T object, OutputType output )
            throws Exception;
    }

    private static final String UTF_8 = "UTF-8";

    private static <TO, FROM extends TO> BiFunction<Options, FROM, TO> identitySerializer()
    {
        return ( options, from ) -> from;
    }

    private final Map<Class<?>, BiFunction<Options, Object, Object>> serializers = new HashMap<>( 16 );
    private final Map<Class<?>, ComplexSerializer<Object, OutputType>> complexSerializers = new HashMap<>( 2 );

    /**
     * Register a Plain Value type serialization Function.
     *
     * @param <T>        Plain Value parametrized Type
     * @param type       Plain Value Type
     * @param serializer Serialization Function
     */
    @SuppressWarnings( "unchecked" )
    protected final <T> void registerSerializer( Class<T> type, BiFunction<Options, T, Object> serializer )
    {
        serializers.put( type, (BiFunction<Options, Object, Object>) serializer );
    }

    /**
     * Register a Complex Value type serialization Function.
     *
     * @param <T>        Complex Value parametrized Type
     * @param type       Complex Value Type
     * @param serializer Serialization Function
     */
    @SuppressWarnings( "unchecked" )
    protected final <T> void registerComplexSerializer( Class<T> type, ComplexSerializer<T, OutputType> serializer )
    {
        complexSerializers.put( type, (ComplexSerializer<Object, OutputType>) serializer );
    }

    public ValueSerializerAdapter()
    {
        // Primitive Value types
        registerSerializer( String.class, ValueSerializerAdapter.identitySerializer() );
        registerSerializer( Character.class, ValueSerializerAdapter.identitySerializer() );
        registerSerializer( Boolean.class, ValueSerializerAdapter.identitySerializer() );
        registerSerializer( Integer.class, ValueSerializerAdapter.identitySerializer() );
        registerSerializer( Long.class, ValueSerializerAdapter.identitySerializer() );
        registerSerializer( Short.class, ValueSerializerAdapter.identitySerializer() );
        registerSerializer( Byte.class, ValueSerializerAdapter.identitySerializer() );
        registerSerializer( Float.class, ValueSerializerAdapter.identitySerializer() );
        registerSerializer( Double.class, ValueSerializerAdapter.identitySerializer() );

        // Number types
        registerSerializer( BigDecimal.class, ( options, bigDecimal ) -> bigDecimal.toString() );
        registerSerializer( BigInteger.class, ( options, bigInteger ) -> bigInteger.toString() );

        // Date types
        registerSerializer( Instant.class, ( options, date ) -> date.toString() );
        registerSerializer( Duration.class, ( options, date ) -> date.toString() );
        registerSerializer( Period.class, ( options, date ) -> date.toString() );
        registerSerializer( ZonedDateTime.class, ( options, date ) -> date.toString() );
        registerSerializer( OffsetDateTime.class, ( options, date ) -> date.toString() );
        registerSerializer( LocalDateTime.class, ( options, date ) -> date.toString() );
        registerSerializer( LocalDate.class, ( options, date ) -> date.toString() );
        registerSerializer( LocalTime.class, ( options, date ) -> date.toString() );

        // Other supported types
        registerSerializer( EntityReference.class, ( options, ref ) -> ref.toString() );
    }

    @Override
    public final <T> Function<T, String> serialize()
    {
        return this::serialize;
    }

    @Override
    public final <T> Function<T, String> serialize( final Options options )
    {
        return object -> serialize( options, object );
    }

    @Override
    @Deprecated
    public final <T> Function<T, String> serialize( final boolean includeTypeInfo )
    {
        return object -> serialize(
            includeTypeInfo ? new Options().withTypeInfo() : new Options().withoutTypeInfo(),
            object );
    }

    @Override
    public final String serialize( Object object )
        throws ValueSerializationException
    {
        return serialize( new Options(), object );
    }

    @Override
    public final String serialize( Options options, Object object )
        throws ValueSerializationException
    {
        try
        {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            serializeRoot( options, object, output );
            return output.toString( UTF_8 );
        }
        catch( ValueSerializationException ex )
        {
            throw ex;
        }
        catch( Exception ex )
        {
            throw new ValueSerializationException( "Could not serialize value", ex );
        }
    }

    @Override
    @Deprecated
    public final String serialize( Object object, boolean includeTypeInfo )
        throws ValueSerializationException
    {
        return serialize( includeTypeInfo ? new Options().withTypeInfo() : new Options().withoutTypeInfo(),
                          object );
    }

    @Override
    public final void serialize( Object object, OutputStream output )
        throws ValueSerializationException
    {
        serialize( new Options(), object, output );
    }

    @Override
    public final void serialize( Options options, Object object, OutputStream output )
        throws ValueSerializationException
    {
        try
        {
            serializeRoot( options, object, output );
        }
        catch( ValueSerializationException ex )
        {
            throw ex;
        }
        catch( Exception ex )
        {
            throw new ValueSerializationException( "Could not serialize value", ex );
        }
    }

    @Override
    @Deprecated
    public final void serialize( Object object, OutputStream output, boolean includeTypeInfo )
        throws ValueSerializationException
    {
        serialize( includeTypeInfo ? new Options().withTypeInfo() : new Options().withoutTypeInfo(),
                   object, output );
    }

    private void serializeRoot( Options options, Object object, OutputStream output )
        throws Exception
    {
        if( object != null )
        {
            if( serializers.get( object.getClass() ) != null )
            {
                // Plain Value
                Object serialized = serializers.get( object.getClass() ).apply( options, object );
                output.write( serialized.toString().getBytes( UTF_8 ) );
            }
            else if( object.getClass().isEnum() )
            {
                // Enum Value
                output.write( object.toString().getBytes( UTF_8 ) );
            }
            else if( object.getClass().isArray() )
            {
                // Array Value
                output.write( serializeBase64Serializable( object ).getBytes( UTF_8 ) );
            }
            else
            {
                // Complex Value
                OutputType adaptedOutput = adaptOutput( output );
                onSerializationStart( object, adaptedOutput );
                doSerialize( options, object, adaptedOutput, true );
                onSerializationEnd( object, adaptedOutput );
            }
        }
    }

    private void doSerialize( Options options, Object object, OutputType output, boolean rootPass )
        throws Exception
    {
        // Null
        if( object == null )
        {
            onValue( output, null );
        }
        else // Registered serializer
            if( serializers.get( object.getClass() ) != null )
            {
                onValue( output, serializers.get( object.getClass() ).apply( options, object ) );
            }
            else if( complexSerializers.get( object.getClass() ) != null )
            {
                complexSerializers.get( object.getClass() ).serialize( options, object, output );
            }
            else // ValueComposite
                if( ValueComposite.class.isAssignableFrom( object.getClass() ) )
                {
                    serializeValueComposite( options, object, output, rootPass );
                }
                else // EntityComposite
                    if( EntityComposite.class.isAssignableFrom( object.getClass() ) )
                    {
                        serializeEntityComposite( object, output );
                    }
                    else // Collection - Iterable
                        if( Iterable.class.isAssignableFrom( object.getClass() ) )
                        {
                            serializeIterable( options, object, output );
                        }
                        else // Array - QUID Remove this and use java serialization for arrays?
                            if( object.getClass().isArray() )
                            {
                                serializeBase64Serializable( object, output );
                            }
                            else // Map
                                if( Map.class.isAssignableFrom( object.getClass() ) )
                                {
                                    serializeMap( options, object, output );
                                }
                                else // Enum
                                    if( object.getClass().isEnum() )
                                    {
                                        onValue( output, object.toString() );
                                    }
                                    else // Fallback to Base64 encoded Java Serialization
                                    {
                                        serializeBase64Serializable( object, output );
                                    }
    }

    private void serializeValueComposite( Options options, Object object, OutputType output, boolean rootPass )
        throws Exception
    {
        CompositeInstance valueInstance = ZestAPI.FUNCTION_COMPOSITE_INSTANCE_OF.apply( (ValueComposite) object );
        ValueDescriptor descriptor = (ValueDescriptor) valueInstance.descriptor();
        AssociationStateHolder state = (AssociationStateHolder) valueInstance.state();

        onObjectStart( output );

        //noinspection ConstantConditions
        if( options.getBoolean( Options.INCLUDE_TYPE_INFO ) && !rootPass )
        {
            onFieldStart( output, "_type" );
            onValueStart( output );
            onValue( output, descriptor.valueType().types().findFirst().get().getName());
            onValueEnd( output );
            onFieldEnd( output );
        }

        descriptor.valueType().properties().forEach( persistentProperty -> {
            Property<?> property = state.propertyFor( persistentProperty.accessor() );
            try
            {
                onFieldStart( output, persistentProperty.qualifiedName().name() );
                onValueStart( output );
                doSerialize( options, property.get(), output, false );
                onValueEnd( output );
                onFieldEnd( output );
            }
            catch( Exception e )
            {
                throw new ValueSerializationException( "Unable to serialize property " + persistentProperty, e );
            }
        } );
        descriptor.valueType().associations().forEach(associationDescriptor ->        {
            Association<?> association = state.associationFor( associationDescriptor.accessor() );
            try
            {
                onFieldStart( output, associationDescriptor.qualifiedName().name() );
                onValueStart( output );
                EntityReference ref = association.reference();
                if( ref == null )
                {
                    onValue( output, null );
                }
                else
                {
                    onValue( output, ref.identity() );
                }
                onValueEnd( output );
                onFieldEnd( output );
            }
            catch( Exception e )
            {
                throw new ValueSerializationException( "Unable to serialize association " + associationDescriptor, e );
            }
        } );
        descriptor.valueType().manyAssociations().forEach( associationDescriptor -> {
            ManyAssociation<?> manyAssociation = state.manyAssociationFor( associationDescriptor.accessor() );
            try
            {
                onFieldStart( output, associationDescriptor.qualifiedName().name() );
                onValueStart( output );
                onArrayStart( output );
                for( EntityReference ref : manyAssociation.references() )
                {
                    onValueStart( output );
                    onValue( output, ref.identity() );
                    onValueEnd( output );
                }
                onArrayEnd( output );
                onValueEnd( output );
                onFieldEnd( output );
            }
            catch( Exception e )
            {
                throw new ValueSerializationException( "Unable to serialize manyassociation " + associationDescriptor, e );
            }
        });
        descriptor.valueType().namedAssociations().forEach( associationDescriptor -> {
            NamedAssociation<?> namedAssociation = state.namedAssociationFor( associationDescriptor.accessor() );
            try
            {
                onFieldStart( output, associationDescriptor.qualifiedName().name() );
                onValueStart( output );
                onObjectStart( output );
                for( String name : namedAssociation )
                {
                    onFieldStart( output, name );
                    onValueStart( output );
                    EntityReference ref = namedAssociation.referenceOf( name );
                    onValue( output, ref.identity() );
                    onValueEnd( output );
                    onFieldEnd( output );
                }
                onObjectEnd( output );
                onValueEnd( output );
                onFieldEnd( output );
            }
            catch( Exception e )
            {
                throw new ValueSerializationException( "Unable to serialize namedassociation " + associationDescriptor, e );
            }
        } );

        onObjectEnd( output );
    }

    private void serializeEntityComposite( Object object, OutputType output )
        throws Exception
    {
        onValue( output, EntityReference.entityReferenceFor( object ) );
    }

    private void serializeIterable( Options options, Object object, OutputType output )
        throws Exception
    {
        @SuppressWarnings( "unchecked" )
        Iterable<Object> collection = (Iterable<Object>) object;
        onArrayStart( output );
        for( Object item : collection )
        {
            onValueStart( output );
            doSerialize( options, item, output, false );
            onValueEnd( output );
        }
        onArrayEnd( output );
    }

    private void serializeMap( Options options, Object object, OutputType output )
        throws Exception
    {
        @SuppressWarnings( "unchecked" )
        Map<Object, Object> map = (Map<Object, Object>) object;
        //noinspection ConstantConditions
        if( options.getBoolean( Options.MAP_ENTRIES_AS_OBJECTS ) )
        {
            onObjectStart( output );
            for( Map.Entry<Object, Object> entry : map.entrySet() )
            {
                onFieldStart( output, entry.getKey().toString() );
                onValueStart( output );
                doSerialize( options, entry.getValue(), output, false );
                onValueEnd( output );
                onFieldEnd( output );
            }
            onObjectEnd( output );
        }
        else
        {
            onArrayStart( output );
            for( Map.Entry<Object, Object> entry : map.entrySet() )
            {
                onObjectStart( output );

                onFieldStart( output, "key" );
                onValueStart( output );
                onValue( output, entry.getKey().toString() );
                onValueEnd( output );
                onFieldEnd( output );

                onFieldStart( output, "value" );
                onValueStart( output );
                doSerialize( options, entry.getValue(), output, false );
                onValueEnd( output );
                onFieldEnd( output );

                onObjectEnd( output );
            }
            onArrayEnd( output );
        }
    }

    private void serializeBase64Serializable( Object object, OutputType output )
        throws Exception
    {
        onValue( output, serializeBase64Serializable( object ) );
    }

    private String serializeBase64Serializable( Object object )
        throws Exception
    {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream( bout ))
        {
            out.writeUnshared( object );
        }
        byte[] bytes = Base64.getEncoder().encode( bout.toByteArray() );
        return new String( bytes, UTF_8 );
    }

    protected abstract OutputType adaptOutput( OutputStream output )
        throws Exception;

    protected void onSerializationStart( Object object, OutputType output )
        throws Exception
    {
        // NOOP
    }

    protected void onSerializationEnd( Object object, OutputType output )
        throws Exception
    {
        // NOOP
    }

    protected abstract void onArrayStart( OutputType output )
        throws Exception;

    protected abstract void onArrayEnd( OutputType output )
        throws Exception;

    protected abstract void onObjectStart( OutputType output )
        throws Exception;

    protected abstract void onObjectEnd( OutputType output )
        throws Exception;

    protected abstract void onFieldStart( OutputType output, String fieldName )
        throws Exception;

    protected void onFieldEnd( OutputType output )
        throws Exception
    {
        // NOOP
    }

    protected void onValueStart( OutputType output )
        throws Exception
    {
        // NOOP
    }

    protected abstract void onValue( OutputType output, Object value )
        throws Exception;

    protected void onValueEnd( OutputType output )
        throws Exception
    {
        // NOOP
    }
}
