import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("com.vanniktech.maven.publish") version "0.30.0"
}

android {
    namespace = "it.pagopa.io.app.cie"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
    }
}

mavenPublishing {
    coordinates("it.pagopa.io.app.cie", "cie", "0.1.4")

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    pom {
        name.set("IOApp CIE SDK Library")
        description.set("A native SDK for reading the Italian Electronic Identity Card (CIE).")
        url.set("https://github.com/pagopa/cie-sdk-android")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://github.com/pagopa/cie-sdk-android/blob/main/LICENSE")
            }
        }

        developers {
            developer {
                id.set("ioapptech")
                name.set("PagoPA S.p.A.")
                email.set("ioapptech@pagopa.it")
            }
        }

        scm {
            connection.set("scm:git:git://github.com/pagopa/cie-sdk-android.git")
            developerConnection.set("scm:git:ssh://github.com/pagopa/cie-sdk-android.git")
            url.set("https://github.com/pagopa/cie-sdk-android")
        }
    }
}

dependencies {
    //network
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.converter.scalars)
    implementation(libs.retrofit)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    testImplementation(libs.mockk)
}
