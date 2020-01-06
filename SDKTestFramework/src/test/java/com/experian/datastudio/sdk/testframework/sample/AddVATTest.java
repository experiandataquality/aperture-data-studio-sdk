package com.experian.datastudio.sdk.testframework.sample;

import com.experian.datastudio.sdk.testframework.SDKTestFramework;
import com.experian.datastudio.sdk.testframework.TestResult;
import com.experian.datastudio.sdk.testframework.datasource.DataSource;
import com.experian.datastudio.sdk.testframework.datasource.DataSourcesBuilder;
import com.experian.datastudio.sdk.testframework.setting.TestSetting;
import com.experian.datastudio.sdk.testframework.setting.TestSettingBuilder;
import com.experian.datastudio.sdk.testframework.step.AddVAT;
import com.experian.datastudio.sdk.testframework.testdefinition.TestStep;
import com.experian.datastudio.sdk.testframework.testdefinition.TestStepBuilder;
import com.experian.datastudio.sdk.testframework.testdefinition.TestSuite;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

class AddVATTest {
    private static final String OUTPUT_ID = "output-1";

    @Test
    void testAddVAT() {
        TestSetting setting = SDKTestFramework.createTestSetting(testSettingCreator());
        TestStep step = SDKTestFramework.createTestStep(testStepCreator());
        List<DataSource> sources = SDKTestFramework.createTestDataSource(dataSourceCreator());

        TestSuite testSuite = TestSuite
                .builder()
                .withTestSetting(setting)
                .withDataSource(sources)
                .withTestStep(step)
                .build();

        TestResult result = testSuite.executeTest(OUTPUT_ID);
        Assert.assertEquals(3, result.getRowCount());
        Assert.assertEquals(1.175, result.getValueAt(0,0).getValue());
        Assert.assertEquals(BigDecimal.valueOf(11), result.getValueAt("Column2", 0).getValue());
    }

    private static Function<TestSettingBuilder, TestSetting> testSettingCreator(){
        return testSettingBuilder -> testSettingBuilder
                .setStepSetting("setting-1", "Test") //Set step setting field
                .build();
    }

    private static Function<DataSourcesBuilder, List<DataSource>> dataSourceCreator(){
        return dataSourcesBuilder ->
                dataSourcesBuilder
                        .addDataSource(datasource -> datasource
                                .createNewSource("Column1", "Column2", "Column3")
                                .connectToInputNodeIds("input-1")
                                .addRow(1, 11, 111)
                                .addRow(2, 22, 222)
                                .addRow(3, 33, 333)
                                .build())
                        .build();
    }

    private static Function<TestStepBuilder, TestStep> testStepCreator(){
        return testStepBuilder -> testStepBuilder
                .loadCustomStep(CustomStepLoader -> CustomStepLoader
                        .fromStepDefinition(new AddVAT()))
                .withPropertyValue(StepPropertiesValueBuilder -> StepPropertiesValueBuilder
                        .setColumnChooserStepPropertyValue("ColumnChooser", Collections.singletonList(0))
                        .setNumberStepPropertyValue("vat-rate", 17.5)
                        .build())
                .build();
    }
}