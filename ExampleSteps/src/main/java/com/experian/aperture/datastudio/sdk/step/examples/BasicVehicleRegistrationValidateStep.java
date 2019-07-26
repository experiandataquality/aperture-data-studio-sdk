/*
 * Copyright © 2017 Experian plc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.experian.aperture.datastudio.sdk.step.examples;

import com.experian.aperture.datastudio.sdk.exception.SDKException;
import com.experian.aperture.datastudio.sdk.step.Cache;
import com.experian.aperture.datastudio.sdk.step.ServerValueUtil;
import com.experian.aperture.datastudio.sdk.step.StepColumn;
import com.experian.aperture.datastudio.sdk.step.StepConfiguration;
import com.experian.aperture.datastudio.sdk.step.StepOutput;
import com.experian.aperture.datastudio.sdk.step.StepProperty;
import com.experian.aperture.datastudio.sdk.step.StepPropertyType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * This example step demonstrates the new ability to call Business Constants into custom steps from Aperture
 *
 * This step will take an input column and validate the value in that column based on the Uk Vehicle Registration Business Constant.
 * It adds a Validated result column to the output view
 */
public class BasicVehicleRegistrationValidateStep extends StepConfiguration {


    public BasicVehicleRegistrationValidateStep() {

        setStepDefinitionName("Custom - Vehicle Registration Validate");
        setStepDefinitionDescription("Validates a given UK Registration number based on basic format validation");
        setStepDefinitionIcon("VALIDATION_GROUP");

        // Define the step properties

        // Create a property to take a column which will be validated
        // It also defines a single output connection for the step.
        final StepProperty arg1 = new StepProperty()
                .ofType(StepPropertyType.COLUMN_CHOOSER)
                .withStatusIndicator(sp -> () -> sp.allowedValuesProvider != null)
                .withIconTypeSupplier(sp -> () -> "VALUES")
                .withArgTextSupplier(sp -> () -> {
                    if (sp.getValue() == null || sp.getValue().toString().isEmpty()) {
                        return "Select a column";
                    } else {
                        return sp.getValue().toString();
                    }
                })
                .havingInputNode(() -> "input0")
                .havingOutputNode(() -> "output0")
                .validateAndReturn();

        setStepProperties(Arrays.asList(arg1));

        // Define and set the step output class
        setStepOutput(new MyStepOutput());
    }

    /**
     * Validate that input is connected
     * If invalid, return false. Data Rows will be disabled as will workflow execution/export.
     * @return false - will not be able to execute/export/show data
     *         true  - will be able to execute/export/show data
     *         null - will revert back to default
     * behaviour, i.e. enabled if step has inputs, and are they complete?
     */
    @Override
    public Boolean isComplete() {
        final List<StepProperty> properties = getStepProperties();
        if (properties != null && !properties.isEmpty()) {
            final StepProperty arg1 = properties.get(0);
            return arg1 != null && arg1.getValue() != null && !arg1.getValue().equals("Select a column");
        }

        return false;
    }


    /**
     * Define the output data view, i.e. rows and columns and title
     */
    private class MyStepOutput extends StepOutput {
        private static final String VEHICLE_REGISTRATION = "vr_cache";

        private String regNumberPattern;

        @Override
        public String getName() {
            return "Validate Registration Number";
        }

        /**
         * Adds a result column to the output
         * @throws SDKException
         */
        @Override
        public void initialise() throws SDKException {
            getColumnManager().addColumn(this, "Result", "The result of validation on the given Registration Column");
        }

        /**
         * Returns the row count of the input
         * @return Row count
         * @throws SDKException
         */
        @Override
        public long execute() throws SDKException {
            return getInput(0).getRowCount();
        }

        /**
         * Return the values in our additional column (result column)
         * return boolean true or false depending on if validation passes or fails
         * @param row The row number required
         * @param col The column number required
         * @return The value
         * @throws SDKException
         */
        @Override
        public Object getValueAt(final long row, final int col) throws SDKException {
            final StepColumn selectedColumn = getColumnManager().getColumnByName(getArgument(0));
            final String regNumber;
            Boolean result;

            try {
                final Object value = selectedColumn.getValue(row);
                regNumber = (value instanceof String) ? (String) value : null;

            } catch (final Exception e) {
                throw new SDKException(e);
            }

            try {
                final Cache cache = getCache(VEHICLE_REGISTRATION);
                final String cacheValue = cache.read(regNumber);
                result = cacheValue == null ? null : Boolean.valueOf(cacheValue);
                if (result == null) {
                    result = isValidFormat(regNumber);
                    cache.write(regNumber, result.toString());
                }

            } catch (final Exception e) {
                throw new SDKException(e);
            }

            //When it is not interactive, this means it is in workflow execution mode, where setting the progress should make some impact
            if (!isInteractive() && row % 10 == 0) {
                final Long rowCount = getInput(0).getRowCount();
                final double progress = ((double) row / rowCount) * 100;
                sendProgress(progress);
            }


            return result;
        }


        /**
         * Take the value given and do a pattern match against the Business Constant found in Aperture
         * return boolean true or false depending on if validation passes or fails
         * Contains example of saving results to a cache
         * @param regNumber the value at current row which represents the registration number that will be validated.
         * @return The value
         * @throws SDKException
         */
        private boolean isValidFormat(final String regNumber) {
            if (regNumber == null) {
                return false;
            }
            if (regNumberPattern == null) {
                //The string is the name of the Business constant in Aperture as it appears in the UI (database style case)
                final Optional<String> registrationNumberConstant = ServerValueUtil.getGlossaryConstant("UK_VEHICLE_REGISTRATION_NUMBER");
                registrationNumberConstant.ifPresent(s -> regNumberPattern = s);
            }
            return regNumber.matches(regNumberPattern);
        }

    }

}