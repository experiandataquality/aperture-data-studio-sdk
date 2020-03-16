# Aperture Data Studio SDK Repository

## Gradle

  If using Gradle, point to the SDK repository in the `build.gradle`:

   ```gradle
   apply plugin: 'java'

   repositories {
       mavenCentral()
       maven {
           url 'https://raw.githubusercontent.com/experiandataquality/aperture-data-studio-sdk/github-maven-repository/maven'
       }
   }

   dependencies {
       compileOnly("com.experian.datastudio:sdkapi:2.0.0")
       compileOnly("com.experian.datastudio:sdklib:2.0.0")
   }
   ```
## Maven
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
       <version>1.0</version>
       <packaging>jar</packaging>
       <!-- replace this accordingly with your custom step name -->
       <name>MyCustomStep</name>
    
       <properties>
            <maven.compiler.source>1.8</maven.compiler.source>
            <maven.compiler.target>1.8</maven.compiler.target>
       </properties>

       <repositories>
           <repository>
               <id>aperture-data-studio-github-repo</id>
               <url>https://raw.githubusercontent.com/experiandataquality/aperture-data-studio-sdk/github-maven-repository/maven/</url>
           </repository>
       </repositories>

       <dependencies>
           <dependency>
               <groupId>com.experian.datastudio</groupId>
               <artifactId>sdkapi</artifactId>
               <version>2.0.0</version>
               <scope>provided</scope>
           </dependency>
           <dependency>
               <groupId>com.experian.datastudio</groupId>
               <artifactId>sdklib</artifactId>
               <version>2.0.0</version>
               <scope>provided</scope>
           </dependency>
       </dependencies>
   </project>
   ```
