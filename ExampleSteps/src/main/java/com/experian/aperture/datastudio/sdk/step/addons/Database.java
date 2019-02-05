package com.experian.aperture.datastudio.sdk.step.examples;

import com.experian.aperture.datastudio.sdk.exception.SDKException;
import com.experian.aperture.datastudio.sdk.step.*;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A test example step that exercises the SDK's datastore and table functions.
 * It tests a couple of scenarios:
 * 1. The display and selection of datasources and their tables, and obtaining the resulting table object for the
 *    selected item.
 * 2. Creates a new file in the user's import datastore if necessary, then appends data to it every time
 *    it is run.
 */
public class Database  extends StepConfiguration {
    private static final String SELECT_DATASTORE =  "<Select a datastore>";
    private static final String SELECT_TABLE =  "<Select a table>";
    String selectedDatastore = null;

    public Database() {
        // Basic step information
        setStepDefinitionName("Custom - Database Test");
        setStepDefinitionDescription("Demonstrates table chooser and creating/appending to a file table");
        setStepDefinitionIcon("DATABASE");
        // A PROCESS_ONLY step can only be connected to other processes - it cannot be connected to data inputs/outputs.
        setStepDefinitionType("PROCESS_ONLY");


        // Define the step properties:
        // 1. A list of datasources accessible to the current user
        // 2. A list of tables for the selected datasource.
        // Input and output definitions are not necessary for PROCESS or PROCESS_ONLY steps.
        StepProperty arg1 = new StepProperty()
                .ofType(StepPropertyType.CUSTOM_CHOOSER) // CUSTOM_CHOOSER allows the values to be specified by .withAllowedValuesProvider
                .withIconTypeSupplier(sp -> () -> {
                    String selectedCol = getSelectedDataStore(sp.getValue());
                    return selectedCol.equals(SELECT_DATASTORE) ? "ERROR" : "OK";
                })
                .withAllowedValuesProvider(() ->
                    getDatastores().stream().map(t -> t.getDisplayName()).collect(Collectors.toList())
                )
                .withArgTextSupplier(sp -> () -> getSelectedDataStore(sp.getValue()))
                .validateAndReturn();

        StepProperty arg2 = new StepProperty()
                .ofType(StepPropertyType.CUSTOM_CHOOSER)
                .withAllowedValuesProvider(() ->
                    getDataStoreTables(arg1.getValue()).stream().map(t -> t.getDisplayName()).collect(Collectors.toList())
                )
                .withIconTypeSupplier(sp -> () -> {
                    String selectedCol = getSelectedTable(sp.getValue());
                    return selectedCol.equals(SELECT_TABLE) ? "ERROR" : "OK";
                })
                .withArgTextSupplier(sp -> () -> getSelectedTable(sp.getValue()))
                .validateAndReturn();

        setStepProperties(Arrays.asList(arg1, arg2));

        // Define and set the step output class
        setStepOutput(new Database.MyStepOutput());
    }

    /**
     * Validate that the step has been configured correctly.
     * If so, return null (the default, or true - it doesn't matter) to enable the rows drilldown,
     * and enable the workflow to be considered valid for execution and export.
     * If invalid, return false. Data Rows will be disabled as will workflow execution/export.
     * @return false - will not be able to execute/export/show data
     *         true  - will be able to execute/export/show data
     *         null  - will revert back to default behaviour, i.e. enabled if step has inputs, and are they complete?
     */
    @Override
    public Boolean isComplete() {
        List<StepProperty> properties = getStepProperties();
        if (properties != null && !properties.isEmpty()) {
            StepProperty arg1 = properties.get(0);
            StepProperty arg2 = properties.get(1);
            if (arg1 != null && arg1.getValue() != null && arg2 != null && arg2.getValue() != null) {
                // verify datastore and table are still valid
                TableSDK selectedTable = getTable(arg1.getValue().toString(), arg2.getValue().toString());
                return selectedTable != null;
            }
        }
        return false;
    }

    /**
     * UI function to get the selected datastore
     * When the datastore changes, clear the selected table.
     * @param dataStoreName
     * @return name of selected datastore
     */
    private String getSelectedDataStore(Object dataStoreName) {
        if (dataStoreName == null) {
            return SELECT_DATASTORE;
        }

        String datastore = dataStoreName.toString();
        if (selectedDatastore != null && selectedDatastore.equals(datastore)) {
            return datastore;
        }

        // clear selected table
        if (selectedDatastore != null) {
            List<StepProperty> properties = getStepProperties();
            if (properties != null && !properties.isEmpty()) {
                StepProperty arg2 = properties.get(1);
                arg2.setValue(null);
            }
        }

        selectedDatastore = datastore;
        return selectedDatastore;
    }

    /**
     * UI Helper Function
     * returns the text to be displayed in the select table argument
     * @param tableName
     * @return string to display
     */
    private static String getSelectedTable(Object tableName) {
        return tableName == null ? SELECT_TABLE : tableName.toString();
    }

    /**
     * Returns a list of datastores accessible by the current user
     * @return a list of datasource objects
     */
    public List<Datastore> getDatastores() {
        return getDatastores(getUserId());
    }

    /**
     * Gets all tables in the given datastore that are visible to the current user
     * @param datastoreName
     * @return a list of tables
     */
    public List<TableSDK> getDataStoreTables(Object datastoreName) {
        Long userId = getUserId();
        if (datastoreName != null && userId != null) {
            return getTables(datastoreName.toString(), userId);
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Gets the datastore object matching the given name
     * @param datastoreName
     * @return the datastore object
     */
    private Datastore getDatastore(String datastoreName) {
        List<Datastore> datastores = getDatastores();
        Optional<Datastore> datastore = datastores.stream().filter(db -> db.getDisplayName().equals(datastoreName)).findFirst();
        return datastore.isPresent() ? datastore.get() : null;
    }

    /**
     * Gets the table from the given datastore and table names
     * @param datastoreName
     * @param tableName
     * @return the table object or null if it cannot be found
     */
    private TableSDK getTable(String datastoreName, String tableName) {
        Datastore ds = getDatastore(datastoreName);
        if (ds != null) {
            List<TableSDK> tables = ds.getTables(getUserId());
            Optional<TableSDK> table = tables.stream().filter(t -> t.getDisplayName().equals(tableName)).findFirst();
            return table.isPresent() ? table.get() : null;
        }
        return null;
    }

    /**
     * define the output data view, i.e. rows and columns
     */
    private class MyStepOutput extends StepOutput {
        @Override
        public String getName() {
            return "Update table";
        }

        /**
         * Gets the datastore object for the current user's personal import datastore
         * @return the datastore object
         * @throws SDKException
         */
        private Datastore getImportDatastore() throws SDKException {
            List<Datastore> dss = getDatastores();
            Optional<Datastore> dso = dss.stream().filter(db -> db.isImport()).findFirst();
            if (dso.isPresent()) {
                return dso.get();
            } else {
                throw new SDKException("Could not find import datastore");
            }
        }

        /**
         * Gets the table from a datastore object given the table name.
         * @param datastore
         * @param tableName
         * @return the table object
         */
        private TableSDK getTableFromDatastore(Datastore datastore, String tableName) {
            List<TableSDK> tables = datastore.getTables(getUserId());
            Optional<TableSDK> newTableOpt = tables.stream().filter(t -> t.getDisplayName().equalsIgnoreCase(tableName)).findFirst();
            return newTableOpt.isPresent() ? newTableOpt.get() : null;
        }

        /**
         * Creates a new file in the datastore, and refreshes the datastore to convert the new file into a table.
         * Only valid for file datastores.
         * @param ds
         * @param fileName
         * @throws SDKException
         */
        private void createNewTable(Datastore ds, String fileName) throws SDKException {
            ds.refreshSynchronous();

            List<TableSDK> tables = ds.getTables(getUserId());

            File file = ds.createNewTable(fileName);
            try {
                boolean res = file.createNewFile();
                if (res) {
                    // write to new file
                    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                    writer.write("heading1, heading2, heading3\n");
                    writer.write("1, data1.1, data1.2\n");
                    writer.write("2, data2.1, data2.2\n");
                    writer.write("3, data3.1, data3.2\n");
                    writer.close();
                }
            } catch (IOException e) {
                // file already exists so return
                return;
            }

            ds.refreshSynchronous();

            List<TableSDK> updatedTables = ds.getTables(getUserId());
            if (updatedTables.size() <= tables.size()) {
                // belt & braces - but should never get here, unless someone else has deleted
                // the datastore's tables during this operation, which is possible.
                throw new SDKException("Table creation failed");
            }
        }

        /**
         * Appends new data to an existing table (file) and caches the table.
         * @param ds
         * @param tableName
         * @return
         * @throws SDKException
         */
        private long appendToFile(Datastore ds, String tableName) throws SDKException {
            TableSDK newTable = getTableFromDatastore(ds, tableName);

            if (newTable.getAttributeNames().size() != 3) {
                throw new SDKException("Incorrect number of attributes.");
            }

            // append some more data to the file
            try {
                File newTableFile = newTable.openFile();

                BufferedWriter writer = new BufferedWriter(new FileWriter(newTableFile, true));
                writer.append("4, data4.1, data4.2\n");
                writer.append("5, data5.1, data5.2\n");
                writer.append("6, data6.1, data6.2\n");
                writer.close();
            } catch (IOException e) {
                throw new SDKException("Failed to write to file: " + e.getMessage());
            }

            newTable.clearCachedData(getUserId());
            newTable.cacheData(getUserId());
            while (newTable.isCaching(getUserId())) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }

            return newTable.getRowCount(getUserId());
        }

        /**
         * Deletes the given table from the datastore.
         * @param ds
         * @param tableName
         * @throws SDKException
         */
        private void deleteTable(Datastore ds, String tableName) throws SDKException {
            TableSDK table = getTableFromDatastore(ds, tableName);
            table.deleteFile(getUserId());
            ds.refreshSynchronous();
        }

        /**
         * Called when the step is executed. This happens when any downstream step is executed.
         * @return row count - is unused
         * @throws SDKException
         */
        @Override
        public long execute() throws SDKException {
            // create new file in My Files datastore
            String fileName = "sdkexample.txt";
            String tableName = "sdkexample";

            Datastore ds = getImportDatastore();
            try {
                // create the new table if necessary
                createNewTable(ds, fileName);
            } catch (SDKException ex) {
                // ignore
            }

            try {
                // append more rows to it
                long rowCount = appendToFile(ds, tableName);
                assert rowCount > 0;
            } catch (SDKException ex) {
                // delete it (for no good reason other than to test the functionality)
                deleteTable(ds, tableName);
            }

            // get table from arguments
            String databaseName = getArgument(0).toString();
            tableName = getArgument(1).toString();
            TableSDK table = getTable(databaseName, tableName);
            assert table != null;

            return 0;
        }

        /**
         * Has to be implemented, but will not be called.
         * @param row The row number required
         * @param columnIndex The index of the column required
         * @return
         * @throws SDKException
         */
        @Override
        public Object getValueAt(long row, int columnIndex) throws SDKException {
            return null;
        }
    }

}
