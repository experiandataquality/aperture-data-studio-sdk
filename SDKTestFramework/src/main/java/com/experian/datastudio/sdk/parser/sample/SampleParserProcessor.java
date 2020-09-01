package com.experian.datastudio.sdk.parser.sample;

import com.experian.datastudio.sdk.api.parser.processor.*;

import java.io.*;
import java.util.*;

public class SampleParserProcessor implements ParserProcessor {
    @Override
    public List<ParserTableDefinition> getTableDefinition(TableDefinitionContext context) throws IOException {
        Objects.requireNonNull(context.getLocale(), "context.getLocale() must not null.");
        Objects.requireNonNull(context.getFilename(), "context.getFilename() must not null.");
        Optional<Object> optEncoding = context.getParameterConfiguration(SampleParser.PARAMETER_KEY_ENCODING);
        if (!optEncoding.isPresent()) {
            throw new IllegalStateException("Expected encoding parameter is presented.");
        }

        InputStream stream = context.getStreamSupplier().get();
        ParserTableDefinitionFactory parserTableDefinitionFactory = context.getParserTableDefinitionFactory();
        ParserColumnDefinitionFactory parserColumnDefinitionFactory = context.getParserColumnDefinitionFactory();

        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        if (reader.ready()) {
            String firstLine = reader.readLine();
            ParserTableDefinition tableDefinition = parserTableDefinitionFactory.createTableDefinition("1", "MySample", "My sample definition");
            Arrays.stream(firstLine.split(","))
                    .forEach(columnName -> tableDefinition.addColumn(parserColumnDefinitionFactory.createColumnDefinition(columnName, columnName)));

            reader.close();
            stream.close();
            return Collections.singletonList(tableDefinition);
        }
        return null;
    }

    @Override
    public ClosableIterator<List<String>> getRowIterator(RowIteratorContext context) {
        Objects.requireNonNull(context.getLocale(), "context.getLocale() must not null.");
        if (!"1".equals(context.getTableId())) {
            throw new IllegalStateException("Expected tableId '" + context.getTableId() + "' equals to '1'.");
        }
        Objects.requireNonNull(context.getTableDefinition(), "context.getTableDefinition() must not null.");
        long maxRow = (long) context.getParameterConfiguration(SampleParser.PARAMETER_KEY_MAX_ROWS).orElse(0L);
        if (maxRow <= 0) {
            throw new IllegalStateException("Expected maxRow '" + maxRow + "' larger than '0'.");
        }

        InputStreamReader streamReader = new InputStreamReader(context.getStreamSupplier().get());
        Scanner scanner = new Scanner(streamReader);
        ClosableIteratorBuilder closableIteratorBuilder = context.getClosableIteratorBuilder();

        if (scanner.hasNextLine()) {
            //skip first line
            scanner.nextLine();
            return closableIteratorBuilder.withHasNext(scanner::hasNextLine)
                    .withNext(() -> Arrays.asList(scanner.nextLine().split(",")))
                    .withProgress(() -> 1.0)
                    .withClose(scanner::close)
                    .build();
        }

        return null;
    }
}
