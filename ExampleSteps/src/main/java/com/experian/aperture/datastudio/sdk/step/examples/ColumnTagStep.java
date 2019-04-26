package com.experian.aperture.datastudio.sdk.step.examples;

import com.experian.aperture.datastudio.sdk.exception.SDKException;
import com.experian.aperture.datastudio.sdk.step.StepColumn;
import com.experian.aperture.datastudio.sdk.step.StepConfiguration;
import com.experian.aperture.datastudio.sdk.step.StepOutput;
import com.experian.aperture.datastudio.sdk.step.StepProperty;
import com.experian.aperture.datastudio.sdk.step.StepPropertyType;

import java.util.Arrays;
import java.util.Optional;

/**
 * Creates a step that auto select arguments with columns tagged with 'Phone' and 'Email'.
 */
public class ColumnTagStep extends StepConfiguration {

    private static final String TAG_PHONE = "Phone";
    private static final String TAG_EMAIL = "Email";

    public ColumnTagStep() {
        setStepDefinitionName("Custom - Column Tag");
        setStepDefinitionDescription("Auto select columns tagged with 'Phone' and 'Email'.");
        setStepDefinitionIcon("INFO");

        final StepProperty spPhone = new StepProperty()
                .ofType(StepPropertyType.COLUMN_CHOOSER)
                .withStatusIndicator(sp -> () -> isPhoneArgOk(sp))
                .withIconTypeSupplier(sp -> () -> isPhoneArgOk(sp) ? "OK" : "ERROR")
                .withArgTextSupplier(sp -> () -> getPhoneArgText(sp))
                .havingInputNode(() -> "input0")
                .havingOutputNode(() -> "output0")
                .validateAndReturn();
        final StepProperty spEmail = new StepProperty()
                .ofType(StepPropertyType.COLUMN_CHOOSER)
                .withStatusIndicator(sp -> () -> isEmailArgOk(sp))
                .withIconTypeSupplier(sp -> () -> isEmailArgOk(sp) ? "OK" : "ERROR")
                .withArgTextSupplier(sp -> () -> getEmailArgText(sp))
                .validateAndReturn();
        setStepProperties(Arrays.asList(spPhone, spEmail));

        setStepOutput(new ColumnTagOutput());
    }

    private boolean isPhoneArgOk(final StepProperty sp) {
        autoSelectTaggedColumn(sp, TAG_PHONE);
        return sp.allowedValuesProvider != null && sp.getValue() != null;
    }

    private String getPhoneArgText(final StepProperty sp) {
        autoSelectTaggedColumn(sp, TAG_PHONE);
        if (sp.allowedValuesProvider == null) {
            return "<Connect an input>";
        } else {
            return sp.getValue() == null ? "<Select a phone column>" : sp.getValue().toString();
        }
    }

    private boolean isEmailArgOk(final StepProperty sp) {
        autoSelectTaggedColumn(sp, TAG_EMAIL);
        return sp.allowedValuesProvider != null && sp.getValue() != null;
    }

    private String getEmailArgText(final StepProperty sp) {
        autoSelectTaggedColumn(sp, TAG_EMAIL);
        if (sp.allowedValuesProvider == null) {
            return "<Connect an input>";
        } else {
            return sp.getValue() == null ? "<Select a email column>" : sp.getValue().toString();
        }
    }

    /**
     * Automatically set step property's value to a column tagged with specific label name.
     * This is done by comparing data tags in all input columns.
     * @param sp StepProperty
     * @param tag tag name
     */
    private void autoSelectTaggedColumn(final StepProperty sp, final String tag) {
        if (sp.allowedValuesProvider != null) {
            resetColumnIfValueNotFound(sp);
            if (sp.getValue() == null) {
                final Optional<StepColumn> tagColumn = sp.getInputColumns().stream().filter(c -> c.getDataTags().contains(tag)).findFirst();
                if (tagColumn.isPresent()) {
                    final String colName = tagColumn.get().getDisplayName();
                    sp.setValue(colName);
                }
            }
        }
    }

    /**
     * Set step property value to null if the value is not exists in input columns.
     * @param sp StepProperty
     */
    private void resetColumnIfValueNotFound(final StepProperty sp) {
        if (sp.getInputColumns() != null
                && sp.getValue() != null
                && sp.getInputColumns().stream().noneMatch(c -> c.getDisplayName().equals(sp.getValue().toString()))) {
            sp.setValue(null);
        }
    }

    @Override
    public Boolean isComplete() {
        for (StepProperty sp : getStepProperties()) {
            if (sp.getValue() == null) {
                return false;
            }
        }
        return null;
    }

    private static class ColumnTagOutput extends StepOutput {

        private int columnPhoneDataTag;
        private int columnPhoneValue;
        private int columnEmailDataTag;
        private int columnEmailValue;

        @Override
        public String getName() {
            return "Column Tag";
        }

        @Override
        public void initialise() throws SDKException {
            columnPhoneDataTag = getColumnManager().addColumn(this, "Phone Data Tags", "").getIndex();
            columnPhoneValue = getColumnManager().addColumn(this, "Phone Value", "").getIndex();
            columnEmailDataTag = getColumnManager().addColumn(this, "Email Data Tags", "").getIndex();
            columnEmailValue = getColumnManager().addColumn(this, "Email Value", "").getIndex();
        }

        @Override
        public Object getValueAt(final long row, final int columnIndex) throws SDKException {
            final String selectedColumn;
            if (columnIndex == columnPhoneDataTag || columnIndex == columnPhoneValue) {
                selectedColumn = getArgument(0);
            } else if (columnIndex == columnEmailDataTag || columnIndex == columnEmailValue) {
                selectedColumn = getArgument(1);
            } else {
                return null;
            }
            final Optional<StepColumn> optionalStepColumn = getInputColumn(0, selectedColumn);
            if (optionalStepColumn.isPresent()) {
                final StepColumn column = optionalStepColumn.get();
                if (columnIndex == columnPhoneDataTag || columnIndex == columnEmailDataTag) {
                    // getDataTags() return all the tags of the input column
                    return column.getDataTags().toString();
                } else if (columnIndex == columnPhoneValue || columnIndex == columnEmailValue) {
                    try {
                        return column.getValue(row);
                    } catch (Exception e) {
                        throw new SDKException(e);
                    }
                }
            }
            return null;
        }
    }

}
