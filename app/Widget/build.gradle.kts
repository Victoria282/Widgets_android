plugins {
    id("kotlin-kapt")
    id("maven-publish")
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "ru.taxcom.widget"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
        targetSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    buildFeatures {
        viewBinding = true
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

val core: groovy.lang.Closure<Any> by ext
val dagger: groovy.lang.Closure<Any> by ext
val coroutines: groovy.lang.Closure<Any> by ext
val retrofit: groovy.lang.Closure<Any> by ext
val navigation: groovy.lang.Closure<Any> by ext
val serialization: groovy.lang.Closure<Any> by ext
val ktor: groovy.lang.Closure<Any> by ext

dependencies {
    ktor(project)
    core(project)
    dagger(project)
    retrofit(project)
    navigation(project)
    coroutines(project)
    serialization(project)
    implementation(project(":CashdeskReport"))
    implementation(project(":CommonRecycler"))
    implementation(project(":CashdeskKit"))
    implementation(project(":TaxcomKit"))
    implementation(project(":FilerKit"))
    implementation(project(":Loggin"))
}