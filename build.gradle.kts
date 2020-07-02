import com.jfrog.bintray.gradle.BintrayExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.util.Date
import java.net.URI


plugins {
	maven
	`maven-publish`
	kotlin("jvm").version("1.3.72")
	id("com.jfrog.bintray").version("1.8.4")
}
group = "net.justmachinery.shellin"
description = "Shell scripting utilities for Kotlin"
version = "0.2.0"

repositories {
	mavenCentral()
	jcenter()
	maven { url = URI("https://dl.bintray.com/scottpjohnson/generic/") }
}


tasks {
	val sourcesJar by registering(Jar::class){
		classifier = "sources"
		from(sourceSets.main.get().allSource)
	}
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


dependencies {
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation(group = "io.github.microutils", name = "kotlin-logging", version = "1.7.10")
	api("com.squareup.okio:okio:2.2.2")
	implementation("com.zaxxer:nuprocess:2.0.0")
	implementation("org.codehaus.plexus:plexus-utils:3.3.0")


	testImplementation(group= "ch.qos.logback", name= "logback-classic", version= "1.2.3")
	testImplementation(group= "ch.qos.logback", name= "logback-core", version= "1.2.3")
	testImplementation("org.slf4j:slf4j-api:1.7.25")
	testImplementation("org.slf4j:jcl-over-slf4j:1.7.25")
	testImplementation("io.kotlintest:kotlintest-runner-junit5:3.1.8")
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.1")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.1")
	testImplementation("org.awaitility:awaitility:4.0.3")

}
