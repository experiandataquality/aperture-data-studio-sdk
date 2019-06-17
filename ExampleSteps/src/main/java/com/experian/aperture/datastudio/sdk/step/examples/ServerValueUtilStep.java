package com.experian.aperture.datastudio.sdk.step.examples;

import com.experian.aperture.datastudio.sdk.step.ServerValueUtil;
import com.experian.aperture.datastudio.sdk.step.StepConfiguration;
import com.experian.aperture.datastudio.sdk.step.StepOutput;
import com.experian.aperture.datastudio.sdk.step.StepProperty;
import com.experian.aperture.datastudio.sdk.step.StepPropertyType;

import java.awt.Color;
import java.util.Arrays;
import java.util.Optional;

public class ServerValueUtilStep extends StepConfiguration {

    public ServerValueUtilStep() {
        setStepDefinitionName("Custom - Server Value Util");
        setStepDefinitionIcon("INFO");
        setStepDefinitionDescription("");
        setStepOutput(new ServerValueOutput());

        final Optional<String> time24HR = ServerValueUtil.getGlossaryConstant("TIME_24HR");
        final Optional<Object> locale = ServerValueUtil.getServerProperty("System.locale");
        final StepProperty arg1 = new StepProperty()
                .ofType(StepPropertyType.INPUT_LABEL)
                .withArgTextSupplier(sp -> () -> locale
                        .map(o -> "Server Property: " + o.toString())
                        .orElse("System.locale is not defined as server properties"))
                .havingInputNode(() -> "input0")
                .havingOutputNode(() -> "output0")
                .validateAndReturn();
        final StepProperty arg2 = new StepProperty()
                .ofType(StepPropertyType.INPUT_LABEL)
                .withArgTextSupplier(sp -> () -> time24HR
                        .map(o -> "Glossary Constant: " + o)
                        .orElse("TIME_24HR glossary constant is not defined"))
                .validateAndReturn();

        setStepProperties(Arrays.asList(arg1, arg2));
    }

    private static class ServerValueOutput extends StepOutput {

        @Override
        public void initialise() {
            getColumnManager().addColumn(this, "Server Property: System.locale", "").setBackground(new Color(217, 236, 233));
            getColumnManager().addColumn(this, "Glossary Constant: TIME_24HR", "").setBackground(new Color(217, 236, 233));
        }

        @Override
        public String getName() {
            return "Server Value Util";
        }

        @Override
        public Object getValueAt(long row, int columnIndex) {
            if (getColumnManager().getColumns().size() - 2 == columnIndex) {
                return ServerValueUtil.getServerProperty("System.locale").orElse("System.locale not present in server property");
            } else {
                return ServerValueUtil.getGlossaryConstant("TIME_24HR").orElse("TIME_24HR not present in glossary constant");
            }
        }
    }

}
