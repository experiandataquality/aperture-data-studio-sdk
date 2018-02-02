/**
 * Copyright © 2017 Experian plc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.experian.aperture.datastudio.sdk.step.addons;

import com.experian.aperture.datastudio.sdk.exception.SDKException;
import com.experian.aperture.datastudio.sdk.step.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Creates a step that adds a user-defined VAT percentage to an input column.
 * The exact column will be defined by the user, and will be replaced by a
 * new column that is renamed with the updated.
 */
public class AddVAT extends StepConfiguration {

    public AddVAT() {
        // Basic step information
        setStepDefinitionName("Custom - Add VAT");
        setStepDefinitionDescription("Add VAT to an existing column");
        setStepDefinitionIcon("PERCENT");

        // Define the step properties:
        // Add a column chooser based on the columns from the first input,
        // Add status indicator to display the property in red if the input is not connected (and there are no columns)
        // Add text supplier to display the appropriate text in the property
        // And define an input and output connection for the step
        StepProperty arg1 = new StepProperty()
                .ofType(StepPropertyType.COLUMN_CHOOSER)
                .withStatusIndicator(sp -> () -> sp.allowedValuesProvider != null)
                .withIconTypeSupplier(sp -> () -> sp.allowedValuesProvider == null ? "ERROR" : "OK")
                .withArgTextSupplier(sp -> () -> sp.allowedValuesProvider == null ? "Connect an input for columns" : (sp.getValue() == null ? "Select a column" : sp.getValue().toString()))
                .havingInputNode(() -> "input0")
                .havingOutputNode(() -> "output0")
                .validateAndReturn();

        // Add a number text entry box
        // Set the text of the property to show user's value or the default value
        StepProperty arg2 = new StepProperty()
                .ofType(StepPropertyType.DECIMAL)
                .withIconTypeSupplier(sp -> () -> "NUMBER")
                .withArgTextSupplier(sp -> () -> {
                    if (sp.getValue() == null || sp.getValue().toString().isEmpty()) {
                        return "Enter VAT rate";
                    } else {
                        try {
                            return "VAT rate: " + Float.parseFloat(sp.getValue().toString());
                        } catch (NumberFormatException ex) {
                            return "VAT rate: 17.5";
                        }
                    }
                })
                .withInitialValue(17.5)
                .validateAndReturn();

        setStepProperties(Arrays.asList(arg1, arg2));

        // Define and set the step output class
        setStepOutput(new MyStepOutput());
    }

    /**
     * Validate that the column has been set, and that a valid VAT has been specified
     * If so, return null (the default, or true - it doesn't matter) to enable the rows drilldown,
     * and enable the workflow to be considered valid for execution and export.
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
            StepProperty arg2 = properties.get(1);
            if (arg1 != null && arg2 != null
                    && arg1.getValue() != null && arg2.getValue() != null) {
                log(getStepDefinitionName() + " - Column Name: " + arg1.getValue() + " V.A.T.@ " + arg2.getValue());
                return null;
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
            return "Add VAT";
        }

        /**
         * Perform initialisation for the view.
         * Define the columns that will be output by it using the columnManager.
         * In this case we replace the user-defined input column with our own, so we can set its value ourselves later
         * @throws SDKException
         */
        @Override
        public void initialise() throws SDKException {
            // clear columns so they are not saved, resulting in undefined columns
            getColumnManager().clearColumns();

            // get user-defined column
            String selectedColumnName = getArgument(0);
            if (selectedColumnName != null) {
                // ensure that our output columns pass through all those from the first input (the default behaviour)
                getColumnManager().setColumnsFromInput(getInput(0));
                // fine the user-selected column
                StepColumn selectedColumn = getColumnManager().getColumnByName(selectedColumnName);
                if (selectedColumn != null) {
                    // get it's position in the column list
                    int selectedColumnPosition = getColumnManager().getColumnPosition(selectedColumnName);
                    // remove it
                    getColumnManager().removeColumn(selectedColumnName);
                    // and add our own column in its place, so we can change its value in getValueAt()
                    getColumnManager().addColumnAt(this, selectedColumnName + " plus VAT", "", selectedColumnPosition);
                } else {
                    logError(getStepDefinitionName() + " - Couldn't find a column by the name of: " + selectedColumnName);
                }
            }
        }

        /**
         * Called to obtain the value of any columns we've created.
         * In this case we get the user-defined column in our input, get the value for the given row,
         * and add our VAT value to it.
         * @param row The row number required
         * @param col The column index required
         * @return The value for the required cell
         * @throws SDKException
         */
        @Override
        public Object getValueAt(long row, int col) throws SDKException {
            // get the user-defined VAT value
            Float vat = Float.parseFloat(getArgument(1));
            // get the user-defined column
            String selectedColumnName = getArgument(0);

            // get the column object from the first input
            Optional<StepColumn> inputColumn = null;
            if (selectedColumnName != null && !selectedColumnName.isEmpty()) {
                inputColumn = getInputColumn(0, selectedColumnName);
            }
            if (inputColumn.isPresent() ) {
                try {
                    // get the input column's value for the selected row
                    String value = inputColumn.get().getValue(row).toString();
                    // add VAT and return it
                    Double dValue = Double.parseDouble(value);
                    return dValue + (dValue * vat / 100);
                } catch (Exception e) {
                    throw new SDKException(e);
                }
            } else {
                // if not found return an empty value. We could alternatively throw an error.
                logError(getStepDefinitionName() + " - There was an Error doing getValueAt Row: " + row + ", Column: " + col);
                return "";
            }
        }
    }
}
