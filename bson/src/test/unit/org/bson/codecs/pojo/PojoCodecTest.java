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

import org.bson.codecs.ObjectCodecProvider;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.entities.CollectionNestedPojoModel;
import org.bson.codecs.pojo.entities.ConcreteCollectionsModel;
import org.bson.codecs.pojo.entities.ConstructorModel;
import org.bson.codecs.pojo.entities.ConventionModel;
import org.bson.codecs.pojo.entities.ConverterModel;
import org.bson.codecs.pojo.entities.GenericHolderModel;
import org.bson.codecs.pojo.entities.NestedGenericHolderMapModel;
import org.bson.codecs.pojo.entities.NestedGenericHolderModel;
import org.bson.codecs.pojo.entities.NestedGenericHolderSimpleGenericsModel;
import org.bson.codecs.pojo.entities.PrimitivesModel;
import org.bson.codecs.pojo.entities.ShapeHolderModel;
import org.bson.codecs.pojo.entities.ShapeModelAbstract;
import org.bson.codecs.pojo.entities.ShapeModelCircle;
import org.bson.codecs.pojo.entities.ShapeModelRectangle;
import org.bson.codecs.pojo.entities.SimpleGenericsModel;
import org.bson.codecs.pojo.entities.SimpleModel;
import org.bson.codecs.pojo.entities.SimpleNestedPojoModel;
import org.bson.types.ObjectId;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.pojo.Conventions.NO_CONVENTIONS;

public final class PojoCodecTest extends PojoTestCase {

    @Test
    public void testRoundTripSimpleModel() {
        SimpleModel model = getSimpleModel();
        roundTrip(getPojoCodecProviderBuilder(SimpleModel.class), model, SIMPLE_MODEL_JSON);
    }

    @Test
    public void testRoundTripPrimitivesModel() {
        PrimitivesModel model = getPrimitivesModel();
        roundTrip(getPojoCodecProviderBuilder(PrimitivesModel.class), model,
                "{ '_t': 'PrimitivesModel', 'myBoolean': true, 'myByte': 1, "
                        + "'myCharacter': '1', 'myDouble': 1.0, 'myFloat': 2.0, 'myInteger': 3, "
                        + "'myLong': { '$numberLong': '5' }, 'myShort': 6}");
    }

    @Test
    public void testRoundTripSimpleGenericsModelWithObjectCodec() {
        CodecRegistry registry = fromProviders(getPojoCodecProviderBuilder(SimpleGenericsModel.class).build(),
                new ValueCodecProvider(), new ObjectCodecProvider());
        SimpleGenericsModel<String, String, Integer> model = getSimpleGenericsModel();
        String json = "{'_t': 'SimpleGenericsModel', 'myIntegerField': 42, 'myGenericField': 'A'"
                + " 'myListField': ['B', 'C'], 'myMapField': {'D': 2, 'E': 3, 'F': 4}}";

        encodesTo(registry, model, json);
        decodesTo(registry, json, model);
    }

    @Test
    public void testRoundTripConcreteCollectionsModel() {
        ConcreteCollectionsModel  model = getConcreteCollectionsModel();
        roundTrip(getPojoCodecProviderBuilder(ConcreteCollectionsModel.class), model,
                "{'_t': 'ConcreteCollectionsModel', 'collection': [1, 2, 3],"
                        + "'list': [4, 5, 6], 'linked': [7, 8, 9], 'map': {'A': 1.1, 'B': 2.2, 'C': 3.3},"
                        + "'concurrent': {'D': 4.4, 'E': 5.5, 'F': 6.6}}");
    }

    @Test
    public void testRoundTripSimpleNestedPojoModel() {
        SimpleNestedPojoModel model = getSimpleNestedPojoModel();
        roundTrip(getPojoCodecProviderBuilder(SimpleNestedPojoModel.class, SimpleModel.class), model,
                "{'_t': 'SimpleNestedPojoModel', 'simple': " + SIMPLE_MODEL_JSON + "}");
    }

    @Test
    public void testRoundTripCollectionNestedPojoModel() {
        CollectionNestedPojoModel model = getCollectionNestedPojoModel();
        roundTrip(getPojoCodecProviderBuilder(CollectionNestedPojoModel.class, SimpleModel.class), model,
                "{'_t': 'CollectionNestedPojoModel',"
                        + "'listSimple': [" + SIMPLE_MODEL_JSON + "],"
                        + "'listListSimple': [[" + SIMPLE_MODEL_JSON + "]],"
                        + "'setSimple': [" + SIMPLE_MODEL_JSON + "],"
                        + "'setSetSimple': [[" + SIMPLE_MODEL_JSON + "]],"
                        + "'mapSimple': {'s': " + SIMPLE_MODEL_JSON + "},"
                        + "'mapMapSimple': {'ms': {'s': " + SIMPLE_MODEL_JSON + "}},"
                        + "'mapListSimple': {'ls': [" + SIMPLE_MODEL_JSON + "]},"
                        + "'mapListMapSimple': {'lm': [{'s': " + SIMPLE_MODEL_JSON + "}]},"
                        + "'mapSetSimple': {'s': [" + SIMPLE_MODEL_JSON + "]},"
                        + "'listMapSimple': [{'s': " + SIMPLE_MODEL_JSON + "}],"
                        + "'listMapListSimple': [{'ls': [" + SIMPLE_MODEL_JSON + "]}],"
                        + "'listMapSetSimple': [{'s': [" + SIMPLE_MODEL_JSON + "]}],"
                        + "}");
    }

    @Test
    public void testShapeModelAbstract() {
        PojoCodecProvider.Builder builder = getPojoCodecProviderBuilder(ShapeModelAbstract.class,
                ShapeModelCircle.class, ShapeModelRectangle.class, ShapeHolderModel.class);

        roundTrip(builder, new ShapeHolderModel(getShapeModelCirce()),
                "{'_t': 'ShapeHolderModel', 'shape': {'_t': 'ShapeModelCircle', 'color': 'orange', 'radius': 4.2}}");

        roundTrip(builder, new ShapeHolderModel(getShapeModelRectangle()),
                "{'_t': 'ShapeHolderModel', 'shape': {'_t': 'ShapeModelRectangle', 'color': 'green', 'width': 22.1, 'height': 105.0} }");
    }

    @Test
    public void testNestedGenericHolderModel() {
        PojoCodecProvider.Builder builder = getPojoCodecProviderBuilder(NestedGenericHolderModel.class, GenericHolderModel.class);
        roundTrip(builder, getNestedGenericHolderModel(),
                "{'_t': 'NestedGenericHolderModel', 'nested': {'_t': 'GenericHolderModel', 'myGenericField': 'generic',"
                        + "'myLongField': {'$numberLong': '1'}}}");
    }

    @Test
    public void testNestedGenericHolderMapModel() {
        PojoCodecProvider.Builder builder = getPojoCodecProviderBuilder(NestedGenericHolderMapModel.class,
                GenericHolderModel.class, SimpleGenericsModel.class, SimpleModel.class);
        roundTrip(builder, getNestedGenericHolderMapModel(),
                "{'_t': 'NestedGenericHolderMapModel', "
                        + "'nested': {'_t': 'GenericHolderModel', 'myGenericField': {'s': " + SIMPLE_MODEL_JSON + "}, "
                        + "           'myLongField': {'$numberLong': '1'}}}");
    }


    @Test
    public void testGenericsRoundTrip() {
        // Multiple levels of nesting
        SimpleModel simpleModel = getSimpleModel();
        Map<String, SimpleModel> map = new HashMap<String, SimpleModel>();
        map.put("A", simpleModel);
        Map<String, Map<String, SimpleModel>> mapB = new HashMap<String, Map<String, SimpleModel>>();
        mapB.put("A", map);
        SimpleGenericsModel<Integer, List<SimpleModel>, Map<String, SimpleModel>> simpleGenericsModel =
                new SimpleGenericsModel<Integer, List<SimpleModel>, Map<String, SimpleModel>>(42, 42,
                        singletonList(singletonList(simpleModel)), mapB);
        GenericHolderModel<SimpleGenericsModel<Integer, List<SimpleModel>, Map<String, SimpleModel>>> nested =
                new GenericHolderModel<SimpleGenericsModel<Integer, List<SimpleModel>, Map<String, SimpleModel>>>(simpleGenericsModel, 42L);

        NestedGenericHolderSimpleGenericsModel model = new NestedGenericHolderSimpleGenericsModel(nested);

        PojoCodecProvider.Builder builder = getPojoCodecProviderBuilder(NestedGenericHolderSimpleGenericsModel.class,
                GenericHolderModel.class, SimpleGenericsModel.class, SimpleModel.class);
        roundTrip(builder, model,
                "{'_t': 'NestedGenericHolderSimpleGenericsModel',"
                        + "'nested': {'_t': 'GenericHolderModel', "
                        + "           'myGenericField': {'_t': 'SimpleGenericsModel', 'myIntegerField': 42, 'myGenericField': 42,"
                        + "                              'myListField': [[" + SIMPLE_MODEL_JSON + "]], "
                        + "                              'myMapField': {'A': {'A': " + SIMPLE_MODEL_JSON + "}}},"
                        + "            'myLongField': {'$numberLong': '42' }}}");
    }

    @Test
    public void testConstructorModel() {
        ConstructorModel model = new ConstructorModel(99);
        roundTrip(getPojoCodecProviderBuilder(ConstructorModel.class), model,
                "{'_t': 'ConstructorModel', 'integerField': 99}");

        model = new ConstructorModel(99, "myString");
        roundTrip(getPojoCodecProviderBuilder(ConstructorModel.class), model,
                "{'_t': 'ConstructorModel', 'integerField': 99, 'stringField': 'myString'}");
    }

    @Test
    public void testConventionsDefault() {
        ConventionModel model = getConventionModel();
        roundTrip(getPojoCodecProviderBuilder(ConventionModel.class, SimpleModel.class), model,
                "{'_id': 'id', '_cls': 'AnnotatedConventionModel', 'myFinalField': 10, 'myIntField': 10, "
                        + "'child': {'_id': 'child', 'myFinalField': 10, 'myIntField': 10,"
                        + "'model': {'integerField': 42, 'stringField': 'myString'}}}");
    }

    @Test
    public void testConventionsEmpty() {
        ConventionModel model = getConventionModel();
        ClassModelBuilder<ConventionModel> classModel = new ClassModelBuilder<ConventionModel>(ConventionModel.class)
                .conventions(NO_CONVENTIONS);
        ClassModelBuilder<SimpleModel> nestedClassModel = new ClassModelBuilder<SimpleModel>(SimpleModel.class).conventions(NO_CONVENTIONS);

        roundTrip(getPojoCodecProviderBuilder(classModel, nestedClassModel), model,
                "{'myFinalField': 10, 'myIntField': 10, 'customId': 'id',"
                        + "'child': {'myFinalField': 10, 'myIntField': 10, 'customId': 'child',"
                        + "          'simpleModel': {'integerField': 42, 'stringField': 'myString' } } }");
    }

    @Test
    public void testConventionsCustom() {
        ConventionModel model = getConventionModel();

        List<Convention> conventions = Collections.<Convention>singletonList(
                new Convention() {
                    @Override
                    public void apply(final ClassModelBuilder<?> classModelBuilder) {
                        for (FieldModelBuilder<?> fieldModelBuilder : classModelBuilder.getFields()) {
                            fieldModelBuilder.documentFieldName(fieldModelBuilder.getFieldName().replaceAll("([^_A-Z])([A-Z])", "$1_$2")
                                    .toLowerCase());
                        }
                        if (classModelBuilder.getField("customId") != null) {
                            classModelBuilder.idField("customId");
                        }
                        classModelBuilder.discriminatorEnabled(true);
                        classModelBuilder.discriminatorKey("_cls");
                        classModelBuilder.discriminator(classModelBuilder.getType().getSimpleName()
                                .replaceAll("([^_A-Z])([A-Z])", "$1_$2").toLowerCase());
                    }
                });

        ClassModelBuilder<ConventionModel> classModel = new ClassModelBuilder<ConventionModel>(ConventionModel.class)
                .conventions(conventions);
        ClassModelBuilder<SimpleModel> nestedClassModel = new ClassModelBuilder<SimpleModel>(SimpleModel.class).conventions(conventions);

        roundTrip(getPojoCodecProviderBuilder(classModel, nestedClassModel), model,
                "{ '_id': 'id', '_cls': 'convention_model', 'my_final_field': 10, 'my_int_field': 10,"
                        + "'child': { '_id': 'child', '_cls': 'convention_model', 'my_final_field': 10, 'my_int_field': 10, "
                        + "           'simple_model': { '_cls': 'simple_model', 'integer_field': 42, 'string_field': 'myString' } } }");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCustomCodec() {
        ObjectId id = new ObjectId();
        ConverterModel model = new ConverterModel(id.toHexString(), "myName");

        ClassModelBuilder<ConverterModel> classModel = new ClassModelBuilder<ConverterModel>(ConverterModel.class);
        FieldModelBuilder<String> idFieldModelBuilder = (FieldModelBuilder<String>) classModel.getField("id");
        idFieldModelBuilder.codec(new StringToObjectIdCodec());

        roundTrip(getPojoCodecProviderBuilder(classModel), model,
                format("{'_id': {'$oid': '%s'}, '_t': 'ConverterModel', 'name': 'myName'}", id.toHexString()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCustomFieldSerializer() {
        SimpleModel model = getSimpleModel();
        model.setIntegerField(null);
        ClassModelBuilder<SimpleModel> classModel = new ClassModelBuilder<SimpleModel>(SimpleModel.class);
        ((FieldModelBuilder<Integer>) classModel.getField("integerField"))
                .fieldModelSerialization(new FieldModelSerialization<Integer>() {
                    @Override
                    public boolean shouldSerialize(final Integer value) {
                        return true;
                    }
                });

        roundTrip(getPojoCodecProviderBuilder(classModel), model, "{'_t': 'SimpleModel', 'integerField': null, 'stringField': 'myString'}");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCanHandleNullValuesForNestedModels() {
        SimpleNestedPojoModel model = getSimpleNestedPojoModel();
        model.setSimple(null);
        ClassModelBuilder<SimpleNestedPojoModel> classModel = new ClassModelBuilder<SimpleNestedPojoModel>(SimpleNestedPojoModel.class);
        ((FieldModelBuilder<SimpleModel>) classModel.getField("simple"))
                .fieldModelSerialization(new FieldModelSerialization<SimpleModel>() {
                    @Override
                    public boolean shouldSerialize(final SimpleModel value) {
                        return true;
                    }
                });
        ClassModelBuilder<SimpleModel> classModelSimple = new ClassModelBuilder<SimpleModel>(SimpleModel.class);

        roundTrip(getPojoCodecProviderBuilder(classModel, classModelSimple), model, "{'_t': 'SimpleNestedPojoModel', 'simple': null}");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCanHandleNullValuesForCollectionsAndMaps() {
        ConcreteCollectionsModel model = getConcreteCollectionsModel();
        model.setCollection(null);
        model.setMap(null);

        ClassModelBuilder<ConcreteCollectionsModel> classModel =
                new ClassModelBuilder<ConcreteCollectionsModel>(ConcreteCollectionsModel.class);
        ((FieldModelBuilder<Collection<Integer>>) classModel.getField("collection"))
                .fieldModelSerialization(new FieldModelSerialization<Collection<Integer>>() {
                    @Override
                    public boolean shouldSerialize(final Collection<Integer> value) {
                        return true;
                    }
                });
        ((FieldModelBuilder<Map<String, Double>>) classModel.getField("map"))
                .fieldModelSerialization(new FieldModelSerialization<Map<String, Double>>() {
                    @Override
                    public boolean shouldSerialize(final Map<String, Double> value) {
                        return true;
                    }
                });

        roundTrip(getPojoCodecProviderBuilder(classModel), model,
                "{'_t': 'ConcreteCollectionsModel', 'collection': null,"
                        + "'list': [4, 5, 6], 'linked': [7, 8, 9], 'map': null,"
                        + "'concurrent': {'D': 4.4, 'E': 5.5, 'F': 6.6}}");
    }

    @Test
    public void testCanHandleExtraData() {
        SimpleModel model = getSimpleModel();
        decodesTo(getCodec(SimpleModel.class), "{'_t': 'SimpleModel', 'integerField': 42, "
                + "'stringField': 'myString', 'extraFieldA': 1, 'extraFieldB': 2}", model);
    }

    @Test
    public void testConstructorModelCanHandleExtraData() {
        ConstructorModel model = new ConstructorModel(99);
        decodesTo(getCodec(ConstructorModel.class), "{'_t': 'ConstructorModel', 'integerField': 99, "
                + "'extraFieldA': 1, 'extraFieldB': 2}", model);

        model = new ConstructorModel(99, "myString");
        decodesTo(getCodec(ConstructorModel.class), "{'_t': 'ConstructorModel', 'integerField': 99, "
                + "'stringField': 'myString', 'extraFieldA': 1, 'extraFieldB': 2}", model);
    }

    @Test
    public void testDataCanHandleMissingData() {
        SimpleModel model = getSimpleModel();
        model.setIntegerField(null);

        decodesTo(getCodec(SimpleModel.class), "{'_t': 'SimpleModel', 'stringField': 'myString'}", model);
    }

    @Test(expected = CodecConfigurationException.class)
    public void testDataUnknownClass() {
        decodingShouldFail(getCodec(SimpleModel.class), "{'_t': 'FakeModel'}");
    }

    @Test(expected = CodecConfigurationException.class)
    public void testInvalidTypeForField() {
        decodingShouldFail(getCodec(SimpleModel.class), "{'_t': 'SimpleModel', 'stringField': 123}");
    }

    @Test(expected = CodecConfigurationException.class)
    public void testInvalidTypeForPrimitiveField() {
        decodingShouldFail(getCodec(PrimitivesModel.class), "{ '_t': 'PrimitivesModel', 'myBoolean': null}");
    }

    @Test(expected = CodecConfigurationException.class)
    public void testInvalidTypeForModelField() {
        decodingShouldFail(getCodec(SimpleNestedPojoModel.class), "{ '_t': 'SimpleNestedPojoModel', 'simple': 123}");
    }

    @Test(expected = CodecConfigurationException.class)
    public void testInvalidDiscriminatorInNestedModel() {
        decodingShouldFail(getCodec(SimpleNestedPojoModel.class), "{ '_t': 'SimpleNestedPojoModel',"
                + "'simple': {'_t': 'FakeModel', 'integerField': 42, 'stringField': 'myString'}}");
    }

}
