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

import com.experian.aperture.datastudio.sdk.step.*;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * A step that acts as a datasource into a workflow.
 * It has no input, and one output, and generates its output based on some parameters provided by the user,
 * the number of rows, number of columns, and whether the randomised data is numeric or alphabetic.
 */
public class DataSource extends StepConfiguration {

    public DataSource() {
        // Basic step information
        setStepDefinitionName("Custom - Data Generation");
        setStepDefinitionDescription("Generate random data for user defined number of rows and columns");
        setStepDefinitionIcon("ROWS");

        // Define the step properties

        // Obtain the number of columns, defaulting to zero
        // and define the output connection (but no input)
        StepProperty arg1 = new StepProperty()
                .ofType(StepPropertyType.INTEGER)
                .withIconTypeSupplier(sp -> () -> "ADD")
                .withArgTextSupplier(sp -> () -> {
                    if (sp.getValue() == null || sp.getValue().toString().isEmpty()) {
                        return "Enter number of columns";
                    } else {
                        try {
                            return "Number of columns: " + Integer.parseInt(sp.getValue().toString());
                        } catch (Exception ex) {
                            return "Number of columns: 0";
                        }
                    }
                })
                .havingOutputNode(() -> "output0")
                .withInitialValue(0)
                .validateAndReturn();

        // Obtain the number of rows, defaulting to zero
        StepProperty arg2 = new StepProperty()
                .ofType(StepPropertyType.INTEGER)
                .withIconTypeSupplier(sp -> () -> "ADD")
                .withArgTextSupplier(sp -> () -> {
                    if (sp.getValue() == null || sp.getValue().toString().isEmpty()) {
                        return "Enter number of rows";
                    } else {
                        try {
                            return "Number of rows: " + Integer.parseInt(sp.getValue().toString());
                        } catch (Exception ex) {
                            return "Number of rows: 0";
                        }
                    }
                })
                .withInitialValue(0)
                .validateAndReturn();

        // Obtain whether the generated data should be alphabetic or numeric using a dropdown selector
        StepProperty arg3 = new StepProperty()
                .ofType(StepPropertyType.STRING)
                .withIconTypeSupplier(sp -> () -> "MENU")
                .withArgTextSupplier(sp -> () -> (sp.getValue() == null || sp.getValue().toString().isEmpty()) ? "Choose data type" : "Data type: " + sp.getValue().toString())
                .withAllowedValuesProvider(() -> Arrays.asList("numeric", "alphabetic"))
                .validateAndReturn();

        setStepProperties(Arrays.asList(arg1, arg2, arg3));

        // Define and set the step output class
        setStepOutput(new MyStepOutput());
    }

    /**
     * Validate that all fields are complete, and that row and column counts are > 0
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
            // get the values of our 3 properties
            StepProperty arg1 = properties.get(0);
            StepProperty arg2 = properties.get(1);
            StepProperty arg3 = properties.get(2);
            // ensure they are non null
            if (arg1 != null && arg2 != null && arg3 != null
                    && arg1.getValue() != null && arg2.getValue() != null && arg3.getValue() != null) {
                // ensure they are numeric and the row/column counts are > 0
                try {
                    Integer colCount = Integer.parseInt(arg1.getValue().toString());
                    Integer rowCount = Integer.parseInt(arg2.getValue().toString());
                    if (arg3.getValue() != null && !arg3.getValue().toString().isEmpty()
                            && colCount > 0 && rowCount > 0) {
                        log(getStepDefinitionName() + " - Chosen Number of Columns: " + arg1.getValue() + " Chosen Number of Rows: " + arg2.getValue() + ", Chosen Datatype: " + arg3.getValue());
                        return true;
                    }
                } catch (NumberFormatException ex) {
                    logError(ex.getMessage());
                }
            }
        }
        return false;
    }

    /**
     * define the output data view, i.e. rows and columns
     */
    private class MyStepOutput extends StepOutput {
        String[][] cells;

        @Override
        public String getName() {
            return "Data generator";
        }

        /**
         * Add the required number of columns to our output data view
         * @throws Exception
         */
        @Override
        public void initialise() throws Exception {
            // clear the columns, just to be sure
            getColumnManager().clearColumns();

            Integer rowCount = Integer.parseInt(getArgument(1).toString());
            Integer colCount = Integer.parseInt(getArgument(0).toString());

            // add the required number of columns to our output list
            for (int i=0; i<colCount; i++) {
                getColumnManager().addColumn(this, "Column " + i, "Auto generated column " + i);
            }

            // initialise our random data cache
            cells = new String[rowCount][colCount];
        }

        /**
         * Return the number of rows in our output.
         * @return Row count
         * @throws Exception
         */
        @Override
        public long execute() throws Exception {
            return Integer.parseInt(getArgument(1));
        }

        /**
         * Called to obtain the value of any columns we've created.
         * In this case we generate a random value and return it.
         * We have to cache the value for the cell as this function is called repeatedly for the same cell
         * and we need to return a consistent value each time.
         * @param row The row number required
         * @param col The column index required
         * @return The value for the required cell
         * @throws Exception
         */
        @Override
        public Object getValueAt(long row, int col) throws Exception {
            // get the numeric/a
            String dataType = getArgument(2);

            // cache results so we don't have to calculate them again, and so they are consistent for the
            // lifetime of the view, because this function is called regularly for the same cell!
            String value = cells[Long.valueOf(row).intValue()][col];
            if (value == null) {
                value = (dataType.equalsIgnoreCase("numeric")
                        ? generateRandomInteger().toString()
                        : generateRandomChars("ABCDEFGHIJKLMNOPQRSTUVWXYZ", 8));
                cells[Long.valueOf(row).intValue()][col] = value;
            }

            return value;
        }
    }

    public static String generateRandomChars(String candidateChars, int length) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(candidateChars.charAt(random.nextInt(candidateChars.length())));
        }

        return sb.toString();
    }

    public static Integer generateRandomInteger() {
        Double rnd = Math.random() * 100;
        return rnd.intValue();
    }
}
