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
import java.util.Optional;

/**
 * Steps to demonstrate the usage of boolean parameter.
 */
public class BooleanParametersTest extends StepConfiguration {

    public BooleanParametersTest() {
        setStepDefinitionName("Custom - Boolean Test");
        setStepDefinitionIcon("INFO");

        final StepProperty arg1 = new StepProperty()
                .ofType(StepPropertyType.COLUMN_CHOOSER)
                .withStatusIndicator(sp -> () -> sp.allowedValuesProvider != null)
                .withIconTypeSupplier(sp -> () -> sp.allowedValuesProvider == null ? "ERROR" : "OK")
                .withArgTextSupplier(sp -> () -> sp.allowedValuesProvider == null ? "Connect an input for columns" : (sp.getValue() == null ? "Select a column" : sp.getValue().toString()))
                .havingInputNode(() -> "input0")
                .havingOutputNode(() -> "output0")
                .validateAndReturn();

        final StepProperty arg2 = new StepProperty()
                .ofType(StepPropertyType.BOOLEAN)
                .withStatusIndicator(sp -> () -> sp.allowedValuesProvider != null)
                .withArgTextSupplier(sp -> () ->  "Boolean Value: " + (sp.getValue() == null ? "None" : sp.getValue().toString()))
                .validateAndReturn();

        setStepProperties(Arrays.asList(arg1, arg2));

        setStepOutput(new MyStepOutput());
    }

    @Override
    public Boolean isComplete() {
        final List<StepProperty> properties = getStepProperties();
        if (properties != null && !properties.isEmpty()) {
            final StepProperty arg2 = properties.get(1);
            if (arg2 != null && arg2.getValue() != null) {
                log(getStepDefinitionName() + " - Boolean: " + arg2.getValue());
                return null;
            }
        }
        return false;
    }

    private class MyStepOutput extends StepOutput {

        @Override
        public String getName() {
            return "Boolean Parameters Test";
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
                    getColumnManager().addColumnAt(this, selectedColumnName + " plus Boolean value", "", selectedColumnPosition);
                } else {
                    logError(getStepDefinitionName() + " - Couldn't find a column by the name of: " + selectedColumnName);
                }
            }
        }

        @Override
        public Object getValueAt(final long row, final int col) throws SDKException {
            final String selectedColumnName = getArgument(0);

            Optional<StepColumn> inputColumn = Optional.empty();
            if (selectedColumnName != null && !selectedColumnName.isEmpty()) {
                inputColumn = getInputColumn(0, selectedColumnName);
            }

            if (inputColumn.isPresent()) {
                final String value;
                try {
                    value = inputColumn.get().getValue(row).toString();
                } catch (final Exception e) {
                    throw new SDKException(e);
                }

                final Object arg2 = getArgument(1);
                if (arg2 != null && !arg2.toString().isEmpty()) {
                    return value + " " + arg2;
                } else {
                    return "";
                }
            } else {
                logError(getStepDefinitionName() + " - There was an Error doing getValueAt Row: " + row + ", Column: " + col);
                return "";
            }
        }
    }

}