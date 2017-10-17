package com.experian.datax.sdk.step.addons;

import com.experian.sdk.step.*;

import java.util.Collections;

/**
 * The most basic step that takes an input and outputs it unchanged.
 */
public class StepTemplate extends StepConfiguration {

    public StepTemplate() {
        // Basic step information
        setStepDefinitionName("Custom - Simple Passthrough");
        setStepDefinitionDescription("Passes input to output");
        setStepDefinitionIcon("INFO");

        // Define the step properties

        // This property simply defines some text ("Connection") and an associated input and output
        // for the step. Steps can have multiple inputs, and in theory can also have multiple outputs,
        // but they are not modelled for custom steps yet.
        StepProperty arg1 = new StepProperty()
                .ofType(StepPropertyType.INPUT_LABEL)
                .withArgTextSupplier(sp -> () -> "Connection")
                .havingInputNode(() -> "input0")
                .havingOutputNode(() -> "output0")
                .validateAndReturn();

        // Add the step properties
        setStepProperties(Collections.singletonList(arg1));

        // Define and set the step output class
        setStepOutput(new MyStepTemplate());
    }

    /**
     * inner class to define the output of the step, i.e. the columns and rows.
     * In this case only the essential abstract methods are implemented,
     * and the getValueAt method is not even used.
     */
    private class MyStepTemplate extends StepOutput {
        @Override
        public String getName() {
            return "Passthrough Step";
        }

        @Override
        public Object getValueAt(long row, int col) throws Exception {
            // will not be called since we have no custom columns
            return null;
        }
    }
}
