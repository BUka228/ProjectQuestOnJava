plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.projectquestonjava"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.projectquestonjava"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Для Room Schema Location, если  Java Annotation Processing
        // javaCompileOptions {
        //     annotationProcessorOptions {
        //         arguments += ["room.schemaLocation": "$projectDir/schemas".toString()]
        //         arguments += ["room.incremental": "true"]
        //     }
        // }
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
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

    buildFeatures {
        viewBinding = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE-notice.md"
            excludes += "/META-INF/LICENSE"
        }
    }
}

dependencies {
    // AndroidX & Google Material (основные)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Hilt (DI)
    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)
    // Для тестирования Hilt
    androidTestImplementation(libs.hilt.android.testing)

    // Room (База данных)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.room.guava)

    // Lifecycle (ViewModel, LiveData)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.common.java8)
    implementation(libs.androidx.lifecycle.reactivestreams)

    // Navigation Component (для XML)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.hilt.navigation.fragment)

    // DataStore Preferences
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.rxjava3)
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")
    implementation("androidx.datastore:datastore-preferences-guava:1.1.1")

    // Timber (Логгирование)
    implementation(libs.timber)

    // BCrypt (Хеширование паролей)
    implementation(libs.bcrypt)

    // Guava (для ListenableFuture с Room и других утилит)
    implementation(libs.guava)

    // Coil (для загрузки изображений в ImageView)
    implementation(libs.coil)

    // Тестирование
    testImplementation(libs.junit4)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.mockk.android)


    implementation(libs.rxjava3)
    implementation(libs.rxandroid3)

    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Для преобразования Flow в LiveData
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")

    // Для преобразования Flow в CompletableFuture (если нужно)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.3")
}