package fi.solita.utils.api.format;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.api.NestedMember;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;
import fi.solita.utils.meta.MetaNamedMember;

public class ChartConversionServiceTest {

    private static final ChartConversionService service = new ChartConversionService(new JsonConversionService(false));

    private static final MetaNamedMember<Row, Object> category = member("category");
    private static final MetaNamedMember<Row, Object> kind = member("kind");
    private static final MetaNamedMember<Row, Object> amount = member("amount");
    private static final MetaNamedMember<Row, Object> number = member("number");
    private static final MetaNamedMember<Row, Object> tags = member("tags");
    private static final MetaNamedMember<Row, Object> optionalCategory = member("optionalCategory");
    private static final MetaNamedMember<Row, Object> instant = member("instant");
    private static final MetaNamedMember<Row, Object> interval = member("interval");
    private static final MetaNamedMember<Row, Object> map = member("map");
    private static final MetaNamedMember<Row, Object> bean = member("bean");
    private static final MetaNamedMember<Row, List<Child>> children = member(Row.class, "children");
    private static final MetaNamedMember<Child, List<String>> childValues = member(Child.class, "values");
    private static final MetaNamedMember<Child, Option<String>> childOptionalValue = member(Child.class, "optionalValue");

    @Test
    public void emptyMembersProduceNoChartRowsOrSeriesNames() {
        // +---------+----------+----------------------+-------------------+
        // | row #   | category | selected members     | produced data     |
        // +---------+----------+----------------------+-------------------+
        // | 1       | a        | <none>               | <no chart rows>   |
        // +---------+----------+----------------------+-------------------+
        // Expected series names: <none>
        Pair<List<Map<Object, Object>>, List<String>> result = calculate(rows(row("a")), Collections.<MetaNamedMember<Row, Object>>emptyList());

        assertTrue(result.left().isEmpty());
        assertTrue(result.right().isEmpty());
    }

    @Test
    public void singleCategoricalMemberProducesCountsInComparableOrder() {
        // Input rows:
        // +---------+----------+------------------+
        // | row #   | category | selected member  |
        // +---------+----------+------------------+
        // | 1       | b        | category         |
        // | 2       | a        | category         |
        // | 3       | b        | category         |
        // +---------+----------+------------------+
        // Produced chart rows:
        // +-----+-------+
        // | c   | count |
        // +-----+-------+
        // | a   | 1     |
        // | b   | 2     |
        // +-----+-------+
        Pair<List<Map<Object, Object>>, List<String>> result = calculate(rows(row("b"), row("a"), row("b")), members(category));

        assertEquals(Arrays.asList("count"), result.right());
        assertEquals(2, result.left().size());
        assertChartRow(result.left().get(0), "a", "count", 1L);
        assertChartRow(result.left().get(1), "b", "count", 2L);
    }

    @Test
    public void singleCollectionMemberOnSingleObjectIsFlattenedAndCountedAsValues() {
        // Input rows:
        // +---------+--------------------------------+------------------+
        // | row #   | tags                           | selected member  |
        // +---------+--------------------------------+------------------+
        // | 1       | [blue, red, blue, green]       | tags             |
        // +---------+--------------------------------+------------------+
        // Produced chart rows after flattening the single collection value:
        // +-------+-------+
        // | c     | count |
        // +-------+-------+
        // | blue  | 2     |
        // | green | 1     |
        // | red   | 1     |
        // +-------+-------+
        Row row = rowWithTags(Arrays.asList("blue", "red", "blue", "green"));

        Pair<List<Map<Object, Object>>, List<String>> result = calculate(rows(row), members(tags));

        assertEquals(Arrays.asList("count"), result.right());
        assertEquals(3, result.left().size());
        assertChartRow(result.left().get(0), "blue", "count", 2L);
        assertChartRow(result.left().get(1), "green", "count", 1L);
        assertChartRow(result.left().get(2), "red", "count", 1L);
    }

    @Test
    public void iterableValuesOnRowsAreFlattenedAndCounted() {
        // Input rows:
        // +---------+----------------+------------------+
        // | row #   | tags           | selected member  |
        // +---------+----------------+------------------+
        // | 1       | [blue, red]    | tags             |
        // | 2       | [blue, green]  | tags             |
        // | 3       | []             | tags             |
        // +---------+----------------+------------------+
        // Produced chart rows after flattening iterable selected values from each row:
        // +-------+-------+
        // | c     | count |
        // +-------+-------+
        // | blue  | 2     |
        // | green | 1     |
        // | red   | 1     |
        // +-------+-------+
        Pair<List<Map<Object, Object>>, List<String>> result = calculate(rows(
                rowWithTags(Arrays.asList("blue", "red")),
                rowWithTags(Arrays.asList("blue", "green")),
                rowWithTags(Collections.<String>emptyList())), members(tags));

        assertEquals(Arrays.asList("count"), result.right());
        assertEquals(3, result.left().size());
        assertChartRow(result.left().get(0), "blue", "count", 2L);
        assertChartRow(result.left().get(1), "green", "count", 1L);
        assertChartRow(result.left().get(2), "red", "count", 1L);
    }

    @Test
    public void optionValuesOnRowsAreFlattenedAndCounted() {
        // Input rows:
        // +---------+------------------+------------------+
        // | row #   | optionalCategory | selected member  |
        // +---------+------------------+------------------+
        // | 1       | Some(blue)       | optionalCategory |
        // | 2       | None             | optionalCategory |
        // | 3       | Some(red)        | optionalCategory |
        // | 4       | Some(blue)       | optionalCategory |
        // +---------+------------------+------------------+
        // Produced chart rows after flattening Option values. None contributes no value:
        // +------+-------+
        // | c    | count |
        // +------+-------+
        // | blue | 2     |
        // | red  | 1     |
        // +------+-------+
        Pair<List<Map<Object, Object>>, List<String>> result = calculate(rows(
                rowWithOptionalCategory(Some("blue")),
                rowWithOptionalCategory(None()),
                rowWithOptionalCategory(Some("red")),
                rowWithOptionalCategory(Some("blue"))), members(optionalCategory));

        assertEquals(Arrays.asList("count"), result.right());
        assertEquals(2, result.left().size());
        assertChartRow(result.left().get(0), "blue", "count", 2L);
        assertChartRow(result.left().get(1), "red", "count", 1L);
    }

    @Test
    public void iterableValuesNestedUnderIterablesAreFlattenedAndCounted() {
        // Input rows:
        // +---------+-----------------------------------------------+-----------------+
        // | row #   | children.values                               | selected member |
        // +---------+-----------------------------------------------+-----------------+
        // | 1       | [[blue, red], [blue]]                         | children.values |
        // | 2       | [[green], []]                                 | children.values |
        // +---------+-----------------------------------------------+-----------------+
        // NestedMember.ofItFlatType(children, childValues) flattens both levels:
        // +-------+-------+
        // | c     | count |
        // +-------+-------+
        // | blue  | 2     |
        // | green | 1     |
        // | red   | 1     |
        // +-------+-------+
        MetaNamedMember<Row, Object> nestedValues = asObjectMember(NestedMember.ofItFlatType(children, childValues));

        Pair<List<Map<Object, Object>>, List<String>> result = calculate(rows(
                rowWithChildren(child(Arrays.asList("blue", "red")), child(Arrays.asList("blue"))),
                rowWithChildren(child(Arrays.asList("green")), child(Collections.<String>emptyList()))), members(nestedValues));

        assertEquals(Arrays.asList("count"), result.right());
        assertEquals(3, result.left().size());
        assertChartRow(result.left().get(0), "blue", "count", 2L);
        assertChartRow(result.left().get(1), "green", "count", 1L);
        assertChartRow(result.left().get(2), "red", "count", 1L);
    }

    @Test
    public void optionValuesNestedUnderIterablesAreFlattenedAndCounted() {
        // Input rows:
        // +---------+---------------------------------------------+------------------------+
        // | row #   | children.optionalValue                      | selected member        |
        // +---------+---------------------------------------------+------------------------+
        // | 1       | [Some(blue), None, Some(red)]               | children.optionalValue |
        // | 2       | [Some(blue)]                               | children.optionalValue |
        // +---------+---------------------------------------------+------------------------+
        // NestedMember.ofOptionFlatType(children, childOptionalValue) flattens the
        // iterable of options. None contributes no value:
        // +------+-------+
        // | c    | count |
        // +------+-------+
        // | blue | 2     |
        // | red  | 1     |
        // +------+-------+
        MetaNamedMember<Row, Object> nestedOptionalValues = asObjectMember(NestedMember.ofOptionFlatType(children, childOptionalValue));

        Pair<List<Map<Object, Object>>, List<String>> result = calculate(rows(
                rowWithChildren(child(Some("blue")), child(None()), child(Some("red"))),
                rowWithChildren(child(Some("blue")))), members(nestedOptionalValues));

        assertEquals(Arrays.asList("count"), result.right());
        assertEquals(2, result.left().size());
        assertChartRow(result.left().get(0), "blue", "count", 2L);
        assertChartRow(result.left().get(1), "red", "count", 1L);
    }

    @Test
    public void multipleCategoricalMembersProduceSortedSeriesNamesAndCountsPerXCategory() {
        // Input rows:
        // +---------+----------+------+------------------+
        // | row #   | category | kind | selected members |
        // +---------+----------+------+------------------+
        // | 1       | b        | late | category, kind   |
        // | 2       | a        | ok   | category, kind   |
        // | 3       | a        | late | category, kind   |
        // | 4       | b        | late | category, kind   |
        // | 5       | b        | ok   | category, kind   |
        // +---------+----------+------+------------------+
        // Produced chart rows and series names [late, ok]:
        // +-----+------+----+
        // | c   | late | ok |
        // +-----+------+----+
        // | a   | 1    | 1  |
        // | b   | 2    | 1  |
        // +-----+------+----+
        Pair<List<Map<Object, Object>>, List<String>> result = calculate(rows(
                row("b", "late"),
                row("a", "ok"),
                row("a", "late"),
                row("b", "late"),
                row("b", "ok")), members(category, kind));

        assertEquals(Arrays.asList("late", "ok"), result.right());
        assertEquals(2, result.left().size());
        assertChartRow(result.left().get(0), "a", "late", 1L);
        assertChartRow(result.left().get(0), "a", "ok", 1L);
        assertChartRow(result.left().get(1), "b", "late", 2L);
        assertChartRow(result.left().get(1), "b", "ok", 1L);
    }

    @Test
    public void numericLinearXProducesValueSeriesRowsWhenLinearFlagIsGiven() {
        // Input rows:
        // +---------+--------+--------+------------------+
        // | row #   | number | amount | selected members |
        // +---------+--------+--------+------------------+
        // | 1       | 2      | 20     | number, amount   |
        // | 2       | 1      | 10     | number, amount   |
        // | 3       | 3      | 30     | number, amount   |
        // +---------+--------+--------+------------------+
        // Produced linear chart rows sorted by number. The visible "amount" key
        // is the serialized label and "_amount" is the numeric value used by amCharts:
        // +---+--------+---------+
        // | c | amount | _amount |
        // +---+--------+---------+
        // | 1 | 10     | 10      |
        // | 2 | 20     | 20      |
        // | 3 | 30     | 30      |
        // +---+--------+---------+
        ChartConversionService plainJsonService = new ChartConversionService(new JsonConversionService(new ObjectMapper()));

        Pair<List<Map<Object, Object>>, List<String>> result = plainJsonService.calculateChartData(rows(
                row(2, 20L),
                row(1, 10L),
                row(3, 30L)), members(number, amount), false, false, false, true);

        assertEquals(Arrays.asList("amount"), result.right());
        assertEquals(3, result.left().size());
        assertEquals(1, result.left().get(0).get("c"));
        assertEquals("10", result.left().get(0).get("amount"));
        assertEquals(10L, result.left().get(0).get("_amount"));
        assertEquals(2, result.left().get(1).get("c"));
        assertEquals("20", result.left().get(1).get("amount"));
        assertEquals(20L, result.left().get(1).get("_amount"));
        assertEquals(3, result.left().get(2).get("c"));
        assertEquals("30", result.left().get(2).get("amount"));
        assertEquals(30L, result.left().get(2).get("_amount"));
    }

    @Test
    public void singleInstantMemberProducesCountsAtEpochMillis() {
        // Input rows:
        // +---------+-------------------+------------------+
        // | row #   | instant           | selected member  |
        // +---------+-------------------+------------------+
        // | 1       | 2026-01-01 12:00 | instant          |
        // | 2       | 2026-01-01 12:00 | instant          |
        // | 3       | 2026-01-01 13:00 | instant          |
        // +---------+-------------------+------------------+
        // Produced chart rows use epoch milliseconds as the category value:
        // +-------------------------+-------+
        // | c                       | count |
        // +-------------------------+-------+
        // | first.getMillis()       | 2     |
        // | second.getMillis()      | 1     |
        // +-------------------------+-------+
        DateTime first = new DateTime(2026, 1, 1, 12, 0);
        DateTime second = first.plusHours(1);

        Pair<List<Map<Object, Object>>, List<String>> result = calculate(rows(
                rowAt(first),
                rowAt(first),
                rowAt(second)), members(instant));

        assertEquals(Arrays.asList("count"), result.right());
        assertEquals(2, result.left().size());
        assertChartRow(result.left().get(0), first.getMillis(), "count", 2L);
        assertChartRow(result.left().get(1), second.getMillis(), "count", 1L);
    }

    @Test
    public void intervalXWithCategoricalYCountsRowsForEachCoalescedTimeRange() {
        // Input rows:
        // +---------+-------------------+-------------------+---------+------------------+
        // | row #   | interval start    | interval end      | kind    | selected members |
        // +---------+-------------------+-------------------+---------+------------------+
        // | 1       | 2026-01-01 12:00 | 2026-01-01 14:00 | planned | interval, kind   |
        // | 2       | 2026-01-01 13:00 | 2026-01-01 15:00 | active  | interval, kind   |
        // +---------+-------------------+-------------------+---------+------------------+
        // Produced chart rows are split at interval boundaries and counted by kind:
        // +--------------------------+--------+---------+
        // | c                        | active | planned |
        // +--------------------------+--------+---------+
        // | start                    | -      | 1       |
        // | start.plusHours(1)       | 1      | 1       |
        // | start.plusHours(2)       | 1      | -       |
        // +--------------------------+--------+---------+
        DateTime start = new DateTime(2026, 1, 1, 12, 0);

        Pair<List<Map<Object, Object>>, List<String>> result = calculate(rows(
                rowDuring(start, start.plusHours(2), "planned"),
                rowDuring(start.plusHours(1), start.plusHours(3), "active")), members(interval, kind));

        assertEquals(Arrays.asList("active", "planned"), result.right());
        assertEquals(3, result.left().size());

        assertEquals(start.getMillis(), result.left().get(0).get("c"));
        assertFalse(result.left().get(0).containsKey("active"));
        assertEquals(1L, result.left().get(0).get("planned"));

        assertEquals(start.plusHours(1).getMillis(), result.left().get(1).get("c"));
        assertEquals(1L, result.left().get(1).get("active"));
        assertEquals(1L, result.left().get(1).get("planned"));

        assertEquals(start.plusHours(2).getMillis(), result.left().get(2).get("c"));
        assertEquals(1L, result.left().get(2).get("active"));
        assertFalse(result.left().get(2).containsKey("planned"));
    }

    @Test
    public void mapMembersAreRejectedAsPureStructure() {
        // Input rows:
        // +---------+----------------+------------------+-----------------+
        // | row #   | map            | selected member  | expected result |
        // +---------+----------------+------------------+-----------------+
        // | 1       | {key=value}    | map              | reject "map"    |
        // +---------+----------------+------------------+-----------------+
        try {
            calculate(rows(rowWithMap()), members(map));
            fail("Expected structure members to be rejected");
        } catch (ChartConversionService.CannotChartByStructureException e) {
            assertEquals("map", e.name);
        }
    }

    @Test
    public void jsonSerializeAsBeanMembersAreRejectedAsPureStructure() {
        // Input rows:
        // +---------+---------------+------------------+------------------+
        // | row #   | bean          | selected member  | expected result  |
        // +---------+---------------+------------------+------------------+
        // | 1       | Bean(value)   | bean             | reject "bean"    |
        // +---------+---------------+------------------+------------------+
        try {
            calculate(rows(rowWithBean()), members(bean));
            fail("Expected bean members to be rejected");
        } catch (ChartConversionService.CannotChartByStructureException e) {
            assertEquals("bean", e.name);
        }
    }

    @Test
    public void calculatesMillionRowsAndPrintsElapsedTime() {
        // Generated input rows:
        // +----------------------+--------------------------+------------------+
        // | row range            | number                   | selected member  |
        // +----------------------+--------------------------+------------------+
        // | 0 .. size - 1        | rowIndex % 10            | number           |
        // +----------------------+--------------------------+------------------+
        // Produced chart rows:
        // +-----+----------------+
        // | c   | count          |
        // +-----+----------------+
        // | 0   | size / 10      |
        // | ... | ...            |
        // | 9   | size / 10      |
        // +-----+----------------+
        final int size = 1000000;
        List<Row> rows = new ArrayList<Row>(size);
        for (int i = 0; i < size; i++) {
            rows.add(row(i % 10, null));
        }

        long started = System.nanoTime();
        Pair<List<Map<Object, Object>>, List<String>> result = calculate(rows, members(number));
        long elapsedNanos = System.nanoTime() - started;

        System.out.println("ChartConversionService.calculateChartData " + size + " rows elapsed "
                + TimeUnit.NANOSECONDS.toMillis(elapsedNanos) + " ms (" + elapsedNanos + " ns)");

        assertEquals(Arrays.asList("count"), result.right());
        assertEquals(10, result.left().size());
        for (int i = 0; i < 10; i++) {
            assertChartRow(result.left().get(i), i, "count", size / 10L);
        }
        assertTrue("Elapsed time should have been measured", elapsedNanos > 0);
    }

    @Test
    public void calculatesMillionRowsForIterableSelectedValuesAndPrintsElapsedTime() {
        final int size = 1000000;

        // Selected value is Iterable<String> on every row.
        // +----------------------+--------------------------+------------------+----------------+
        // | row range            | tags                     | selected member  | expected rows  |
        // +----------------------+--------------------------+------------------+----------------+
        // | 0 .. size - 1        | [value-(rowIndex % 10)] | tags             | value-0 .. -9  |
        // +----------------------+--------------------------+------------------+----------------+
        Pair<List<Map<Object, Object>>, List<String>> iterableResult = calculateAndPrintElapsed("iterable selected values", size,
                millionRows(size, new RowFactory() {
                    @Override
                    public Row row(int index) {
                        return rowWithTags(Arrays.asList(value(index)));
                    }
                }), members(tags));
        assertCountRows(iterableResult, 10, size / 10L);
    }

    @Test
    public void calculatesMillionRowsForOptionSelectedValuesAndPrintsElapsedTime() {
        final int size = 1000000;

        // Selected value is Option<String> on every row.
        // +----------------------+-------------------------------+------------------+-----------------------+
        // | row range            | optionalCategory              | selected member  | expected rows         |
        // +----------------------+-------------------------------+------------------+-----------------------+
        // | bucket 0 .. 8        | Some(value-bucket)            | optionalCategory | value-0 .. value-8    |
        // | bucket 9             | None                          | optionalCategory | no produced row       |
        // +----------------------+-------------------------------+------------------+-----------------------+
        Pair<List<Map<Object, Object>>, List<String>> optionResult = calculateAndPrintElapsed("option selected values", size,
                millionRows(size, new RowFactory() {
                    @Override
                    public Row row(int index) {
                        int bucket = index % 10;
                        return rowWithOptionalCategory(bucket == 9 ? None() : Some(value(index)));
                    }
                }), members(optionalCategory));
        assertCountRows(optionResult, 9, size / 10L);
    }

    @Test
    public void calculatesMillionRowsForNestedIterableSelectedValuesAndPrintsElapsedTime() {
        final int size = 1000000;

        // Selected value is Iterable<String> nested under Iterable<Child>.
        // +----------------------+-------------------------------+-----------------+----------------+
        // | row range            | children.values               | selected member | expected rows  |
        // +----------------------+-------------------------------+-----------------+----------------+
        // | 0 .. size - 1        | [[value-(rowIndex % 10)]]     | children.values | value-0 .. -9  |
        // +----------------------+-------------------------------+-----------------+----------------+
        final MetaNamedMember<Row, Object> nestedValues = asObjectMember(NestedMember.ofItFlatType(children, childValues));
        Pair<List<Map<Object, Object>>, List<String>> nestedIterableResult = calculateAndPrintElapsed("nested iterable selected values", size,
                millionRows(size, new RowFactory() {
                    @Override
                    public Row row(int index) {
                        return rowWithChildren(child(Arrays.asList(value(index))));
                    }
                }), members(nestedValues));
        assertCountRows(nestedIterableResult, 10, size / 10L);
    }

    @Test
    public void calculatesMillionRowsForNestedOptionSelectedValuesAndPrintsElapsedTime() {
        final int size = 1000000;

        // Selected value is Option<String> nested under Iterable<Child>.
        // +----------------------+-------------------------------+------------------------+-----------------------+
        // | row range            | children.optionalValue        | selected member        | expected rows         |
        // +----------------------+-------------------------------+------------------------+-----------------------+
        // | bucket 0 .. 8        | [Some(value-bucket)]          | children.optionalValue | value-0 .. value-8    |
        // | bucket 9             | [None]                        | children.optionalValue | no produced row       |
        // +----------------------+-------------------------------+------------------------+-----------------------+
        final MetaNamedMember<Row, Object> nestedOptionalValues = asObjectMember(NestedMember.ofOptionFlatType(children, childOptionalValue));
        Pair<List<Map<Object, Object>>, List<String>> nestedOptionResult = calculateAndPrintElapsed("nested option selected values", size,
                millionRows(size, new RowFactory() {
                    @Override
                    public Row row(int index) {
                        int bucket = index % 10;
                        return rowWithChildren(child(bucket == 9 ? None() : Some(value(index))));
                    }
                }), members(nestedOptionalValues));
        assertCountRows(nestedOptionResult, 9, size / 10L);
    }

    @Test
    public void calculatesMillionRowsForMultipleCategoricalSelectedValuesAndPrintsElapsedTime() {
        final int size = 1000000;

        // Selected values are two categorical members.
        // +----------------------+--------------------------+------------------------+------------------+
        // | row range            | category                 | kind                   | selected members |
        // +----------------------+--------------------------+------------------------+------------------+
        // | 0 .. size - 1        | value-(rowIndex % 10)   | kind-((rowIndex/10)%2) | category, kind   |
        // +----------------------+--------------------------+------------------------+------------------+
        // Each category receives 50000 rows for kind-0 and 50000 rows for kind-1.
        Pair<List<Map<Object, Object>>, List<String>> multiMemberResult = calculateAndPrintElapsed("multiple categorical selected values", size,
                millionRows(size, new RowFactory() {
                    @Override
                    public Row row(int index) {
                        return ChartConversionServiceTest.row(value(index), "kind-" + ((index / 10) % 2));
                    }
                }), members(category, kind));

        assertEquals(Arrays.asList("kind-0", "kind-1"), multiMemberResult.right());
        assertEquals(10, multiMemberResult.left().size());
        for (int i = 0; i < 10; i++) {
            assertChartRow(multiMemberResult.left().get(i), "value-" + i, "kind-0", size / 20L);
            assertChartRow(multiMemberResult.left().get(i), "value-" + i, "kind-1", size / 20L);
        }
    }

    @Test
    public void calculatesMillionRowsForSingleInstantSelectedValuesAndPrintsElapsedTime() {
        final int size = 1000000;
        final DateTime start = new DateTime(2026, 1, 1, 12, 0);

        // Selected value is DateTime on every row.
        // +----------------------+-------------------------------+------------------+-----------------------+
        // | row range            | instant                       | selected member  | expected rows         |
        // +----------------------+-------------------------------+------------------+-----------------------+
        // | 0 .. size - 1        | start + (rowIndex % 10) min  | instant          | 10 epoch-millis rows  |
        // +----------------------+-------------------------------+------------------+-----------------------+
        Pair<List<Map<Object, Object>>, List<String>> result = calculateAndPrintElapsed("single instant selected values", size,
                millionRows(size, new RowFactory() {
                    @Override
                    public Row row(int index) {
                        return rowAt(start.plusMinutes(index % 10));
                    }
                }), members(instant));

        assertTemporalCountRows(result, start, size / 10L);
    }

    @Test
    public void calculatesMillionRowsForSingleIntervalSelectedValuesAndPrintsElapsedTime() {
        final int size = 1000000;
        final DateTime start = new DateTime(2026, 1, 1, 12, 0);

        // Selected value is Interval on every row. Intervals are intentionally disjoint
        // so each produced chart row maps directly to one interval bucket.
        // +----------------------+----------------------------------------+------------------+-----------------------+
        // | row range            | interval                               | selected member  | expected rows         |
        // +----------------------+----------------------------------------+------------------+-----------------------+
        // | 0 .. size - 1        | bucket start .. bucket start + 1 min   | interval         | 10 epoch-millis rows  |
        // +----------------------+----------------------------------------+------------------+-----------------------+
        Pair<List<Map<Object, Object>>, List<String>> result = calculateAndPrintElapsed("single interval selected values", size,
                millionRows(size, new RowFactory() {
                    @Override
                    public Row row(int index) {
                        int bucket = index % 10;
                        DateTime bucketStart = start.plusMinutes(bucket * 2);
                        return rowDuring(bucketStart, bucketStart.plusMinutes(1), null);
                    }
                }), members(interval));

        assertTemporalCountRows(result, start, 2, size / 10L);
    }

    @Test
    public void calculatesMillionRowsForInstantWithCategoricalYAndPrintsElapsedTime() {
        final int size = 1000000;
        final DateTime start = new DateTime(2026, 1, 1, 12, 0);

        // Selected values are DateTime x-axis and categorical y-axis.
        // +----------------------+------------------------------+------------------------+------------------+
        // | row range            | instant                      | kind                   | selected members |
        // +----------------------+------------------------------+------------------------+------------------+
        // | 0 .. size - 1        | start + (rowIndex % 10) min | kind-((rowIndex/10)%2) | instant, kind    |
        // +----------------------+------------------------------+------------------------+------------------+
        Pair<List<Map<Object, Object>>, List<String>> result = calculateAndPrintElapsed("instant selected values with categorical y", size,
                millionRows(size, new RowFactory() {
                    @Override
                    public Row row(int index) {
                        return rowAt(start.plusMinutes(index % 10), "kind-" + ((index / 10) % 2));
                    }
                }), members(instant, kind));

        assertTemporalCategoricalRows(result, start, 1, size / 20L);
    }

    @Test
    public void calculatesMillionRowsForIntervalWithCategoricalYAndPrintsElapsedTime() {
        final int size = 1000000;
        final DateTime start = new DateTime(2026, 1, 1, 12, 0);

        // Selected values are Interval x-axis and categorical y-axis. Intervals are
        // intentionally disjoint so each produced chart row maps directly to one interval bucket.
        // +----------------------+--------------------------------------+------------------------+------------------+
        // | row range            | interval                             | kind                   | selected members |
        // +----------------------+--------------------------------------+------------------------+------------------+
        // | 0 .. size - 1        | bucket start .. bucket start + 1 min | kind-((rowIndex/10)%2) | interval, kind   |
        // +----------------------+--------------------------------------+------------------------+------------------+
        Pair<List<Map<Object, Object>>, List<String>> result = calculateAndPrintElapsed("interval selected values with categorical y", size,
                millionRows(size, new RowFactory() {
                    @Override
                    public Row row(int index) {
                        int bucket = index % 10;
                        DateTime bucketStart = start.plusMinutes(bucket * 2);
                        return rowDuring(bucketStart, bucketStart.plusMinutes(1), "kind-" + ((index / 10) % 2));
                    }
                }), members(interval, kind));

        assertTemporalCategoricalRows(result, start, 2, size / 20L);
    }

    @Test
    public void calculatesMillionRowsForNumericLinearSelectedValuesAndPrintsElapsedTime() {
        final int size = 1000000;
        ChartConversionService plainJsonService = new ChartConversionService(new JsonConversionService(new ObjectMapper()));

        // Selected values are numeric x-axis and numeric y-axis, with the linear flag
        // enabled explicitly to exercise the line-series/value branch at million-row scale.
        // +----------------------+--------------------------+------------------------+------------------+
        // | row range            | number                   | amount                 | selected members |
        // +----------------------+--------------------------+------------------------+------------------+
        // | 0 .. size - 1        | rowIndex                 | rowIndex % 10          | number, amount   |
        // +----------------------+--------------------------+------------------------+------------------+
        long started = System.nanoTime();
        Pair<List<Map<Object, Object>>, List<String>> result = plainJsonService.calculateChartData(
                millionRows(size, new RowFactory() {
                    @Override
                    public Row row(int index) {
                        return ChartConversionServiceTest.row(index, Long.valueOf(index % 10));
                    }
                }), members(number, amount), false, false, false, true);
        long elapsedNanos = System.nanoTime() - started;

        System.out.println("ChartConversionService.calculateChartData " + size + " rows (numeric linear selected values) elapsed "
                + TimeUnit.NANOSECONDS.toMillis(elapsedNanos) + " ms (" + elapsedNanos + " ns)");

        assertEquals(Arrays.asList("amount"), result.right());
        assertEquals(size, result.left().size());
        assertLinearChartRow(result.left().get(0), 0, "0", 0L);
        assertLinearChartRow(result.left().get(size / 2), size / 2, "0", 0L);
        assertLinearChartRow(result.left().get(size - 1), size - 1, "9", 9L);
        assertTrue("Elapsed time should have been measured", elapsedNanos > 0);
    }

    private static Pair<List<Map<Object, Object>>, List<String>> calculate(Iterable<Row> rows, List<MetaNamedMember<Row, Object>> members) {
        boolean xIsInstant = !members.isEmpty() && DateTime.class.isAssignableFrom(ChartConversionService.resolveType(members.get(0)));
        boolean xIsInterval = !members.isEmpty() && Interval.class.isAssignableFrom(ChartConversionService.resolveType(members.get(0)));
        boolean xIsTemporal = xIsInstant || xIsInterval;
        boolean xIsLinear = !members.isEmpty() && service.isLinear(members.get(0));
        return service.calculateChartData(rows, members, xIsInstant, xIsInterval, xIsTemporal, xIsLinear);
    }

    @SafeVarargs
    private static List<Row> rows(Row... rows) {
        return Arrays.asList(rows);
    }

    @SafeVarargs
    private static List<MetaNamedMember<Row, Object>> members(MetaNamedMember<Row, Object>... members) {
        return Arrays.asList(members);
    }

    private static void assertChartRow(Map<Object, Object> row, Object category, String valueName, Object value) {
        assertEquals(category, row.get("c"));
        assertEquals(value, row.get(valueName));
    }

    private static void assertLinearChartRow(Map<Object, Object> row, Object category, String valueName, Object value) {
        assertEquals(category, row.get("c"));
        assertEquals(valueName, row.get("amount"));
        assertEquals(value, row.get("_amount"));
    }

    private static void assertCountRows(Pair<List<Map<Object, Object>>, List<String>> result, int expectedRows, long expectedCount) {
        assertEquals(Arrays.asList("count"), result.right());
        assertEquals(expectedRows, result.left().size());
        for (int i = 0; i < expectedRows; i++) {
            assertChartRow(result.left().get(i), "value-" + i, "count", expectedCount);
        }
    }

    private static void assertTemporalCountRows(Pair<List<Map<Object, Object>>, List<String>> result, DateTime start, long expectedCount) {
        assertTemporalCountRows(result, start, 1, expectedCount);
    }

    private static void assertTemporalCountRows(Pair<List<Map<Object, Object>>, List<String>> result, DateTime start, int minuteStep, long expectedCount) {
        assertEquals(Arrays.asList("count"), result.right());
        assertEquals(10, result.left().size());
        for (int i = 0; i < 10; i++) {
            assertChartRow(result.left().get(i), start.plusMinutes(i * minuteStep).getMillis(), "count", expectedCount);
        }
    }

    private static void assertTemporalCategoricalRows(Pair<List<Map<Object, Object>>, List<String>> result, DateTime start, int minuteStep,
            long expectedCount) {
        assertEquals(Arrays.asList("kind-0", "kind-1"), result.right());
        assertEquals(10, result.left().size());
        for (int i = 0; i < 10; i++) {
            assertChartRow(result.left().get(i), start.plusMinutes(i * minuteStep).getMillis(), "kind-0", expectedCount);
            assertChartRow(result.left().get(i), start.plusMinutes(i * minuteStep).getMillis(), "kind-1", expectedCount);
        }
    }

    private static Pair<List<Map<Object, Object>>, List<String>> calculateAndPrintElapsed(String scenario, int size,
            Iterable<Row> rows, List<MetaNamedMember<Row, Object>> members) {
        long started = System.nanoTime();
        Pair<List<Map<Object, Object>>, List<String>> result = calculate(rows, members);
        long elapsedNanos = System.nanoTime() - started;

        System.out.println("ChartConversionService.calculateChartData " + size + " rows (" + scenario + ") elapsed "
                + TimeUnit.NANOSECONDS.toMillis(elapsedNanos) + " ms (" + elapsedNanos + " ns)");

        assertTrue("Elapsed time should have been measured", elapsedNanos > 0);
        return result;
    }

    private static Iterable<Row> millionRows(final int size, final RowFactory rowFactory) {
        return new Iterable<Row>() {
            @Override
            public Iterator<Row> iterator() {
                return new Iterator<Row>() {
                    private int index = 0;

                    @Override
                    public boolean hasNext() {
                        return index < size;
                    }

                    @Override
                    public Row next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        return rowFactory.row(index++);
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    private static String value(int index) {
        return "value-" + (index % 10);
    }

    private static Row row(String category) {
        return new Row(category, null, null, null, null, null, null, null, null);
    }

    private static Row row(String category, String kind) {
        return new Row(category, kind, null, null, null, null, null, null, null);
    }

    private static Row row(Integer number, Long amount) {
        return new Row(null, null, amount, number, null, null, null, null, null);
    }

    private static Row rowWithTags(List<String> tags) {
        return new Row(null, null, null, null, tags, null, null, null, null, null, null);
    }

    private static Row rowWithOptionalCategory(Option<String> optionalCategory) {
        return new Row(null, null, null, null, null, optionalCategory, null, null, null, null, null);
    }

    private static Row rowWithChildren(Child... children) {
        return new Row(null, null, null, null, null, null, null, null, null, null, Arrays.asList(children));
    }

    private static Row rowAt(DateTime instant) {
        return new Row(null, null, null, null, null, null, instant, null, null, null, null);
    }

    private static Row rowAt(DateTime instant, String kind) {
        return new Row(null, kind, null, null, null, null, instant, null, null, null, null);
    }

    private static Row rowDuring(DateTime start, DateTime end, String kind) {
        return new Row(null, kind, null, null, null, null, null, new Interval(start, end), null, null, null);
    }

    private static Row rowWithMap() {
        return new Row(null, null, null, null, null, null, null, null, Collections.singletonMap("key", "value"), null, null);
    }

    private static Row rowWithBean() {
        return new Row(null, null, null, null, null, null, null, null, null, new Bean("value"), null);
    }

    private static Child child(List<String> values) {
        return new Child(values, None());
    }

    private static Child child(Option<String> optionalValue) {
        return new Child(Collections.<String>emptyList(), optionalValue);
    }

    @SuppressWarnings("unchecked")
    private static <T> MetaNamedMember<T, Object> asObjectMember(MetaNamedMember<T, ?> member) {
        return (MetaNamedMember<T, Object>) member;
    }

    private static MetaNamedMember<Row, Object> member(String fieldName) {
        return member(Row.class, fieldName);
    }

    private static <T, V> MetaNamedMember<T, V> member(Class<T> ownerType, String fieldName) {
        try {
            Field field = ownerType.getDeclaredField(fieldName);
            field.setAccessible(true);
            return new FieldMember<T, V>(fieldName, field);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class FieldMember<T, V> implements MetaNamedMember<T, V> {
        private final String name;
        private final Field field;

        private FieldMember(String name, Field field) {
            this.name = name;
            this.field = field;
        }

        @SuppressWarnings("unchecked")
        @Override
        public V apply(T t) {
            try {
                return (V) field.get(t);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public AccessibleObject getMember() {
            return field;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    private interface RowFactory {
        Row row(int index);
    }

    public static final class Row {
        public final String category;
        public final String kind;
        public final Long amount;
        public final Integer number;
        public final List<String> tags;
        public final Option<String> optionalCategory;
        public final DateTime instant;
        public final Interval interval;
        public final Map<String, String> map;
        public final Bean bean;
        public final List<Child> children;

        public Row(String category, String kind, Long amount, Integer number, List<String> tags, DateTime instant,
                Interval interval, Map<String, String> map, Bean bean) {
            this(category, kind, amount, number, tags, null, instant, interval, map, bean, null);
        }

        public Row(String category, String kind, Long amount, Integer number, List<String> tags, Option<String> optionalCategory,
                DateTime instant, Interval interval, Map<String, String> map, Bean bean, List<Child> children) {
            this.category = category;
            this.kind = kind;
            this.amount = amount;
            this.number = number;
            this.tags = tags;
            this.optionalCategory = optionalCategory;
            this.instant = instant;
            this.interval = interval;
            this.map = map;
            this.bean = bean;
            this.children = children;
        }
    }

    public static final class Child {
        public final List<String> values;
        public final Option<String> optionalValue;

        public Child(List<String> values, Option<String> optionalValue) {
            this.values = values;
            this.optionalValue = optionalValue;
        }
    }

    @JsonSerializeAsBean
    public static final class Bean {
        public final String value;

        public Bean(String value) {
            this.value = value;
        }
    }
}
