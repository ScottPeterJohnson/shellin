import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	`maven-publish`
	signing
	kotlin("jvm").version("1.8.0")
	id("com.github.ben-manes.versions").version("0.47.0")  //For finding outdated dependencies
}
group = "net.justmachinery.shellin"
description = "Shell scripting utilities for Kotlin"
version = "0.2.8"

repositories {
	mavenCentral()
}

kotlin {
	explicitApi()
	jvmToolchain(12)
}
tasks.withType<KotlinCompile> {
	kotlinOptions.freeCompilerArgs = kotlinOptions.freeCompilerArgs + "-Xjvm-default=enable"
}

tasks.withType<Test>().configureEach {
	useJUnitPlatform()
}


val sourcesJar by tasks.registering(Jar::class){
	archiveClassifier.set("sources")
	from(sourceSets.main.get().allSource)
}
val javadocJar by tasks.registering(Jar::class){
	dependsOn.add(JavaPlugin.JAVADOC_TASK_NAME)
	archiveClassifier.set("javadoc")
	from(tasks.getByName("javadoc"))
}

artifacts {
	archives(sourcesJar)
	archives(javadocJar)
}

publishing {
	(publications) {
		create<MavenPublication>("shellin") {
			from(components["kotlin"])
			groupId = "net.justmachinery.shellin"
			artifactId = "shellin"
			version = project.version as String?
			pom {
				name.set("Shellin")
				description.set("Shell scripting utilities for Kotlin")
				url.set("https://github.com/ScottPeterJohnson/shellin")
				licenses {
					license {
						name.set("The Apache License, Version 2.0")
						url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
					}
				}
				developers {
					developer {
						id.set("scottj")
						name.set("Scott Johnson")
						email.set("mavenshellin@justmachinery.net")
					}
				}
				scm {
					connection.set("scm:git:git://github.com/ScottPeterJohnson/shellin.git")
					developerConnection.set("scm:git:ssh://github.com/ScottPeterJohnson/shellin.git")
					url.set("http://github.com/ScottPeterJohnson/shellin")
				}
			}
			artifact(sourcesJar)
			artifact(javadocJar)
		}
	}
	repositories {
		maven {
			name = "central"
			val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
			val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots/")
			url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
			credentials {
				username = findProperty("ossrhUsername") as? String
				password = findProperty("ossrhPassword") as? String
			}
		}
	}

	signing {
		sign(publishing.publications["shellin"])
	}
}

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation(group = "io.github.microutils", name = "kotlin-logging", version = "3.0.5")
	api("com.squareup.okio:okio:3.3.0")
	implementation("com.zaxxer:nuprocess:2.0.6")
	implementation("org.codehaus.plexus:plexus-utils:4.0.0")
	implementation("net.justmachinery.futility:futility-core:1.0.4")

	val kotestVersion = "5.6.2"
	testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
	testImplementation("io.kotest:kotest-property:$kotestVersion")

	testImplementation(group= "ch.qos.logback", name= "logback-classic", version= "1.4.8")
	testImplementation(group= "ch.qos.logback", name= "logback-core", version= "1.4.8")
	testImplementation("org.slf4j:slf4j-api:2.0.7")
	testImplementation("org.slf4j:jcl-over-slf4j:2.0.7")
	testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
	testImplementation("org.awaitility:awaitility:4.2.0")

}
