@file:Suppress("DSL_SCOPE_VIOLATION")

buildscript {
  repositories {
    mavenCentral()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
  }

  dependencies {
    classpath(libs.kotlinx.knit)
  }
}

allprojects {
  repositories {
    mavenCentral()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
  }
}

plugins {
  base
  alias(libs.plugins.dokka)
  alias(libs.plugins.animalSniffer) apply false
  alias(libs.plugins.kotest.multiplatform) apply false
  alias(libs.plugins.kotlinx.kover)
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.kotlin.binaryCompatibilityValidator)
  alias(libs.plugins.arrowGradleConfig.nexus)
  alias(libs.plugins.arrowGradleConfig.versioning)
}

apply(plugin = libs.plugins.kotlinx.knit.get().pluginId)

configure<kotlinx.knit.KnitPluginExtension> {
  siteRoot = "https://arrow-kt.io/"
  rootDir = file("arrow-libs")
  files = fileTree(file("arrow-libs")) {
    include("**/*.md")
    include("**/*.kt")
    include("**/*.kts")

    exclude("**/build/**")
    exclude("**/.gradle/**")
  }
}

koverMerged {
  enable()
  filters {
    projects {
      excludes.addAll(
        listOf(
          ":arrow-annotations",
          ":arrow-site",
          ":arrow-stack",
          ":arrow-optics-ksp-plugin"
        )
      )
    }
  }
}

allprojects {
  group = property("projects.group").toString()
}

val enableCompatibilityMetadataVariant =
  providers.gradleProperty("kotlin.mpp.enableCompatibilityMetadataVariant")
    .orNull?.toBoolean() == true

tasks {
  val generateDoc by creating(Exec::class) {
    group = "documentation"
    commandLine("sh", "gradlew", "dokkaGfm")
  }
  val buildDoc by creating(Exec::class) {
    group = "documentation"
    description = "Generates and validates the documentation"
    dependsOn(generateDoc)
  }

  val undocumentedProjects =
    listOf(project(":arrow-optics-ksp-plugin"))

  dokkaGfmMultiModule { removeChildTasks(undocumentedProjects) }
  dokkaHtmlMultiModule { removeChildTasks(undocumentedProjects) }
  dokkaJekyllMultiModule { removeChildTasks(undocumentedProjects) }
}

apiValidation {
  val ignoreApiValidation = if (!enableCompatibilityMetadataVariant) {
    listOf("arrow-optics-ksp-plugin", "arrow-site")
  } else {
    listOf("arrow-optics-ksp-plugin")
  }

  ignoredProjects.addAll(ignoreApiValidation)
}

rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
  rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().apply {
    versions.webpackDevServer.version = "4.11.1"
    versions.webpack.version = "5.75.0"
    versions.webpackCli.version = "4.10.0"
    versions.karma.version = "6.4.1"
    versions.mocha.version = "10.2.0"
  }
}
