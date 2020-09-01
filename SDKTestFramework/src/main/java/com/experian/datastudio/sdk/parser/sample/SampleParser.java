package com.experian.datastudio.sdk.parser.sample;

import com.experian.datastudio.sdk.api.CustomTypeMetadata;
import com.experian.datastudio.sdk.api.CustomTypeMetadataBuilder;
import com.experian.datastudio.sdk.api.parser.CustomParserDefinition;
import com.experian.datastudio.sdk.api.parser.configuration.*;

import java.nio.charset.StandardCharsets;

public class SampleParser implements CustomParserDefinition {
    static final String PARAMETER_KEY_ENCODING = "&encoding";
    static final String PARAMETER_KEY_MAX_ROWS = "&maxRow";

    @Override
    public ParserConfiguration createConfiguration(ParserConfigurationBuilder parserConfigurationBuilder) {
        return parserConfigurationBuilder
                .withParserId("&sample_parser")
                .withSupportedFileExtensions(supportedFileExtensionsBuilder -> supportedFileExtensionsBuilder
                        .add(supportedFileExtensionBuilder -> supportedFileExtensionBuilder
                                .withSupportedFileExtension("sample")
                                .withFileExtensionName("sample name")
                                .withFileExtensionDescription("sample description")
                                .build())
                        .build())
                .withParserParameterDefinition(parameterDefinitionsBuilder -> parameterDefinitionsBuilder
                        .add(parameterDefinitionBuilder -> parameterDefinitionBuilder
                                .withId(PARAMETER_KEY_ENCODING)
                                .withName("Encoding")
                                .withDescription("Select encoding")
                                .setAsRequired(true)
                                .setTypeAs(ParserParameterValueType.STRING)
                                .setDisplayTypeAs(ParserParameterDisplayType.CHARSET)
                                .withDefaultValueAsString(StandardCharsets.UTF_8.name())
                                .affectsTableStructure(false)
                                .build())
                        .add(parameterDefinitionBuilder -> parameterDefinitionBuilder
                                .withId(PARAMETER_KEY_MAX_ROWS)
                                .withName("Max row")
                                .withDescription("Maximum row displayed")
                                .setAsRequired(false)
                                .setTypeAs(ParserParameterValueType.INTEGER)
                                .setDisplayTypeAs(ParserParameterDisplayType.TEXTFIELD)
                                .withDefaultValueAsNumber(10)
                                .affectsTableStructure(false)
                                .build())
                        .build())
                .withProcessor(new SampleParserProcessor())
                .build();
    }

    @Override
    public CustomTypeMetadata createMetadata(CustomTypeMetadataBuilder metadataBuilder) {
        return metadataBuilder
                .withName("Sample parser")
                .withDescription("The sample parser")
                .withMajorVersion(1)
                .withMinorVersion(0)
                .withPatchVersion(0)
                .withDeveloper("Test")
                .withLicense("License 1.0")
                .build();
    }
}
