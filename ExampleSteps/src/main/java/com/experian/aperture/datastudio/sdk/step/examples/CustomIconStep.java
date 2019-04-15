package com.experian.aperture.datastudio.sdk.step.examples;

import com.experian.aperture.datastudio.sdk.step.StepConfiguration;
import com.experian.aperture.datastudio.sdk.step.StepOutput;
import com.experian.aperture.datastudio.sdk.step.StepProperty;
import com.experian.aperture.datastudio.sdk.step.StepPropertyType;

import java.util.Collections;

public class CustomIconStep extends StepConfiguration {

    public CustomIconStep() {
        setStepDefinitionName("Custom - Icon");
        setStepDefinitionDescription("Custom step with custom icon");
        setStepDefinitionIcon("experian.png");

        StepProperty arg1 = new StepProperty()
                .ofType(StepPropertyType.INPUT_LABEL)
                .withArgTextSupplier(sp -> () -> "Connection")
                .havingInputNode(() -> "input0")
                .havingOutputNode(() -> "output0")
                .validateAndReturn();
        setStepProperties(Collections.singletonList(arg1));

        setStepOutput(new CustomIconOutput());
    }

    private static class CustomIconOutput extends StepOutput {

        @Override
        public String getName() {
            return "Custom Icon";
        }

        @Override
        public Object getValueAt(long row, int columnIndex) {
            return null;
        }
    }

}
