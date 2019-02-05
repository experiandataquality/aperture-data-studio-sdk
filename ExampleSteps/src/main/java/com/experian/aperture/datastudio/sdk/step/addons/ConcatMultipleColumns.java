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
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

/**
 * This is a custom step definition that concatentates multiple
 * user-defined columns together using a space
 * and displays them in a new column inserted before the other input columns.
 */
public class ConcatMultipleColumns extends StepConfiguration {

    public ConcatMultipleColumns() {
        // Basic step information
        setStepDefinitionName("Custom - Concatenate Multiple Columns");
        setStepDefinitionDescription("Concatenate multiple columns with spaces");
        setStepDefinitionIcon("ALPHA_NUMERIC");

        // Define the step properties:
        // A multi column chooser based on the columns from the first input
        StepProperty arg1 = new StepProperty()
                .ofType(StepPropertyType.MULTI_COLUMN_CHOOSER)
                .withStatusIndicator(sp -> () -> sp.allowedValuesProvider != null)
                .withIconTypeSupplier(sp -> () -> sp.allowedValuesProvider == null ? "ERROR" : "OK")
                .withArgTextSupplier(sp -> () -> {
                    // return the text shown in the UI
                    if (sp.allowedValuesProvider == null) {
                        // if input is not connected
                        return "Connect an input";
                    } else {
                        Object value = sp.getValue();
                        if (value != null) {
                            // value is not null, so convert into JSONArray of column names, as selected by user
                            try {
                                JSONArray columnNameArray = new JSONArray(value.toString());
                                if (columnNameArray != null && columnNameArray.length() > 0) {
                                    return columnNameArray.length() == 1
                                            ? columnNameArray.get(0).toString()
                                            : columnNameArray.length() + " columns selected";
                                }
                            } catch (Exception ex){
                                // ignore
                            }
                        }
                        // no columns selected
                        return "Select columns";
                    }
                })
                .havingInputNode(() -> "input0")
                .havingOutputNode(() -> "output0")
                .validateAndReturn();

        setStepProperties(Arrays.asList(arg1));

        // Define and set the step output class
        setStepOutput(new ConcatValuesOutput());
    }

    /**
     * Validate that some columns have been defined
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
            if (arg1 != null && arg1.getValue() != null && arg1.getValue() instanceof JSONArray && ((JSONArray) arg1.getValue()).length() > 0) {
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
         * @throws SDKException
         */
        @Override
        public void initialise() throws SDKException {
            // initialise the columns with the first input's columns
            getColumnManager().setColumnsFromInput(getInput(0));
            // add new column at position 0 i.e. before all others
            getColumnManager().addColumnAt(this, "Concatenated", "Concatenated values",0);
        }

        /**
         * Called to obtain the value of any columns we've created.
         * In this case we get the selected columns from the input and concatenate their values with spaces
         * @param row The row number required
         * @param col The column index required
         * @return The value for the required cell
         * @throws SDKException
         */
        @Override
        public Object getValueAt(long row, int col) throws SDKException {
            String value = "";

            // get the user-defined columns
            String selectedColumns = getArgument(0);
            try {
                JSONArray columnNameArray = new JSONArray(selectedColumns);

                // get the value for each user-defined column and concatenate them with spaces
                for (int i=0; i<columnNameArray.length(); i++) {
                    String columnName = columnNameArray.get(i).toString();
                    value += value.length() == 0 ? "" : " ";
                    value += getInputColumn(0, columnName).get().getValue(row);
                }

                return value;
            } catch (Exception ex) {
                // ignore for now
            }
            return "";
        }
    }

}
