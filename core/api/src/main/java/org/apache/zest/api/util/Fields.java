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
package org.apache.zest.api.util;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.zest.functional.Iterables;

import static org.apache.zest.functional.Iterables.iterable;

/**
 * Useful methods for handling Fields.
 */
public final class Fields
{
    public static final Function<Type, Stream<Field>> FIELDS_OF =
        Classes.forClassHierarchy( type -> Arrays.stream( type.getDeclaredFields() ) );

    public static final BiFunction<Class<?>, String, Field> FIELD_NAMED = ( clazz, name ) ->
        FIELDS_OF.apply( clazz ).filter( Classes.memberNamed( name ) ).findFirst().orElse( null );

    public static Stream<Field> fieldsOf( Type type )
    {
        return Stream.of( type ).flatMap( FIELDS_OF );
    }
}