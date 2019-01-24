package com.experian.aperture.datastudio.sdk.parser.shipped;

import com.experian.aperture.datastudio.sdk.parser.CustomParser;
import com.experian.aperture.datastudio.sdk.parser.ParserParameter;
import com.experian.aperture.datastudio.sdk.parser.ParserProperty;
import com.experian.aperture.datastudio.sdk.parser.ParserPropertyType;
import com.experian.aperture.datastudio.sdk.step.Column;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

/**
 * Example custom parser class that simply generates row data.
 */
public class ParserTemplate extends CustomParser {
    /**
     * List of supported file types.
     */
    private static String[] allowableFileTypes = {
        "template"
    };

    /**
     * The arbitrary number of columns.
     */
    private static final int COLUMN_COUNT = 10;

    /**
     * The arbitrary number of rows (can be set by user).
     */
    private static final int DEFAULT_ROW_COUNT = 1000;

    /**
     * The properties associated with this parser.
     */
    private List<ParserProperty> properties;

    /**
     * The ID for the parameter.
     */
    private static final String ROW_COUNT_ID = "RowCount";

    /**
     * Constructor for the parser template.
     */
    public ParserTemplate() {
        // Pass the name and description to the parent class.
        super("Template", "Parser Template");

        // Create a single property that controls the number of rows returned (configured in Data Explorer)
        // Use a string to make processing more robust
        // This property controls the number of rows that are loaded into Data Studio
        properties = new ArrayList<>();

        ParserProperty rows = new ParserProperty(ParserPropertyType.STRING, "Row count", "The number of rows in each table", ROW_COUNT_ID);

        properties.add(rows);
    }

    @Override
    public final ParseResult attemptParse(final Supplier<InputStream> streamSupplier, final String filename, final List<ParserParameter> parameterConfiguration) throws CustomParseException {
        // Log the fact that we are attempting to parse a file.
        String started = String.format("Template parsing attempted for %s", filename);
        log(started);

        List<ParserParameter> localParameters = new ArrayList<>();

        // Use the provided parameters or create the default
        if (parameterConfiguration == null) {
            ParserParameter.updateParameters(localParameters, ROW_COUNT_ID, String.valueOf(DEFAULT_ROW_COUNT));
        } else {
            localParameters.addAll(parameterConfiguration);
        }

        List<ParseOutput> outputs = new ArrayList<>();
        int tableCount = 2;

        // Output multiple tables to demonstrate parser capability.
        for (int table = 0; table < tableCount; table++) {
            List<Column> columns = new ArrayList<>();

            // Create a number of columns and their headers
            for (int i = 0; i < COLUMN_COUNT; i++) {
                String name = String.format("T%d H%d", table + 1, i + 1);
                Column column = new Column(null, i, name, name);
                columns.add(column);
            }

            // Add each as a new output, or table.
            ParseOutput output = new ParseOutput(String.format("%s Table %d", filename, table + 1), String.format("This is table number %d", table + 1));
            output.setColumns(columns);

            outputs.add(output);
        }

        // Create the result - will 'parse' anything that is provided
        // Data Studio will only pass files in that meet the supported extensions.
        ParseResult result = new ParseResult();

        // Set this status as appropriate
        result.setStatus(ParseStatus.PARSED);

        // Attach the outputs
        result.setOutputs(outputs);

        // And apply any parameters that were supplied, or created by the parse attempt.
        result.setParameters(localParameters);

        return result;
    }

    @Override
    public final Iterator<Object[]> parse(final InputStream input, final String filename, final int outputIndex, final List<ParserParameter> parameterConfiguration, final int maxRows) throws CustomParseException {
        int rowTotal;

        // Attempt to parse the parameters - catch all exceptions for robustness.
        try {
            String rows = ParserParameter.getStringParameter(parameterConfiguration, ROW_COUNT_ID);

            rowTotal = Integer.valueOf(rows);
        } catch (Exception ex) {
            // Any exception in parsing the string - revert to the default
            rowTotal = DEFAULT_ROW_COUNT;
        }

        final int rowCount = rowTotal;

        // parse returns an iterator with each element an array of objects
        return new Iterator<Object[]>() {
            /**
             * The current row being read.
             */
            private int currentRow = 0;

            @Override
            public boolean hasNext() {
                // We are just testing the number of rows read against an overal count/
                return currentRow < rowCount;
            }

            @Override
            public Object[] next() {
                // Ensure that rows remain
                if (hasNext()) {
                    Object[] result = new Object[COLUMN_COUNT];
                    if (currentRow < rowCount) {
                        ++currentRow;

                        // Just pack each cell with a 3D co-ordinate (table, row, column)
                        for (int c = 0; c < COLUMN_COUNT; c++) {
                            result[c] = String.format("T%d R%d C%d", outputIndex + 1, currentRow, c + 1);
                        }
                    }

                    return result;
                }

                // At the end, throw the appropriate exception.
                throw new NoSuchElementException();
            }
        };
    }

    @Override
    public final List<ParserProperty> getProperties() {
        // The parameters that have been set up.
        return properties;
    }

    @Override
    public final List<String> getSupportedFileExtensions() {
        // Return all allowed file types.
        return Arrays.asList(allowableFileTypes);
    }
}
