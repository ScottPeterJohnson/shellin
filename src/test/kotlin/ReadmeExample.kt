import io.kotlintest.shouldBe
import net.justmachinery.shellin.Shellin
import net.justmachinery.shellin.bash
import net.justmachinery.shellin.collectStdout
import net.justmachinery.shellin.command
import net.justmachinery.shellin.exec.InvalidExitCodeException

fun main(){
    readmeExample()
}

internal fun readmeExample(){
    //This code is used in the readme; kept here to make sure it compiles.
    val shell = Shellin().apply {
        //You can configure some default behaviors for commands run here
        logCommands(true)
    }
    shell {
        //Synchronously run and wait for a command.
        command("echo Hello world from Shellin!").waitFor()

        //Commands will fail by default if give a nonzero exit code
        try {
            command("sleep --badflags").waitFor()
        } catch(t : InvalidExitCodeException){
            println("Uh oh, $t")
        }

        command("echo"){
            +"You can add arguments and other configuration for a command in a block like this"
            //This discards stderr, for example
            stderr(null)
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