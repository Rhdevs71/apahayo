plugins {
    alias(libs.plugins.android.library)
}

android {
    compileSdk = 35
    namespace = "stub"
    defaultConfig {
        minSdk = 27
    }
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_17
//        targetCompatibility = JavaVersion.VERSION_17
//    }
}

androidComponents {
    beforeVariants(selector().all()) { variant ->
        variant.enableAndroidTest = false
    }
}
