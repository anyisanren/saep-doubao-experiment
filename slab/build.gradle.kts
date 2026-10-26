plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.saep.lab"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.saep.lab.base"
        minSdk = 30
        targetSdk = 35
        versionCode = providers.gradleProperty("slabVersionCode").orNull?.toInt() ?: 20
        versionName = "2.0"
    }
    flavorDimensions += "condition"
    productFlavors {
        val labels = linkedMapOf(
            "base" to "BASE", "a" to "A", "b" to "B",
            "g21" to "21", "g22" to "22", "g23" to "23",
            "g24a" to "24A", "g24b" to "24B", "g24c" to "24C",
            "g25" to "25", "g26" to "26", "g27" to "27",
            "g28" to "28", "g29" to "29", "g2a" to "2A",
            "g2f" to "2F", "g2b" to "2B", "g2c" to "2C",
            "g2d" to "2D", "g2e" to "2E", "g43" to "43", "dyn" to "DYN"
        )
        labels.forEach { (profile, label) ->
            create(profile) {
                dimension = "condition"
                applicationId = "com.saep.lab.$profile"
                resValue("string", "app_name", "SLAB-$label")
            }
        }
    }
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
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { buildConfig = false }
}
