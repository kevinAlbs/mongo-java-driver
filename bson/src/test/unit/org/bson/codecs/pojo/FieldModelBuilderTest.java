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

import org.bson.codecs.IntegerCodec;
import org.bson.codecs.pojo.annotations.Property;
import org.bson.codecs.pojo.entities.SimpleGenericsModel;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public final class FieldModelBuilderTest {

    @Test
    @SuppressWarnings("rawtypes")
    public void testDefaults() {
        FieldModelBuilder fieldModelBuilder = new FieldModelBuilder();

        assertNull(fieldModelBuilder.getDocumentFieldName());
        assertNull(fieldModelBuilder.getFieldName());
        assertTrue(fieldModelBuilder.getAnnotations().isEmpty());
        assertNull(fieldModelBuilder.getFieldModelSerialization());
        assertNull(fieldModelBuilder.getTypeData());
        assertTrue(fieldModelBuilder.isDiscriminatorEnabled());
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void testFieldMapping() throws NoSuchFieldException {
        Field field = SimpleGenericsModel.class.getDeclaredField("myListField");
        FieldModelBuilder<SimpleGenericsModel> fieldModelBuilder = new FieldModelBuilder<SimpleGenericsModel>(field);

        assertEquals(field.getName(), fieldModelBuilder.getFieldName());
        assertEquals(field.getName(), fieldModelBuilder.getDocumentFieldName());
        assertTrue(fieldModelBuilder.getAnnotations().isEmpty());
        assertTrue(fieldModelBuilder.isDiscriminatorEnabled());
    }

    @Test
    public void testFieldOverrides() throws NoSuchFieldException {
        IntegerCodec integerCodec = new IntegerCodec();
        Field field = SimpleGenericsModel.class.getDeclaredField("myIntegerField");
        FieldModelBuilder<Integer> fieldModelBuilder = new FieldModelBuilder<Integer>(field)
                .fieldName("fieldName")
                .codec(integerCodec)
                .documentFieldName("altDocumentFieldName")
                .annotations(TEST_ANNOTATIONS)
                .fieldModelSerialization(CUSTOM_SERIALIZATION)
                .typeData(TypeData.builder(Integer.class).build())
                .discriminatorEnabled(false);

        assertEquals("fieldName", fieldModelBuilder.getFieldName());
        assertEquals("altDocumentFieldName", fieldModelBuilder.getDocumentFieldName());
        assertEquals(integerCodec, fieldModelBuilder.getCodec());
        assertEquals(Integer.class, fieldModelBuilder.getTypeData().getType());
        assertEquals(TEST_ANNOTATIONS, fieldModelBuilder.getAnnotations());
        assertEquals(CUSTOM_SERIALIZATION, fieldModelBuilder.getFieldModelSerialization());
        assertFalse(fieldModelBuilder.isDiscriminatorEnabled());
    }

    @Test(expected = IllegalStateException.class)
    public void testCallingBuildValidates() {
        new FieldModelBuilder<Object>().build();
    }

    private static final List<Annotation> TEST_ANNOTATIONS = Collections.<Annotation>singletonList(
            new Property() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return Property.class;
                }

                @Override
                public String name() {
                    return "";
                }

                @Override
                public boolean useDiscriminator() {
                    return true;
                }
            });

    private static final FieldModelSerialization<Integer> CUSTOM_SERIALIZATION = new FieldModelSerialization<Integer>() {
        @Override
        public boolean shouldSerialize(final Integer value) {
            return false;
        }
    };
}
