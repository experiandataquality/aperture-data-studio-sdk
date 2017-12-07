# Aperture Data Studio SDK

The SDK provides a simple Java library to create your own custom steps and extend the capabilities of Aperture Data Studio.

This repo contains the SDK jar and a pre-configured Java project that uses Gradle to easily build your own custom step. Alternatively, you can add the SDK as a dependency to your own project by downloading the SDK jar from the `libs` folder.


## Building a custom step with the sample project

The steps below show how to generate a compatible jar file using Gradle:

1. Clone the repo.
2. Open the project in your favourite IDE.
3. Create a new class within the MyCustomSteps module. For the output jar to work correctly it will need to be in the com.experian.aperture.datastudio.sdk.step.addons package - the template class is located there. We recommend that you base your class on one of the examples or use the template class.
4. Open the Gradle window in your IDE and run the MyCustomSteps build task. This will build the jar for the steps you have created.
5. Your new jar will be built to build/libs/MyCustomSteps.jar

## Building a custom step from a new or existing project 

If you don't wish to use Gradle, you'll need to configure your own java project to generate a compatible jar artifact:

1. Create a new java project or open an existing one
2. Download the [sdk.jar](https://github.com/experiandataquality/aperture-data-studio-sdk/raw/master/libs/sdk.jar) file
3. Create a libs folder and add in the sdk.jar as a library
4. Create a new package - com.experian.aperture.datastudio.sdk.step.addons
5. Create a new class in the package you just created
4. Configure your project to output a jar file as an artifact, this will be done differently depending on your IDE.
 
## Adding a custom step to Aperture Data Studio

To make your custom step available in the Aperture Data Studio UI:

1. Copy your new jar into the addons folder in your Aperture Data Studio installation directory.
2. Restart the Aperture Data Studio service.
3. Test your new step by dragging it into the workflow like any other step.

## Examples

The project comes with an ExampleSteps module which, when built, will output the SDK examples jar. The example classes demonstrate some key functionality of the SDK along with providing a template class which can be used as a starting point for your own custom steps.

## Documentation

View the SDK [user guide](http://edq.com/documentation/applications/aperture-data-studio/sdk-guide) for more information about the SDK works.
You can find the SDK Javadoc in the class comments as normal. Alternatively open the index.html file from the sdk-javadoc.jar file in the libs folder.

