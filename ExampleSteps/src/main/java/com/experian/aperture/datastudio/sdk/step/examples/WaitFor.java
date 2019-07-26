package com.experian.aperture.datastudio.sdk.step.examples;

import com.experian.aperture.datastudio.sdk.exception.SDKException;
import com.experian.aperture.datastudio.sdk.step.StepConfiguration;
import com.experian.aperture.datastudio.sdk.step.StepOutput;
import com.experian.aperture.datastudio.sdk.step.StepProperty;
import com.experian.aperture.datastudio.sdk.step.StepPropertyType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * A simple demonstration of process steps.
 * This step that waits for a (user-configurable) number of milliseconds before continuing.
 */
public class WaitFor extends StepConfiguration {

    public WaitFor() {
        // Basic step information
        setStepDefinitionName("Custom - Wait For");
        setStepDefinitionDescription("Wait for a trigger before continuing");
        setStepDefinitionIcon("PAUSE");
        // A PROCESS_ONLY step can only be connected to other processes - it cannot be connected to data inputs/outputs.
        setStepDefinitionType("PROCESS_ONLY");

        // Define the step properties:
        // Add a number field to allow specification of the number of milliseconds to wait,
        // Input and output definitions are not required for PROCESS steps as a single PROCESS input and PROCESS output
        // is added automatically.
        final StepProperty arg1 = new StepProperty()
                .ofType(StepPropertyType.DECIMAL)
                .withIconTypeSupplier(sp -> () -> "NUMBER")
                .withArgTextSupplier(sp -> () -> {
                    if (sp.getValue() == null || sp.getValue().toString().isEmpty()) {
                        return "Wait for milliseconds";
                    } else {
                        String stringValue = sp.getValue().toString();
                        return tryParsePositiveInteger(stringValue).isPresent() ? "Wait for milliseconds: " + stringValue : "Enter milliseconds";
                    }
                })
                .withStatusIndicator(sp -> () -> {
                    return sp.getValue() != null && tryParsePositiveInteger(sp.getValue().toString()).isPresent();
                })
                .validateAndReturn();

        setStepProperties(Arrays.asList(arg1));

        // Define and set the step output class
        setStepOutput(new MyStepOutput());
    }

    private static Optional<Integer> tryParsePositiveInteger(String millisecondsString){
        Integer milliseconds = null;

        try {
            milliseconds = Integer.parseInt(millisecondsString);

            if (milliseconds >= 0) {
                return Optional.of(milliseconds);
            } else {
                return Optional.empty();
            }

        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Validate that the step has been configured correctly.
     * If so, return null (the default, or true - it doesn't matter) to enable the rows drilldown,
     * and enable the workflow to be considered valid for execution and export.
     * If invalid, return false. Data Rows will be disabled as will workflow execution/export.
     * @return false - will not be able to execute/export/show data
     *         true  - will be able to execute/export/show data
     *         null  - will revert back to default behaviour, i.e. enabled if step has inputs, and are they complete?
     */
    @Override
    public Boolean isComplete() {
        final List<StepProperty> properties = getStepProperties();
        if (properties != null && !properties.isEmpty()) {
            final StepProperty arg1 = properties.get(0);
            if (arg1 != null && arg1.getValue() != null && !arg1.getValue().toString().isEmpty() && tryParsePositiveInteger(arg1.getValue().toString()).isPresent()) {
                return true;
            }
        }
        return false;
    }

    /**
     * define the output data view, i.e. rows and columns
     * In this case we are not outputting any data at all,
     * but we do our waiting in the execute method.
     */
    private class MyStepOutput extends StepOutput {
        @Override
        public String getName() {
            return "Wait For";
        }

        @Override
        public long execute() throws SDKException {
            try {
                final String millisecondsString = getArgument(0);
                final Optional<Integer> milliseconds = tryParsePositiveInteger(millisecondsString);

                if (milliseconds.isPresent()){
                    Thread.sleep(milliseconds.get());
                }

            } catch (InterruptedException e) {
                // do nothing instead of interrupting the thread.
            }
            return 0;
        }

        @Override
        public Object getValueAt(final long row, final int columnIndex) throws SDKException {
            return null;
        }
    }
}