package com.experian.aperture.datastudio.sdk.step.examples;

import com.experian.datastudio.sdk.api.CustomTypeMetadata;
import com.experian.datastudio.sdk.api.CustomTypeMetadataBuilder;
import com.experian.datastudio.sdk.api.step.CustomStepDefinition;
import com.experian.datastudio.sdk.api.step.configuration.*;
import com.experian.datastudio.sdk.api.step.processor.StepProcessor;
import com.experian.datastudio.sdk.api.step.processor.StepProcessorBuilder;

public class IPGeolocation implements CustomStepDefinition {

    private static final String INPUT_ID = "input0";
    private static final String OUTPUT_ID = "output0";
    private static final String COLUMN_CHOOSER_ID = "columnchooser0";
    private static final String SETTING_ID = "setting0";
    private static final String COLUMN_HEADER_COUNTRY_NAME = "Country Name";

    @Override
    public CustomTypeMetadata createMetadata(CustomTypeMetadataBuilder metadataBuilder) {
        return metadataBuilder
                .withName("Example: IP Geolocation")
                .withDescription("Language settings for Country Name")
                .withMajorVersion(0)
                .withMinorVersion(0)
                .withPatchVersion(0)
                .withDeveloper("Experian")
                .withLicense("Apache License Version 2.0")
                .build();
    }

    @Override
    public StepConfiguration createConfiguration(StepConfigurationBuilder configurationBuilder) {
        return configurationBuilder
                .withNodes(stepNodeBuilder -> stepNodeBuilder
                        .addInputNode(inputNodeBuilder ->inputNodeBuilder
                                .withId(INPUT_ID)
                                .withIsRequired(true)
                                .build())
                        .addOutputNode(OUTPUT_ID)
                        .build())
                .withStepProperties(stepPropertiesBuilder -> stepPropertiesBuilder
                        .addStepProperty(stepPropertyBuilder -> stepPropertyBuilder
                                .asColumnChooser(COLUMN_CHOOSER_ID)
                                .forInputNode(INPUT_ID)
                                .withIsRequired(true)
                                .withMultipleSelect(false)
                                .withAllowSelectAll(false)
                                .withAllowSearch(true)
                                .build())
                        .build())
                .withOutputLayouts(outputLayoutBuilder -> outputLayoutBuilder
                        .forOutputNode(OUTPUT_ID, outputColumnBuilder -> outputColumnBuilder
                                .addColumns(outputColumnBuilderContext -> outputColumnBuilderContext
                                        .getInputContext(INPUT_ID)
                                        .getColumns())
                                .addColumn(COLUMN_HEADER_COUNTRY_NAME)
                                .build())
                        .build())
                .withIcon(StepIcon.ROOM)
                .withStepSetting(customStepSettingBuilder -> customStepSettingBuilder
                        .addField(customStepSettingFieldBuilder -> customStepSettingFieldBuilder
                                .withId(SETTING_ID)
                                .withName("Lang Localization Setting (e.g. en, de, zh-CN)")
                                .withIsRequired(true)
                                .withFieldType(StepSettingType.TEXT)
                                .build())
                        .build())
                .build();
    }

    @Override
    public StepProcessor createProcessor(StepProcessorBuilder processorBuilder) {
        // StepProcessorFunction for IP Geolocation example step is extracted to IPGeolocationProcessor
        return processorBuilder.forOutputNode(OUTPUT_ID, new IPGeolocationProcessor()).build();
    }
}