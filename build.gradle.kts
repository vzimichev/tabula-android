import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByName

plugins {
    id("com.android.application") version "8.5.2" apply false
    id("com.android.library") version "8.5.2"
    id("maven-publish")
}

group = "technology.tabula"
version = "0.1.0-alpha01"

android {
    namespace = "technology.tabula"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("src/main/AndroidManifest.xml")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    exclude("technology/tabula/CommandLineApp.java")
    exclude("technology/tabula/debug/**")
}

dependencies {
    api("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation("org.locationtech.jts:jts-core:1.20.0")
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("commons-cli:commons-cli:1.8.0")
    implementation("org.apache.commons:commons-csv:1.11.0")
    implementation("com.google.code.gson:gson:2.11.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}

publishing {
    publications {
        create<MavenPublication>("release") {
            afterEvaluate {
                from(components.getByName("release"))
            }
            groupId = project.group.toString()
            artifactId = "tabula-android"
            version = project.version.toString()

            pom {
                name.set("Tabula Android")
                description.set("Android port of the Tabula PDF table extraction engine powered by pdfbox-android.")
                url.set("https://github.com/tabulapdf/tabula-java")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/license/mit")
                    }
                }
                developers {
                    developer {
                        name.set("Tabula contributors")
                        organization.set("Tabula")
                        organizationUrl.set("https://github.com/tabulapdf")
                    }
                }
                scm {
                    url.set("https://github.com/tabulapdf/tabula-java")
                    connection.set("scm:git:https://github.com/tabulapdf/tabula-java.git")
                    developerConnection.set("scm:git:https://github.com/tabulapdf/tabula-java.git")
                }
            }
        }
    }
}
