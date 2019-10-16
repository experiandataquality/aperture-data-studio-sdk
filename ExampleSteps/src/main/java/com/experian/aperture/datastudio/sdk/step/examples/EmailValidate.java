/**
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
import com.experian.aperture.datastudio.sdk.step.StepColumn;
import com.experian.aperture.datastudio.sdk.step.StepConfiguration;
import com.experian.aperture.datastudio.sdk.step.StepOutput;
import com.experian.aperture.datastudio.sdk.step.StepProperty;
import com.experian.aperture.datastudio.sdk.step.StepPropertyType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Custom SDK Step used to implement email validation (stubbed out).
 * Demonstrates use of threading to make multiple concurrent Rest Api calls
 * in order to vastly improve performance.
 */
public class EmailValidate extends StepConfiguration {

    public EmailValidate() {
        setStepDefinitionName("Custom - Email Validation");
        setStepDefinitionDescription("Validate Email Addresses");
        setStepDefinitionIcon("EMAIL");

        // Define the step properties:
        // First, a column chooser based on the columns from the first input
        // a compulsory field (as enforced by the isComplete() method) which defines the source email column
        final StepProperty arg1 = new StepProperty()
                .ofType(StepPropertyType.COLUMN_CHOOSER)
                .withStatusIndicator(sp -> () -> sp.allowedValuesProvider != null)
                .withIconTypeSupplier(sp -> () -> sp.getValue() == null ? "ERROR" : "OK")
                .withArgTextSupplier(sp -> () -> sp.allowedValuesProvider == null ? "Connect an input" : (sp.getValue() == null ? "<Select email column>" : sp.getValue().toString()))
                .havingInputNode(() -> "input0")
                .havingOutputNode(() -> "output0")
                .validateAndReturn();

        // Add the step properties
        setStepProperties(Arrays.asList(arg1));

        // Define and set the step output class
        setStepOutput(new MyStepTemplate());
    }

    /**
     * Validate that the column has been defined, and that a validation method has been specified
     * If so, return null to enable the rows drilldown,
     * and enable the workflow to be considered valid for execution and export.
     * If invalid, return false. Data Rows will be disabled as will workflow execution/export.
     *
     * @return false - will not be able to execute/export/show data
     * true  - will be able to execute/export/show data
     * null  - will revert back to default behaviour, i.e. enabled if step has inputs, and are they complete?
     */
    @Override
    public Boolean isComplete() {
        final List<StepProperty> properties = getStepProperties();
        if (properties != null && !properties.isEmpty()) {
            final StepProperty arg1 = properties.get(0);
            if (arg1 != null && arg1.getValue() != null) {
                return null;
            }
        }
        return false;
    }

    /**
     * inner class to define the output of the step, i.e. the columns and rows.
     * In this case we add three columns - the outputs from the email validation
     * In execute we thread the calls to the Email Validation REST API to improve performance
     * <p>
     * Improvements include: caching the email validation data
     * Retrying after server errors on some values
     * Using more lightweight REST library (i.e. not spring)
     */
    private class MyStepTemplate extends StepOutput {
        static final int BLOCK_SIZE = 1000;
        static final int THREAD_SIZE = 24;

        private HashMap<String, EmailResponse> responses = new HashMap<>();

        @Override
        public String getName() {
            return "Email Validation";
        }

        @Override
        public void initialise() throws SDKException {
            // initialise the columns with the first input's columns
            getColumnManager().setColumnsFromInput(getInput(0));
            // add new columns after all others
            getColumnManager().addColumnAt(this, "Certainty", "", getColumnManager().getColumnCount());
            getColumnManager().addColumnAt(this, "Corrections", "", getColumnManager().getColumnCount());
            getColumnManager().addColumnAt(this, "Message", "", getColumnManager().getColumnCount());
        }

        @Override
        public long execute() throws SDKException {
            final Long rowCount = Long.valueOf(getInput(0).getRowCount());
            final ExecutorService es = Executors.newFixedThreadPool(THREAD_SIZE);

            final String selectedColumnName = getArgument(0);
            final StepColumn selectedColumn = getColumnManager().getColumnByName(selectedColumnName);

            // queue up to THREAD_SIZE threads, for processing BLOCK_SIZE times simultaneously
            final List<Future> futures = new ArrayList<>();
            for (long rowId = 0L; rowId < rowCount; rowId++) {
                try {
                    final String emailAddress = (String) selectedColumn.getValue(rowId);
                    futures.add(es.submit(() -> performValidation(emailAddress)));
                } catch (final Exception e) {
                    throw new SDKException(e);
                }

                if (rowId % BLOCK_SIZE == 0) {
                    waitForFutures(futures);
                    final Double progress = ((double) rowId / rowCount) * 100;
                    sendProgress(progress);
                }
            }

            // process the remaining futures
            waitForFutures(futures);

            // close all threads
            es.shutdown();

            return rowCount;
        }

        @Override
        public Object getValueAt(final long row, final int col) throws SDKException {
            // get value for user-defined column at row location
            final String selectedColumnName = getArgument(0);
            final StepColumn selectedColumn = getColumnManager().getColumnByName(selectedColumnName);
            String emailAddress = null;
            try {
                emailAddress = (String) selectedColumn.getValue(row);
            } catch (final Exception e) {
                throw new SDKException(e);
            }

            // get validation results for email address
            final EmailResponse response = responses.getOrDefault(emailAddress, null);

            // switch by required column name
            if (response != null) {
                // get our custom column name from the col index
                final String colName = getColumnManager().getColumnFromIndex(col).getDisplayName();
                switch (colName) {
                    case "Certainty":
                        return response.getCertainty();
                    case "Corrections":
                        return response.getCorrections();
                    case "Message":
                        return response.getMessage();
                    default:
                        return "unknown";
                }
            }

            return "<server error>";
        }

        /**
         * Class to store the response from our fictional REST Api call.
         */
        private class EmailResponse {
            private String email;
            private String certainty;
            private String message;
            private List<String> corrections;

            EmailResponse(final String email,
                          final String certainty,
                          final String message,
                          final List<String> corrections) {
                this.email = email;
                this.certainty = certainty;
                this.message = message;
                this.corrections = corrections;
            }

            public String getEmail() {
                return email;
            }

            public String getCertainty() {
                return certainty;
            }

            public String getMessage() {
                return message;
            }

            public List<String> getCorrections() {
                return corrections;
            }
        }

        /**
         * Implement your call into your slow Rest (or other) API here.
         * It will be called concurrently THREAD_SIZE times in order to improve performance.
         * It currently obtains and returns a composite object that can be customised or changed as required.
         *
         * @param emailAddress
         * @return EmailResponse
         */
        private EmailResponse performValidation(final String emailAddress) {
            return new EmailResponse(emailAddress, "certainty", "message", Arrays.asList("correction1", "correction2"));
        }

        /**
         * Store email validation results in a local array.
         * Ideally results should be persisted on disk for reuse by other instances of this step or re-runs of this step,
         * so that (in this case) the email is only verified once no matter what step instance is invoking the functionality.
         *
         * @param futures
         * @throws SDKException
         */
        private void waitForFutures(final List<Future> futures) throws SDKException {
            for (Future future : futures) {
                final Object emr;
                try {
                    emr = future.get();
                } catch (InterruptedException | ExecutionException e) {
                    Thread.currentThread().interrupt();
                    throw new SDKException(e);
                }
                if (emr instanceof EmailResponse) {
                    responses.put(((EmailResponse) emr).getEmail(), (EmailResponse) emr);
                }
            }
            futures.clear();
        }
    }
}
