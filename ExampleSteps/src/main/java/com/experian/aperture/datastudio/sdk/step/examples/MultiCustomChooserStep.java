package com.experian.aperture.datastudio.sdk.step.examples;

import com.experian.aperture.datastudio.sdk.step.StepConfiguration;
import com.experian.aperture.datastudio.sdk.step.StepOutput;
import com.experian.aperture.datastudio.sdk.step.StepProperty;
import com.experian.aperture.datastudio.sdk.step.StepPropertyType;
import org.json.JSONArray;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Creates a step that demo StepPropertyType.MULTI_CUSTOM_CHOOSER.
 */
public class MultiCustomChooserStep extends StepConfiguration {

    public MultiCustomChooserStep() {
        setStepDefinitionName("Custom - Multi Custom Chooser");
        setStepDefinitionDescription("Custom step with Multi Custom Chooser");
        setStepDefinitionIcon("INFO");

        final StepProperty arg1 = new StepProperty()
                .ofType(StepPropertyType.MULTI_CUSTOM_CHOOSER)
                .withAllowedValuesProvider(() -> Arrays.asList("Team Alpha", "Team Beta", "Team Gamma", "Team Delta"))
                .withArgTextSupplier(sp -> () -> {
                            if (sp.getValue() == null) {
                                return "Select a Team";
                            }
                            final JSONArray selected = (JSONArray) sp.getValue();
                            if (selected.length() == 0) {
                                return "Select a Team";
                            }
                            return selected.toList().stream().map(Object::toString).collect(Collectors.joining(","));
                        }
                )
                .withStatusIndicator(sp -> () -> isArgSelected(sp))
                .withIconTypeSupplier(sp -> () -> isArgSelected(sp) ? "OK" : "ERROR")
                .havingInputNode(() -> "input0")
                .havingOutputNode(() -> "output0")
                .validateAndReturn();
        setStepProperties(Collections.singletonList(arg1));

        setStepOutput(new MultiCustomChooserOutput());
    }

    private boolean isArgSelected(final StepProperty arg1) {
        return arg1.getValue() != null && ((JSONArray) arg1.getValue()).length() > 0;
    }

    @Override
    public Boolean isComplete() {
        if (!isArgSelected(getStepProperties().get(0))) {
            return false;
        }
        return null;
    }

    private static class MultiCustomChooserOutput extends StepOutput {

        @Override
        public void initialise() {
            getColumnManager().addColumn(this, "Assigned To", "Assigned To");
        }

        @Override
        public String getName() {
            return "Multi Custom Chooser";
        }

        @Override
        public Object getValueAt(long row, int columnIndex) {
            final String arg = getArgument(0);
            if (arg == null) {
                return null;
            }
            final JSONArray selected = new JSONArray(arg);
            final int index = (int) (row % selected.length());
            return selected.get(index);
        }
    }

}
