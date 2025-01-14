plugins {
	kotlin("jvm") version "2.1.0"
	id("java-library")
	id("maven-publish")
	id("nebula.release") version "19.0.10"
}

group = "org.shypl.tool"

kotlin {
	jvmToolchain(21)
}

repositories {
	mavenCentral()
	mavenLocal()
}

dependencies {
	api("org.shypl.tool:tool-lang:1.0.0-SNAPSHOT")
	api("org.shypl.tool:tool-logging:1.0.0-SNAPSHOT")
	api("org.shypl.tool:tool-utils:1.0.0-SNAPSHOT")
	api("org.shypl.tool:tool-depin:1.0.0-SNAPSHOT")
	
	implementation("ch.qos.logback:logback-classic:1.5.16")
	
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
	implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.2")
	implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-properties:2.18.2")
}

java {
	withSourcesJar()
}

publishing {
	publications.create<MavenPublication>("Library") {
		from(components["java"])
	}
}