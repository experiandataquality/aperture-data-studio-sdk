package com.experian.datax.sdk.step.addons;

import com.experian.sdk.step.*;

import java.util.Arrays;
import java.util.List;

/**
 * This example step demonstrates capture and validation of numbers and
 * a way to link icons with dropdown list items.
 * This view also generates a single cell output based on the entered number.
 */
public class SimpleNumberSource extends StepConfiguration {

    public SimpleNumberSource() {
        // Basic step information
        setStepDefinitionName("Custom - Number Validation");
        setStepDefinitionDescription("Validating user input");
        setStepDefinitionIcon("INTEGER");

        // Define the step properties

        // Create a property to enter/display an even number, with text that changes dependent on what is entered,
        // and an initial value for the number. Validation is performed during the
        // It also defines a single output connection for the step.
        StepProperty arg1 = new StepProperty()
                .ofType(StepPropertyType.INTEGER)
                .withIconTypeSupplier(sp -> () -> "NUMBER")
                .withArgTextSupplier(sp -> () -> {
                    if (sp.getValue() == null || sp.getValue().toString().isEmpty()) {
                        return "Enter an even number";
                    } else {
                        try {
                            return "Even number: " + Integer.parseInt(sp.getValue().toString());
                        } catch (Exception ex) {
                            return "Even number: 78";
                        }
                    }
                })
                .withInitialValue(78)
                .havingOutputNode(() -> "output0")
                .validateAndReturn();

        // Create a dropdown list containing three numbers with icons to match each of them
        final List<String> icons = Arrays.asList("NUMBER_1", "NUMBER_2", "NUMBER_3");
        StepProperty arg2 = new StepProperty()
                .ofType(StepPropertyType.STRING)
                .withIconTypeSupplier(sp -> () -> (sp.getValue() == null || sp.getValue().toString().isEmpty())
                        ? "MENU"
                        : icons.get(Integer.parseInt(sp.getValue().toString()) - 1)
                )
                .withArgTextSupplier(sp -> () -> (sp.getValue() == null || sp.getValue().toString().isEmpty()) ? "Select number" : "Number: " + sp.getValue().toString())
                .withAllowedValuesProvider(() -> Arrays.asList("1", "2", "3"))
                .withInitialValue(1)
                .validateAndReturn();

        setStepProperties(Arrays.asList(arg1, arg2));

        // Define and set the step output class
        setStepOutput(new MyStepOutput());
    }

    /**
     * Validate that the number properties have valid values and that the first is an even number.
     * If invalid, return false. Data Rows will be disabled as will workflow execution/export.
     * @return false - will not be able to execute/export/show data
     *         true  - will be able to execute/export/show data
     *         null  - will revert back to default behaviour, i.e. enabled if step has inputs, and are they complete?
     */
    @Override
    public Boolean isComplete() {
        // if false will not be able to execute/export/show data
        // if true, will be able to execute/export/show data
        // if null, will revert back to default behaviour, i.e. enabled if step has inputs, and are they complete?
        List<StepProperty> properties = getStepProperties();
        if (properties != null && !properties.isEmpty()) {
            StepProperty arg1 = properties.get(0);
            if (arg1 != null) {
                try {
                    Integer userDefinedInteger = Integer.parseInt(arg1.getValue().toString());
                    if (userDefinedInteger != null && userDefinedInteger % 2 == 0) {
                        return true;
                    }
                } catch (NumberFormatException ex) {
                }
            }
        }
        return false;
    }

    /**
     * define the output data view, i.e. rows and columns
     */
    private class MyStepOutput extends StepOutput {
        @Override
        public String getName() {
            return "Validate Number";
        }

        /**
         * Adds a single column to the output
         * @throws Exception
         */
        @Override
        public void initialise() throws Exception {
            getColumnManager().clearColumns();
            getColumnManager().addColumn(this, "User Defined Integer", "The Even Integer entered by the user");
        }

        /**
         * Returns a row count of 1
         * @return Row count
         * @throws Exception
         */
        @Override
        public long execute() throws Exception {
            return 1;
        }

        /**
         * The single cell contains the number defined by the first step property
         * @param row The row number required
         * @param col The column number required
         * @return The value
         * @throws Exception
         */
        @Override
        public Object getValueAt(long row, int col) throws Exception {
            return "Number: " + getArgument(0);
        }
    }
}

