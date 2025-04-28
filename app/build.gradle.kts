plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "it.pagopa.io.app.cie_example"
    compileSdk = 35

    signingConfigs {
        create("app-debug") {
            keyAlias = "key0"
            keyPassword = "d1g1touch"
            storeFile = file("../certificate.keystore")
            storePassword = "d1g1touch"
        }

        create("app-release") {
            keyAlias = "key0"
            keyPassword = "d1g1touch"
            storeFile = file("../certificate.keystore")
            storePassword = "d1g1touch"
        }
    }

    defaultConfig {
        applicationId = "it.pagopa.io.app.cie_example"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "BASE_URL_IDP",
            "\"https://idserver.servizicie.interno.gov.it/idp/\""
        )
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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    stabilityConfigurationFiles.add {
        rootProject.layout.projectDirectory.file("stability_config.conf").asFile
    }
 }

dependencies {
    implementation(project(":ReadCie"))
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.play.services.basement)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.ext)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}