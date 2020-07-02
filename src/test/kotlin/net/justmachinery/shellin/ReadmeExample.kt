package net.justmachinery.shellin

import io.kotlintest.shouldBe
import net.justmachinery.shellin.exec.InvalidExitCodeException
import java.nio.file.Files

fun main(){
    readmeExample()
}

internal fun readmeExample(){
    //This code is used in the readme; kept here to make sure it compiles.
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

        //Commands will fail by default if give a nonzero exit code
        try {
            command("sleep --badflags").waitFor()
        } catch(t : InvalidExitCodeException){
            println("Uh oh, $t")
        }

        command("echo"){
            +"You can add arguments and other configuration for a command in a block like this"
            //This discards stderr, for example
            stderr = null
        }

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
}