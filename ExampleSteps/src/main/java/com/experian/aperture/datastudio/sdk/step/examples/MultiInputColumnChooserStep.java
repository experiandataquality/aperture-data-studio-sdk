package com.experian.aperture.datastudio.sdk.step.examples;

import com.experian.aperture.datastudio.sdk.exception.SDKException;
import com.experian.aperture.datastudio.sdk.step.StepColumn;
import com.experian.aperture.datastudio.sdk.step.StepConfiguration;
import com.experian.aperture.datastudio.sdk.step.StepOutput;
import com.experian.aperture.datastudio.sdk.step.StepProperty;
import com.experian.aperture.datastudio.sdk.step.StepPropertyType;
import org.json.JSONArray;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Creates a step that demo multiple inputs.
 */
public class MultiInputColumnChooserStep extends StepConfiguration {

    private static final String ICON_ERROR = "ERROR";
    private static final String ICON_OK = "OK";
    private static final String ARG_TEXT_CONNECT_INPUT_1 = "<Connect 1st input>";
    private static final String ARG_TEXT_CONNECT_INPUT_2 = "<Connect 2nd input>";
    private static final String INPUT_0 = "input0";
    private static final String INPUT_1 = "input1";

    public MultiInputColumnChooserStep() {
        setStepDefinitionName("Custom - Multi Input Column Chooser");
        setStepDefinitionDescription("");
        setStepDefinitionIcon("INFO");
        setStepOutput(new MultiInputColumnChooserOutput());

        final StepProperty arg0 = new StepProperty()
                .ofType(StepPropertyType.COLUMN_CHOOSER)
                .withStatusIndicator(sp -> () -> sp.allowedValuesProvider != null && sp.getValue() != null)
                .withIconTypeSupplier(sp -> () -> sp.allowedValuesProvider != null && sp.getValue() != null ? ICON_OK : ICON_ERROR)
                .withArgTextSupplier(sp -> () -> sp.allowedValuesProvider == null
                        ? ARG_TEXT_CONNECT_INPUT_1
                        : (sp.getValue() == null ? "<Select 1st Input 1st column>" : sp.getValue().toString()))
                .havingInputNode(() -> INPUT_0) // add new input node `input0` and define arg0's options
                .havingOutputNode(() -> "output0")
                .validateAndReturn();

        final StepProperty arg1 = new StepProperty()
                .ofType(StepPropertyType.COLUMN_CHOOSER)
                .withStatusIndicator(sp -> () -> sp.allowedValuesProvider != null && sp.getValue() != null)
                .withIconTypeSupplier(sp -> () -> sp.allowedValuesProvider != null && sp.getValue() != null ? ICON_OK : ICON_ERROR)
                .withArgTextSupplier(sp -> () -> sp.allowedValuesProvider == null
                        ? ARG_TEXT_CONNECT_INPUT_2
                        : (sp.getValue() == null ? "<Select 2nd input 1st column>" : sp.getValue().toString()))
                .havingInputNode(() -> INPUT_1) // add new input node `input1` and define arg1's options
                .validateAndReturn();

        final StepProperty arg2 = new StepProperty()
                .ofType(StepPropertyType.MULTI_COLUMN_CHOOSER)
                .withStatusIndicator(sp -> () -> sp.allowedValuesProvider != null && sp.getValue() != null)
                .withIconTypeSupplier(sp -> () -> sp.allowedValuesProvider != null && sp.getValue() != null ? ICON_OK : ICON_ERROR)
                .withArgTextSupplier(sp -> () -> sp.allowedValuesProvider == null
                        ? ARG_TEXT_CONNECT_INPUT_2
                        : (sp.getValue() == null ? "<Select 2nd input multi columns>" : sp.getValue().toString()))
                .havingInputNode(() -> INPUT_1) // define arg2's options with input1's columns
                .validateAndReturn();

        final StepProperty arg3 = new StepProperty()
                .ofType(StepPropertyType.MULTI_COLUMN_CHOOSER)
                .withStatusIndicator(sp -> () -> sp.allowedValuesProvider != null && sp.getValue() != null)
                .withIconTypeSupplier(sp -> () -> sp.allowedValuesProvider != null && sp.getValue() != null ? ICON_OK : ICON_ERROR)
                .withArgTextSupplier(sp -> () -> sp.allowedValuesProvider == null
                        ? ARG_TEXT_CONNECT_INPUT_1
                        : (sp.getValue() == null ? "<Select 1st input multi columns>" : sp.getValue().toString()))
                .havingInputNode(() -> INPUT_0) // define arg3's options with input0's columns
                .validateAndReturn();

        setStepProperties(Arrays.asList(arg0, arg1, arg2, arg3));

    }

    @Override
    public Boolean isComplete() {
        if (getStepProperties().stream().anyMatch(stepProperty -> stepProperty.getValue() == null)) {
            return false;
        }
        return null;
    }

    private static class MultiInputColumnChooserOutput extends StepOutput {

        @Override
        public void initialise() throws SDKException {
            getColumnManager().addColumn(this, "Added", "").setBackground(new Color(217, 236, 233));
        }

        @Override
        public String getName() {
            return "MultiInputColumnChooser";
        }

        @Override
        public Object getValueAt(long row, int columnIndex) {
            final String firstInputFirstColumn = getArgument(0);
            final String secondInputFirstColumn = getArgument(1);
            final String secondInputMultiColumns = getArgument(2);
            final String firstInputMultiColumns = getArgument(3);
            final List<Optional<StepColumn>> list = new ArrayList<>();
            list.add(getInputColumn(0, firstInputFirstColumn));
            list.add(getInputColumn(1, secondInputFirstColumn));
            final JSONArray secondInputJson = new JSONArray(secondInputMultiColumns);
            for (int i = 0; i < secondInputJson.length(); i++) {
                list.add(getInputColumn(1, secondInputJson.get(i).toString()));
            }
            final JSONArray firstInputJson = new JSONArray(firstInputMultiColumns);
            for (int i = 0; i < firstInputJson.length(); i++) {
                list.add(getInputColumn(0, firstInputJson.get(i).toString()));
            }
            return list.stream().filter(Optional::isPresent).map(stepColumn -> {
                try {
                    return stepColumn.get().getValue(row).toString();
                } catch (Exception ignore) {
                    // intentionally empty
                }
                return "";
            }).collect(Collectors.joining(", "));
        }
    }

}
