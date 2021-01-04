package net.justmachinery.shellin

/**
 * Not a proper "test", but a check to make sure that JVM shutdown handles cleanly
 */
fun main(){
    val process = shellin {  }.command("sleep", "10")
    Runtime.getRuntime().addShutdownHook(Thread {
        println("Hook")
        process.exitCode.get()
        println("Hook done")
    })
    println("Shutdown")
}