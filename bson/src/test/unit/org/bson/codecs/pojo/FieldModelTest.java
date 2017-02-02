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

import org.bson.codecs.Codec;
import org.bson.codecs.IntegerCodec;
import org.bson.codecs.pojo.entities.SimpleGenericsModel;
import org.bson.codecs.pojo.entities.SimpleModel;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class FieldModelTest {

    @Test
    public void testSimpleModelInteger() throws NoSuchFieldException {
        String fieldName = "integerField";
        FieldModel<Integer> fieldModel = new FieldModelBuilder<Integer>(SimpleModel.class.getDeclaredField(fieldName)).build();

        assertEquals(fieldName, fieldModel.getFieldName());
        assertEquals(fieldName, fieldModel.getDocumentFieldName());
        assertEquals(Integer.class, fieldModel.getFieldType());
        assertTrue(fieldModel.getTypeParameters().isEmpty());
        assertNull(fieldModel.getCodec());
        assertTrue(fieldModel.useDiscriminator());
        assertTrue(fieldModel.shouldSerialize(1));
    }

    @Test
    public void testSimpleModelGeneric() throws NoSuchFieldException {
        FieldModel<Object> fieldModel = new FieldModelBuilder<Object>(SimpleGenericsModel.class.getDeclaredField("myGenericField")).build();

        assertEquals(Object.class, fieldModel.getFieldType());
        assertTrue(fieldModel.getTypeParameters().isEmpty());
        assertNull(fieldModel.getCodec());
        assertTrue(fieldModel.shouldSerialize(1));
        assertTrue(fieldModel.useDiscriminator());
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void testSimpleModelGenericList() throws NoSuchFieldException {
        FieldModel<?> fieldModel = new FieldModelBuilder(SimpleGenericsModel.class.getDeclaredField("myListField")).build();

        assertEquals(List.class, fieldModel.getFieldType());
        assertTrue(fieldModel.getTypeParameters().isEmpty());
    }

    @Test
    public void testConverter() throws NoSuchFieldException {
        String fieldName = "integerField";
        Codec<Integer> codec = new IntegerCodec();
        FieldModel<Integer> fieldModel = new FieldModelBuilder<Integer>(SimpleModel.class.getDeclaredField(fieldName))
                .codec(codec).build();
        assertEquals(codec, fieldModel.getCodec());
    }

}
