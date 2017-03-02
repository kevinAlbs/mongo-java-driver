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

import org.bson.codecs.pojo.annotations.Property;
import org.bson.codecs.pojo.entities.ConcreteCollectionsModel;
import org.bson.codecs.pojo.entities.GenericHolderModel;
import org.bson.codecs.pojo.entities.InvalidMapModel;
import org.bson.codecs.pojo.entities.NestedGenericHolderModel;
import org.bson.codecs.pojo.entities.SimpleGenericsModel;
import org.bson.codecs.pojo.entities.UpperBoundsModel;
import org.bson.codecs.pojo.entities.UpperBoundsSubClassModel;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@SuppressWarnings("rawtypes")
public final class ClassModelBuilderTest {

    @Test
    public void testDefaults() {
        ClassModelBuilder builder = new ClassModelBuilder();

        assertTrue(builder.getFields().isEmpty());
        assertEquals(2, builder.getConventions().size());
        assertTrue(builder.getAnnotations().isEmpty());
        assertNull(builder.getType());
        assertNull(builder.getIdField());
        assertNull(builder.isDiscriminatorEnabled());
        assertNull(builder.getDiscriminatorKey());
        assertNull(builder.getDiscriminator());
        assertNull(builder.getField("Missing"));
    }

    @Test
    public void testReturnsConfiguredValues() {
        Class<SimpleGenericsModel> clazz = SimpleGenericsModel.class;
        ClassModelBuilder<SimpleGenericsModel> builder = new ClassModelBuilder<SimpleGenericsModel>(clazz);
        assertEquals(4, builder.getFields().size());
        for (Field field : clazz.getDeclaredFields()) {
            assertEquals(field.getName(), builder.getField(field.getName()).getDocumentFieldName());
        }
        assertEquals(2, builder.getConventions().size());
        assertTrue(builder.getAnnotations().isEmpty());
        assertEquals(clazz, builder.getType());
        assertNull(builder.getIdField());
        assertNull(builder.isDiscriminatorEnabled());
        assertNull(builder.getDiscriminator());
    }

    @Test
    public void testMappedBoundedClasses() {
        ClassModelBuilder<? extends UpperBoundsModel> builder = new ClassModelBuilder<UpperBoundsModel>(UpperBoundsModel.class);
        assertEquals(Number.class, builder.getField("myGenericField").getType());

        builder = new ClassModelBuilder<UpperBoundsSubClassModel>(UpperBoundsSubClassModel.class);
        assertEquals(Long.class, builder.getField("myGenericField").getType());
    }

    @Test
    public void testNestedGenericHolderModel() {
        ClassModelBuilder<NestedGenericHolderModel> builder =
                new ClassModelBuilder<NestedGenericHolderModel>(NestedGenericHolderModel.class);
        assertEquals(GenericHolderModel.class, builder.getField("nested").getType());
        assertEquals(singletonList(Long.class), builder.getField("nested").getTypeParameters());
    }

    @Test
    public void testFieldsMappedClassTypes() {
        ClassModelBuilder<ConcreteCollectionsModel> builder =
                new ClassModelBuilder<ConcreteCollectionsModel>(ConcreteCollectionsModel.class);

        assertEquals(ArrayList.class, builder.getField("collection").getType());
        assertEquals(ArrayList.class, builder.getField("list").getType());
        assertEquals(LinkedList.class, builder.getField("linked").getType());
        assertEquals(HashMap.class, builder.getField("map").getType());
        assertEquals(ConcurrentHashMap.class, builder.getField("concurrent").getType());
    }

    @Test
    public void testOverrides() throws NoSuchFieldException {
        Class<SimpleGenericsModel> clazz = SimpleGenericsModel.class;
        Field myIntegerField = clazz.getDeclaredFields()[0];
        FieldModelBuilder<Integer> field = new FieldModelBuilder<Integer>(myIntegerField);
        ClassModelBuilder<SimpleGenericsModel> builder = new ClassModelBuilder<SimpleGenericsModel>()
                .type(clazz)
                .addField(field)
                .annotations(TEST_ANNOTATIONS)
                .conventions(TEST_CONVENTIONS)
                .discriminatorKey("_cls")
                .discriminator("myColl")
                .discriminatorEnabled(true)
                .idField("myIntegerField");

        assertEquals(TEST_ANNOTATIONS, builder.getAnnotations());
        assertEquals(TEST_CONVENTIONS, builder.getConventions());
        assertEquals("myIntegerField", builder.getIdField());
        assertEquals(clazz, builder.getType());
        assertTrue(builder.isDiscriminatorEnabled());
        assertEquals("_cls", builder.getDiscriminatorKey());
        assertEquals("myColl", builder.getDiscriminator());
    }

    @Test
    public void testCanRemoveField() {
        ClassModelBuilder<SimpleGenericsModel> builder = new ClassModelBuilder<SimpleGenericsModel>(SimpleGenericsModel.class)
                .idField("ID");
        assertEquals(4, builder.getFields().size());
        builder.removeField("myIntegerField");
        assertEquals(3, builder.getFields().size());

        builder.removeField("myIntegerField");
        assertEquals(3, builder.getFields().size());
    }

    @Test(expected = IllegalStateException.class)
    public void testValidationClassModelRequiresType() {
        new ClassModelBuilder<Object>().build();
    }

    @Test(expected = IllegalStateException.class)
    public void testValidationCollectionName() {
        new ClassModelBuilder<SimpleGenericsModel>().type(SimpleGenericsModel.class)
                .conventions(Collections.<Convention>emptyList()).build();
    }

    @Test(expected = IllegalStateException.class)
    public void testValidationIdField() {
        new ClassModelBuilder<SimpleGenericsModel>(SimpleGenericsModel.class).idField("ID").build();
    }

    @Test(expected = IllegalStateException.class)
    public void testValidationDuplicateField() {
        ClassModelBuilder<SimpleGenericsModel> builder = new ClassModelBuilder<SimpleGenericsModel>(SimpleGenericsModel.class);
        builder.addField(builder.getField("myIntegerField"));
        builder.build();
    }

    @Test(expected = IllegalStateException.class)
    public void testValidationDuplicateFieldName() {
        ClassModelBuilder<SimpleGenericsModel> builder = new ClassModelBuilder<SimpleGenericsModel>(SimpleGenericsModel.class);
        builder.getField("myIntegerField").fieldName("myGenericField");
        builder.build();
    }

    @Test(expected = IllegalStateException.class)
    public void testValidationDuplicateDocumentFieldName() {
        ClassModelBuilder<SimpleGenericsModel> builder = new ClassModelBuilder<SimpleGenericsModel>(SimpleGenericsModel.class);
        builder.getField("myIntegerField").documentFieldName("myGenericField");
        builder.build();
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalMapKey() {
        new ClassModelBuilder<InvalidMapModel>(InvalidMapModel.class).build();
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

    private static final List<Convention> TEST_CONVENTIONS = Collections.<Convention>singletonList(
            new Convention() {
                @Override
                public void apply(final ClassModelBuilder<?> builder) {
                }
            });
}
