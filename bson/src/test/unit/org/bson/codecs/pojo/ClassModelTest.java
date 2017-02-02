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
        ClassModel<?> classModel = new ClassModelBuilder<SimpleGenericsModel>(SimpleGenericsModel.class).build();
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
    public void testAnnotationModel() {
        ClassModel<?> classModel = new ClassModelBuilder<AnnotationModel>(AnnotationModel.class).build();
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
