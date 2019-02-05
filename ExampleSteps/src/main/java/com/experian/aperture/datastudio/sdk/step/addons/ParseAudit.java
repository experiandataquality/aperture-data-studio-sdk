package com.experian.aperture.datastudio.sdk.step.examples;

import com.experian.aperture.datastudio.sdk.parser.CustomParser;
import com.experian.aperture.datastudio.sdk.parser.ParserParameter;
import com.experian.aperture.datastudio.sdk.parser.ParserProperty;
import com.experian.aperture.datastudio.sdk.parser.ParserPropertyType;
import com.experian.aperture.datastudio.sdk.step.Column;
import org.apache.logging.log4j.LogManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ParseAudit extends CustomParser {

    private static String fileExtension = "audit";

    private Map<String, ParserProperty> properties = new HashMap<>();

    public ParseAudit() {
        super("Json Parser", "Parses Json files");

        // IdField (name of the field to use for persistent id)
        ParserProperty name = new ParserProperty(ParserPropertyType.STRING, "Audit table name", "Demo string for audit name", "NAME", true);
        properties.put(name.getId(), name);
    }

    @Override
    public ParseResult attemptParse(Supplier<InputStream> streamSupplier, String filename, List<ParserParameter> parameterConfiguration) throws CustomParseException {
        LogManager.getLogger().info("Parsing " + filename);

        ParseResult result = new ParseResult();

        List<ParserParameter> localParameters = new ArrayList();
        if (parameterConfiguration != null) {
            localParameters.addAll(parameterConfiguration);
        }

        boolean parsable = false;
        String filenameNoExtension = filename;
        int finalDot = filename.lastIndexOf('.');
        if (finalDot > 0) {
            String extension = filename.substring(finalDot + 1);
            parsable = extension.equalsIgnoreCase(fileExtension);
            filenameNoExtension = filename.substring(0, finalDot);
        }

        List<ParseOutput> outputs = new ArrayList();

        if (parsable) {
            try (InputStream input = streamSupplier.get()) {
                if (input == null) {
                    throw new CustomParseException("File not loaded");
                } else {
                    try (InputStreamReader reader = new InputStreamReader(input)) {
                        buildHeadings(reader, filenameNoExtension, outputs);
                    }
                }
            } catch (Exception ex) {
                parsable = false;
                result.addError("Could not parse file");
                result.addError(ex.toString());

                buildErrorHeadings(filenameNoExtension, ex.getMessage(), outputs);
            }
        }

        result.setOutputs(outputs);
        result.setStatus(parsable ? ParseStatus.PARSED : ParseStatus.NOT_PARSABLE);
        result.setParameters(localParameters);

        return result;
    }

    private void buildHeadings(final InputStreamReader reader, final String filename, final List<ParseOutput> outputs) throws IOException {
        // configure file metadata
        String shortName = new File(filename).getName();
        ParseOutput output = new ParseOutput(shortName, "Audit File");
        output.setEstimatedSize(10L);
        outputs.add(output);

        // read file to get heading
        String header = getHeader(reader);

        // add one test heading
        List<Column> columns = new ArrayList<>();
        Column column = new Column(null, columns.size(), header, "HEADING (Primary)");
        columns.add(column);

        output.setColumns(columns);
    }

    private String getHeader(InputStreamReader reader) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(reader);
        String header = "";
        while(bufferedReader.ready()) {
            header = bufferedReader.readLine();
        }
        return header;
    }

    private void buildErrorHeadings(final String filenameNoExtension, final String error, final List<ParseOutput> outputs) {
        // create error metadata and create error header
        List<Column> columns = new ArrayList<>();
        Column column = new Column(null, 0, error, error);
        columns.add(column);

        ParseOutput output = new ParseOutput(new File(filenameNoExtension).getName(), "Error");
        output.setEstimatedSize(1L);
        output.setColumns(columns);

        outputs.add(output);
    }

    BufferedReader bufferedInputStream = null;

    @Override
    public Iterator<Object[]> parse(InputStream input, String filename, int outputIndex, List<ParserParameter> parameterConfiguration, int maxRows) throws CustomParseException {

        // We could parse without this but enforcing for now.
        if (parameterConfiguration == null) {
            throw new CustomParseException("No parameters supplied.");
        }

        if (bufferedInputStream == null) {
            try {
                InputStreamReader inputStreamReader = new InputStreamReader(input);
                // do away with the header
                getHeader(inputStreamReader);
                bufferedInputStream = new BufferedReader(inputStreamReader);
            } catch (IOException ex) {
                // do something
            }
        }

        return new Iterator<Object[]>() {
            String line;

            @Override
            public boolean hasNext() {
                try {
                    line = bufferedInputStream.readLine();
                    return true;
                } catch (IOException e) {
                    return false;
                }
            }

            @Override
            public Object[] next() {
                String line;

                return rowsRetrieved.get(currentRow++);
            }
        };
    }

    @Override
    public List<ParserProperty> getProperties() {
        return new ArrayList(properties.values());
    }

    @Override
    public List<String> getSupportedFileExtensions() {
        return Arrays.asList(fileExtension);
    }
}
