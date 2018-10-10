import com.jfrog.bintray.gradle.BintrayExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.util.Date
import java.net.URI

buildscript {
	repositories {
		mavenCentral()
	}
	dependencies {
		classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.2.71")
	}
}
plugins {
	`java-gradle-plugin`
	maven
	`maven-publish`
	kotlin("jvm").version("1.2.71")
	id("com.jfrog.bintray").version("1.8.4")
}
group = "net.justmachinery.shellin"
description = "Shell scripting utilities for Kotlin"
version = "0.1.0"

repositories {
	mavenCentral()
	jcenter()
	maven { url = URI("https://dl.bintray.com/scottpjohnson/generic/") }
}

publishing {
	(publications) {
		create<MavenPublication>("shellin") {
			from(components["java"])
			groupId = "net.justmachinery.shellin"
			artifactId = name
			version = project.version as String?
			artifact(tasks.getByName("sourcesJar"))
		}
	}
}

bintray {
	user = project.property("BINTRAY_USER") as String?
	key = project.property("BINTRAY_KEY") as String?
	publish = true

	val pkgOps = closureOf<BintrayExtension.PackageConfig> {
		repo = "generic"
		name = "shellin"
		vcsUrl = "https://github.com/ScottPeterJohnson/shellin.git"
		version(closureOf<BintrayExtension.VersionConfig> {
			name = project.version as String?
			desc = "$project.name version $project.version"
			released = Date().toString()
			vcsTag = "$project.version"
		})
		setProperty("licenses", arrayOf("Apache-2.0"))
	}
	pkg(pkgOps)
	this.setProperty("publications", arrayOf("shellin"))
}


java.sourceSets["test"].withConvention(KotlinSourceSet::class) {
	kotlin.srcDir(file("build/generated-sources/kotlin"))
}

tasks {
	"sourcesJar"(Jar::class) {
		classifier = "sources"
		from(java.sourceSets["main"].allSource)
	}
}


dependencies {
	compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.2.71")
	compile(group= "org.apache.commons", name= "commons-exec", version= "1.3")

	testCompile("io.kotlintest:kotlintest:2.0.3")
}
