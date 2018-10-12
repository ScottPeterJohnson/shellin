package net.justmachinery.shellin

fun ShellContext.env(name : String) : String? = System.getenv(name)