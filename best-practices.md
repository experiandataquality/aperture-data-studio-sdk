# Best practices and limitation for custom steps

## Best practices

### One custom step per jar
It is recommended that you bundle one custom step per JAR. This will simplifies the process of updating or removing a single custom step. And it will also promote security by isolation. A problematic custom step can be easily detected and isolated without affecting after custom step.

### Fat jar
Bundle the custom step in a fat jar where all dependencies are bundled into a single jar. This allow plugin to depends on specific version of libraries without affecting other plugin. For more details, [read this](README.md#class-isolation).



### Progress bar handling
Please take note that `progressChanged()` must not be called inside `outputColumnManager.onValue()`. [See example.](README.md#Progress-bar-handling)

### Use provided HTTP client library
It is recommended to use the provided HTTP client library to access external endpoints through the HTTP protocol.  [See example.](README.md#The-HTTP-Client-library)

### Use provided Logger
It is also recommended to use the provided `Logger` to write custom step logs.  [See example.](README.md#The-Logging-library) Custom step logs can be retrieve from C:\ApertureDataStudio\data\log


### Avoid temporary field
Given the example below, `newColumns` acts as a temporary field to hold the new column names defined in the `createConfiguration()` method. The `createProcessor()` method also uses `newColumns` to provide the result for the newly add columns. As you can see `newColumns` field is only set if `createConfiguration()` is called. This will cause unexpected result if `createProcessor()` is called before `createConfiguration()`.

```java
private List<String> newColumns;

@Override
public StepConfiguration createConfiguration(final StepConfigurationBuilder configurationBuilder) {
    return configurationBuilder
            .withNodes(stepNodeBuilder -> stepNodeBuilder
                    .addInputNode(INPUT_ID)
                    .addOutputNode(OUTPUT_ID)
                    .build())
            .withStepProperties(stepPropertiesBuilder -> stepPropertiesBuilder.build())
            .withOutputLayouts(outputLayoutBuilder -> outputLayoutBuilder
                    .forOutputNode(OUTPUT_ID, outputColumnBuilder ->
                            {
                                this.newColumns = Arrays.asList("Column A", "Column B");
                                this.newColumns.forEach(newColumn -> outputColumnBuilder.addColumn(newColumn));
                                return outputColumnBuilder.build();
                            })
                    .build())
            .build();
}

@Override
public StepProcessor createProcessor(final StepProcessorBuilder processorBuilder) {
    return processorBuilder
            .forOutputNode(OUTPUT_ID, (processorContext, outputColumnManager) -> {
                // newColumns might be empty, as createConfiguration() might not 
                // be called before createProcessor()
                this.newColumns.forEach(newColumn -> outputColumnManager.onValue(newColumn, rowIndex -> "Value of " + newColumn));
                final ProcessorInputContext inputContext = processorContext.getInputContext(INPUT_ID).orElseThrow(IllegalArgumentException::new);
                return inputContext.getRowCount();
            })
            .build();
}
```

## Limitation
### Both custom chooser and column chooser cannot be reset
Once the value for the custom chooser or column chooser is selected, it cannot be reset to its initial state anymore. For example, once hash tag is selected for the prefix custom chooser, prefix custom chooser cannot be reset to display "Select a value".

![cannot be reset](images/concatTwoColumnStep.png)


### Single output node
Currently the custom step only supports single output node, as you can assign the result of the custom step to only *one* output node.

### Cannot add column during processing step
No new column can be added in the `StepProcessor`, column can only be added in the `StepConfiguration`.

### Not able to tag output column
Adding tags to output column is not supported.
