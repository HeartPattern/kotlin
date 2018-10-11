
import org.gradle.jvm.tasks.Jar

description = "Compiler runner + daemon client"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

dependencies {
    compile(project(":kotlin-build-common"))
    compileOnly(project(":compiler:cli-common"))
    compileOnly(project(":kotlin-preloader"))
    compileOnly(project(":compiler:frontend.java"))
    compileOnly(project(":compiler:daemon-common"))
    compileOnly(project(":compiler:daemon-common-new"))
    compile(project(":kotlin-daemon-client"))
    compile(project(":kotlin-daemon-client-new"))
    compileOnly(project(":compiler:util"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    runtimeOnly(projectRuntimeJar(":kotlin-compiler-embeddable"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

val jar: Jar by tasks

runtimeJar(rewriteDepsToShadedCompiler(jar))
sourcesJar()
javadocJar()

publish()
