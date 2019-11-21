# Aperture Data Studio SDK

## Snapshot Maven Repository

This branch contains maven **snapshot** repository for both `sdk` and `sdk-test-framework`.

## Configuration

[sdkapi pom template](maven-template/sdkapi-%7B%7Bversion%7D%7D.pom)

To upload a new version: 

1. change directory to [maven](maven) folder
1. Ensure that maven is installed then run (change the path accordingly): 
   `mvn org.apache.maven.plugins:maven-install-plugin:3.0.0-M1:install-file -Dfile="C:\path\to\sdkapi\sdkapi.jar" -Dpackaging=jar -DpomFile="C:\path\to\sdkapi\sdkapi.pom" -DcreateChecksum=true -DlocalRepositoryPath=.`
1. Push the changes into Github.
