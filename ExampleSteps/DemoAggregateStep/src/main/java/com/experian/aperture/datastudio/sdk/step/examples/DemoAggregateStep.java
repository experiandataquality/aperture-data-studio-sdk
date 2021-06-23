package com.experian.aperture.datastudio.sdk.step.examples;

import com.experian.datastudio.sdk.api.CustomTypeMetadata;
import com.experian.datastudio.sdk.api.CustomTypeMetadataBuilder;
import com.experian.datastudio.sdk.api.step.CustomStepDefinition;
import com.experian.datastudio.sdk.api.step.configuration.StepConfiguration;
import com.experian.datastudio.sdk.api.step.configuration.StepConfigurationBuilder;
import com.experian.datastudio.sdk.api.step.configuration.StepIcon;
import com.experian.datastudio.sdk.api.step.processor.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class DemoAggregateStep implements CustomStepDefinition {
    private static final String INDEX_NAME = "groupindex";
    private static final String INPUT_ID = "input0";
    private static final String OUTPUT_ID = "output0";
    private static final String GROUP_COLUMN_PROP = "groupcolumnproperty";
    private static final String AGGREGATE_COLUMN_PROP = "aggregatecolumnproperty";
    private static final String AGGREGATE_TYPE_PROP = "aggregatetypeproperty";

    private static final String GROUP_COLUMN = "Group";
    private static final String AGGREGATE_COLUMN = "Aggregate";

    private static final String SUM = "sum";
    private static final String AVG = "avg";
    private static final String MAX = "max";
    private static final String MIN = "min";
    private static final String COUNT = "count";
    private static final String FIRST = "first";
    private static final String LAST = "last";
    private static final String PIPE = "pipe";

    @Override
    public StepConfiguration createConfiguration(StepConfigurationBuilder configurationBuilder) {
        return configurationBuilder
                .withNodes(stepNodeBuilder -> stepNodeBuilder
                        .addInputNode(INPUT_ID)
                        .addOutputNode(OUTPUT_ID)
                        .build())
                .withStepProperties(stepPropertiesBuilder -> stepPropertiesBuilder
                        .addStepProperty(stepPropertyBuilder -> stepPropertyBuilder
                                .asColumnChooser(GROUP_COLUMN_PROP)
                                .forInputNode(INPUT_ID)
                                .withAllowSelectAll(false)
                                .withMultipleSelect(false)
                                .withIsRequired(true)
                                // .withShouldRebuildIndex(true) step property by default will rebuild index on change (true).
                                .withLabelSupplier(uiCallbackContext -> "Group Column")
                                .build())
                        .addStepProperty(stepPropertyBuilder -> stepPropertyBuilder
                                .asColumnChooser(AGGREGATE_COLUMN_PROP)
                                .forInputNode(INPUT_ID)
                                .withAllowSelectAll(false)
                                .withMultipleSelect(false)
                                .withIsRequired(true)
                                // .withShouldRebuildIndex(true) step property by default will rebuild index on change (true).
                                .withLabelSupplier(uiCallbackContext -> "Aggregate Column")
                                .build())
                        .addStepProperty(stepPropertyBuilder -> stepPropertyBuilder
                                .asCustomChooser(AGGREGATE_TYPE_PROP)
                                .withAllowValuesProvider(ctx -> Arrays.asList(SUM, AVG, MAX, MIN, COUNT, FIRST, LAST, PIPE))
                                // should not rebuild index on property change because technically the group index for all aggregate type is the same
                                .withShouldRebuildIndex(false)
                                .withAllowSelectAll(false)
                                .withMultipleSelect(false)
                                .withIsRequired(true)
                                .withLabelSupplier(uiCallbackContext -> "Aggregate type")
                                .build())
                        .build())
                .withOutputLayouts(outputLayoutBuilder -> outputLayoutBuilder
                        .forOutputNode(OUTPUT_ID, outputColumnBuilder -> outputColumnBuilder
                                .addColumn(GROUP_COLUMN)
                                .addColumn(AGGREGATE_COLUMN)
                                .build())
                        .build())
                .withIcon(StepIcon.FILTER_NONE)
                .build();
    }

    @Override
    public StepProcessor createProcessor(StepProcessorBuilder processorBuilder) {
        return processorBuilder
                .registerIndex(INDEX_NAME, indexBuilder -> indexBuilder
                        .indexTypeRows()
                        .provideIndexValues(ctx -> {
                            final InputColumn groupColumn = ctx.getColumnFromChooserValues(GROUP_COLUMN_PROP).get(0);
                            final long rowCount = ctx.getInputContext(INPUT_ID).orElseThrow(IllegalArgumentException::new)
                                    .getRowCount();

                            final Set<Integer> constructedGroup = new HashSet<>();
                            final BitSet visitedRows = new BitSet((int) rowCount);
                            final AtomicInteger outputRowCount = new AtomicInteger();

                            // the first index's row is just the total aggregate row number
                            ctx.appendRow(() -> Collections.singletonList(outputRowCount.get()));

                            for (long i = 0; i < rowCount; i++) {
                                final int currentGroupRow = (int) i;
                                final CellValue currentGroup = groupColumn.getValueAt(i);
                                final int currentGroupHash = currentGroup.hashCode();
                                if (constructedGroup.contains(currentGroupHash)) {
                                    // this group already fully constructed, we don't have to iterate the rows again
                                    continue;
                                }

                                // index structure outputRowIndex + 1 -> [groupValue, aggregateColumnRow1Value, aggregateColumnRow2Value, ...]
                                ctx.appendRow(() -> {
                                    // This callback is executed lazily as iterator exactly before the index row is written.
                                    final List<Integer> groupRow = new ArrayList<>();
                                    // first index column is always the group values.
                                    groupRow.add(currentGroupRow);

                                    // safe to cast to int here since max rowcount for index type rows is Integer.MAX_VALUE
                                    for (int row = visitedRows.nextClearBit(0); row < rowCount; row++) {
                                        if (visitedRows.get(row) || !groupColumn.getValueAt(row).equals(currentGroup)) {
                                            continue; // this row belong to another group, so skip...
                                        }
                                        groupRow.add(row);
                                        visitedRows.set(row);
                                    }
                                    return groupRow;
                                });
                                constructedGroup.add(currentGroupHash);
                            }
                            outputRowCount.set(constructedGroup.size());
                        })
                        .build())
                .forOutputNode(OUTPUT_ID, (ctx, columnManager) -> {
                    //noinspection unchecked
                    final String aggregateType = ((List<String>) ctx.getStepPropertyValue(AGGREGATE_TYPE_PROP).orElseThrow(IllegalArgumentException::new)).get(0);

                    columnManager.onValue(GROUP_COLUMN, row -> {
                        final List<CellValue> indexRow = ctx.getIndexRowValues(INDEX_NAME, (int) row + 1);
                        final InputColumn groupColumn = ctx.getColumnFromChooserValues(GROUP_COLUMN_PROP).get(0);
                        if (indexRow.isEmpty()) {
                            throw new IllegalStateException("Index row " + row + " must not be empty.");
                        }
                        return groupColumn.getValueAt(indexRow.get(0).toLong());
                    });

                    columnManager.onValue(AGGREGATE_COLUMN, row -> {
                        final List<CellValue> indexRow = ctx.getIndexRowValues(INDEX_NAME, (int) row + 1);
                        final InputColumn aggregateColumn = ctx.getColumnFromChooserValues(AGGREGATE_COLUMN_PROP).get(0);
                        if (indexRow.size() <= 1) {
                            throw new IllegalStateException("Index row " + row + " has invalid structure: index columns size <= 1");
                        }
                        final List<CellValue> values = indexRow.subList(1, indexRow.size());
                        switch (aggregateType) {
                            case SUM:
                                return values.stream().map(v -> aggregateColumn.getValueAt(v.toLong())).mapToDouble(v -> v.toDouble()).sum();
                            case AVG:
                                return values.stream().map(v -> aggregateColumn.getValueAt(v.toLong())).mapToDouble(v -> v.toDouble()).average().orElse(0.0);
                            case MAX:
                                return values.stream().map(v -> aggregateColumn.getValueAt(v.toLong())).mapToDouble(v -> v.toDouble()).max().orElse(0.0);
                            case MIN:
                                return values.stream().map(v -> aggregateColumn.getValueAt(v.toLong())).mapToDouble(v -> v.toDouble()).min().orElse(0.0);
                            case COUNT:
                                return values.size();
                            case FIRST:
                                return aggregateColumn.getValueAt(values.get(0).toLong());
                            case LAST:
                                return aggregateColumn.getValueAt(values.get(values.size() - 1).toLong());
                            case PIPE:
                                return values.stream().map(v -> aggregateColumn.getValueAt(v.toLong()).toString()).collect(Collectors.joining("|"));
                            default:
                                throw new IllegalStateException("Unsupported aggregate operation: " + aggregateType);
                        }
                    });

                    final List<CellValue> indexRow = ctx.getIndexRowValues(INDEX_NAME, 0);
                    if (indexRow.isEmpty()) {
                        throw new IllegalStateException("Index row " + 0 + " must not be empty.");
                    }
                    return indexRow.get(0).toLong();
                })
                .build();
    }

    @Override
    public CustomTypeMetadata createMetadata(CustomTypeMetadataBuilder metadataBuilder) {
        return metadataBuilder
                .withName("Example: Aggregate Step")
                .withDescription("Step to demonstrate aggregate using index API")
                .withMajorVersion(0)
                .withMinorVersion(0)
                .withPatchVersion(0)
                .withDeveloper("Experian")
                .withLicense("Apache License Version 2.0")
                .build();
    }
}
