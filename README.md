[ ![Download](https://api.bintray.com/packages/scottpjohnson/generic/shellin/images/download.svg) ](https://bintray.com/scottpjohnson/generic/shellin/_latestVersion)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
 
shellin is a convenience library for efficiently starting programs in Kotlin. It uses [NuProcess](https://github.com/brettwooldridge/NuProcess) to copy and read from program input and output using a minimal number
of threads. 

  
## Use
### Install
Add the following to your build.gradle where appropriate:
```groovy

dependencies {
    compile 'net.justmachinery.shellin:shellin:<VERSION>'
}
```
You will need to replace `<VERSION>` with the latest version of shellin.


### Basic usage

```kotlin
val shell = shellin {
        //You can configure some default behaviors for commands run here
        logCommands = true
    }

    //Synchronously run and wait for a command.
    shell.command("echo Hello world from Shellin!").waitFor()

    //If we want to do a bunch of commands or change the default shell state:
    shell.new {
        //From here we can change the working directory without affecting the default configuration
        workingDirectory = Files.createTempDirectory("shellin")

        //Commands will fail by default if the program returns a nonzero exit code
        try {
            command("sleep --badflags").waitFor()
        } catch(t : InvalidExitCodeException){
            println("Uh oh, $t")
        }

        val launched = command("echo"){
            +"You can add arguments and other configuration for a command in a block like this"
            //This discards stderr, for example
            stderr = null
        }
        //Notice that we haven't waited on the last command yet, so it could still be starting or running.
        launched.waitFor()

        //It's also easy to collect output:
        collectStdout {
            command("echo", "Hello there!")
        }.text shouldBe "Hello there!\n"

        //Or provide input:
        collectStdout {
            command("cat"){
                stdin("Hello there!\n".byteInputStream())
            }
        }.text shouldBe "Hello there!\n"

        //There's also a helper function for bash scripts:
        bash("echo foo >> test.txt; cat test.txt; rm test.txt")
    }
    shell.new {
        //Shellin provides utilities for transforming program output into logs
        logStdout { process ->
            val programName = "program.${process.arguments[0]}"
            { line : CharSequence -> println("$programName: INFO: $line") }
        }
        logStderr { process ->
            val programName = "program.${process.arguments[0]}"
            { line : CharSequence -> println("$programName: ERROR: $line") }
        }
        bash("echo First; echo Second; echo Third 1>&2;").waitFor()
    }
```
