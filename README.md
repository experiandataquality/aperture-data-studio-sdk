# Aperture Data Studio SDK

The SDK provides a simple Java library to create and test your own custom Workflow steps, extending Aperture Data Studio capabilities. You can also add your own custom parsers which will enable Data Studio to load data from files. 

This repo contains the SDK JAR and a pre-configured Java project that uses Gradle, allowing you to easily build your own custom step. Alternatively, you can add the SDK as a dependency to your own project by downloading the SDK JAR from the `sdkapi` folder.

## Table of contents

- [Generating a custom step from a new or existing project](#generating-a-custom-step-from-a-new-or-existing-project)
- [Comparison of SDK v1.0 and v2.0](#comparison-of-sdk-v1.0-and-v2.0)
- [Creating a custom step](#creating-a-custom-step)
    - [Importing the step SDK](#importing-the-step-sdk)
	- [Creating your metadata](#configuring-your-step)
		- [Adding metadata](#adding-metadata)
		- [Metadata sample code](#metadata-sample-code)
    - [Configuring your step](#configuring-your-step)
        - [Adding nodes](#adding-nodes)
		- [Adding step properties](#adding-step-properties)
		- [Configure IsCompleteHandler](#configure-iscompletehandler)
		- [Configure column layouts](#configure-column-layouts)
		- [StepConfigurationBuilder sample code](#stepconfigurationbuilder-sample-code)
    - [Processing your step](#processing-your-step)
        - [Execute step](#execute-step)
		- [StepProcessorBuilder sample code](#stepprocessorbuilder-sample-code)
- [The logging library](#the-logging-library)
- [The cache configuration](#the-cache-configuration)
- [The http client library](#the-http-client-library)


## Generating a custom step from a new or existing project

1. You can either use Gradle or Maven: 

  If using Gradle, point to the SDK repository in the `build.gradle`:

   ```gradle
   apply plugin: 'java'

   repositories {
       mavenCentral()
       maven {
           url 'https://raw.githubusercontent.com/experiandataquality/aperture-data-studio-sdk/github-maven-snapshot-repository/maven'
       }
   }

   dependencies {
       compileOnly("com.experian.datastudio:sdkapi:2.0.0-SNAPSHOT")
   }
   ```

  If you don't want to use Gradle, you'll have to configure your own Java project to generate a compatible JAR artifact:
   - Create a new Java project or open an existing one.
   - Download and install the [sdkapi.jar]([TO BE CHANGED]) file.

  If using Maven, modify `pom.xml` to add the SDK GitHub repository:

   ```xml
   <project xmlns="http://maven.apache.org/POM/4.0.0"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                                http://maven.apache.org/maven-v4_0_0.xsd">
       <modelVersion>4.0.0</modelVersion>
       <groupId>com.experian.aperture.datastudio.sdk.step.addons</groupId>
       <!-- replace this accordingly with your custom step name -->
       <artifactId>MyCustomStep</artifactId>
       <!-- replace this accordingly with your custom step version -->
       <version>1.0-SNAPSHOT</version>
       <packaging>jar</packaging>
       <!-- replace this accordingly with your custom step name -->
       <name>MyCustomStep</name>

       <repositories>
           <repository>
               <id>aperture-data-studio-github-repo-snapshot</id>
               <url>https://raw.githubusercontent.com/experiandataquality/aperture-data-studio-sdk/github-maven-snapshot-repository/maven/</url>
           </repository>
       </repositories>

       <dependencies>
           <dependency>
               <groupId>com.experian.datastudio</groupId>
               <artifactId>sdkapi</artifactId>
               <version>2.0.0-SNAPSHOT</version>
               <scope>provided</scope>
           </dependency>
       </dependencies>
   </project>
   ```
3. (Skip this step if using Maven or Gradle). If you've downloaded the JAR manually, create a *libs* folder and add in the *sdkapi.jar* as a library.
4. Create a new package and class.
5. Configure your project to output a .jar file as an artifact. Note that this will be done differently depending on your IDE.

## Comparison of SDK v1.0 and v2.0

Here are the main differences between the v1.0 and v2.0 of the SDK:

| Features                      |            SDK v1.0           |                                       SDK v2.0                                    |
|-------------------------------|-------------------------------|-----------------------------------------------------------------------------------|
| Design                        | Extending Abstract class      | Implementing interface                                                            |
| Register step details         | Using `setStepDefinition` methods in `StepConfiguration` class | Using  `CustomTypeMetadataBuilder`  [Sample code](#metadata-sample-code)            |
| Configure step property  | Using `setStepProperties()` in `StepConfiguration` class | Using `StepConfigurationBuilder` [Sample code](#stepconfigurationbuilder-sample-code) |
| Configure *isComplete* handling | Override `isComplete()` in `StepConfiguration` class | Using `StepConfigurationBuilder` [Sample code](#stepconfigurationbuilder-sample-code) |
| Configure column step         | Override `initialise()` in `StepOutput` class                                      | Using `StepConfigurationBuilder` [Sample code](#stepprocessorbuilder-sample-code)         |  
| Execute and retrieve value from step    | Override `execute()` and `getValueAt()` in `StepOutput` class        | Using `StepProcessorBuilder` [Sample code](#stepprocessorbuilder-sample-code)         |
| Logging in step               | Using `logError()` from base class                                                     | Using `StepLogManager` library [Sample Code](#logging-library)                          |

 
## Creating a custom step

Once your project is set up, you can create a new class and implement the `CustomStepDefinition` interface. The newly created class will be picked up by the Data Studio UI.

Note that you can bundle multiple custom steps into a single JAR, as long as they're implementing the `CustomStepDefinition`.

### Importing the step SDK

To use the interfaces, classes and methods, you have to import the SDK into your class. Add an import statement below the package name to import all the SDK classes and methods:
``` java
import com.experian.datastudio.sdk.api.configuration.*;
import com.experian.datastudio.sdk.api.processor.*;
import com.experian.datastudio.sdk.api.step.*;
```
Your new class should look something like this:

``` java
package com.experian.datastudio.customstep;

import com.experian.datastudio.sdk.api.configuration.*;
import com.experian.datastudio.sdk.api.processor.*;
import com.experian.datastudio.sdk.api.step.*;

public class DemoStep implements CustomStepDefinition{
}
```
All the SDK interfaces, classes and methods will now available.

### Creating your metadata

#### Adding metadata

Use `CustomTypeMetadataBuilder` in `createMetadata` method to create metadata such as the custom step name, description, version and licenses. 

#### Metadata sample code
``` java
@Override
public CustomTypeMetadata createMetadata(final CustomTypeMetadataBuilder metadataBuilder) {
        return metadataBuilder
                .withName("Example: StepsTemplate")
                .withDescription("Step Template Example")
                .withMajorVersion(0)
                .withMinorVersion(0)
                .withPatchVersion(0)
                .withDeveloper("Experian")
                .withLicense("Apache License Version 2.0")
                .build();
}
```

### Configuring your step

Use `StepConfigurationBuilder` in `createConfiguration` method to configure your custom step (e.g. nodes, step properties, column layouts) and ensure it displays correctly in the Data Studio UI.
#### Adding nodes

Nodes represent the input and output nodes in the step. You can define how many nodes the step will have. For example, to create a step with 1 input and 1 output node:

``` java
.withNodes(stepNodeBuilder -> stepNodeBuilder
                        .addInputNode(INPUT_ID)
                        .addOutputNode(OUTPUT_ID)
                        .build())
```	

##### Process Node
``` java
.withNodes(stepNodeBuilder -> stepNodeBuilder
		.addInputNode(inputNodeBuilder -> inputNodeBuilder
				.withId(INPUT_ID)
				.withType(NodeType.PROCESS)
				.build())
		.addOutputNode(outputNodeBuilder -> outputNodeBuilder
				.withId(OUTPUT_ID)
				.withType(NodeType.PROCESS)
				.build())
		.build())

```

#### Adding step properties

Step properties represent the UI elements of the step. These properties include displaying information about the step, allowing the user to input something or selecting a column to manipulate. 
For example, to add a column chooser to the step:

``` java
.withStepProperties(stepPropertiesBuilder -> stepPropertiesBuilder
                        .addStepProperty(stepPropertyBuilder ->
                                stepPropertyBuilder
                                        .asColumnChooser(ARG_ID_COLUMN_CHOOSER)
                                        .forInputNode(INPUT_ID)
                                        .build())
                        .build()))
```

|StepPropertyType|Description                |
|----------------|---------------------------|
|asBoolean       |A `true` or `false` field  |
|asString        |A text field               |
|asNumber        |A number without fraction    |
|asColumnChooser |An input column drop-down list|
|asCustomChooser |A custom drop-down list      |
|asInputLabel    |A label for the input node    |

##### asBoolean

| Method           | Description                       |
|------------------|-----------------------------------|
| asBoolean        |Set a Boolean field              |
| withDefaultValue |Set a default value in the field |
| build            |Build the step property        |

##### asString

| Method           | Description                            |
|------------------|----------------------------------------|
| asString         | Set a string field                    |
| withIsRequired   | Set whether the field is mandatory  |
| withDefaultValue | Set a default value in the field      |
| build            | Build the step property             |

##### asNumber

| Method           | Description                             |
|------------------|-----------------------------------------|
| asNumber         | Set a number field                     |
| withAllowDecimal | Set whether the field accepts decimal values |
| withMaxValue     | Set a maximum value in the field       |
| withMinValue     | Set a minimum value in the field       |
| withIsRequired   | Set whether the field is mandatory   |
| withDefaultValue | Set a default value in the field       |
| build            | Build the step property              |

##### asColumnChooser

| Method             | Description                                           |
|--------------------|-------------------------------------------------------|
| asColumnChooser    | Set an input column from a drop-down list               |
| forInputNode       | Set an input node                    |
| withMultipleSelect | Set whether multiple fields are allowed |
| build              | Build the step property                            |

##### asCustomChooser

| Method                  | Description                                           |
|-------------------------|-------------------------------------------------------|
| asCustomChooser         | Set an input column from a custom drop-down list        |
| withAllowValuesProvider | Set the custom list for selection                  |
| withAllowSearch         | Set whether there's a field search             |
| withAllowSelectAll()    | Set whether you can select all fields        |
| withIsRequired()        | Set whether the field is mandatory                 |
| withMultipleSelect()    | Set whether multiple fields can be selected |
| build                   | Build the step property                            |

##### asInputLabel

| Method       | Description                        |
|--------------|------------------------------------|
| asInputLabel | Set a label for the input node     |
| forInputNode | Set an input node |
| build        | Build the step property         |


#### Configure isCompleteHandler

The *CompleteHandler* determines the completeness based on the condition of the step prior to execution or data exploration. For example, you can set the step to always be in a complete state:

``` java
.withIsCompleteHandler(context -> true)
```

#### Configure column layouts

Column layouts represent column(s) that will be displayed in the step. For example, to configure existing columns from an input source and a new "MyColumn" for the output columns:  
  
``` java
.withOutputLayouts(outputLayoutBuilder -> outputLayoutBuilder
                        .forOutputNode(OUTPUT_ID, outputColumnBuilder -> outputColumnBuilder
                            .addColumns(context -> {
                                final Boolean hasLimit = context.getStepPropertyValue(ARG_ID_HAS_LIMIT);
                                final List<Column> columns = context.getInputContext(INPUT_ID).getColumns();
                                final Number limit = context.getStepPropertyValue(ARG_ID_COLUMN_LIMIT);
                                if (Boolean.TRUE.equals(hasLimit) && limit != null) {
                                    return columns.stream().limit(limit.intValue()).collect(Collectors.toList());
                                }
                                return columns;
                            })
                            .addColumn(MY_OUTPUT_COLUMN)
                            .build())
                        .build())

```
						
#### StepConfigurationBuilder sample code

``` java
@Override
    public StepConfiguration createConfiguration(final StepConfigurationBuilder configurationBuilder) {
        return configurationBuilder
                /** Define input and output node */
                .withNodes(stepNodeBuilder -> stepNodeBuilder
                        .addInputNode(INPUT_ID)
                        .addOutputNode(OUTPUT_ID)
                        .build())
                /** Define step properties */
                .withStepProperties(stepPropertiesBuilder -> stepPropertiesBuilder
                        .addStepProperty(stepPropertyBuilder ->
                                stepPropertyBuilder
                                        .asInputLabel("Input coin")
                                        .forInputNode(INPUT_ID)
                                        .build())
                        .addStepProperty(stepPropertyBuilder ->
                                stepPropertyBuilder
                                        .asBoolean(ARG_ID_HAS_LIMIT)
                                        .withLabelSupplier(context -> "columns?")
                                        .build())
                        .addStepProperty(stepPropertyBuilder ->
                                stepPropertyBuilder
                                        .asNumber(ARG_ID_COLUMN_LIMIT)
                                        .withAllowDecimal(false)
                                        .withIsDisabledSupplier(context -> {
                                            final Boolean hasLimit = context.getStepPropertyValue(ARG_ID_HAS_LIMIT);
                                            return !hasLimit;
                                        })
                                        .withIsRequired(true)
                                        .withLabelSupplier(context -> "limit?")
                                        .build())
                        .build())
                /** Prevent the step from executing until the input node has been completed.
                 *  This is an optional value, in below case which is always true
                 */
                .withIsCompleteHandler(context -> true)
                /** Define how the output will look like, i.e. the columns and rows*/
                .withOutputLayouts(outputLayoutBuilder -> outputLayoutBuilder
                        .forOutputNode(OUTPUT_ID, outputColumnBuilder -> outputColumnBuilder
                            .addColumns(context -> {
                                final Boolean hasLimit = context.getStepPropertyValue(ARG_ID_HAS_LIMIT);
                                final List<Column> columns = context.getInputContext(INPUT_ID).getColumns();
                                final Number limit = context.getStepPropertyValue(ARG_ID_COLUMN_LIMIT);
                                if (Boolean.TRUE.equals(hasLimit) && limit != null) {
                                    return columns.stream().limit(limit.intValue()).collect(Collectors.toList());
                                }
                                return columns;
                            })
                            .addColumn(MY_OUTPUT_COLUMN)
                            .build())
                        .build())
                .build();
    }
```

### Processing your step

Use `StepProcessorBuilder` in the `createProcessor` method to implement the logic of the output step. 

#### Execute step

This method is used to apply logic to the input source and the computed value will become the output to a specified output column. The example below shows the logic of appending "-processed" to the value from `MY_INPUT_COLUMN` and displayed into `MY_OUTPUT_COLUMN`

#### StepProcessorBuilder sample code

```java
@Override
    public StepProcessor createProcessor(final StepProcessorBuilder processorBuilder) {
        return processorBuilder
                // execute
                .forOutputNode(OUTPUT_ID, (processorContext, outputColumnManager) -> {
                    final ProcessorInputContext inputManager = processorContext.getInputContext(INPUT_ID).orElseThrow(IllegalArgumentException::new);
                    final Optional<InputColumn> column = inputManager.getColumns().stream().findFirst();
                    column.ifPresent(inputColumn -> outputColumnManager.onValue(MY_OUTPUT_COLUMN, rowIndex -> {
                        final CellValue cellValue = inputColumn.getValueAt(rowIndex);
                        return cellValue.toString() + "-processed";
                    }));
                    return inputManager.getRowCount();
                })
                .build();
    }
```
#### isInteractive() flag
Interactive is a flag that set to `true` when the user explores the output of a step on the Data Studio Grid. 
It is set to `false` when running the whole workflow.
``` java
@Override
public StepProcessor createProcessor(final StepProcessorBuilder processorBuilder) {
	return processorBuilder
			.forOutputNode(OUTPUT_ID, (processorContext, outputColumnManager) -> {
				if (processorContext.isInteractive()) {
                    ...
```

## The logging library
``` java
private static final Logger LOGGER = StepLogManager.getLogger(StepsTemplate.class, Level.INFO);
```

## The cache configuration
### Create cache
```java
    final StepCacheManager cacheManager = context.getCacheManager();
    final StepCacheConfiguration<String, String> cacheConfiguration1 = context.getCacheConfigurationBuilder()
            .withCacheName(CACHE_NAME_1)
            .withTtlForUpdate(10L, TimeUnit.MINUTES)
            .withScope(StepCacheScope.STEP)
            .build(String.class, String.class);
    final StepCache<String, String> cache1 = cacheManager.getOrCreateCache(cacheConfiguration1);
``` 

### Assigning value to cache
```java
    cache1.put(cacheKey, value);
```

### Getting value from cache
```java
    cache1.get(cacheKey);
```

## The http client library
### Create http client object
```java
    WebHttpClient
        .builder()
        .withHttpVersion(..) // Http protocol version
        .withProxy(..) // specifying any required proxy
        .withConnectionTimeout(..) // maximum time to establish connection
        .withSocketTimeout(..) // maximum time to retrieve data
        .build()    
```

### Create http GET request object
```java
    WebHttpRequest
        .builder()
        .get(..) // passing in the url
        .withQueryString(..) // specifying query string in key value pair, alternatively can use .addQueryString(..)
        .withHeader(..) // specifying headers value, alternatively can use .addHeader(..)
        .build()
```

### Create http POST request object
```java
    WebHttpRequest
        .builder()
        .post(..) // passing in the url
        .withBody(..) // specifying the body
        .withQueryString(..) // specifying query string in key value pair, alternatively can use .addQueryString(..)
        .withHeader(..) // specifying headers value, alternatively can use .addHeader(..)
        .build()
```

### Create http PUT request object
```java
    WebHttpRequest
        .builder()
        .put(..) // passing in the url
        .withBody(..) // specifying the body
        .withQueryString(..) // specifying query string in key value pair, alternatively can use .addQueryString(..)
        .withHeader(..) // specifying headers value, alternatively can use .addHeader(..)
        .build()
```

### Create http DELETE request object
```java
    WebHttpRequest
        .builder()
        .delete(..) // passing in the url
        .withBody(..) // specifying the body
        .withQueryString(..) // specifying query string in key value pair, alternatively can use .addQueryString(..)
        .withHeader(..) // specifying headers value, alternatively can use .addHeader(..)
        .build()
```

### Send http request through WebHttpClient
```java
    WebHttpClient client = WebHttpClient
                                   .builder()
                                   .withHttpVersion(..) // Http protocol version
                                   .withProxy(..) // specifying any required proxy
                                   .withConnectionTimeout(..) // maximum time to establish connection
                                   .withSocketTimeout(..) // maximum time to retrieve data
                                   .build();
   
    client.sendAsync(request);
```