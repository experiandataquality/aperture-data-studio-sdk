package com.experian.datax.sdk.step.addons;

import com.experian.sdk.step.*;

import java.util.Arrays;
import java.util.List;

public class NumberArgValidation extends StepConfiguration {

    public NumberArgValidation() {
        init();
    }

    private void init() {

        /*  STEP_DEFINITION  */
        setStepDefinitionName("Custom - Number Validation");
        setStepDefinitionDescription("Validating user input");
        setStepDefinitionIcon("NO");

        /*  QUERY_BUILDER_STEP  */
        StepProperty arg1 = new StepProperty()
                .ofType(StepPropertyType.INTEGER)
                .withStatusIndicator(sp -> () -> true)
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

        /*  ABSTRACT_VIEW   */
        setStepOutput(new MyStepOutput());
    }

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

    private class MyStepOutput extends StepOutput {
        @Override
        public String getName() {
            return "Validate Number";
        }

        @Override
        public void initialise() throws Exception {
            getColumnManager().addColumn(this, "User Defined Integer", "The Even Integer entered by the user");
        }

        @Override
        public long execute() throws Exception {
            return 1;
        }

        @Override
        public Object getValueAt(long row, int col) throws Exception {
            return "Number: " + getArgument(0);
        }
    }
}

