package com.experian.aperture.datastudio.sdk.step.examples.testframework;

import com.experian.aperture.datastudio.sdk.exception.SDKException;
import com.experian.aperture.datastudio.sdk.step.ColumnManager;
import com.experian.aperture.datastudio.sdk.step.ServerValueUtil;
import com.experian.aperture.datastudio.sdk.step.StepColumn;
import com.experian.aperture.datastudio.sdk.step.StepConfiguration;
import com.experian.aperture.datastudio.sdk.step.StepOutput;
import com.experian.aperture.datastudio.sdk.step.StepProperty;
import com.experian.aperture.datastudio.sdk.step.StepPropertyType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * Sample step that connect to rest api service.
 */
public class RestServiceSampleStep extends StepConfiguration {
    public static final String COLOR_NAME_COLUMN = "Color name";
    public static final String ADDITIONAL_ATTRIBUTE_COLUMN = "Additional attribute";
    public static final String ATTRIBUTE_CODE = "Color code";
    public static final String ATTRIBUTE_YEAR = "Year";
    public static final String COLOR_SERVICE_URL_KEY = "ColorServiceUrl";
    private static final String AUTH_KEY = "EmailValidation.authKey";
    private static final List<Object> ADDITIONAL_ITEMS_LIC = Arrays.asList(ATTRIBUTE_CODE, ATTRIBUTE_YEAR);
    private static final List<Object> ADDITIONAL_ITEMS_UNLIC = Arrays.asList(ATTRIBUTE_CODE);

    public RestServiceSampleStep() {
        this(new ColorService(HttpClientFactory.getHttpClient()));
    }

    public RestServiceSampleStep(final ColorService colorService) {
        // Basic step information
        setStepDefinitionName("Custom - Rest Service Sample Step");
        setStepDefinitionDescription("Custom step that uses rest api service");

        setStepProperties(Arrays.asList(createInputColumnNodeArg(), createAdditionalAttributeArg()));
        setStepDefinitionIcon("EARTH");
        this.setStepOutput(new MyStepOutput(colorService));
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

    private StepProperty createInputColumnNodeArg() {
        return new StepProperty()
                .ofType(StepPropertyType.COLUMN_CHOOSER)
                .withStatusIndicator(sp -> () -> sp.allowedValuesProvider != null)
                .withIconTypeSupplier(sp -> () -> sp.allowedValuesProvider == null ? "ERROR" : "OK")
                .withArgTextSupplier(sp -> () -> {
                    if (sp.allowedValuesProvider == null) {
                        return "Connect an input";
                    } else {
                        return sp.getValue() == null ? "Select a column" : sp.getValue().toString();
                    }
                })
                .havingInputNode(() -> "input0")
                .havingOutputNode(() -> "output0")
                .validateAndReturn();
    }

    private StepProperty createAdditionalAttributeArg() {
        return new StepProperty()
                .ofType(StepPropertyType.CUSTOM_CHOOSER)
                .withIconTypeSupplier(sp -> () -> sp.allowedValuesProvider == null ? "ERROR" : "OK")
                .withArgTextSupplier(sp -> () -> (sp.getValue() == null || sp.getValue().toString().isEmpty()) ? "Select type" : sp.getValue().toString())
                .withAllowedValuesProvider(() -> {
                    final String authKey = getServerProperty(AUTH_KEY);
                    final boolean fullCapabilities = !(authKey == null || authKey.isEmpty());
                    return fullCapabilities ? ADDITIONAL_ITEMS_LIC : ADDITIONAL_ITEMS_UNLIC;
                })
                .validateAndReturn();
    }

    private static class MyStepOutput extends StepOutput {
        private final Semaphore controlFlag = new Semaphore(2);
        private final ColorService colorService;
        private final ConcurrentHashMap<Long, CompletableFuture<ColorResponse>> results = new ConcurrentHashMap<>();

        MyStepOutput(final ColorService colorService) {
            this.colorService = colorService;
        }

        @Override
        public String getName() {
            return "Step test template";
        }

        @Override
        public void initialise() {
            Optional<String> urlConstant = ServerValueUtil.getGlossaryConstant(COLOR_SERVICE_URL_KEY);
            urlConstant.ifPresent(colorService::setBaseUri);

            final ColumnManager columnManager = this.getColumnManager();
            columnManager.addColumn(this, COLOR_NAME_COLUMN, COLOR_NAME_COLUMN);
            columnManager.addColumn(this, ADDITIONAL_ATTRIBUTE_COLUMN, ADDITIONAL_ATTRIBUTE_COLUMN);
        }

        @Override
        public long execute() {
            final long totalRows = this.getInput(0).getRowCount();
            if (this.isInteractive()) {
                return totalRows;
            }

            long rowId = 0;
            for (; rowId < totalRows; rowId++) {
                try {
                    controlFlag.acquire(); // Simulate limit to two
                    final long currentRow = rowId;
                    final CompletableFuture<ColorResponse> response = this.getColor(rowId)
                            .thenApply(result -> {
                                controlFlag.release();
                                updateProgress(totalRows, currentRow);
                                return result;
                            }).exceptionally(e -> {
                                controlFlag.release();
                                updateProgress(totalRows, currentRow);
                                return null;
                            });

                    results.put(rowId, response);
                } catch (final Exception e) {
                    controlFlag.release();
                    updateProgress(totalRows, rowId);
                }
            }

            return rowId;
        }

        @Override
        public Object getValueAt(final long row, final int columnIndex) throws SDKException {
            final ColorResponse response = this.isInteractive() ? this.getColor(row).join() : this.results.get(row).join();
            if (response == null) {
                return null;
            }

            final String colName = getColumnManager().getColumnFromIndex(columnIndex).getDisplayName();
            switch (colName) {
                case ADDITIONAL_ATTRIBUTE_COLUMN:
                    return getAdditionalAttribute(response);
                case COLOR_NAME_COLUMN:
                    return response.getName();
                default:
                    return null;
            }
        }

        private Object getAdditionalAttribute(final ColorResponse response) {
            final String selectedAttribute = this.getArgument(1);
            switch (selectedAttribute) {
                case ATTRIBUTE_CODE:
                    return response.getColor();
                case ATTRIBUTE_YEAR:
                    return response.getYear();
                default:
                    return "Unknown";
            }
        }

        private CompletableFuture<ColorResponse> getColor(final long row) throws SDKException {
            final StepColumn selectedColumn = getColumnManager().getColumnByName(getArgument(0));
            try {
                final String requestId = selectedColumn.getValue(row).toString();

                return this.colorService.get(requestId);
            } catch (final Exception e) {
                throw new SDKException(e);
            }
        }

        private void updateProgress(final double totalRows, final double rowId) {
            // Update progress only on even count
            final double percentage = 100.0;
            if (rowId != 0 && rowId % 2 == 0) {
                this.sendProgress((rowId / totalRows) * percentage);
            }
        }
    }
}
