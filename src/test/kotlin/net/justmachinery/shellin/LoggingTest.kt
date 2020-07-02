package net.justmachinery.shellin

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import org.awaitility.Awaitility
import org.hamcrest.core.IsEqual
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class LoggingTest : StringSpec() {
    init {
        val shell = shellin {}
        "simple logging".config() {
            val logs = mutableListOf<String>()
            shell.new {
                logStdout { { logs.add(it.toString()) } }

                command("echo", "foo").waitFor()
                command("echo", "foo\nbar\nbaz").waitFor()
                Thread.sleep(1000)
            }
            logs shouldBe listOf("foo", "foo", "bar", "baz")
        }
        "crazy binary logging" {
            repeat(100) {
                val bits = Random.nextBytes(Random.nextInt(125_000))
                bits.forEachIndexed {index, byte ->
                    if(byte == '\n'.toByte()){
                        bits[index] = ' '.toByte()
                    }
                }
                //Add a few newlines
                repeat(Random.nextInt(4)){
                    bits[Random.nextInt(bits.size)] = '\n'.toByte()
                }

                val logs = Collections.synchronizedList(mutableListOf<String>())
                shell.new {
                    logStdout {
                        {
                            val str = it.toString()
                            logs.add(str)
                        }
                    }
                    command("cat -"){
                        stdin(bits.inputStream())
                    }.waitFor()
                }

                val straightDecoding = Charsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.IGNORE)
                    .onUnmappableCharacter(CodingErrorAction.IGNORE)
                    .decode(ByteBuffer.wrap(bits)).toString().replace("\n", "")
                try {
                    Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(50, TimeUnit.MILLISECONDS)
                        .until({ synchronized(logs) { logs.joinToString("") } }, IsEqual(straightDecoding))
                }catch(t : Throwable){ //Better messages in intellij console I guess
                    synchronized(logs) { logs.joinToString("") } shouldBe straightDecoding
                }
            }
        }
    }
}