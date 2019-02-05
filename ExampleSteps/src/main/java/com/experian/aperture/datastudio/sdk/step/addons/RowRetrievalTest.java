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
package com.experian.aperture.datastudio.sdk.step.examples;

import com.experian.aperture.datastudio.sdk.exception.SDKException;
import com.experian.aperture.datastudio.sdk.step.StepColumn;
import com.experian.aperture.datastudio.sdk.step.StepConfiguration;
import com.experian.aperture.datastudio.sdk.step.StepOutput;
import com.experian.aperture.datastudio.sdk.step.StepProperty;
import com.experian.aperture.datastudio.sdk.step.StepPropertyType;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Hayley.Kearey
 */
/**
 * The aim of the Row Retrieval Step is to return the row values of a given
 * table according to a row id chosen by the user. This was written to test and
 * give an example of the method StepOutput.getInputRow()
 *
 */
public class RowRetrievalTest extends StepConfiguration {

    public RowRetrievalTest() {
        setStepDefinitionName("Row Retrieval Test");
        setStepDefinitionIcon("ROWS");

        StepProperty arg1 = new StepProperty()
                .ofType(StepPropertyType.INPUT_LABEL)
                .withIconTypeSupplier(sp -> () -> "ROWS")
                .withArgTextSupplier(sp -> () -> "Input Table")
                .havingInputNode(() -> "input0")
                .havingOutputNode(() -> "output0")
                .validateAndReturn();

        StepProperty arg2 = new StepProperty()
                .ofType(StepPropertyType.INTEGER)
                .withStatusIndicator(sp -> () -> sp.getValue() != null)
                .withArgTextSupplier(sp -> () -> "Number Value: " + ((sp.getValue() == null || sp.getValue().toString().isEmpty()) ? "None" : sp.getValue().toString()))
                .withInitialValue(null)
                .validateAndReturn();
        setStepProperties(Arrays.asList(arg1, arg2));

        setStepOutput(new MyStepOutput());
    }

    /**
     *
     * @return true for enabling show data false for disabling show data null
     * for using the default action
     *
     * In RowRetrievalTest we just want to make sure there is a valid Integer in
     * the user input argument(1)
     */
    @Override
    public Boolean isComplete() {
        List<StepProperty> properties = getStepProperties();
        if (properties != null && !properties.isEmpty()) {
            StepProperty arg1 = properties.get(1);

            if (arg1 != null && !(arg1.getValue() == null)) {
                try {
                    Integer userDefinedInt = Integer.parseInt(arg1.getValue().toString());
                    log(getStepDefinitionName() + " - Chosen Number: " + userDefinedInt);
                    return true;

                } catch (NumberFormatException ex) {
                    logError(ex.getMessage());
                    return false;
                }

            }
        }

        return false;
    }

    private class MyStepOutput extends StepOutput {

        @Override
        public String getName() {
            return "Row Retrieval Test";
        }

        /**
         * Initialise the columns we want to return. Add our info column at the
         * first position, to hold the actual Row Id Then a column in place for
         * each column in the input table. We need the columns to have the same
         * title and description so that we can populate them with the given
         * data from getInputRow();
         *
         * @throws SDKException
         */
        @Override
        public void initialise() throws SDKException {
            // set the columns up
            final int colCount = getColumns().size();
            final StepColumn[] cols = new StepColumn[colCount];
            for (int i = 0; i < colCount; i++) {
                cols[i] = getColumns().get(i);
            }

            getColumnManager().clearColumns();
            for (int i = 0; i < colCount + 1; i++) {
                getColumnManager().addColumnAt(this, i == 0 ? "Row Id" : cols[i - 1].getDisplayName(), i == 0 ? "" : cols[i - 1].getDescription(), i);

            }

        }

        /**
         *
         * @return @throws SDKException
         */
        @Override
        public long execute() throws SDKException {
            return 1;
        }

        /**
         * This will not be called unless we have custom columns In this test we
         * are using getRowInput to get the values from the Row Id chosen by the
         * user
         *
         * @param row We can ignore the row that's passed in, because we are
         * only outputting one row to our data view
         * @param col Used to choose the value to return
         * @returns a value to put in the data view at (row, col)
         * @throws SDKException
         */
        @Override
        public Object getValueAt(long row, int col) throws SDKException {
            List<StepProperty> properties = getStepProperties();
            if (properties != null && !properties.isEmpty()) {
                String arg1 = getArgument(1);

                if (arg1 != null) {
                    try {
                        Integer userDefinedInt = Integer.parseInt(arg1);
                        // Our custom column
                        if (col == 0) {
                            return userDefinedInt;
                        }

                        // Need to correct the userDefinedInt as it gets passed to getInputRow,
                        // Because users will expect 1 to be the index of the first row, but we have a zero-based index here.
                        Object[] rowValues = getInputRow(0, userDefinedInt - 1);

                        // Need to correct the column index that we get the value for, 
                        // to allow for our extra column which we have already defined a value for.
                        // e.g. we want the value from the previous column Index because they have all shifted right by one
                        return rowValues[col - 1];

                    } catch (NumberFormatException ex) {
                        logError(ex.getMessage());
                    }
                }
            } else {
                throw new SDKException(new NullPointerException("Properties is null or empty"));
            }
            return null;
        }
    }
}
