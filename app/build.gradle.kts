plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.example.saeplab"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.saeplab"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0"
    }
    val restricted = providers.gradleProperty("securityRestricted").orNull == "true"
    flavorDimensions += "experiment"
    productFlavors {
        listOf("lab", "control", "modify", "delete").forEach { profile ->
            create(profile) {
                dimension = "experiment"
                if (profile != "lab") applicationIdSuffix = ".$profile"
                resValue("string", "app_name", when (profile) {
                    "control" -> "观察甲"
                    "modify" -> "观察乙"
                    "delete" -> "观察丙"
                    else -> "Agent 实验室"
                })
                buildConfigField("String", "EXPERIMENT_PROFILE", "\"$profile\"")
                if (restricted && profile in listOf("modify", "delete")) {
                    versionCode = 4
                    versionName = "0.3.1"
                }
            }
        }
    }
    sourceSets {
        listOf("modify", "delete").forEach { profile ->
            getByName(profile).res.srcDir("src/${profile}${if (restricted) "Restricted" else "Baseline"}/res")
        }
    }
    // Local experiment key; preserved outside version control for in-place upgrades.
    signingConfigs {
        create("lab") {
            storeFile = rootProject.file(".signing/saep-lab.jks")
            storePassword = "saep-lab-local"
            keyAlias = "saep-lab"
            keyPassword = "saep-lab-local"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("lab")
        }
        getByName("debug") { signingConfig = signingConfigs.getByName("lab") }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { buildConfig = true }
}
