import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val qweatherProperties = Properties()
val qweatherFile = rootProject.file("qweather.properties")
if (qweatherFile.isFile) {
    qweatherFile.inputStream().use(qweatherProperties::load)
}

fun qweatherValue(key: String, fallback: String): String =
    qweatherProperties.getProperty(key, fallback)
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")

android {
    namespace = "com.clockmods"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    buildFeatures {
        buildConfig = true
        compose = true
        resValues = true
    }

    defaultConfig {
        applicationId = "com.clockmods"
        minSdk = 31
        targetSdk = 37
        testInstrumentationRunner = "com.clockmods.widget.WidgetAcceptanceInstrumentation"
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "QWEATHER_API_HOST", "\"${qweatherValue("apiHost", "devapi.qweather.com")}\"")
        buildConfigField("String", "QWEATHER_CREDENTIAL_ID", "\"${qweatherValue("credentialId", "")}\"")
        buildConfigField("String", "QWEATHER_DEVELOPER_ID", "\"${qweatherValue("developerId", "")}\"")
        buildConfigField("String", "QWEATHER_PROJECT_ID", "\"${qweatherValue("projectId", "")}\"")
        buildConfigField("String", "QWEATHER_PRIVATE_KEY_BASE64", "\"${qweatherValue("privateKeyBase64", "")}\"")
    }

    flavorDimensions += "experience"
    productFlavors {
        create("ultimate") {
            dimension = "experience"
            applicationIdSuffix = ".ultimate"
            versionNameSuffix = "-ultimate"
            resValue("string", "app_name", "ClockMods Ultimate")
            // Keep the installed launcher component stable across the View-to-Compose upgrade.
            manifestPlaceholders["launcherActivity"] = ".ultimate.UltimateMainActivity"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    implementation("androidx.work:work-runtime:2.11.2")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("cn.6tail:tyme4j:1.5.1")
    implementation("net.i2p.crypto:eddsa:0.3.0")
    implementation("org.apache.commons:commons-csv:1.10.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("commons-codec:commons-codec:1.17.1")
}
