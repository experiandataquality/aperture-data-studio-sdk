# Aperture Custom Steps SDK

The custom step SDK provides a simple Java library to create your own custom steps and extend the capabilities of Aperture.

You can make use of the Gradle project here to quickly build your own custom step jar. Alternatively, you can add the SDK as a dependency to your own project by downloading the SDK jar from the `libs` folder.


## Using

1. Clone the repo.
2. Open the project in your favourite IDE.
3. Create a new class within the MyCustomSteps module. We recommend that you base it on one of the examples or use the template class.
4. Run the relevant Gradle build task, the MyCustomSteps task will build the jar for the steps you have created.
5. Copy jar from the build output into addons folder in the Aperture installation.
6. Restart the Aperture server.
7. Test step by dragging into the workflow like any other step.

## Examples

The project comes with an ExampleSteps module which, when built, will output the SDK examples jar. The example classes demonstrate some key functionality of the SDK along with providing a template class which can be used as a starting point for your own custom steps.

## Documentation

View the SDK [user guide](http://edq.com/documentation/applications/aperture/sdk-guide) for information on how the SDK works and how to write the code for your own custom step.
You can find the SDK Javadoc in the class comments as normal. Alternatively open the index.html file from the sdk-javadoc.jar file in the libs folder.

