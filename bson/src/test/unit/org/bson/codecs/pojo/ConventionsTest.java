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

import org.bson.codecs.pojo.entities.SimpleModel;
import org.bson.codecs.pojo.entities.conventions.AnnotationDefaultsModel;
import org.bson.codecs.pojo.entities.conventions.AnnotationModel;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.util.Collections;

import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.bson.codecs.pojo.Conventions.ANNOTATION_CONVENTION;
import static org.bson.codecs.pojo.Conventions.CLASS_AND_FIELD_CONVENTION;
import static org.bson.codecs.pojo.Conventions.DEFAULT_CONVENTIONS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public final class ConventionsTest {

    @Test
    public void testDefaultConventions() {
        ClassModel<AnnotationModel> classModel = new ClassModelBuilder<AnnotationModel>(AnnotationModel.class)
                .conventions(DEFAULT_CONVENTIONS).build();

        assertTrue(classModel.useDiscriminator());
        assertEquals("_cls", classModel.getDiscriminatorKey());
        assertEquals("MyAnnotationModel", classModel.getDiscriminator());

        assertEquals(3, classModel.getFieldModels().size());
        FieldModel<?> idFieldModel = classModel.getIdFieldModel();
        assertNotNull(idFieldModel);
        assertEquals("customId", idFieldModel.getFieldName());
        assertEquals("_id", idFieldModel.getDocumentFieldName());

        FieldModel<?> childFieldModel = classModel.getFieldModel("child");
        assertNotNull(childFieldModel);
        assertFalse(childFieldModel.useDiscriminator());

        FieldModel<?> renamedFieldModel = classModel.getFieldModel("renamed");
        assertNotNull(renamedFieldModel);
    }

    @Test
    public void testAnnotationDefaults() {
        ClassModel<AnnotationDefaultsModel> classModel = new ClassModelBuilder<AnnotationDefaultsModel>(AnnotationDefaultsModel.class)
                .conventions(singletonList(ANNOTATION_CONVENTION)).build();

        assertTrue(classModel.useDiscriminator());
        assertEquals("_t", classModel.getDiscriminatorKey());
        assertEquals("AnnotationDefaultsModel", classModel.getDiscriminator());

        assertEquals(2, classModel.getFieldModels().size());
        FieldModel<?> idFieldModel = classModel.getIdFieldModel();
        assertNotNull(idFieldModel);
        assertEquals("customId", idFieldModel.getFieldName());
        assertEquals("_id", idFieldModel.getDocumentFieldName());

        FieldModel<?> childFieldModel = classModel.getFieldModel("child");
        assertNotNull(childFieldModel);
        assertTrue(childFieldModel.useDiscriminator());
    }

    @Test
    public void testClassAndFieldConventionDoesNotOverwrite() {
        ClassModelBuilder<SimpleModel> builder = new ClassModelBuilder<SimpleModel>()
                .type(SimpleModel.class)
                .discriminatorEnabled(true)
                .discriminatorKey("_cls")
                .discriminator("Simples")
                .conventions(singletonList(CLASS_AND_FIELD_CONVENTION))
                .classAccessorFactory(new ClassAccessorFactoryImpl<SimpleModel>(Collections.<Constructor<SimpleModel>>emptyList(), null));

        FieldModelBuilder<Integer> fieldModelBuilder = new FieldModelBuilder<Integer>()
                .typeData(TypeData.builder(Integer.class).build())
                .fieldName("id")
                .fieldModelSerialization(new FieldModelSerializationImpl<Integer>());

        FieldModelBuilder<Integer> fieldModelBuilder2 = new FieldModelBuilder<Integer>()
                .typeData(TypeData.builder(Integer.class).build())
                .fieldName("customId")
                .documentFieldName("_id")
                .fieldModelSerialization(new FieldModelSerializationImpl<Integer>());

        ClassModel<SimpleModel> classModel  = builder.idField("customId").addField(fieldModelBuilder).addField(fieldModelBuilder2).build();

        assertTrue(classModel.useDiscriminator());
        assertEquals("_cls", classModel.getDiscriminatorKey());
        assertEquals("Simples", classModel.getDiscriminator());

        assertEquals(2, classModel.getFieldModels().size());
        FieldModel<?> idFieldModel = classModel.getIdFieldModel();
        assertEquals("customId", idFieldModel.getFieldName());
        assertEquals("_id", idFieldModel.getDocumentFieldName());

        FieldModel<?> childFieldModel = classModel.getFieldModel("id");
        assertTrue(childFieldModel.useDiscriminator());
    }
}
