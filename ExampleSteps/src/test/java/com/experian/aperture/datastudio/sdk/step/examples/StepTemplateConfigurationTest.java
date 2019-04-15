package com.experian.aperture.datastudio.sdk.step.examples;

import com.experian.aperture.datastudio.sdk.step.StepConfiguration;
import com.experian.aperture.datastudio.sdk.testframework.StepTestBuilder;
import com.experian.aperture.datastudio.sdk.testframework.TestSession;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StepTemplateConfigurationTest {
    private StepConfiguration targetStep;

    @Before
    public void setUp() {
        this.targetStep = new StepTemplate();
    }

    /**
     * Validates that the basic information of the step are correct.
     */
    @Test
    public void stepShouldHaveCorrectAttributes() {
        assertEquals("Custom - Simple Passthrough", this.targetStep.getStepDefinitionName());
        assertEquals("Passes input to output", this.targetStep.getStepDefinitionDescription());
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