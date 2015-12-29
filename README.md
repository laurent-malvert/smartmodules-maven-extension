# SmartModules Maven Extension

SmartModules is an extension for the Apache Maven build tool which enables automatic descendant module discovery in a multi-module build tree.

## Example ##

**Given** a Maven multi-module project with the following tree:

```
    parent-project/
      `-- pom.xml
      `-- module1/
      |    `-- pom.xml
      `-- module2/
           `-- pom.xml
```

**When** adding the `smartmodules-maven-extension` to your build and _without_ declaring a `<modules>` list in the `parent-project` POM and running a normal maven build command (e.g `mvn clean test`), Maven will now automatically detect the `module1` and `module2` modules and add them to the reactor.

## Usage ##

### Using the SmartModules Extension from an artifact Repository ###

Add the following to your project's POM:

```
  <build>
    <extensions>
      <extension>
        <groupId>com.googlecode.maven.extensions</groupId>
        <artifactId>smartmodules-maven-extension</artifactId>
        <version>0.1</version>
      </extension>
    </extensions>
  </build>
```

**Note:** `smartmodules-maven-extension` is [available from the Maven Central repository](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.googlecode.smartmodules-maven-extension%22%20AND%20a%3A%22smartmodules-maven-extension%22).

### Building the SmartModules Extension and installing to your Local Repository ###

Run the following from your command-line:

```
  hg clone https//code.google.com/p/smartmodules-maven-extension/
  cd smartmodules-maven-extension
  mvn clean install
```

## Big Fat Warning ##

**In brief: this may not work as expected.**

This is rather **experimental**. It works for what I needed it to do, and while I can say I haven't run into any issues, it has been widely tested. So...

**Beware:** it may screw up your build process, and it hooks itself savagely at the beginning of the Maven build as a `LifeCycleParticipant` and messes around with the project list and the module list for each project. While I haven't noticed any, there may be unwanted side-effects and other oddities I haven't figured out so far.

If you run into a problem, feel free to [report an issue](https://code.google.com/p/smartmodules-maven-extension/issues/entry).

## Legacy

This was originally hosted on:

https://code.google.com/p/smartmodules-maven-extension/
