package com.experian.aperture.datastudio.sdk.step.examples.testframework;

import com.experian.aperture.datastudio.sdk.step.StepConfiguration;
import com.experian.aperture.datastudio.sdk.testframework.StepTestBuilder;
import com.experian.aperture.datastudio.sdk.testframework.TestSession;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Test coverage on the step configuration for UI.
 */
public class StepConfigurationTest {
    private StepConfiguration targetStep;

    @Before
    public void setUp() {
        // Mock the http service so that step can be unit tested
        final ColorService mockColorService = mock(ColorService.class);
        this.targetStep = new RestServiceSampleStep(mockColorService);
    }

    /**
     * Validates that the basic information of the step are correct.
     */
    @Test
    public void stepShouldHaveCorrectAttributes() {
        assertEquals("Custom - Rest Service Sample Step", this.targetStep.getStepDefinitionName());
        assertEquals("Custom step that uses rest api service", this.targetStep.getStepDefinitionDescription());
        assertEquals("DATA", this.targetStep.getStepDefinitionType());
    }

    /**
     * Covers the basic requirement of a custom step that validates:
     * 1 - Custom step is in the right package.
     * 2 - {@Code StepOutput} is registered.
     */
    @Test
    public void stepShouldBeAbleToLoadSuccessfully() {
        StepTestBuilder.fromCustomStep(this.targetStep)
                .build();
    }

    /**
     * Validates that the argument defined in {@setStepProperties} is correct.
     */
    @Test
    public void stepShouldHaveCorrectArgument() {
        final int argumentCount = StepTestBuilder.fromCustomStep(this.targetStep)
                .build()
                .getArgumentCount();

        assertEquals(2, argumentCount);
    }

    /**
     * Validates that the inputs and outputs defined in {@setStepProperties} could build successfully with the correct count.
     * Test framework would throw {@link com.experian.aperture.datastudio.sdk.testframework.exception.SDKTestException}
     * if the inputs or outputs fail the validation in {@code build()}.
     */
    @Test
    public void stepShouldHaveCorrectInputsAndOutputs() {
        final TestSession stepTest = StepTestBuilder.fromCustomStep(this.targetStep).build();

        assertEquals(1, stepTest.getInputNodeCount());
        assertEquals(1, stepTest.getOutputNodeCount());
    }
}