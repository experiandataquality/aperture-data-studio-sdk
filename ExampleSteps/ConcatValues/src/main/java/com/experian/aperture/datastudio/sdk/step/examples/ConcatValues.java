package com.experian.aperture.datastudio.sdk.step.examples;

import com.experian.datastudio.sdk.api.CustomTypeMetadata;
import com.experian.datastudio.sdk.api.CustomTypeMetadataBuilder;
import com.experian.datastudio.sdk.api.step.CustomStepDefinition;
import com.experian.datastudio.sdk.api.step.configuration.StepConfiguration;
import com.experian.datastudio.sdk.api.step.configuration.StepConfigurationBuilder;
import com.experian.datastudio.sdk.api.step.configuration.StepIcon;
import com.experian.datastudio.sdk.api.step.processor.*;
import com.experian.datastudio.sdk.lib.logging.SdkLogManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This is a custom step definition that concatenates two
 * user-defined columns together using a user-defined delimiter
 * into a new column inserted after the other input columns.
 */
public class ConcatValues implements CustomStepDefinition {

    private static final Logger LOGGER = SdkLogManager.getLogger(ConcatValues.class, Level.INFO);
    private static final String INPUT_ID = "input-1";
    private static final String OUTPUT_ID = "output-1";
    private static final String ARG_ID_COLUMN_CHOOSER_1ST = "ColumnChooser-1";
    private static final String ARG_ID_COLUMN_CHOOSER_2ND = "ColumnChooser-2";
    private static final String ARG_ID_CUSTOM_CHOOSER = "CustomChooser";
    private static final String OUTPUT_COLUMN_HEADER = "Concat Value";

    @Override
    public CustomTypeMetadata createMetadata(final CustomTypeMetadataBuilder metadataBuilder) {
        return metadataBuilder
                .withName("Example: Concatenate Two Columns")
                .withDescription("Concatenate two columns using selected delimiter")
                .withMajorVersion(0)
                .withMinorVersion(0)
                .withPatchVersion(0)
                .withDeveloper("Experian")
                .withLicense("Apache License Version 2.0")
                .build();
    }

    @Override
    public StepConfiguration createConfiguration(final StepConfigurationBuilder configurationBuilder) {
        return configurationBuilder
                .withNodes(stepNodeBuilder -> stepNodeBuilder
                        .addInputNode(inputNodeBuilder -> inputNodeBuilder
                                .withId(INPUT_ID)
                                .withIsRequired(true)
                                .withLabelDisplayed(false)
                                .build())
                        .addOutputNode(OUTPUT_ID)
                        .build())
                .withStepProperties(stepPropertiesBuilder -> stepPropertiesBuilder
                        .addStepProperty(stepPropertyBuilder -> stepPropertyBuilder
                                .asColumnChooser(ARG_ID_COLUMN_CHOOSER_1ST)
                                .forInputNode(INPUT_ID)
                                .withLabelSupplier(context -> "Select first column")
                                .withIsRequired(true)
                                .build())
                        .addStepProperty(stepPropertyBuilder -> stepPropertyBuilder
                                .asColumnChooser(ARG_ID_COLUMN_CHOOSER_2ND)
                                .forInputNode(INPUT_ID)
                                .withLabelSupplier(context -> "Select second column")
                                .withIsRequired(true)
                                .build())
                        .addStepProperty(stepPropertyBuilder -> stepPropertyBuilder
                                .asCustomChooser(ARG_ID_CUSTOM_CHOOSER)
                                .withAllowValuesProvider(context -> Arrays.asList("Comma", "Space", "Pipe", "Colon"))
                                .withAllowSearch(true)
                                .build())
                        .build())
                .withOutputLayouts(outputLayoutBuilder -> outputLayoutBuilder
                        .forOutputNode(OUTPUT_ID, outputColumnBuilder -> outputColumnBuilder
                                .addColumns(context -> context.getInputContext(INPUT_ID).getColumns())
                                .addColumn(OUTPUT_COLUMN_HEADER)
                                .build())
                        .build())
                .withIcon(StepIcon.JOIN)
                .build();
    }

    @Override
    public StepProcessor createProcessor(final StepProcessorBuilder processorBuilder) {
        return processorBuilder
                .forOutputNode(OUTPUT_ID, (processorContext, outputColumnManager) -> {
                    @SuppressWarnings("unchecked")
                    final List<String> customChooser = (List<String>) processorContext
                            .getStepPropertyValue(ARG_ID_CUSTOM_CHOOSER)
                            .orElse(Collections.emptyList());
                    final String delimiterString = customChooser.isEmpty() ? "" : customChooser.get(0);
                    String delimiter;

                    switch (delimiterString) {
                        case "Comma":
                            delimiter = ", ";
                            break;
                        case "Space":
                            delimiter = " ";
                            break;
                        case "Pipe":
                            delimiter = " | ";
                            break;
                        case "Colon":
                        default:
                            delimiter = " : ";
                            break;
                    }

                    final List<InputColumn> firstColumnList = processorContext.getColumnFromChooserValues(ARG_ID_COLUMN_CHOOSER_1ST);
                    final List<InputColumn> secondColumnList = processorContext.getColumnFromChooserValues(ARG_ID_COLUMN_CHOOSER_2ND);
                    final InputColumn firstColumn = firstColumnList.isEmpty() ? null : firstColumnList.get(0);
                    final InputColumn secondColumn = secondColumnList.isEmpty() ? null : secondColumnList.get(0);
                    LOGGER.info("1st column={}, 2nd column={}, delimiter={}", firstColumn == null ? "NULL" : firstColumn.getName(), secondColumn == null ? "NULL" : secondColumn.getName(), delimiter);

                    if (firstColumn != null && secondColumn != null) {
                        outputColumnManager.onValue(OUTPUT_COLUMN_HEADER, (rowIndex,outputCellBuilder) -> {
                            final CellValue cellValue1 = firstColumn.getValueAt(rowIndex);
                            final CellValue cellValue2 = secondColumn.getValueAt(rowIndex);
                            final String value = cellValue1.toString() + delimiter + cellValue2.toString();
                            return outputCellBuilder
                                    .withValue(value)
                                    .withStyle(CustomValueStyle.SUCCESS)
                                    .build();
                        });
                    }
                    final ProcessorInputContext inputContext = processorContext.getInputContext(INPUT_ID).orElseThrow(IllegalArgumentException::new);
                    return inputContext.getRowCount();
                })
                .build();
    }
}
