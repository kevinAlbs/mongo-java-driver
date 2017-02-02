/*
 * Copyright 2017 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bson.codecs.pojo;

import com.fasterxml.classmate.AnnotationConfiguration;
import com.fasterxml.classmate.AnnotationInclusion;
import com.fasterxml.classmate.MemberResolver;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.members.RawConstructor;
import com.fasterxml.classmate.members.ResolvedField;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.bson.assertions.Assertions.notNull;

final class PojoHelper {

    @SuppressWarnings("unchecked")
    static <T> void configureClassModelBuilder(final ClassModelBuilder<T> classModelBuilder, final Class<T> clazz) {

        classModelBuilder.type(notNull("clazz", clazz)).annotations(asList(clazz.getAnnotations()));

        TypeResolver resolver = new TypeResolver();
        MemberResolver memberResolver = new MemberResolver(resolver);
        ResolvedType resolved = resolver.resolve(clazz);

        ResolvedTypeWithMembers resolvedType =
                memberResolver.resolve(resolved, new AnnotationConfiguration.StdConfiguration(AnnotationInclusion
                        .INCLUDE_AND_INHERIT_IF_INHERITED), null);

        List<Constructor<T>> publicConstructors = new ArrayList<Constructor<T>>();
        for (RawConstructor rawConstructor : resolved.getConstructors()) {
            if (rawConstructor.isPublic()) {
                publicConstructors.add((Constructor<T>) rawConstructor.getRawMember());
            }
        }

        Map<String, Field> fieldsMap = new HashMap<String, Field>();
        List<ResolvedField> resolvedFields = new ArrayList<ResolvedField>(asList(resolvedType.getMemberFields()));
        for (final ResolvedField resolvedField : resolvedFields) {
            if (resolvedField.isTransient()) {
                continue;
            }
            classModelBuilder.addField(getFieldModelBuilder(resolvedField));
            fieldsMap.put(resolvedField.getName(), resolvedField.getRawMember());
            resolvedField.getRawMember().setAccessible(true);
        }
        classModelBuilder.classAccessorFactory(new ClassAccessorFactoryImpl<T>(publicConstructors, fieldsMap));
    }

    @SuppressWarnings("unchecked")
    static <T> FieldModelBuilder<T> getFieldModelBuilder(final ResolvedField resolvedField) {
        Class<T> erasedType = (Class<T>) resolvedField.getType().getErasedType();
        return configureFieldModelBuilder(new FieldModelBuilder<T>(), resolvedField.getRawMember())
                .type(erasedType)
                .typeParameters(extractTypeParameters(resolvedField.getType()));
    }

    @SuppressWarnings("unchecked")
    static <T> FieldModelBuilder<T> configureFieldModelBuilder(final FieldModelBuilder<T> builder, final Field field) {
        Class<T> type = (Class<T>) field.getType();
        return builder
                .fieldName(field.getName())
                .documentFieldName(field.getName())
                .type(type)
                .fieldName(field.getName())
                .documentFieldName(field.getName())
                .annotations(asList(field.getDeclaredAnnotations()))
                .fieldModelSerialization(new FieldModelSerializationImpl<T>());
    }

    static List<Class<?>> extractTypeParameters(final ResolvedType type) {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        Class<?> erasedType = type.getErasedType();
        if (Collection.class.isAssignableFrom(erasedType)) {
            ResolvedType collectionType = type.getTypeParameters().get(0);
            Class<?> containerClass;
            if (Set.class.equals(erasedType)) {
                containerClass = HashSet.class;
            } else if (List.class.equals(erasedType) || Collection.class.equals(erasedType)) {
                containerClass = ArrayList.class;
            } else {
                containerClass = erasedType;
            }
            classes.add(containerClass);
            classes.addAll(extractTypeParameters(collectionType));
        } else if (Map.class.isAssignableFrom(erasedType)) {
            List<ResolvedType> types = type.getTypeParameters();
            ResolvedType keyType = types.get(0);
            ResolvedType valueType = types.get(1);
            if (!keyType.getErasedType().equals(String.class)) {
                throw new IllegalStateException(format("Invalid Map type. Map key types MUST be Strings, found %s instead.",
                        keyType.getErasedType()));
            }
            Class<?> containerClass;
            if (Map.class.equals(erasedType)) {
                containerClass = HashMap.class;
            } else {
                containerClass = erasedType;
            }
            classes.add(containerClass);
            classes.addAll(extractTypeParameters(valueType));
        } else {
            classes.add(type.getErasedType());
        }

        return classes;
    }

    static <V> V stateNotNull(final String property, final V value) {
        if (value == null) {
            throw new IllegalStateException(format("%s cannot be null", property));
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    static <T> Codec<T> getCodecFromDocument(final BsonReader reader, final boolean useDiscriminator, final String discriminatorKey,
                                             final CodecRegistry registry, final Codec<T> defaultCodec) {
        Codec<T> codec = defaultCodec;
        if (useDiscriminator) {
            reader.mark();
            reader.readStartDocument();
            boolean discriminatorKeyFound = false;
            while (!discriminatorKeyFound && reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                String name = reader.readName();
                if (discriminatorKey.equals(name)) {
                    discriminatorKeyFound = true;
                    String className = reader.readString();
                    try {
                        codec = (Codec<T>) registry.get(Class.forName(className));
                    } catch (final ClassNotFoundException e) {
                        throw new CodecConfigurationException("A mapped class could not be found: " + className);
                    }
                } else {
                    reader.skipValue();
                }
            }
            reader.reset();
        }
        return codec;
    }

    private PojoHelper() {
    }

}
