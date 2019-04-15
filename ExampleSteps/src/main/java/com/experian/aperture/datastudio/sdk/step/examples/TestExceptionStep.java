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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Step to demonstrate the usage of {@code SDKException}
 */
public class TestExceptionStep extends StepConfiguration {

    public TestExceptionStep() {
        setStepDefinitionName("Custom - Test Exception");
        setStepDefinitionDescription("Testing that the SDK Exception is correctly thrown.");
        setStepDefinitionIcon("ERROR");

        final StepProperty arg1 = new StepProperty()
                .ofType(StepPropertyType.COLUMN_CHOOSER)
                .withStatusIndicator(sp -> () -> sp.allowedValuesProvider != null)
                .withIconTypeSupplier(sp -> () -> sp.allowedValuesProvider == null ? "ERROR" : "OK")
                .withArgTextSupplier(sp -> () -> sp.allowedValuesProvider == null ? "Connect an input for columns" : (sp.getValue() == null ? "Select a column" : sp.getValue().toString()))
                .havingInputNode(() -> "input0")
                .havingOutputNode(() -> "output0")
                .validateAndReturn();

        setStepProperties(Collections.singletonList(arg1));

        setStepOutput(new MyStepOutput());
    }

    @Override
    public Boolean isComplete() {
        final List<StepProperty> properties = getStepProperties();
        if (properties != null && !properties.isEmpty()) {
            final StepProperty arg1 = properties.get(0);
            if (arg1 != null && arg1.getValue() != null) {
                log(getStepDefinitionName() + " - Column Name: " + arg1.getValue());
                return null;
            }
        }
        return false;
    }

    private class MyStepOutput extends StepOutput {

        @Override
        public String getName() {
            return "Test Exception Step";
        }

        @Override
        public void initialise() throws SDKException {
            getColumnManager().clearColumns();

            final String selectedColumnName = getArgument(0);
            if (selectedColumnName != null) {
                getColumnManager().setColumnsFromInput(getInput(0));
                final StepColumn selectedColumn = getColumnManager().getColumnByName(selectedColumnName);
                if (selectedColumn != null) {
                    final int selectedColumnPosition = getColumnManager().getColumnPosition(selectedColumnName);
                    getColumnManager().removeColumn(selectedColumnName);
                    getColumnManager().addColumnAt(this, selectedColumnName + " plus Row value", "", selectedColumnPosition);
                } else {
                    logError(getStepDefinitionName() + " - Couldn't find a column by the name of: " + selectedColumnName);
                }
            }
        }

        @Override
        public Object getValueAt(final long row, final int col) throws SDKException {
            if (row == 0) {
                throw new SDKException("Testing that row 1 throws an exception.");
            } else {
                final String selectedColumnName = getArgument(0);
                final Optional<StepColumn> inputColumn = selectedColumnName != null && !selectedColumnName.isEmpty()
                        ? getInputColumn(0, selectedColumnName)
                        : Optional.empty();
                if (inputColumn.isPresent()) {
                    try {
                        return inputColumn.get().getValue(row) + " " + row + 1;
                    } catch (final Exception e) {
                        throw new SDKException(e);
                    }
                } else {
                    logError(getStepDefinitionName() + " - There was an Error doing getValueAt Row: " + row + ", Column: " + col);
                    return "";
                }
            }
        }
    }
}