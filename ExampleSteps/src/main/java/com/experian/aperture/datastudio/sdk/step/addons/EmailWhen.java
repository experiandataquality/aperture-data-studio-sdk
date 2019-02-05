package com.experian.aperture.datastudio.sdk.step.addons;

import com.experian.aperture.datastudio.sdk.exception.SDKException;
import com.experian.aperture.datastudio.sdk.step.StepConfiguration;
import com.experian.aperture.datastudio.sdk.step.StepOutput;
import com.experian.aperture.datastudio.sdk.step.StepProperty;
import com.experian.aperture.datastudio.sdk.step.StepPropertyType;

import java.util.Arrays;
import java.util.List;

/**
 * A template email process step can be inserted into a data flow at any point, and is used to
 * (optionally) examine the data or some other state, and fire off an email (not implemented) when necessary.
 * This is an example of a process step, as it connects anywhere in a workflow, does not change the
 * data, but passes input data to its output unchanged.
 */
public class EmailWhen extends StepConfiguration {

    public EmailWhen() {
        // Basic step information
        setStepDefinitionName("Custom - Email When");
        setStepDefinitionDescription("A process step template to email someone when something happens");
        setStepDefinitionIcon("EMAIL");
        // The PROCESS step type will connect to output data (or process data) and pass that data through
        // to its output unchanged. It can only be connected to an output data step if it has already been connected
        // to an input data step, thus changing its output type from a PROCESS node to a DATA node.
        setStepDefinitionType("PROCESS");

        // Define the step properties:
        // Add a string field to allow entry of an email address,
        // Input and output definitions are not required for PROCESS steps as a single PROCESS input and PROCESS output
        // is added automatically.
        StepProperty arg1 = new StepProperty()
                .ofType(StepPropertyType.STRING)
                .withIconTypeSupplier(sp -> () -> "EMAIL")
                .withArgTextSupplier(sp -> () -> {
                    if (sp.getValue() == null || sp.getValue().toString().isEmpty()) {
                        return "Enter email address";
                    } else {
                        return sp.getValue().toString();
                    }
                })
                .validateAndReturn();

        setStepProperties(Arrays.asList(arg1));

        // Define and set the step output class
        setStepOutput(new EmailWhen.MyStepOutput());
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
        List<StepProperty> properties = getStepProperties();
        if (properties != null && !properties.isEmpty()) {
            StepProperty arg1 = properties.get(0);
            // if email address has been entered. Do additional validation here if necessary.
            if (arg1 != null && arg1.getValue() != null && !arg1.getValue().toString().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * define the output data view, i.e. rows and columns
     * In this case we are not outputting any data.
     * Data passing through is handled automatically.
     */
    private class MyStepOutput extends StepOutput {
        @Override
        public String getName() {
            return "Email When...";
        }

        @Override
        public Object getValueAt(long row, int columnIndex) throws SDKException {
            return null;
        }
    }
}