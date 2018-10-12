package net.justmachinery.shellin

import java.nio.file.Path
import java.nio.file.Paths

data class ShellContext(
    var workingDirectory : Path,
    var printCommands : Boolean
)


inline fun <T> shell(cb : ShellContext.()->T) : T {
    return cb(ShellContext(
        workingDirectory = Paths.get(".").toAbsolutePath(),
        printCommands = true
    ))
}