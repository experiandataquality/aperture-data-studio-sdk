package com.experian.datastudio.sdk.parser.sample;

import com.experian.datastudio.sdk.testframework.SDKTestFramework;
import com.experian.datastudio.sdk.testframework.assertion.TableResultAssert;
import com.experian.datastudio.sdk.testframework.customparser.*;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

public class SampleParserTest {

    @Test
    public void testCustomParser() {
        final TestParser parser = SDKTestFramework.createTestParser(
                parserBuilder -> parserBuilder
                        .loadCustomParser(customParserLoader ->
                                customParserLoader.fromParserDefinition(new SampleParser()))
                        .withLocale(Locale.CANADA)
                        .addParserParameter("&encoding", "UTF-8")
                        .build());

        final TestParserSource source = SDKTestFramework.createTestParserSource(
                parserSourceBuilder -> parserSourceBuilder
                        .loadFile("/csvdata/columns_123.sample")
                        .build());

        final TestParserSetting setting = SDKTestFramework.createTestParserSetting(
                parserSettingBuilder -> parserSettingBuilder
                        .assignDataTypeToColumn(ParserDataType.NUMERIC, "Customer Id")
                        .build());

        final ParserTestSuite testSuite = ParserTestSuiteBuilderFactory
                .newBuilder()
                .withParser(parser)
                .withSource(source)
                .withSetting(setting)
                .build();

        final ParserTestResult result = testSuite.execute();
        assertThat(result.getFirstTableDefinition().getName()).isEqualTo("MySample");
        TableResultAssert.assertThat(result.getTableResult("1")).compareOutputWithCsv("/csvdata/columns_123.sample");
    }
}