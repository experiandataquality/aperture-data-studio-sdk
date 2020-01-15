package com.experian.aperture.datastudio.sdk.step.examples;

import com.experian.datastudio.sdk.api.CustomTypeMetadata;
import com.experian.datastudio.sdk.api.CustomTypeMetadataBuilder;
import com.experian.datastudio.sdk.api.step.Column;
import com.experian.datastudio.sdk.api.step.CustomStepDefinition;
import com.experian.datastudio.sdk.api.step.configuration.ConfigurationInputContext;
import com.experian.datastudio.sdk.api.step.configuration.StepConfiguration;
import com.experian.datastudio.sdk.api.step.configuration.StepConfigurationBuilder;
import com.experian.datastudio.sdk.api.step.configuration.StepIcon;
import com.experian.datastudio.sdk.api.step.processor.*;
import com.experian.datastudio.sdk.lib.logging.SdkLogManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Optional;

/**
 * Creates a step that adds a user-defined VAT percentage to an input column.
 * The exact column will be defined by the user, and will be replaced by a
 * new column that is renamed with the updated.
 *
 * Limitation:
 * Not able to determine OutputLayout (output column name) based on input cell value type
 */
public class AddVAT implements CustomStepDefinition {

    private static final Logger LOGGER = SdkLogManager.getLogger(AddVAT.class, Level.INFO);
    private static final String INPUT_ID = "input-1";
    private static final String OUTPUT_ID = "output-1";
    private static final String ARG_ID_COLUMN_CHOOSER = "ColumnChooser";
    private static final String ARG_ID_VAT_RATE = "vat-rate";
    private static final Double VAT_RATE = 17.5;
    private static final String COLUMN_HEADER_SUFFIX = " plus VAT";

    @Override
    public CustomTypeMetadata createMetadata(final CustomTypeMetadataBuilder metadataBuilder) {
        return metadataBuilder
                .withName("Example: AddVAT")
                .withDescription("Add VAT to an existing column")
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
                                .asColumnChooser(ARG_ID_COLUMN_CHOOSER)
                                .forInputNode(INPUT_ID)
                                .withLabelSupplier(context -> "Select a column")
                                .withIsRequired(true)
                                .build())
                        .addStepProperty(stepPropertyBuilder -> stepPropertyBuilder
                                .asNumber(ARG_ID_VAT_RATE)
                                .withAllowDecimal(true)
                                .withDefaultValue(VAT_RATE)
                                .withLabelSupplier(context -> "Enter VAT rate")
                                .withIsRequired(true)
                                .build())
                        .build())
                .withOutputLayouts(outputLayoutBuilder -> outputLayoutBuilder
                        .forOutputNode(OUTPUT_ID, outputColumnBuilder -> outputColumnBuilder
                                .addColumns(context -> {
                                    final ConfigurationInputContext inputContext = context.getInputContext(INPUT_ID);
                                    final List<Column> columns = inputContext.getColumns();
                                    final List<Column> columnChooserValues = context.getColumnFromChooserValues(ARG_ID_COLUMN_CHOOSER);
                                    if (!columnChooserValues.isEmpty()) {
                                        final Column selectedColumn = columnChooserValues.get(0);
                                        final Column newColumn = context.createNewColumn(selectedColumn.getName() + COLUMN_HEADER_SUFFIX);
                                        final int columnIndex = columns.indexOf(selectedColumn);
                                        columns.remove(columnIndex);
                                        columns.add(columnIndex, newColumn);
                                    }
                                    return columns;
                                })
                                .build())
                        .build())
                .withIcon(StepIcon.ADD)
                .build();
    }

    @Override
    public StepProcessor createProcessor(final StepProcessorBuilder processorBuilder) {
        return processorBuilder
                .forOutputNode(OUTPUT_ID, (processorContext, outputColumnManager) -> {
                    final Optional<Number> vatRateOptional = processorContext.getStepPropertyValue(ARG_ID_VAT_RATE);
                    final Number vatRate = vatRateOptional.orElseThrow(() -> new IllegalArgumentException("StepPropertyValue [" + ARG_ID_VAT_RATE + "] not found."));

                    final List<InputColumn> columnChooserValues = processorContext.getColumnFromChooserValues(ARG_ID_COLUMN_CHOOSER);
                    if (!columnChooserValues.isEmpty()) {
                        final InputColumn column = columnChooserValues.get(0);
                        LOGGER.info("Selected column={}, VAT rate={}", column.getName(), vatRate);
                        outputColumnManager.onValue(column.getName() + COLUMN_HEADER_SUFFIX, rowIndex -> {
                            final CellValue cellValue = column.getValueAt(rowIndex);
                            final double doubleValue = cellValue.toDouble();
                            if (doubleValue != 0.0) {
                                return doubleValue + (doubleValue * vatRate.doubleValue() / 100);
                            } else {
                                return cellValue.toObject();
                            }
                        });
                    }

                    final ProcessorInputContext inputContext = processorContext.getInputContext(INPUT_ID).orElseThrow(IllegalArgumentException::new);
                    return inputContext.getRowCount();
                })
                .build();
    }
}
