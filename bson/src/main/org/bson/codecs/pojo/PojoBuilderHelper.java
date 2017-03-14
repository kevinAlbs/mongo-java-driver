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
import com.fasterxml.classmate.TypeBindings;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.classmate.members.RawConstructor;
import com.fasterxml.classmate.members.ResolvedField;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.bson.assertions.Assertions.notNull;

final class PojoBuilderHelper {

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

        List<String> genericTypeNames = new ArrayList<String>();
        for (TypeVariable<Class<T>> classTypeVariable : clazz.getTypeParameters()) {
            genericTypeNames.add(classTypeVariable.getName());
        }

        
        Map<String, Field> fieldsMap = new HashMap<String, Field>();
        List<ResolvedField> resolvedFields = new ArrayList<ResolvedField>(asList(resolvedType.getMemberFields()));
        List<String> genericFieldOrder = new ArrayList<String>();
        for (final ResolvedField resolvedField : resolvedFields) {
            if (resolvedField.isTransient()) {
                continue;
            }
            String name = resolvedField.getName();
            Field field = resolvedField.getRawMember();
            fieldsMap.put(name, field);

            int genericTypeIndex = fieldGenericTypeIndex(genericTypeNames, resolvedField);
            if (genericTypeIndex > -1) {
                genericFieldOrder.add(genericTypeIndex, name);
            }

            classModelBuilder.addField(getFieldBuilder(resolvedField.getRawMember(), resolvedField.getType().getErasedType(),
                    resolvedField.getType()));

            resolvedField.getRawMember().setAccessible(true);
        }
        classModelBuilder
                .genericFieldNames(genericFieldOrder)
                .classAccessorFactory(new ClassAccessorFactoryImpl<T>(publicConstructors, fieldsMap));
    }

    static int fieldGenericTypeIndex(final List<String> genericTypeNames, final ResolvedField resolvedField) {
        Type fieldGenericType = resolvedField.getRawMember().getGenericType();
        int index = genericTypeNames.indexOf(fieldGenericType.toString());
        if (index == -1 && !resolvedField.getType().getTypeBindings().getTypeParameters().isEmpty()) {
            Type type = resolvedField.getRawMember().getGenericType();
            if (type instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) type;
                for (Type tp : pt.getActualTypeArguments()) {
                    index = genericTypeNames.indexOf(tp.toString());
                    if (index != -1) {
                        return index;
                    }
                }
            }
        }
        return index;
    }


    @SuppressWarnings("unchecked")
    static <T> FieldModelBuilder<T> configureFieldModelBuilder(final FieldModelBuilder<T> builder, final Field field) {
        return builder
                .fieldName(field.getName())
                .documentFieldName(field.getName())
                .fieldName(field.getName())
                .typeData((TypeData<T>) getFieldTypeDataFromClass(field.getType()))
                .annotations(asList(field.getDeclaredAnnotations()))
                .fieldModelSerialization(new FieldModelSerializationImpl<T>());
    }

    private static <T> FieldModelBuilder<T> getFieldBuilder(final Field field, final Class<T> clazz, final ResolvedType resolvedType) {
        return FieldModel.<T>builder(field).typeData(getFieldTypeData(TypeData.builder(clazz), resolvedType));
    }

    private static <T> TypeData<T> getFieldTypeData(final TypeData.Builder<T> type, final ResolvedType resolvedType) {
        TypeBindings bindings = resolvedType.getTypeBindings();
        for (int i = 0; i < bindings.getTypeParameters().size(); i++) {
            ResolvedType boundType = bindings.getBoundType(i);
            type.addTypeParameter(getFieldTypeData(TypeData.builder(boundType.getErasedType()), boundType));
        }
        return type.build();
    }

    @SuppressWarnings("unchecked")
    private static <T> TypeData<T> getFieldTypeDataFromClass(final Class<T> type) {
        TypeData.Builder<T> builder = TypeData.builder(type);
        if (Collection.class.isAssignableFrom(type)) {
            builder.addTypeParameter(TypeData.builder(Object.class).build());
        } else if (Map.class.isAssignableFrom(type)) {
            builder.addTypeParameter(TypeData.builder(String.class).build());
            builder.addTypeParameter(TypeData.builder(Object.class).build());
        }
        return builder.build();
    }

    static <V> V stateNotNull(final String property, final V value) {
        if (value == null) {
            throw new IllegalStateException(format("%s cannot be null", property));
        }
        return value;
    }


    private PojoBuilderHelper() {
    }

}
