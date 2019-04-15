package com.experian.aperture.datastudio.sdk.step.examples.testframework;

import com.experian.aperture.datastudio.sdk.step.StepConfiguration;
import com.experian.aperture.datastudio.sdk.step.StepPropertyType;
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
import java.util.concurrent.CompletableFuture;

import static com.experian.aperture.datastudio.sdk.testframework.ErrorMessages.INVALID_STEP_PROPERTY_VALUE;
import static com.experian.aperture.datastudio.sdk.testframework.ErrorMessages.STEP_IS_COMPLETE_RETURN_FALSE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StepExecutionTest {
    private static final String AUTH_KEY_PROPERTY = "EmailValidation.authKey";
    private static final String AUTH_KEY_VALUE = "key";
    private static final String COLOR_ID_COLUMN = "Color Id";
    private StepConfiguration targetStep;
    private ColorService mockColorService;
    private String csvInput;

    @Before
    public void setUp() throws URISyntaxException {
        // Mock the http service so that step can be unit tested
        mockColorService = mock(ColorService.class);
        this.targetStep = new RestServiceSampleStep(mockColorService);
        this.setupClientMock();
        this.csvInput = new File(this.getClass().getResource("/InputData.csv").toURI())
                .getAbsolutePath();
    }

    /**
     * Validates that the custom step is adding the correct custom columns at the specified index.
     */
    @Test
    public void stepShouldHaveCorrectOutputColumns() {
        int inputColumnLastIndex = 2; // input column count based on the csv test data
        StepTestBuilder.fromCustomStep(this.targetStep)
                .withCsvInput(csvInput)
                .withStepPropertyValue(0, COLOR_ID_COLUMN) // Select input column
                .withStepPropertyValue(1, RestServiceSampleStep.ATTRIBUTE_CODE)
                .build()
                .execute()
                .assertColumnName(++inputColumnLastIndex, RestServiceSampleStep.COLOR_NAME_COLUMN)
                .assertColumnName(++inputColumnLastIndex, RestServiceSampleStep.ADDITIONAL_ATTRIBUTE_COLUMN);
    }

    /**
     * Validates that the custom step is able to choose an argument value when a specific required Server Property
     * exists. The value is controlled in the argument {@code allowedValuesProvider}.
     */
    @Test
    public void stepShouldBeAbleToSelectLicensedArgument() {
        StepTestBuilder.fromCustomStep(this.targetStep)
                .withCsvInput(csvInput)
                .withServerProperty(AUTH_KEY_PROPERTY, AUTH_KEY_VALUE) // Set auth key
                .withStepPropertyValue(0, COLOR_ID_COLUMN) // Select input column
                .withStepPropertyValue(1, RestServiceSampleStep.ATTRIBUTE_YEAR) // Can only be chosen if auth key present
                .build()
                .execute()
                .assertArgumentValue(1, RestServiceSampleStep.ATTRIBUTE_YEAR);
    }

    /**
     * Validates that the custom step is able to choose an argument value when a specific required Server Property
     * exists. The value is controlled in the argument {@code allowedValuesProvider}.
     */
    @Test
    public void stepShouldNotBeAbleToSelectUnlicensedArgument() {
        assertThatThrownBy(() ->
                StepTestBuilder.fromCustomStep(this.targetStep)
                        .withCsvInput(csvInput)
                        .withStepPropertyValue(0, COLOR_ID_COLUMN) // Select input column
                        .withStepPropertyValue(1, RestServiceSampleStep.ATTRIBUTE_YEAR) // Can only be chosen if auth key present
                        .build()
                        .execute())
                .isInstanceOf(SDKTestException.class)
                .hasMessageContaining(
                        String.format(INVALID_STEP_PROPERTY_VALUE.message(), StepPropertyType.CUSTOM_CHOOSER, RestServiceSampleStep.ATTRIBUTE_YEAR));
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
        final int colorNameColumnIndex = 3;
        final int additionalAttributeColumnIndex = 4;
        final int inputRowsCount = 10;

        StepTestBuilder.fromCustomStep(this.targetStep)
                .withCsvInput(csvInput)
                .withServerProperty(AUTH_KEY_PROPERTY, AUTH_KEY_VALUE)
                .withStepPropertyValue(0, COLOR_ID_COLUMN)
                .withStepPropertyValue(1, RestServiceSampleStep.ATTRIBUTE_YEAR)
                .build()
                .execute()
                .assertThatAllRowsIsExecuted() // Convenience method to assert all input csv rows are executed
                .assertColumnValueAt(0, colorNameColumnIndex, "cerulean")
                .assertColumnValueAt(1, additionalAttributeColumnIndex, "2003")
                .waitForAssertion();

        // For workflow execution, non-interactive mode, each rows should calls the rest service.
        // This essentially means the execute() process row by row blocking the progress.
        verify(this.mockColorService, times(inputRowsCount)).get(anyString());
    }

    /**
     * Validates that the custom step should update the progress when the workflow is executing.
     */
    @Test
    public void stepShouldExecuteByChunkAndReportProgressAccordingly() {
        final String progressMessage = "Step test template";
        final List<ProgressRecord> expectedProgressRecords = Arrays.asList(
                new ProgressRecord(progressMessage, 0.2),
                new ProgressRecord(progressMessage, 0.4),
                new ProgressRecord(progressMessage, 0.6),
                new ProgressRecord(progressMessage, 0.8),
                new ProgressRecord(progressMessage, 1.0));

        final TestSession test = StepTestBuilder.fromCustomStep(this.targetStep)
                .withCsvInput(csvInput)
                .withServerProperty(AUTH_KEY_PROPERTY, AUTH_KEY_VALUE)
                .withStepPropertyValue(0, COLOR_ID_COLUMN)
                .withStepPropertyValue(1, RestServiceSampleStep.ATTRIBUTE_YEAR)
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
        final int colorNameColumnIndex = 3;
        final int additionalAttributeColumnIndex = 4;

        StepTestBuilder.fromCustomStep(this.targetStep)
                .withCsvInput(csvInput)
                .withServerProperty(AUTH_KEY_PROPERTY, AUTH_KEY_VALUE)
                .withStepPropertyValue(0, COLOR_ID_COLUMN)
                .withStepPropertyValue(1, RestServiceSampleStep.ATTRIBUTE_YEAR)
                .isInteractive(true)
                .build()
                .execute()
                .assertColumnValueAt(8, colorNameColumnIndex, "true red")
                .assertColumnValueAt(1, additionalAttributeColumnIndex, "2003")
                .waitForAssertion();

        // For interactive mode, step could process the requested row/col in getValueAt(rowIdx, colIdx) without
        // waiting for the execute() to finish. The execute() would just return the number of rows from previous step.
        verify(this.mockColorService, times(2)).get(anyString());
    }

    private void setupClientMock() {
        when(this.mockColorService.get("1"))
                .thenReturn(CompletableFuture.completedFuture(new ColorResponse("cerulean", "#98B2D1", "2000")));
        when(this.mockColorService.get("2"))
                .thenReturn(CompletableFuture.completedFuture(new ColorResponse("fuchsia rose", "#C74375", "2001")));
        when(this.mockColorService.get("3"))
                .thenReturn(CompletableFuture.completedFuture(new ColorResponse("true red", "#BF1932", "2002")));
        when(this.mockColorService.get("4"))
                .thenReturn(CompletableFuture.completedFuture(new ColorResponse("aqua sky", "#7BC4C4", "2003")));
        when(this.mockColorService.get("5"))
                .thenReturn(CompletableFuture.completedFuture(new ColorResponse("tigerlily", "#E2583E", "2004")));
    }
}
