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

import org.bson.codecs.pojo.entities.SimpleGenericsModel;
import org.bson.codecs.pojo.entities.conventions.AnnotationModel;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class ClassModelTest {

    @Test
    @SuppressWarnings("rawtypes")
    public void testSimpleGenericsModel() {
        ClassModel<?> classModel = ClassModel.builder(SimpleGenericsModel.class).build();
        FieldModel<?> fieldModel = classModel.getFieldModels().get(0);

        assertEquals("SimpleGenericsModel", classModel.getName());
        assertEquals(SimpleGenericsModel.class, classModel.getType());
        assertTrue(classModel.useDiscriminator());
        assertEquals("_t", classModel.getDiscriminatorKey());
        assertEquals("SimpleGenericsModel", classModel.getDiscriminator());
        assertNull(classModel.getIdFieldModel());
        assertEquals(4, classModel.getFieldModels().size());
        assertEquals(fieldModel, classModel.getFieldModel(fieldModel.getDocumentFieldName()));
        assertTrue(classModel.getClassAccessorFactory() instanceof ClassAccessorFactoryImpl);
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void testCollectionNestedPojoModelFieldTypes() {
//        ClassModel<?> classModel = ClassModel.builder(CollectionNestedPojoModel.class).build();
//        assertEquals(singletonList(SimpleModel.class), classModel.getFieldModels().get(0).getTypeParameters());
//        assertEquals(asList(ArrayList.class, SimpleModel.class), classModel.getFieldModels().get(1).getTypeParameters());
//
//        assertEquals(singletonList(SimpleModel.class), classModel.getFieldModels().get(2).getTypeParameters());
//        assertEquals(asList(HashSet.class, SimpleModel.class), classModel.getFieldModels().get(3).getTypeParameters());
//
//        assertEquals(asList(String.class, SimpleModel.class), classModel.getFieldModels().get(4).getTypeParameters());
//        assertEquals(asList(String.class, HashMap.class, String.class, SimpleModel.class),
//                classModel.getFieldModels().get(5).getTypeParameters());
//
//        assertEquals(asList(String.class, ArrayList.class, SimpleModel.class), classModel.getFieldModels().get(6).getTypeParameters());
//        assertEquals(asList(String.class, ArrayList.class, HashMap.class, String.class, SimpleModel.class),
//                classModel.getFieldModels().get(7).getTypeParameters());
//        assertEquals(asList(String.class, HashSet.class, SimpleModel.class), classModel.getFieldModels().get(8).getTypeParameters());
//
//        assertEquals(asList(HashMap.class, String.class, SimpleModel.class), classModel.getFieldModels().get(9).getTypeParameters());
//        assertEquals(asList(HashMap.class, String.class, ArrayList.class, SimpleModel.class),
//                classModel.getFieldModels().get(10).getTypeParameters());
//        assertEquals(asList(HashMap.class, String.class, HashSet.class, SimpleModel.class),
//                classModel.getFieldModels().get(11).getTypeParameters());
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void testMappingConcreteGenericTypes() {
//        ClassModel<?> classModel = ClassModel.builder(NestedGenericHolderMapModel.class).build();
//        assertEquals(GenericHolderModel.class, classModel.getFieldModels().get(0).getFieldType());
//        assertEquals(asList(HashMap.class, String.class, SimpleModel.class), classModel.getFieldModels().get(0).getTypeParameters());
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void testMappingSimpleGenericsModelTypes() {
//        ClassModel<?> classModel = ClassModel.builder(SimpleGenericsModel.class).build();
//        assertEquals(Integer.class, classModel.getFieldModels().get(0).getFieldType());
//        assertEquals(emptyList(), classModel.getFieldModels().get(0).getTypeParameters());
//
//        assertEquals(Object.class, classModel.getFieldModels().get(1).getFieldType());
//        assertEquals(emptyList(), classModel.getFieldModels().get(1).getTypeParameters());
//
//        assertEquals(ArrayList.class, classModel.getFieldModels().get(2).getFieldType());
//        assertEquals(singletonList(Object.class), classModel.getFieldModels().get(2).getTypeParameters());
//
//        assertEquals(HashMap.class, classModel.getFieldModels().get(3).getFieldType());
//        assertEquals(asList(String.class, Object.class), classModel.getFieldModels().get(3).getTypeParameters());
    }

    @Test
    public void testAnnotationModel() {
        ClassModel<?> classModel = ClassModel.builder(AnnotationModel.class).build();
        FieldModel<?> fieldModel = classModel.getFieldModels().get(0);

        assertEquals("AnnotationModel", classModel.getName());
        assertEquals(AnnotationModel.class, classModel.getType());
        assertTrue(classModel.useDiscriminator());
        assertEquals("_cls", classModel.getDiscriminatorKey());
        assertEquals("MyAnnotationModel", classModel.getDiscriminator());
        assertEquals(fieldModel, classModel.getIdFieldModel());
        assertEquals(3, classModel.getFieldModels().size());
        assertEquals(fieldModel, classModel.getFieldModel(fieldModel.getDocumentFieldName()));
        assertTrue(classModel.getClassAccessorFactory() instanceof ClassAccessorFactoryImpl);
    }

}
