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

package com.experian.datax.sdk.step.addons;

import com.experian.datax.sdk.step.*;

import java.util.Arrays;
import java.util.List;

/**
 * This is a custom step definition that concatentates two
 * user-defined columns together using a user-defined delimiter
 * into a new column inserted before the other input columns.
 */
public class ConcatValues extends StepConfiguration {

    public ConcatValues() {
        // Basic step information
        setStepDefinitionName("Custom - Concatenate Two Columns");
        setStepDefinitionDescription("Concatenate two columns using selected delimiter");
        setStepDefinitionIcon("ALPHA_NUMERIC");

        // Define the step properties:
        // A column chooser based on the columns from the first input, defines the first column
        // A fixed list of delimiters
        // Another column chooser based on columns from the first input, defines the second column
        StepProperty arg1 = new StepProperty()
                .ofType(StepPropertyType.COLUMN_CHOOSER)
                .withStatusIndicator(sp -> () -> sp.allowedValuesProvider != null)
                .withIconTypeSupplier(sp -> () -> sp.allowedValuesProvider == null ? "ERROR" : "OK")
                .withArgTextSupplier(sp -> () -> sp.allowedValuesProvider == null ? "Connect an input" : (sp.getValue() == null ? "Select first column" : sp.getValue().toString()))
                .havingInputNode(() -> "input0")
                .havingOutputNode(() -> "output0")
                .validateAndReturn();

        // Add a dropdown list containing the delimiters we want to concatenate with
        // the status indicator can be used to visually feed back that the user has chosen the wrong item (commented out in this case)
        StepProperty arg2 = new StepProperty()
                .ofType(StepPropertyType.STRING)
                //.statusIndicator(sp -> () -> "Comma".equals(sp.getValue()))
                .withIconTypeSupplier(sp -> () -> "MENU")
                .withArgTextSupplier(sp -> () -> (sp.getValue() == null || sp.getValue().toString().isEmpty()) ? "Select delimiter" : "Delimiter: " + sp.getValue().toString())
                .withAllowedValuesProvider(() -> Arrays.asList("Comma", "Space", "Pipe", "Colon"))
                .validateAndReturn();

        // The second column name
        StepProperty arg3 = new StepProperty()
                .ofType(StepPropertyType.COLUMN_CHOOSER)
                .withStatusIndicator(sp -> () -> sp.allowedValuesProvider != null)
                .withIconTypeSupplier(sp -> () -> sp.allowedValuesProvider == null ? "ERROR" : "OK")
                .withArgTextSupplier(sp -> () -> sp.allowedValuesProvider == null ? "Connect an input" : (sp.getValue() == null ? "Select second column" : sp.getValue().toString()))
                .validateAndReturn();

        setStepProperties(Arrays.asList(arg1, arg2, arg3));

        // Define and set the step output class
        setStepOutput(new ConcatValuesOutput());
    }

    /**
     * Validate that the columns have been defined, and that a valid delimiter has been specified
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
            StepProperty arg2 = properties.get(1);
            StepProperty arg3 = properties.get(2);
            if (arg1 != null && arg2 != null && arg3 != null
                    && arg1.getValue() != null && arg2.getValue() != null && arg3.getValue() != null) {
                log(getStepDefinitionName() + " - Column Name 1: " + arg1.getValue() + ", Delimiter Chosen:  " + arg2.getValue() + " Column Name 2: " + arg3.getValue());
                return null;
            }
        }
        return false;
    }

    /**
     * define the output data view, i.e. rows and columns
     */
    private class ConcatValuesOutput extends StepOutput {
        @Override
        public String getName() {
            return "Concatenate two input fields";
        }

        /**
         * Initialise the columns from the Input 
         * Insert a new column before the input columns.
         * @throws Exception
         */
        @Override
        public void initialise() throws Exception {
            // initialise the columns with the first input's columns
            getColumnManager().setColumnsFromInput(getInput(0));
            // add new column at position 0 i.e. before all others
            getColumnManager().addColumnAt(this, "Concatenated", "Concatenated values",0);
        }

        /**
         * Called to obtain the value of any columns we've created.
         * In this case we get the delimiter string, and the two selected column names - we find them in our output columns list
         * and get the values from them, concatenate them together with our delimiter and return the value
         * @param row The row number required
         * @param col The column index required
         * @return The value for the required cell
         * @throws Exception
         */
        @Override
        public Object getValueAt(long row, int col) throws Exception {
            // get the delimiter value (argument 1)
            String delimiterString = getArgument(1);
            String delimiter = "";
            switch (delimiterString) {
                case "Comma":
                    delimiter = ", ";
                    break;
                case "Space":
                    delimiter = " ";
                    break;
                case "Pipe":
                    delimiter = " | ";
                    break;
                case "Colon":
                    delimiter = " : ";
                    break;
            }

            // get the user-defined column names and get the associated columns from the ColumnManager
            
            String selectedColumnName1 = getArgument(0);
            String selectedColumnName2 = getArgument(2);
            StepColumn selectedColumn1 = getColumnManager().getColumnByName(selectedColumnName1);
            StepColumn selectedColumn2 = getColumnManager().getColumnByName(selectedColumnName2);
            
            // Concatenate the Values from each column and the chosen delimiter.
            if (selectedColumn1 != null && selectedColumn2 != null) {
                return selectedColumn1.getValue(row) + delimiter + selectedColumn2.getValue(row);
            
            } else {
                logError(getStepDefinitionName() + " - There was an Error doing getValueAt Row: " + row + ", Column: " + col);
                return null;
            }
            
        }
    }

}
