package com.experian.aperture.datastudio.sdk.step.examples;

import com.experian.aperture.datastudio.sdk.exception.SDKException;
import com.experian.aperture.datastudio.sdk.step.Cache;
import com.experian.aperture.datastudio.sdk.step.CacheConfiguration;
import com.experian.aperture.datastudio.sdk.step.StepConfiguration;
import com.experian.aperture.datastudio.sdk.step.StepOutput;
import com.experian.aperture.datastudio.sdk.step.StepProperty;
import com.experian.aperture.datastudio.sdk.step.StepPropertyType;

import java.util.Collections;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public final class NewCacheStep extends StepConfiguration {

    public NewCacheStep() {
        setStepDefinitionName("Custom - Cache");
        setStepDefinitionDescription("Sample step to demonstrate the simple usage of Cache API in Custom Step");
        setStepDefinitionIcon("INFO");
        setStepOutput(new NewCacheOutput());

        StepProperty sp2 = new StepProperty()
                .ofType(StepPropertyType.INPUT_LABEL)
                .withArgTextSupplier(sp -> () -> "Connect an input")
                .withIconTypeSupplier(sp -> () -> "OK")
                .withStatusIndicator(sp -> () -> true)
                .havingInputNode(() -> "input0")
                .havingOutputNode(() -> "output0")
                .validateAndReturn();
        setStepProperties(Collections.singletonList(sp2));
    }

    private static final class NewCacheOutput extends StepOutput {

        private static final String FIRST_CACHE = "1ST";
        private static final String SECOND_CACHE = "2ND";
        private Cache firstCache = null;
        private final Random random = new Random();
        private int startColumn = 0;

        @Override
        public void initialise() {
            startColumn = getColumnManager().getColumnCount();
            getColumnManager().addColumn(this, "First", "");
            getColumnManager().addColumn(this, "Second", "");
            firstCache = getCache(CacheConfiguration.withName(FIRST_CACHE).withTtl(60, TimeUnit.SECONDS));
        }

        @Override
        public String getName() {
            return "Custom - New Cache";
        }

        @Override
        public Object getValueAt(long row, int col) throws SDKException {
            try {
                String key = row + ":" + col;
                if (col == startColumn) {
                    String value = firstCache.read(key);
                    if (value == null) {
                        Thread.sleep(100);
                        value = String.valueOf(random.nextLong());
                        firstCache.write(key, value);
                    }
                    return value;
                } else if (col == startColumn + 1) {
                    final Cache secondCache = getCache(CacheConfiguration.withName(SECOND_CACHE).withTtl(30, TimeUnit.SECONDS));
                    String value = secondCache.read(key);
                    if (value == null) {
                        Thread.sleep(100);
                        value = String.valueOf(random.nextLong());
                        secondCache.write(key, value);
                    }
                    return value;
                }
                return null;
            } catch (Exception ex) {
                throw new SDKException(ex);
            }
        }
    }
}
