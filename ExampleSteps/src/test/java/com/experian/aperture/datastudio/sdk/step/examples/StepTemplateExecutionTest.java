package com.experian.aperture.datastudio.sdk.step.examples;

import com.experian.aperture.datastudio.sdk.step.StepConfiguration;
import com.experian.aperture.datastudio.sdk.testframework.ProgressRecord;
import com.experian.aperture.datastudio.sdk.testframework.StepTestBuilder;
import com.experian.aperture.datastudio.sdk.testframework.TestSession;
import com.experian.aperture.datastudio.sdk.testframework.exception.SDKTestException;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import static com.experian.aperture.datastudio.sdk.testframework.ErrorMessages.STEP_IS_COMPLETE_RETURN_FALSE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class StepTemplateExecutionTest {
    private static final String ARG_VALUE = "Color Id";
    private String csvInput = "";
    private StepConfiguration targetStep;

    @Before
    public void setUp() throws URISyntaxException {
        this.targetStep = new StepTemplate();
        this.csvInput = new File(this.getClass().getResource("/InputData.csv").toURI())
                .getAbsolutePath();
    }

    /**
     * Validates that the custom step is adding the correct custom columns at the specified index.
     */
    @Test
    public void stepShouldHaveCorrectOutputColumns() {
        StepTestBuilder.fromCustomStep(this.targetStep)
                .withCsvInput(csvInput)
                .withStepPropertyValue(1, ARG_VALUE) // Select input column
                .build()
                .execute()
                .assertColumnSize(3); // Pass through step, so column count remain the same as input.
    }

    /**
     * Validates that the custom step should not be able to execute if the {@code isComplete()} return {@code false}.
     * In Aperture Data Studio, this would prevent the workflow containing the step would not be able to execute.
     */
    @Test
    public void stepShouldNotBeAbleToExecuteIfItIsNotComplete() {
        assertThatThrownBy(() ->
                StepTestBuilder.fromCustomStep(this.targetStep)
                        .withCsvInput(csvInput)
                        .build()
                        .execute()) // Execute without setting the arguments value (.withStepPropertyValue(...))
                .isInstanceOf(SDKTestException.class)
                .hasMessageContaining(STEP_IS_COMPLETE_RETURN_FALSE.message());
    }

    /**
     * Validates the step execution returns expected column and values.
     */
    @Test
    public void stepShouldExecuteSuccessfully() {
        final int nameColumnIndex = 0;
        final int countryColumnIndex = 1;

        StepTestBuilder.fromCustomStep(this.targetStep)
                .withCsvInput(csvInput)
                .withStepPropertyValue(1, ARG_VALUE)
                .build()
                .execute()
                .assertThatAllRowsIsExecuted() // Convenience method to assert all input csv rows are executed
                .assertColumnValueAt(0, nameColumnIndex, "John Smith")
                .assertColumnValueAt(2, countryColumnIndex, "Canada")
                .waitForAssertion();
    }

    /**
     * Validates that the custom step should update the progress when the workflow is executing.
     */
    @Test
    public void stepShouldExecuteByChunkAndReportProgressAccordingly() {
        final String progressMessage = "Passthrough Step";

        // Current step is not updating custom progress. A default progress on 100%
        // completion will be reported by default.
        final List<ProgressRecord> expectedProgressRecords = Arrays.asList(
                new ProgressRecord(progressMessage, 1.0));

        final TestSession test = StepTestBuilder.fromCustomStep(this.targetStep)
                .withCsvInput(csvInput)
                .withStepPropertyValue(1, ARG_VALUE)
                .build();
        test.execute();

        assertThat(test.getProgressRecords()).containsSequence(expectedProgressRecords);
    }

    /**
     * Validates the step execution process the requested rows only in interactive mode.
     * assertColumnValueAt() simulates the current rows/column appearing in the grid in interactive mode.
     */
    @Test
    public void stepShouldGetResultInRealTimeWhenItIsInteractive() {
        final int nameColumnIndex = 0;
        final int countryColumnIndex = 1;

        StepTestBuilder.fromCustomStep(this.targetStep)
                .withCsvInput(csvInput)
                .withStepPropertyValue(1, ARG_VALUE)
                .isInteractive(true)
                .build()
                .execute()
                .assertColumnValueAt(8, nameColumnIndex, "Vanya Stravorski")
                .assertColumnValueAt(9, countryColumnIndex, "Singapore")
                .waitForAssertion();
    }
}
