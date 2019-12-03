# Aperture Data Studio SDK

The SDK provides a simple Java library to create and test your own custom Workflow steps, extending Aperture Data Studio capabilities. You can also add your own custom parsers which will enable Data Studio to load data from files. 

This repo contains the SDK JAR and a pre-configured Java project that uses Gradle, allowing you to easily build your own custom step. Alternatively, you can add the SDK as a dependency to your own project by downloading the SDK JAR from the `sdkapi` folder.

## Table of contents

- [Generating a custom step from a new or existing project](#generating-a-custom-step-from-a-new-or-existing-project)
- [Comparison of SDK v1.0 and v2.0](#comparison-of-sdk-v1.0-and-v2.0)
- [Creating a custom step](#creating-a-custom-step)
    - [Importing the step SDK](#importing-the-step-sdk)
    - [Creating your metadata](#creating-your-metadata)
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
- [Generating a custom parser from a new or existing project](#generating-a-custom-parser-from-a-new-or-existing-project)
- [Creating a custom parser](#creating-a-custom-parser)
  - [Importing the parser SDK](#importing-the-parser-sdk)
  - [Creating your metadata](#creating-your-metadata-1)
  - [Configuring your parser](#configuring-your-parser)
    - [Supported file extension](#supported-file-extension)
    - [Parameter definition](#parameter-definition)
    - [Display type](#display-type)
    - [Set default value](#set-default-value)
    - [Set processor](#set-processor)
  - [Parser Processor](#parser-processor)
    - [Get table definition](#get-table-definition)
      - [TableDefinitionContext](#tabledefinitioncontext)
    - [Get row iterator](#get-row-iterator)
      - [TableDefinitionContext](#tabledefinitioncontext-1)
      - [ClosableIteratorBuilder](#closableiteratorbuilder)
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

| Features                          |            SDK v1.0           |                                       SDK v2.0                                    |
|-----------------------------------|-------------------------------|-----------------------------------------------------------------------------------|
| Design                            | Extending Abstract class                                                  | Implementing interface                                                                |
| Register step details             | Using `setStepDefinition` methods in `StepConfiguration` class            | Using  `CustomTypeMetadataBuilder`  [Sample code](#metadata-sample-code)              |
| Configure step property           | Using `setStepProperties()` in `StepConfiguration` class                  | Using `StepConfigurationBuilder` [Sample code](#stepconfigurationbuilder-sample-code) |
| Configure *isComplete* handling   | Override `isComplete()` in `StepConfiguration` class                      | Using `StepConfigurationBuilder` [Sample code](#stepconfigurationbuilder-sample-code) |
| Configure column step             | Override `initialise()` in `StepOutput` class                             | Using `StepConfigurationBuilder` [Sample code](#stepprocessorbuilder-sample-code)     |  
| Execute and retrieve value from step    | Override `execute()` and `getValueAt()` in `StepOutput` class       | Using `StepProcessorBuilder` [Sample code](#stepprocessorbuilder-sample-code)         |
| Logging in step                   | Using `logError()` from base class                                        | Using `SdkLogManager` library [Sample Code](#the-logging-library)                     |

 
## Creating a custom step

Once your project is set up, you can create a new class and implement the `CustomStepDefinition` interface. The newly created class will be picked up by the Data Studio UI.

Note that it is recommended that you bundle *one* custom step per JAR.

### Importing the step SDK

To use the interfaces, classes and methods, you have to import the SDK into your class. Add an import statement below the package name to import all the SDK classes and methods:
``` java
import com.experian.datastudio.sdk.api.*;
import com.experian.datastudio.sdk.api.step.*;
import com.experian.datastudio.sdk.api.step.configuration.*;
import com.experian.datastudio.sdk.api.step.processor.*;
```

Your new class should look something like this:

``` java
package com.experian.datastudio.customstep;

import com.experian.datastudio.sdk.api.*;
import com.experian.datastudio.sdk.api.step.*;
import com.experian.datastudio.sdk.api.step.configuration.*;
import com.experian.datastudio.sdk.api.step.processor.*;

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

By default, the input and output nodes are DATA node, which receive data or produce data.
You can create a custom step that doesn't change the data in the workflow.
For example, a custom step that sends email or calls REST API when the execution reaches that step.
Please take note that PROCESS output node cannot connect to DATA input node.
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
        .addStepProperty(stepPropertyBuilder -> stepPropertyBuilder
                .asColumnChooser(ARG_ID_COLUMN_CHOOSER)
                .forInputNode(INPUT_ID)
                .build())
        .build()))
```

|StepPropertyType|Description                   |
|----------------|------------------------------|
|asBoolean       |A `true` or `false` field     |
|asString        |A text field                  |
|asNumber        |A number without fraction     |
|asColumnChooser |An input column drop-down list|
|asCustomChooser |A custom drop-down list       |

##### asBoolean

| Method           | Description                    |
|------------------|--------------------------------|
| asBoolean        |Set a Boolean field             |
| withDefaultValue |Set a default value in the field|
| build            |Build the step property         |

##### asString

| Method           | Description                        |
|------------------|------------------------------------|
| asString         | Set a string field                 |
| withIsRequired   | Set whether the field is mandatory |
| withDefaultValue | Set a default value in the field   |
| build            | Build the step property            |

##### asNumber

| Method           | Description                                    |
|------------------|------------------------------------------------|
| asNumber         | Set a number field                             |
| withAllowDecimal | Set whether the field accepts decimal values   |
| withMaxValue     | Set a maximum value in the field               |
| withMinValue     | Set a minimum value in the field               |
| withIsRequired   | Set whether the field is mandatory             |
| withDefaultValue | Set a default value in the field               |
| build            | Build the step property                        |

##### asColumnChooser

| Method           | Description                                    |
|------------------|------------------------------------------------|
| asColumnChooser    | Set an input column from a drop-down list    |
| forInputNode       | Set an input node                            |
| withMultipleSelect | Set whether multiple fields are allowed      |
| build              | Build the step property                      |

##### asCustomChooser

| Method                  | Description                                         |
|-------------------------|-----------------------------------------------------|
| asCustomChooser         | Set an input column from a custom drop-down list    |
| withAllowValuesProvider | Set the custom list for selection                   |
| withAllowSearch         | Set whether there's a field search                  |
| withAllowSelectAll()    | Set whether you can select all fields               |
| withIsRequired()        | Set whether the field is mandatory                  |
| withMultipleSelect()    | Set whether multiple fields can be selected         |
| build                   | Build the step property                             |


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
                    final Optional<Boolean> hasLimitOptional = context.getStepPropertyValue(ARG_ID_HAS_LIMIT);
                    final Boolean hasLimit = hasLimitOptional.orElse(Boolean.FALSE);
                    final List<Column> columns = context.getInputContext(INPUT_ID).getColumns();
                    if (Boolean.TRUE.equals(hasLimit)) {
                        final Optional<Number> limitOptional = context.getStepPropertyValue(ARG_ID_COLUMN_LIMIT);
                        if (limitOptional.isPresent()) {
                            final Number limit = limitOptional.get();
                            return columns.stream().limit(limit.intValue()).collect(Collectors.toList());
                        }
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
                    .addStepProperty(stepPropertyBuilder -> stepPropertyBuilder
                            .asBoolean(ARG_ID_HAS_LIMIT)
                            .withLabelSupplier(context -> "columns?")
                            .build())
                    .addStepProperty(stepPropertyBuilder -> stepPropertyBuilder
                            .asNumber(ARG_ID_COLUMN_LIMIT)
                            .withAllowDecimal(false)
                            .withIsDisabledSupplier(context -> {
                                final Optional<Boolean> hasLimitOptional = context.getStepPropertyValue(ARG_ID_HAS_LIMIT);
                                final Boolean hasLimit = hasLimitOptional.orElse(Boolean.FALSE);
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
            /** Define how the output will look like, i.e. the columns and rows */
            .withOutputLayouts(outputLayoutBuilder -> outputLayoutBuilder
                    .forOutputNode(OUTPUT_ID, outputColumnBuilder -> outputColumnBuilder
                            .addColumns(context -> {
                                final Optional<Boolean> hasLimitOptional = context.getStepPropertyValue(ARG_ID_HAS_LIMIT);
                                final Boolean hasLimit = hasLimitOptional.orElse(Boolean.FALSE);
                                final List<Column> columns = context.getInputContext(INPUT_ID).getColumns();
                                if (Boolean.TRUE.equals(hasLimit)) {
                                    final Optional<Number> limitOptional = context.getStepPropertyValue(ARG_ID_COLUMN_LIMIT);
                                    if (limitOptional.isPresent()) {
                                        final Number limit = limitOptional.get();
                                        return columns.stream().limit(limit.intValue()).collect(Collectors.toList());
                                    }
                                }
                                return columns;
                            })
                            .addColumn(MY_OUTPUT_COLUMN)
                            .build())
                    .build())
            .withIcon(StepIcon.ARROW_FORWARD)
            .build();
}
```

### Processing your step

Use `StepProcessorBuilder` in the `createProcessor` method to implement the logic of the output step. 

#### Execute step

This method is used to apply logic to the input source and the computed value will become the output to a specified output column. The example below shows the logic of appending "-processed" to the value from `MY_INPUT_COLUMN` and displayed into `MY_OUTPUT_COLUMN`

#### StepProcessorBuilder sample code

``` java
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
private static final Logger LOGGER = SdkLogManager.getLogger(StepsTemplate.class, Level.INFO);
```

## The cache configuration
### Create cache
``` java
final StepCacheManager cacheManager = context.getCacheManager();
final StepCacheConfiguration<String, String> cacheConfiguration1 = context.getCacheConfigurationBuilder()
        .withCacheName(CACHE_NAME_1)
        .withTtlForUpdate(10L, TimeUnit.MINUTES)
        .withScope(StepCacheScope.STEP)
        .build(String.class, String.class);
final StepCache<String, String> cache1 = cacheManager.getOrCreateCache(cacheConfiguration1);
``` 

### Assigning value to cache
``` java
cache1.put(cacheKey, value);
```

### Getting value from cache
``` java
cache1.get(cacheKey);
```

## The http client library
### Create http client object
``` java
WebHttpClient.builder()
        .withHttpVersion(..) // Http protocol version
        .withProxy(..) // specifying any required proxy
        .withConnectionTimeout(..) // maximum time to establish connection
        .withSocketTimeout(..) // maximum time to retrieve data
        .build()    
```

### Create http GET request object
``` java
WebHttpRequest.builder()
        .get(..) // passing in the url
        .withQueryString(..) // specifying query string in key value pair, alternatively can use .addQueryString(..)
        .withHeader(..) // specifying headers value, alternatively can use .addHeader(..)
        .build()
```

### Create http POST request object
``` java
WebHttpRequest.builder()
        .post(..) // passing in the url
        .withBody(..) // specifying the body
        .withQueryString(..) // specifying query string in key value pair, alternatively can use .addQueryString(..)
        .withHeader(..) // specifying headers value, alternatively can use .addHeader(..)
        .build()
```

### Create http PUT request object
``` java
WebHttpRequest.builder()
        .put(..) // passing in the url
        .withBody(..) // specifying the body
        .withQueryString(..) // specifying query string in key value pair, alternatively can use .addQueryString(..)
        .withHeader(..) // specifying headers value, alternatively can use .addHeader(..)
        .build()
```

### Create http DELETE request object
``` java
WebHttpRequest.builder()
        .delete(..) // passing in the url
        .withBody(..) // specifying the body
        .withQueryString(..) // specifying query string in key value pair, alternatively can use .addQueryString(..)
        .withHeader(..) // specifying headers value, alternatively can use .addHeader(..)
        .build()
```

### Send http request through WebHttpClient
``` java
WebHttpClient client = WebHttpClient.builder()
        .withHttpVersion(..) // Http protocol version
        .withProxy(..) // specifying any required proxy
        .withConnectionTimeout(..) // maximum time to establish connection
        .withSocketTimeout(..) // maximum time to retrieve data
        .build();

client.sendAsync(request);
```

## Generating a custom parser from a new or existing project

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
       <groupId>com.experian.aperture.datastudio.sdk.custom.addons</groupId>
       <!-- replace this accordingly with your custom parser name -->
       <artifactId>MyCustomParser</artifactId>
       <!-- replace this accordingly with your custom step version -->
       <version>1.0-SNAPSHOT</version>
       <packaging>jar</packaging>
       <!-- replace this accordingly with your custom step name -->
       <name>MyCustomParser</name>

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

## Creating a custom parser

Once your project is set up, you can create a new class and implement the `CustomParserDefinition` interface. The newly created class will be picked up by the Data Studio UI.

Note that it is recommended that you bundle *one* custom parser per JAR.

### Importing the parser SDK

To use the interfaces, classes and methods, you have to import the SDK into your class. Add an import statement below the package name to import all the SDK classes and methods:
``` java
import com.experian.datastudio.sdk.api.*;
import com.experian.datastudio.sdk.api.parser.*;
import com.experian.datastudio.sdk.api.parser.configuration.*;
import com.experian.datastudio.sdk.api.parser.processor.*;
```
Your new class should look something like this:

``` java
package com.experian.datastudio.customparser;

import com.experian.datastudio.sdk.api.*;
import com.experian.datastudio.sdk.api.parser.*;
import com.experian.datastudio.sdk.api.parser.configuration.*;
import com.experian.datastudio.sdk.api.parser.processor.*;

public class DemoParser implements CustomParserDefinition{
}
```
All the SDK interfaces, classes and methods will now available.

### Creating your metadata
[Same as creating metadata for custom step](#creating-your-metadata)
 
### Configuring your parser

Use `ParserConfigurationBuilder` in `createConfiguration` method to configure your custom parser.

#### Supported file extension
Define the file extension that the custom parser is able to parse.

``` java
.withSupportedFileExtensions(supportedFileExtensionsBuilder ->
                        supportedFileExtensionsBuilder
                                .add(supportedFileExtensionBuilder ->
                                        supportedFileExtensionBuilder
                                                .withSupportedFileExtension(".testFile")
                                                .withFileExtensionName("Test extension")
                                                .withFileExtensionDescription("Test extension")
                                                .build())
                                .build())
```

#### Parameter definition
Each parser can be configured in the UI. It will determine the behaviour of the custom parser.
You can have multiple parameter defined using the `parameterDefinitionsBuilder` in the `withParserParameterDefinition`.

Example:
For delimited parser, user can choose either comma, tab, etc as a delimiter. 

``` java
.withParserParameterDefinition(parameterDefinitionsBuilder ->
                        parameterDefinitionsBuilder
                                .add(parameterDefinitionBuilder ->
                                        parameterDefinitionBuilder
                                                .withId("delimiter")
                                                .withName("Delimiter")
                                                .withDescription("Character that separates columns in plain text")
                                                .setAsRequired(true)
                                                .setTypeAs(ParserParameterValueType.STRING)
                                                .setDisplayTypeAs(ParserParameterDisplayType.TEXTFIELD)
                                                .withDefaultValueAsString(",")
                                                .affectsTableStructure(true)
                                                .build())
                                .build())
```


| Method                    | Description                                                                                         |
| ------------------------- | --------------------------------------------------------------------------------------------------- |
| withId                    | Set Id for the parameter. Id can be used to retrieve the parameter value in the parser processor    |
| withName                  | Set parameter name                                                                                  |
| withDescription           | Set parameter description                                                                           |
| setTypeAs                 | Set data type of the parameter value. The available type are boolean, string, integer and character |
| setDisplayTypeAs          | Set display format of the parameter                                                                 |
| withDefaultValueAsString  | Set a default value in the field                                                                    |
| withDefaultValueAsBoolean | Set a default value in the field                                                                    |
| withDefaultValueAsString  | Set a default value in the field                                                                    |
| affectsTableStructure     | Set this flag to true if the parameter value influence the table structure of the parser output     |


#### Display type

| ParserParameterDisplayType | Description                                                                                                                                |
| -------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| TEXTFIELD                  | Display a textfield                                                                                                                        |
| CHECKBOX                   | Display a checkbox                                                                                                                         |
| CHARSET                    | Display a dropdown where user able to select one of the character set. Example: UTF-8, UTF-16, US-ASCII                                    |
| DELIMITER                  | Display a dropdown where user able to select one of the delimiter. Example: Comma (,),  Tab(\t), Pipe (                                    | ) |
| LOCALE                     | Display a dropdown where user able to select one of the locale. Example: English (United States), English (United Kingdom, French (France) |
| ENDOFLINE                  | Display a dropdown where user able to select one of the end of line flag. Example: eg: \r\n, \n, \r                                        |
| QUOTE                      | Display a dropdown where user able to select one of the quote. Example: Double ("), Single ('), Grave (`)                                  |

#### Set default value 
You can set default value based on the content of the file. Use `ParameterContext` to in set default value method, to retrieve the file input stream.


### Set processor

Set your parser processor class.

``` java
.withProcessor(new SampleParserProcessor())
```

## Parser Processor

Parser processor contains the processor to take the file source and convert it to `ParserTableDefintion` and `ClosableIterator`
To build a parser processor, create a new class that implements `ParserProcessor`. There are 2 methods in the interface, `getTableDefinition` and `ClosableIterator`

### Get table definition

In Aperture Data Studio, data in the files are present in form of table with rows and columns. `ParserTableDefinition` is the definition of the parsed table. You can have single or multiple table in a file source. For each table, you can have single or multiple columns. Use the `ParserTableDefinitionFactory` and `ParserColumnDefinitionFactory` in the `TableDefinitionContext` to create `ParserTableDefinition`. 

#### TableDefinitionContext

| Method                           | Description                                     |
| -------------------------------- | ----------------------------------------------- |
| getStreamSupplier                | Contains the stream of the file source          |
| getFilename                      | Name of the file source                         |
| getParameterConfiguration        | Get the parameter value selected by the user    |
| getParserTableDefinitionFactory  | Factory that generated `ParserTableDefinition`  |
| getParserColumnDefinitionFactory | Factory that generated `ParserColumnDefinition` |


## Get row iterator

Return a closable iterator over a collection of table row. Use the `ClosableIteratorBuilder` in the `RowIteratorContext` to build the `ClosableIterator`

#### TableDefinitionContext

| Method                     | Description                                  |
| -------------------------- | -------------------------------------------- |
| getTableId                 | Returns the id of `ParserTableDefinition`    |
| getStreamSupplier          | Contains the stream of the file source       |
| getParameterConfiguration  | Get the parameter value selected by the user |
| getTableDefinition         | Returns the `ParserTableDefinition`          |
| getClosableIteratorBuilder | Returns the builder for a closable iterator. |


#### ClosableIteratorBuilder
| Method      | Description                                                                    |
| ----------- | ------------------------------------------------------------------------------ |
| withHasNext | Returns true if iterations has more rows                                       |
| withNext    | Returns the next row in the iteration                                          |
| withClose   | Closes any streams and releases system resources associated with the iterator. |

