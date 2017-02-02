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

import org.bson.BsonType;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.Codec;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.entities.SimpleGenericsModel;
import org.bson.codecs.pojo.entities.SimpleModel;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.pojo.Conventions.NO_CONVENTIONS;
import static org.junit.Assert.assertNull;

public final class PojoCodecProviderTest extends PojoTestCase {

    @Test
    public void testClassNotFound() {
        PojoCodecProvider provider = PojoCodecProvider.builder().build();
        CodecRegistry registry = fromProviders(provider, new ValueCodecProvider());
        Codec<SimpleModel> codec = provider.get(SimpleModel.class, registry);
        assertNull(codec);
    }

    @Test
    public void testRegisterClass() {
        PojoCodecProvider provider = PojoCodecProvider.builder().register(SimpleModel.class).build();
        CodecRegistry registry = fromProviders(provider, new ValueCodecProvider());

        SimpleModel model = getSimpleModel();
        roundTrip(registry, model, "{'_t': 'SimpleModel', 'integerField': 42, 'stringField': 'myString'}");
    }

    @Test
    public void testRegisterPackage() {
        PojoCodecProvider provider = PojoCodecProvider.builder().register("org.bson.codecs.pojo.entities").build();
        CodecRegistry registry = fromProviders(provider, new ValueCodecProvider());

        roundTrip(registry, getSimpleModel(), "{'_t': 'SimpleModel', 'integerField': 42, 'stringField': 'myString'}");
        roundTrip(registry, getConventionModel(), "{'_id': 'id', '_cls': 'AnnotatedConventionModel', 'myFinalField': 10,"
                + "'myIntField': 10, 'child': {'_id': 'child', 'myFinalField': 10, 'myIntField': 10,"
                + "                            'model': {'integerField': 42, 'stringField': 'myString'}}}");
    }

    @Test
    public void testConventions() {
        PojoCodecProvider provider = PojoCodecProvider.builder().conventions(NO_CONVENTIONS)
                .register("org.bson.codecs.pojo.entities").build();
        CodecRegistry registry = fromProviders(provider, new ValueCodecProvider());

        roundTrip(registry, getConventionModel(),
                "{'myFinalField': 10, 'myIntField': 10, 'customId': 'id',"
                        + "'child': {'myFinalField': 10, 'myIntField': 10, 'customId': 'child',"
                        + "          'simpleModel': {'integerField': 42, 'stringField': 'myString' } } }");
    }

    @Test
    public void testBsonTypeMap() {
        HashMap<BsonType, Class<?>> replacements = new HashMap<BsonType, Class<?>>();
        replacements.put(BsonType.DOCUMENT, SimpleModel.class);
        BsonTypeClassMap bsonTypeClassMap = new BsonTypeClassMap(replacements);

        PojoCodecProvider provider = PojoCodecProvider.builder().bsonTypeClassMap(bsonTypeClassMap).register(SimpleGenericsModel.class)
                .register(SimpleModel.class).build();
        CodecRegistry registry = fromProviders(provider, new ValueCodecProvider());

        SimpleModel simpleModel = getSimpleModel();
        SimpleGenericsModel<SimpleModel, Integer, Integer> model = new SimpleGenericsModel<SimpleModel,  Integer, Integer>(42,
                simpleModel, Collections.<Integer>emptyList(), null);
        decodesTo(registry, "{'_t': 'SimpleGenericsModel', 'myIntegerField': 42, 'myGenericField': "
                + "{'integerField': 42, 'stringField': 'myString'}, 'myListField': []}", model);
    }

}
