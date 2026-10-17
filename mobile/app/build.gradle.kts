import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")

    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

fun configurationValue(name: String, fallback: String = ""): String {
    return providers.gradleProperty(name).orNull
        ?: providers.environmentVariable(name).orNull
        ?: localProperties.getProperty(name)
        ?: fallback
}

fun String.asBuildConfigString(): String {
    return "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""
}

val apiBaseUrl = configurationValue(
    name = "API_BASE_URL",
    fallback = "https://ojociudadano.dev.censei.cl/"
)
val devAuthEmail = configurationValue("DEV_AUTH_EMAIL")
val devAuthPassword = configurationValue("DEV_AUTH_PASSWORD")

android {
    namespace = "cl.uct.ojociudadano"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "cl.uct.ojociudadano"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "API_BASE_URL",
            apiBaseUrl.asBuildConfigString()
        )
        buildConfigField("String", "DEV_AUTH_EMAIL", "\"\"")
        buildConfigField("String", "DEV_AUTH_PASSWORD", "\"\"")
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "DEV_AUTH_EMAIL",
                devAuthEmail.asBuildConfigString()
            )
            buildConfigField(
                "String",
                "DEV_AUTH_PASSWORD",
                devAuthPassword.asBuildConfigString()
            )
        }
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    testImplementation(libs.okhttp.mockwebserver)
}
