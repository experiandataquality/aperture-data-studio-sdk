/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.experian.aperture.datastudio.sdk.step.addons;

import com.experian.aperture.datastudio.sdk.step.*;
import java.util.Arrays;
import java.util.List;

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
         * @throws Exception
         */
        @Override
        public void initialise() throws Exception {
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
         * @return @throws Exception
         */
        @Override
        public long execute() throws Exception {
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
         * @throws Exception
         */
        @Override
        public Object getValueAt(long row, int col) throws Exception {
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
                return new NullPointerException("Properties is null or empty");
            }
            return null;
        }
    }
}
