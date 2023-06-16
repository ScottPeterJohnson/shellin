package net.justmachinery.shellin


import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import net.justmachinery.futility.bytes.KiB
import net.justmachinery.shellin.exec.InputPumper
import java.io.InputStream
import kotlin.random.Random

class BasicTest : StringSpec() {
    init {
        val shell = shellin {}
        "can run programs" {
            val cmd = shell.command("echo", "foo")
            cmd.successful().get().shouldBeTrue()
            shell.bash("exit 1"){ exitValues = listOf(1) }.successful().get().shouldBeTrue()
        }

        "can collect output" {
            shell.new {
                repeat(1000){
                    collectStdout {
                        command("echo", "foo")
                    }.text shouldBe "foo\n"

                    val longString = "foo".repeat(Random.nextInt(5000) + 7500)
                    logCommands = false
                    collectStdout {
                        command("echo", longString)
                    }.text shouldBe "$longString\n"
                }
            }
        }
        "can read stdin" {
            shell.new {
                collectStdout {
                    bash("read line; echo done"){
                        stdin("foo bar\n".byteInputStream())
                    }
                }.text shouldBe "done\n"

                repeat(1000) {
                    val bytes = Random.nextBytes(Random.nextInt(12500))

                    collectStdout {
                        program("cat"){
                            stdin(bytes.inputStream())
                        }
                    }.stream.readAllBytes() shouldBe bytes
                }
            }
        }
        "can collect stderr" {
            shell.new {
                repeat(1000){
                    collectStderr {
                        bash("echo foo 1>&2")
                    }.text shouldBe "foo\n"

                    val longString = "foo".repeat(Random.nextInt(5000) + 7500)
                    logCommands = false
                    collectStderr {
                        bash("echo $1 1>&2"){
                            +"scriptname"
                            +longString
                        }
                    }.text shouldBe "$longString\n"
                }
            }
        }
        "thou shalt not read all stdin at once" {
            shell.new {
                val qs = InfiniteStreamOfQ()
                val proc = bash("sleep 1; read -N${256.KiB}; sleep 1"){
                    stdin(qs)
                }
                Thread.sleep(500)
                val totalBuffered = qs.qsRead
                //Note that linux buffers stdin a bit itself
                totalBuffered shouldBeLessThan (InputPumper.DEFAULT_BUFFER_SIZE + 128.KiB)
                proc.waitFor()
                qs.qsRead shouldBe (totalBuffered + 256.KiB)
            }
        }
        "example code should work probably" {
            readmeExample()
        }
    }
}

private class InfiniteStreamOfQ : InputStream() {
    var qsRead = 0L
    override fun read(): Int {
        qsRead += 1
        return 'Q'.code
    }
}

