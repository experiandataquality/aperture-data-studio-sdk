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
import com.experian.aperture.datastudio.sdk.step.*;

import java.util.*;

/**
 * The most basic step that takes an input and outputs it unchanged.
 */
public class StepTemplate extends StepConfiguration {

    public StepTemplate() {
        
        
        // Basic step information
        setStepDefinitionName("Custom - Simple Passthrough");
        setStepDefinitionDescription("Passes input to output");
        setStepDefinitionIcon("INFO");
        log("Loading " + getStepDefinitionName());
        // Define the step properties

        // This property simply defines some text ("Connection") and an associated input and output
        // for the step. Steps can have multiple inputs, and in theory can also have multiple outputs,
        // but they are not modelled for custom steps yet.
        StepProperty arg1 = new StepProperty()
                .ofType(StepPropertyType.INPUT_LABEL)
                .withArgTextSupplier(sp -> () -> "Connection")
                .havingInputNode(() -> "input0")
                .havingOutputNode(() -> "output0")
                .validateAndReturn();

        // This property does nothing. It is to demonstrate the isComplete() to enable/disable execution
        // based on a required argument/value.
        final StepProperty arg2 = new StepProperty()
                .ofType(StepPropertyType.STRING)
                .withArgTextSupplier(sp -> () -> sp.getValue() == null || sp.getValue().toString().isEmpty()
                        ? "Enter value" : sp.getValue().toString())
                .validateAndReturn();

        // Add the step properties
        setStepProperties(Arrays.asList(arg1, arg2));

        // Define and set the step output class
        setStepOutput(new MyStepTemplate());
    }

    @Override
    public Boolean isComplete() {
        // Prevent from executing this step until input node has been connected.
        // INPUT_LABEL type does not have value, thus we don't need to validate.
        final StepProperty arg2 = getStepProperties().get(1);
        return arg2.getValue() == null || arg2.getValue().toString().isEmpty()
                ? Boolean.FALSE : null;
    }

    /**
     * inner class to define the output of the step, i.e. the columns and rows.
     * In this case only the essential abstract methods are implemented,
     * and the getValueAt method is not even used.
     */
    private class MyStepTemplate extends StepOutput {
        @Override
        public String getName() {
            return "Passthrough Step";
        }

        @Override
        public Object getValueAt(long row, int col) throws SDKException {
            // will not be called since we have no custom columns
            return null;
        }
    }
}
