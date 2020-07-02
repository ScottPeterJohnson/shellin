import io.kotlintest.matchers.boolean.shouldBeTrue
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import net.justmachinery.shellin.*
import net.justmachinery.shellin.exec.InputPumper
import net.justmachinery.shellin.exec.InvalidExitCodeException
import java.io.InputStream
import kotlin.random.Random

class BasicTest : StringSpec() {
    init {
        val shell = Shellin()
        "can run programs" {
            shell {
                val cmd = command("echo", "foo")
                cmd.successful().get().shouldBeTrue()
                bash("exit 1"){ exitValues(listOf(1)) }.successful().get().shouldBeTrue()
            }
        }

        "can collect output" {
            shell {
                repeat(1000){
                    collectStdout {
                        command("echo", "foo")
                    }.text shouldBe "foo\n"

                    val longString = "foo".repeat(Random.nextInt(5000) + 7500)
                    collectStdout {
                        command("echo", longString)
                    }.text shouldBe "$longString\n"
                }
            }
        }
        "can read stdin" {
            shell {
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
                    }.stream.readAllBytes() should { it!!.contentEquals(bytes) }
                }
            }
        }
        "can collect stderr" {
            shell {
                repeat(1000){
                    collectStderr {
                        bash("echo foo 1>&2")
                    }.text shouldBe "foo\n"

                    val longString = "foo".repeat(Random.nextInt(5000) + 7500)
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
            shell {
                val qs = InfiniteStreamOfQLines()
                val proc = bash("sleep 1; read; sleep 1"){
                    stdin(qs)
                }
                Thread.sleep(500)
                qs.qsRead should { it == InputPumper.DEFAULT_BUFFER_SIZE }
                proc.waitFor()
                qs.qsRead should { it == InputPumper.DEFAULT_BUFFER_SIZE + 100 }
            }
        }
        "example code should work probably" {
            readmeExample()
        }
    }
}

private class InfiniteStreamOfQLines : InputStream() {
    var qsRead = 0L
    override fun read(): Int {
        qsRead += 1
        if(qsRead > 0 && qsRead % 100 == 0L){
            return '\n'.toInt()
        }
        return 'Q'.toInt()
    }
}

