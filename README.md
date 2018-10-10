[ ![Download](https://api.bintray.com/packages/scottpjohnson/generic/shellin/images/download.svg) ](https://bintray.com/scottpjohnson/generic/shellin/_latestVersion)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
 
shellin is a work in progress utility library for writing shell scripts with Kotlin using [kscript](https://github.com/holgerbrandl/kscript).
It will work in any JVM environment, however.
  
## Use
### Install
Add the following to your build.gradle where appropriate:
```groovy
repositories {
    jcenter()
    maven { url 'https://dl.bintray.com/scottpjohnson/generic/' }
}

dependencies {
    compile 'net.justmachinery.shellin:shellin:<VERSION>'
}
```
You will need to replace `<VERSION>` with the latest version of shellin.


### Basic examples
