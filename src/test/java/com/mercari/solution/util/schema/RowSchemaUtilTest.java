package com.mercari.solution.util.schema;

import com.google.cloud.ByteArray;
import com.mercari.solution.TestDatum;
import org.apache.beam.sdk.extensions.sql.impl.utils.CalciteUtils;
import org.apache.beam.sdk.schemas.Schema;
import org.apache.beam.sdk.schemas.logicaltypes.EnumerationType;
import org.apache.beam.sdk.values.Row;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class RowSchemaUtilTest {

    @Test
    public void testToBuilder() {

        final Schema schema = Schema.builder()
                .addField("booleanField", Schema.FieldType.BOOLEAN.withNullable(true))
                .addField("stringField", Schema.FieldType.STRING.withNullable(true))
                .addField("bytesField", Schema.FieldType.BYTES.withNullable(true))
                .addField("intField", Schema.FieldType.INT32.withNullable(true))
                .addField("longField", Schema.FieldType.INT64.withNullable(true))
                .addField("floatField", Schema.FieldType.FLOAT.withNullable(true))
                .addField("doubleField", Schema.FieldType.DOUBLE.withNullable(true))
                .addField("datetimeField", Schema.FieldType.DATETIME.withNullable(true))
                .addField("enumField", Schema.FieldType.logicalType(EnumerationType.create("a", "b", "c")).withNullable(true))
                .addField("dateField", Schema.FieldType.logicalType(CalciteUtils.DATE.getLogicalType()).withNullable(true))
                .addField("timeField", Schema.FieldType.logicalType(CalciteUtils.TIME.getLogicalType()).withNullable(true))
                .addField(Schema.Field.of("booleanArrayField", Schema.FieldType.array(Schema.FieldType.BOOLEAN).withNullable(true)))
                .addField(Schema.Field.of("stringArrayField", Schema.FieldType.array(Schema.FieldType.STRING).withNullable(true)))
                .addField(Schema.Field.of("bytesArrayField", Schema.FieldType.array(Schema.FieldType.BYTES).withNullable(true)))
                .addField(Schema.Field.of("intArrayField", Schema.FieldType.array(Schema.FieldType.INT32).withNullable(true)))
                .addField(Schema.Field.of("longArrayField", Schema.FieldType.array(Schema.FieldType.INT64).withNullable(true)))
                .addField(Schema.Field.of("floatArrayField", Schema.FieldType.array(Schema.FieldType.FLOAT).withNullable(true)))
                .addField(Schema.Field.of("doubleArrayField", Schema.FieldType.array(Schema.FieldType.DOUBLE).withNullable(true)))
                .addField(Schema.Field.of("datetimeArrayField", Schema.FieldType.array(Schema.FieldType.DATETIME).withNullable(true)))
                .addField(Schema.Field.of("enumArrayField", Schema.FieldType.array(Schema.FieldType.logicalType(EnumerationType.create("a", "b", "c"))).withNullable(true)))
                .addField(Schema.Field.of("dateArrayField", Schema.FieldType.logicalType(CalciteUtils.DATE.getLogicalType())).withNullable(true))
                .addField(Schema.Field.of("timeArrayField", Schema.FieldType.logicalType(CalciteUtils.TIME.getLogicalType())).withNullable(true))
                .build();

        final List<String> fieldNames = schema
                .getFields().stream()
                .map(Schema.Field::getName)
                .collect(Collectors.toList());
        final List<String> fieldNamesNew = RowSchemaUtil.toBuilder(schema).build()
                .getFields().stream()
                .map(Schema.Field::getName)
                .collect(Collectors.toList());
        Assert.assertEquals(fieldNames, fieldNamesNew);

        final List<String> fieldNamesNewFilter = RowSchemaUtil.toBuilder(schema, Arrays.asList("doubleField", "timeArrayField")).build()
                .getFields().stream()
                .map(Schema.Field::getName)
                .collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList("doubleField", "timeArrayField"), fieldNamesNewFilter);


        final Row row = Row.withSchema(schema)
                .withFieldValue("booleanField", true)
                .withFieldValue("stringField", "stringValue")
                .withFieldValue("bytesField", ByteArray.copyFrom("Hello").toByteArray())
                .withFieldValue("intField", -123)
                .withFieldValue("longField", 123421L)
                .withFieldValue("floatField", 0.12f)
                .withFieldValue("doubleField", 1.55)
                .withFieldValue("datetimeField", Instant.parse("2021-03-02T00:00:00Z"))
                .withFieldValue("dateField", LocalDate.of(1990, 12, 31))
                .withFieldValue("timeField", LocalTime.of(15, 24, 1, 123456789))
                .withFieldValue("enumField", new EnumerationType.Value(0))
                .withFieldValue("booleanArrayField", Arrays.asList(true, false, true))
                .withFieldValue("stringArrayField", Arrays.asList("S", "D", "K"))
                .build();

        final Row.FieldValueBuilder builder = RowSchemaUtil.toBuilder(row);
        final Row newRow = builder.build();
        Assert.assertEquals(row.getBoolean("booleanField"), newRow.getBoolean("booleanField"));
        Assert.assertEquals(row.getString("stringField"), newRow.getString("stringField"));
        Assert.assertEquals(row.getBytes("bytesField"), newRow.getBytes("bytesField"));
        Assert.assertEquals(row.getInt32("intField"), newRow.getInt32("intField"));
        Assert.assertEquals(row.getInt64("longField"), newRow.getInt64("longField"));
        Assert.assertEquals(row.getFloat("floatField"), newRow.getFloat("floatField"));
        Assert.assertEquals(row.getDouble("doubleField"), newRow.getDouble("doubleField"));
        Assert.assertEquals(row.getDateTime("datetimeField"), newRow.getDateTime("datetimeField"));
        Assert.assertEquals(
                row.getLogicalTypeValue("dateField", LocalDate.class),
                newRow.getLogicalTypeValue("dateField", LocalDate.class));
        Assert.assertEquals(
                row.getLogicalTypeValue("timeField", LocalTime.class),
                newRow.getLogicalTypeValue("timeField", LocalTime.class));
        Assert.assertEquals(
                row.getLogicalTypeValue("enumField", EnumerationType.Value.class),
                newRow.getLogicalTypeValue("enumField", EnumerationType.Value.class));

        Assert.assertEquals(row.getArray("booleanArrayField"), newRow.getArray("booleanArrayField"));
        Assert.assertEquals(row.getArray("stringArrayField"), newRow.getArray("stringArrayField"));
    }

    @Test
    public void testSelectFields() {
        final Row row = TestDatum.generateRow();
        final List<String> fields = Arrays.asList(
                "stringField", "intField", "longField",
                "recordField.stringField", "recordField.doubleField", "recordField.booleanField",
                "recordField.recordField.intField", "recordField.recordField.floatField",
                "recordField.recordArrayField.intField", "recordField.recordArrayField.floatField",
                "recordArrayField.stringField", "recordArrayField.timestampField",
                "recordArrayField.recordField.intField", "recordArrayField.recordField.floatField",
                "recordArrayField.recordArrayField.intField", "recordArrayField.recordArrayField.floatField");

        final Schema schema = RowSchemaUtil.selectFields(row.getSchema(), fields);

        // schema test
        Assert.assertEquals(5, schema.getFieldCount());
        Assert.assertTrue(schema.hasField("stringField"));
        Assert.assertTrue(schema.hasField("intField"));
        Assert.assertTrue(schema.hasField("longField"));
        Assert.assertTrue(schema.hasField("recordField"));
        Assert.assertTrue(schema.hasField("recordArrayField"));

        final Schema schemaChild = schema.getField("recordField").getType().getRowSchema();
        Assert.assertEquals(5, schemaChild.getFieldCount());
        Assert.assertTrue(schemaChild.hasField("stringField"));
        Assert.assertTrue(schemaChild.hasField("doubleField"));
        Assert.assertTrue(schemaChild.hasField("booleanField"));
        Assert.assertTrue(schemaChild.hasField("recordField"));
        Assert.assertTrue(schemaChild.hasField("recordArrayField"));

        Assert.assertEquals(Schema.TypeName.ARRAY, schemaChild.getField("recordArrayField").getType().getTypeName());
        final Schema schemaChildChildren = schemaChild.getField("recordArrayField").getType().getCollectionElementType().getRowSchema();
        Assert.assertEquals(2, schemaChildChildren.getFieldCount());
        Assert.assertTrue(schemaChildChildren.hasField("intField"));
        Assert.assertTrue(schemaChildChildren.hasField("floatField"));

        final Schema schemaGrandchild = schemaChild.getField("recordField").getType().getRowSchema();
        Assert.assertEquals(2, schemaGrandchild.getFieldCount());
        Assert.assertTrue(schemaGrandchild.hasField("intField"));
        Assert.assertTrue(schemaGrandchild.hasField("floatField"));

        Assert.assertEquals(Schema.TypeName.ARRAY, schema.getField("recordArrayField").getType().getTypeName());
        final Schema schemaChildren = schema.getField("recordArrayField").getType().getCollectionElementType().getRowSchema();
        Assert.assertEquals(4, schemaChildren.getFieldCount());
        Assert.assertTrue(schemaChildren.hasField("stringField"));
        Assert.assertTrue(schemaChildren.hasField("timestampField"));
        Assert.assertTrue(schemaChildren.hasField("recordField"));
        Assert.assertTrue(schemaChildren.hasField("recordArrayField"));

        final Schema schemaChildrenChild = schemaChildren.getField("recordField").getType().getRowSchema();
        Assert.assertEquals(2, schemaChildrenChild.getFieldCount());
        Assert.assertTrue(schemaChildrenChild.hasField("intField"));
        Assert.assertTrue(schemaChildrenChild.hasField("floatField"));

        Assert.assertEquals(Schema.TypeName.ARRAY, schemaChildren.getField("recordArrayField").getType().getTypeName());
        final Schema schemaChildrenChildren = schemaChildren.getField("recordArrayField").getType().getCollectionElementType().getRowSchema();
        Assert.assertEquals(2, schemaChildrenChildren.getFieldCount());
        Assert.assertTrue(schemaChildrenChildren.hasField("intField"));
        Assert.assertTrue(schemaChildrenChildren.hasField("floatField"));

        // row test
        final Row selectedRow = RowSchemaUtil.toBuilder(schema, row).build();
        Assert.assertEquals(5, selectedRow.getFieldCount());
        Assert.assertEquals(TestDatum.getStringFieldValue(), selectedRow.getString("stringField"));
        Assert.assertEquals(TestDatum.getIntFieldValue(), selectedRow.getInt32("intField"));
        Assert.assertEquals(TestDatum.getLongFieldValue(), selectedRow.getInt64("longField"));

        final Row selectedRowChild = selectedRow.getRow("recordField");
        Assert.assertEquals(5, selectedRowChild.getFieldCount());
        Assert.assertEquals(TestDatum.getStringFieldValue(), selectedRowChild.getString("stringField"));
        Assert.assertEquals(TestDatum.getDoubleFieldValue(), selectedRowChild.getDouble("doubleField"));
        Assert.assertEquals(TestDatum.getBooleanFieldValue(), selectedRowChild.getBoolean("booleanField"));

        final Row selectedRowGrandchild = selectedRowChild.getRow("recordField");
        Assert.assertEquals(2, selectedRowGrandchild.getFieldCount());
        Assert.assertEquals(TestDatum.getIntFieldValue(), selectedRowGrandchild.getInt32("intField"));
        Assert.assertEquals(TestDatum.getFloatFieldValue(), selectedRowGrandchild.getFloat("floatField"));

        Assert.assertEquals(2, selectedRow.getArray("recordArrayField").size());
        for(final Row child : selectedRow.<Row>getArray("recordArrayField")) {
            Assert.assertEquals(4, child.getFieldCount());
            Assert.assertEquals(TestDatum.getStringFieldValue(), child.getString("stringField"));
            Assert.assertEquals(TestDatum.getTimestampFieldValue(), child.getDateTime("timestampField"));

            Assert.assertEquals(2, child.getArray("recordArrayField").size());
            for(final Row grandchild : child.<Row>getArray("recordArrayField")) {
                Assert.assertEquals(2, grandchild.getFieldCount());
                Assert.assertEquals(TestDatum.getIntFieldValue(), grandchild.getInt32("intField"));
                Assert.assertEquals(TestDatum.getFloatFieldValue(), grandchild.getFloat("floatField"));
            }

            final Row grandchild = child.getRow("recordField");
            Assert.assertEquals(TestDatum.getIntFieldValue(), grandchild.getInt32("intField"));
            Assert.assertEquals(TestDatum.getFloatFieldValue(), grandchild.getFloat("floatField"));
        }

        // null fields row test
        final Row rowNull = TestDatum.generateRowNull();
        final List<String> newFields = new ArrayList<>(fields);
        newFields.add("recordFieldNull");
        newFields.add("recordArrayFieldNull");
        final Schema schemaNull = RowSchemaUtil.selectFields(rowNull.getSchema(), newFields);

        final Row selectedRowNull = RowSchemaUtil.toBuilder(schemaNull, rowNull).build();
        Assert.assertEquals(7, selectedRowNull.getFieldCount());
        Assert.assertNull(selectedRowNull.getString("stringField"));
        Assert.assertNull(selectedRowNull.getInt32("intField"));
        Assert.assertNull(selectedRowNull.getInt64("longField"));
        Assert.assertNull(selectedRowNull.getInt64("recordFieldNull"));
        Assert.assertNull(selectedRowNull.getInt64("recordArrayFieldNull"));

        final Row selectedRowChildNull = selectedRowNull.getRow("recordField");
        Assert.assertEquals(5, selectedRowChildNull.getFieldCount());
        Assert.assertNull(selectedRowChildNull.getString("stringField"));
        Assert.assertNull(selectedRowChildNull.getDouble("doubleField"));
        Assert.assertNull(selectedRowChildNull.getBoolean("booleanField"));

        final Row selectedRowGrandchildNull = selectedRowChildNull.getRow("recordField");
        Assert.assertEquals(2, selectedRowGrandchildNull.getFieldCount());
        Assert.assertNull(selectedRowGrandchildNull.getInt32("intField"));
        Assert.assertNull(selectedRowGrandchildNull.getFloat("floatField"));

        Assert.assertEquals(2, selectedRowNull.getArray("recordArrayField").size());
        for(final Row child : selectedRowNull.<Row>getArray("recordArrayField")) {
            Assert.assertEquals(4, child.getFieldCount());
            Assert.assertNull(child.getString("stringField"));
            Assert.assertNull(child.getDateTime("timestampField"));

            Assert.assertEquals(2, child.getArray("recordArrayField").size());
            for(final Row grandchild : child.<Row>getArray("recordArrayField")) {
                Assert.assertEquals(2, grandchild.getFieldCount());
                Assert.assertNull(grandchild.getInt32("intField"));
                Assert.assertNull(grandchild.getFloat("floatField"));
            }

            final Row grandchild = child.getRow("recordField");
            Assert.assertNull(grandchild.getInt32("intField"));
            Assert.assertNull(grandchild.getFloat("floatField"));
        }

    }

    @Test
    public void testFlattenFields() {
        final Schema grandchildSchema = Schema.builder()
                .addStringField("gcstringField")
                .build();
        final Schema childSchema = Schema.builder()
                .addStringField("cstringField")
                .addRowField("grandchild", grandchildSchema)
                .addArrayField("grandchildren", Schema.FieldType.row(grandchildSchema))
                .build();
        final Schema schema = Schema.builder()
                .addStringField("stringField")
                .addArrayField("children", Schema.FieldType.row(childSchema))
                .build();

        final Schema resultSchema1 = RowSchemaUtil.flatten(schema, "children", true);
        Set<String> fieldNames1 = resultSchema1.getFields().stream()
                .map(Schema.Field::getName)
                .collect(Collectors.toSet());
        Assert.assertEquals(
                Set.of("stringField", "children_cstringField", "children_grandchild", "children_grandchildren"),
                fieldNames1);
        resultSchema1.getFields().forEach(f -> {
            if(f.getName().equals("stringField") || f.getName().equals("children_cstringField")) {
                Assert.assertEquals(
                        Schema.TypeName.STRING,
                        f.getType().getTypeName());
            } else if(f.getName().equals("children_grandchild")) {
                Assert.assertEquals(
                        Schema.TypeName.ROW,
                        f.getType().getTypeName());
            } else {
                Assert.assertEquals(
                        Schema.TypeName.ARRAY,
                        f.getType().getTypeName());
                Assert.assertEquals(
                        Schema.TypeName.ROW,
                        f.getType().getCollectionElementType().getTypeName());
            }
        });

        final Schema resultSchema2 = RowSchemaUtil.flatten(schema, "children.grandchildren", true);
        Set<String> fieldNames2 = resultSchema2.getFields().stream().map(Schema.Field::getName).collect(Collectors.toSet());
        Assert.assertEquals(
                Set.of("stringField", "children_cstringField", "children_grandchild", "children_grandchildren_gcstringField"),
                fieldNames2);
    }

    @Test
    public void testFlattenValues() {
        final Schema grandchildSchema = Schema.builder()
                .addStringField("gcstringField")
                .build();
        final Row grandchild = Row.withSchema(grandchildSchema)
                .withFieldValue("gcstringField", "gcstringValue")
                .build();
        final Schema childSchema = Schema.builder()
                .addStringField("cstringField")
                .addRowField("grandchild", grandchildSchema)
                .addArrayField("grandchildren", Schema.FieldType.row(grandchildSchema))
                .addField("grandchildrenNull", Schema.FieldType.array(Schema.FieldType.row(grandchildSchema)).withNullable(true))
                .build();
        final Row child = Row.withSchema(childSchema)
                .withFieldValue("cstringField", "cstringValue")
                .withFieldValue("grandchild", grandchild)
                .withFieldValue("grandchildren", Arrays.asList(grandchild, grandchild))
                .withFieldValue("grandchildrenNull", null)
                .build();
        final Schema schema = Schema.builder()
                .addStringField("stringField")
                .addField("children", Schema.FieldType.array(Schema.FieldType.row(childSchema)).withNullable(true))
                .addField("childrenNull", Schema.FieldType.array(Schema.FieldType.row(childSchema)).withNullable(true))
                .build();
        final Row row = Row.withSchema(schema)
                .withFieldValue("stringField", "stringValue")
                .withFieldValue("children", Arrays.asList(child, child))
                .withFieldValue("childrenNull", null)
                .build();

        // one path
        final Schema resultSchemaChildren1 = RowSchemaUtil.flatten(schema, "children", true);
        final List<Row> resultRowChildren1 = RowSchemaUtil.flatten(resultSchemaChildren1, row, "children", true);
        Assert.assertEquals(2, resultRowChildren1.size());

        for(final Row childrenRow : resultRowChildren1) {
            Assert.assertEquals("stringValue", childrenRow.getString("stringField"));
            Assert.assertEquals("cstringValue", childrenRow.getString("children_cstringField"));

            final Row grandchildRow = childrenRow.getRow("children_grandchild");
            Assert.assertEquals("gcstringValue", grandchildRow.getString("gcstringField"));

            final Collection<Row> grandchildrenRows = childrenRow.getArray("children_grandchildren");
            Assert.assertEquals(2, grandchildrenRows.size());
            for(final Row grandchildrenRow : grandchildrenRows) {
                Assert.assertEquals("gcstringValue", grandchildrenRow.getString("gcstringField"));
            }
        }

        // two path
        final Schema resultSchemaChildren2 = RowSchemaUtil.flatten(schema, "children.grandchildren", true);
        final List<Row> resultRowChildren2 = RowSchemaUtil.flatten(resultSchemaChildren2, row, "children.grandchildren", true);
        Assert.assertEquals(4, resultRowChildren2.size());

        for(final Row childrenRow : resultRowChildren2) {
            Assert.assertEquals("stringValue", childrenRow.getString("stringField"));
            Assert.assertEquals("cstringValue", childrenRow.getString("children_cstringField"));
            Assert.assertEquals("gcstringValue", childrenRow.getString("children_grandchildren_gcstringField"));

            final Row grandchildRow = childrenRow.getRow("children_grandchild");
            Assert.assertEquals("gcstringValue", grandchildRow.getString("gcstringField"));
        }

        // one path without prefix
        final Schema resultSchemaChildren1WP = RowSchemaUtil.flatten(schema, "children", false);
        final List<Row> resultRowChildren1WP = RowSchemaUtil.flatten(resultSchemaChildren1WP, row, "children", false);
        Assert.assertEquals(2, resultRowChildren1.size());

        for(final Row childrenRow : resultRowChildren1WP) {
            Assert.assertEquals("stringValue", childrenRow.getString("stringField"));
            Assert.assertEquals("cstringValue", childrenRow.getString("cstringField"));

            final Row grandchildRow = childrenRow.getRow("grandchild");
            Assert.assertEquals("gcstringValue", grandchildRow.getString("gcstringField"));

            final Collection<Row> grandchildrenRows = childrenRow.getArray("grandchildren");
            Assert.assertEquals(2, grandchildrenRows.size());
            for(final Row grandchildrenRow : grandchildrenRows) {
                Assert.assertEquals("gcstringValue", grandchildrenRow.getString("gcstringField"));
            }
        }

        // two path without prefix
        final Schema resultSchemaChildren2WP = RowSchemaUtil.flatten(schema, "children.grandchildren", false);
        final List<Row> resultRowChildren2WP = RowSchemaUtil.flatten(resultSchemaChildren2WP, row, "children.grandchildren", false);
        Assert.assertEquals(4, resultRowChildren2WP.size());

        for(final Row childrenRow : resultRowChildren2WP) {
            Assert.assertEquals("stringValue", childrenRow.getString("stringField"));
            Assert.assertEquals("cstringValue", childrenRow.getString("cstringField"));
            Assert.assertEquals("gcstringValue", childrenRow.getString("gcstringField"));

            final Row grandchildRow = childrenRow.getRow("grandchild");
            Assert.assertEquals("gcstringValue", grandchildRow.getString("gcstringField"));
        }


        // Null check
        // one path null
        final Schema resultSchemaChildren1Null = RowSchemaUtil.flatten(schema, "childrenNull", true);
        final List<Row> resultRowChildren1Null = RowSchemaUtil.flatten(resultSchemaChildren1Null, row, "childrenNull", true);
        Assert.assertEquals(1, resultRowChildren1Null.size());

        for(final Row childrenRow : resultRowChildren1Null) {
            Assert.assertEquals("stringValue", childrenRow.getString("stringField"));

            Assert.assertNull(childrenRow.getString("childrenNull_cstringField"));
            Assert.assertNull(childrenRow.getRow("childrenNull_grandchild"));
            Assert.assertNull(childrenRow.getArray("childrenNull_grandchildren"));
        }

        // two path null
        final Schema resultSchemaChildren2Null = RowSchemaUtil.flatten(schema, "children.grandchildrenNull", true);
        final List<Row> resultRowChildren2Null = RowSchemaUtil.flatten(resultSchemaChildren2Null, row, "children.grandchildrenNull", true);
        Assert.assertEquals(2, resultRowChildren2Null.size());

        for(final Row childrenRow : resultRowChildren2Null) {
            Assert.assertEquals("stringValue", childrenRow.getString("stringField"));
            Assert.assertEquals("cstringValue", childrenRow.getString("children_cstringField"));

            Assert.assertNull(childrenRow.getString("children_grandchildrenNull_gcstringField"));
        }


    }

}
