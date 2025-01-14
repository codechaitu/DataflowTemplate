package com.mercari.solution.util.schema;

import com.mercari.solution.TestDatum;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AvroSchemaUtilTest {

    @Test
    public void testSelectFields() {
        final GenericRecord record = TestDatum.generateRecord();
        final List<String> fields = Arrays.asList(
                "stringField", "intField", "longField",
                "recordField.stringField", "recordField.doubleField", "recordField.booleanField",
                "recordField.recordField.intField", "recordField.recordField.floatField",
                "recordField.recordArrayField.intField", "recordField.recordArrayField.floatField",
                "recordArrayField.stringField", "recordArrayField.timestampField",
                "recordArrayField.recordField.intField", "recordArrayField.recordField.floatField",
                "recordArrayField.recordArrayField.intField", "recordArrayField.recordArrayField.floatField");
        final Schema schema = AvroSchemaUtil.selectFields(record.getSchema(), fields);

        // schema test
        Assert.assertEquals(5, schema.getFields().size());
        Assert.assertNotNull(schema.getField("stringField"));
        Assert.assertNotNull(schema.getField("intField"));
        Assert.assertNotNull(schema.getField("longField"));
        Assert.assertNotNull(schema.getField("recordField"));
        Assert.assertNotNull(schema.getField("recordArrayField"));

        final Schema schemaChild = AvroSchemaUtil.unnestUnion(schema.getField("recordField").schema());
        Assert.assertEquals(5, schemaChild.getFields().size());
        Assert.assertNotNull(schemaChild.getField("stringField"));
        Assert.assertNotNull(schemaChild.getField("doubleField"));
        Assert.assertNotNull(schemaChild.getField("booleanField"));
        Assert.assertNotNull(schemaChild.getField("recordField"));
        Assert.assertNotNull(schemaChild.getField("recordArrayField"));

        Assert.assertEquals(Schema.Type.ARRAY, AvroSchemaUtil.unnestUnion(schemaChild.getField("recordArrayField").schema()).getType());
        final Schema schemaChildChildren = AvroSchemaUtil.unnestUnion(AvroSchemaUtil.unnestUnion(schemaChild.getField("recordArrayField").schema()).getElementType());
        Assert.assertEquals(2, schemaChildChildren.getFields().size());
        Assert.assertNotNull(schemaChildChildren.getField("intField"));
        Assert.assertNotNull(schemaChildChildren.getField("floatField"));

        final Schema schemaGrandchild = AvroSchemaUtil.unnestUnion(schemaChild.getField("recordField").schema());
        Assert.assertEquals(2, schemaGrandchild.getFields().size());
        Assert.assertNotNull(schemaGrandchild.getField("intField"));
        Assert.assertNotNull(schemaGrandchild.getField("floatField"));

        Assert.assertEquals(Schema.Type.ARRAY, AvroSchemaUtil.unnestUnion(schema.getField("recordArrayField").schema()).getType());
        final Schema schemaChildren = AvroSchemaUtil.unnestUnion(AvroSchemaUtil.unnestUnion(schema.getField("recordArrayField").schema()).getElementType());
        Assert.assertEquals(4, schemaChildren.getFields().size());
        Assert.assertNotNull(schemaChildren.getField("stringField"));
        Assert.assertNotNull(schemaChildren.getField("timestampField"));
        Assert.assertNotNull(schemaChildren.getField("recordField"));
        Assert.assertNotNull(schemaChildren.getField("recordArrayField"));

        final Schema schemaChildrenChild = AvroSchemaUtil.unnestUnion(schemaChildren.getField("recordField").schema());
        Assert.assertEquals(2, schemaChildrenChild.getFields().size());
        Assert.assertNotNull(schemaChildrenChild.getField("intField"));
        Assert.assertNotNull(schemaChildrenChild.getField("floatField"));

        Assert.assertEquals(Schema.Type.ARRAY, AvroSchemaUtil.unnestUnion(schemaChildren.getField("recordArrayField").schema()).getType());
        final Schema schemaChildrenChildren = AvroSchemaUtil.unnestUnion(AvroSchemaUtil.unnestUnion(schemaChildren.getField("recordArrayField").schema()).getElementType());
        Assert.assertEquals(2, schemaChildrenChildren.getFields().size());
        Assert.assertNotNull(schemaChildrenChildren.getField("intField"));
        Assert.assertNotNull(schemaChildrenChildren.getField("floatField"));


        // record test
        final GenericRecord selectedRecord = AvroSchemaUtil.toBuilder(schema, record).build();
        Assert.assertEquals(5, selectedRecord.getSchema().getFields().size());
        Assert.assertEquals(TestDatum.getStringFieldValue(), selectedRecord.get("stringField"));
        Assert.assertEquals(TestDatum.getIntFieldValue(), selectedRecord.get("intField"));
        Assert.assertEquals(TestDatum.getLongFieldValue(), selectedRecord.get("longField"));

        final GenericRecord selectedRecordChild = (GenericRecord) selectedRecord.get("recordField");
        Assert.assertEquals(5, selectedRecordChild.getSchema().getFields().size());
        Assert.assertEquals(TestDatum.getStringFieldValue(), selectedRecordChild.get("stringField"));
        Assert.assertEquals(TestDatum.getDoubleFieldValue(), selectedRecordChild.get("doubleField"));
        Assert.assertEquals(TestDatum.getBooleanFieldValue(), selectedRecordChild.get("booleanField"));

        final GenericRecord selectedRecordGrandchild = (GenericRecord) selectedRecordChild.get("recordField");
        Assert.assertEquals(2, selectedRecordGrandchild.getSchema().getFields().size());
        Assert.assertEquals(TestDatum.getIntFieldValue(), selectedRecordGrandchild.get("intField"));
        Assert.assertEquals(TestDatum.getFloatFieldValue(), selectedRecordGrandchild.get("floatField"));

        Assert.assertEquals(2, ((List)selectedRecord.get("recordArrayField")).size());
        for(final GenericRecord child : (List<GenericRecord>)selectedRecord.get("recordArrayField")) {
            Assert.assertEquals(4, child.getSchema().getFields().size());
            Assert.assertEquals(TestDatum.getStringFieldValue(), child.get("stringField"));
            Assert.assertEquals(TestDatum.getTimestampFieldValue(), Instant.ofEpochMilli((Long)child.get("timestampField") / 1000));

            Assert.assertEquals(2, ((List)child.get("recordArrayField")).size());
            for(final GenericRecord grandchild : (List<GenericRecord>)child.get("recordArrayField")) {
                Assert.assertEquals(2, grandchild.getSchema().getFields().size());
                Assert.assertEquals(TestDatum.getIntFieldValue(), grandchild.get("intField"));
                Assert.assertEquals(TestDatum.getFloatFieldValue(), grandchild.get("floatField"));
            }

            final GenericRecord grandchild = (GenericRecord) child.get("recordField");
            Assert.assertEquals(TestDatum.getIntFieldValue(), grandchild.get("intField"));
            Assert.assertEquals(TestDatum.getFloatFieldValue(), grandchild.get("floatField"));
        }


        // null fields record test
        final GenericRecord recordNull = TestDatum.generateRecordNull();
        final List<String> newFields = new ArrayList<>(fields);
        newFields.add("recordFieldNull");
        newFields.add("recordArrayFieldNull");
        final Schema schemaNull = AvroSchemaUtil.selectFields(recordNull.getSchema(), newFields);

        final GenericRecord selectedRecordNull = AvroSchemaUtil.toBuilder(schemaNull, recordNull).build();
        Assert.assertEquals(7, selectedRecordNull.getSchema().getFields().size());
        Assert.assertNull(selectedRecordNull.get("stringField"));
        Assert.assertNull(selectedRecordNull.get("intField"));
        Assert.assertNull(selectedRecordNull.get("longField"));
        Assert.assertNull(selectedRecordNull.get("recordFieldNull"));
        Assert.assertNull(selectedRecordNull.get("recordArrayFieldNull"));

        final GenericRecord selectedRecordChildNull = (GenericRecord) selectedRecordNull.get("recordField");
        Assert.assertEquals(5, selectedRecordChildNull.getSchema().getFields().size());
        Assert.assertNull(selectedRecordChildNull.get("stringField"));
        Assert.assertNull(selectedRecordChildNull.get("doubleField"));
        Assert.assertNull(selectedRecordChildNull.get("booleanField"));

        final GenericRecord selectedRecordGrandchildNull = (GenericRecord) selectedRecordChildNull.get("recordField");
        Assert.assertEquals(2, selectedRecordGrandchildNull.getSchema().getFields().size());
        Assert.assertNull(selectedRecordGrandchildNull.get("intField"));
        Assert.assertNull(selectedRecordGrandchildNull.get("floatField"));

        Assert.assertEquals(2, ((List)selectedRecordNull.get("recordArrayField")).size());
        for(final GenericRecord child : (List<GenericRecord>)selectedRecordNull.get("recordArrayField")) {
            Assert.assertEquals(4, child.getSchema().getFields().size());
            Assert.assertNull(child.get("stringField"));
            Assert.assertNull(child.get("timestampField"));

            Assert.assertEquals(2, ((List)child.get("recordArrayField")).size());
            for(final GenericRecord grandchild : (List<GenericRecord>)child.get("recordArrayField")) {
                Assert.assertEquals(2, grandchild.getSchema().getFields().size());
                Assert.assertNull(grandchild.get("intField"));
                Assert.assertNull(grandchild.get("floatField"));
            }

            final GenericRecord grandchild = (GenericRecord) child.get("recordField");
            Assert.assertNull(grandchild.get("intField"));
            Assert.assertNull(grandchild.get("floatField"));
        }
    }

}
