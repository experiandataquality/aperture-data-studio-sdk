package com.experian.aperture.datastudio.sdk.parser.examples;

import com.experian.aperture.datastudio.sdk.parser.CustomParser;
import com.experian.aperture.datastudio.sdk.parser.ParserProperty;
import com.experian.aperture.datastudio.sdk.step.Column;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Example custom parser class that simply generates row data.
 */
public class FakeParser extends CustomParser {
    /**
     * The arbitrary number of columns.
     */
    private final int columnCount = 10;

    /**
     * The arbitary number of rows.
     */
    private final int rowCount = 1000;

    /**
     * The current row being read.
     */
    private int currentRow = 0;

    /**
     * Constructor for the fake parser.
     */
    public FakeParser() {
        super("Fake", "Fake Parser");
    }

    @Override
    public final ParseResult attemptParse(final Supplier<InputStream> streamSupplier, final String filename, final List<Object> parameterConfiguration) throws CustomParseException {
        int finalDot = filename.lastIndexOf('.');
        boolean parsable = false;

        if (finalDot > 0) {
            String extension = filename.substring(finalDot + 1);
            parsable = extension.equalsIgnoreCase("custom");
        }

        ParseResult result = new ParseResult();

        result.setStatus(parsable ? ParseStatus.PARSED : ParseStatus.NOT_PARSABLE);

        if (parsable) {
            List<Column> columns = new ArrayList<>();

            for (int i = 1; i <= columnCount; i++) {
                String name = "H" + i;
                Column column = new Column(null, -1, name, name);
                columns.add(column);
            }

            ParseOutput output = new ParseOutput(filename, filename);
            output.setColumns(columns);

            List<ParseOutput> outputs = new ArrayList<>();
            outputs.add(output);

            result.setOutputs(outputs);
        }

        return result;
    }

    @Override
    public final Iterator<Object[]> parse(final InputStream input, final String filename, final int outputIndex, final List<Object> parameterConfiguration, final int maxRows) throws CustomParseException {
        currentRow = 0;

        return new Iterator<Object[]>() {
            @Override
            public boolean hasNext() {
                return currentRow < rowCount;
            }

            @Override
            public Object[] next() {
                if (hasNext()) {
                    Object[] result = new Object[columnCount];
                    if (currentRow < rowCount) {
                        ++currentRow;
                        for (int c = 0; c < columnCount; c++) {
                            result[c] = String.format("R%dC%d", currentRow, c + 1);
                        }
                    }

                    return result;
                }

                return null;
            }
        };
    }

    @Override
    public final String getParserDescription() {
        return getParserName();
    }

    @Override
    public final List<ParserProperty> getProperties() {
        return null;
    }

    @Override
    public final List<String> getSupportedFileExtensions() {
        return null;
    }
}
