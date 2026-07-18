plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("org.openapi.generator") version "7.6.0"
}

android {
    namespace = "com.mealplanplus"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mealplanplus.v2"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "2.0"
        buildConfigField("boolean", "ZERO_BILLING_MODE", "true")
        buildConfigField("boolean", "FORBID_PAID_FIREBASE_FEATURES", "true")
        buildConfigField("String", "API_BASE_URL", "\"https://mealplan-api-rfo22lhanq-ez.a.run.app\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            isMinifyEnabled = false
            isShrinkResources = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
    kotlinOptions { jvmTarget = "17" }

    ksp { arg("room.schemaLocation", "$projectDir/schemas") }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }

    sourceSets {
        getByName("main") {
            java.srcDir(layout.buildDirectory.dir("generated/openapi/src/main/kotlin").get().asFile)
        }
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // WorkManager (sync)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Charts (Vico)
    implementation("com.patrykandpatrick.vico:compose:1.13.1")
    implementation("com.patrykandpatrick.vico:compose-m3:1.13.1")
    implementation("com.patrykandpatrick.vico:core:1.13.1")

    // Health Connect
    implementation("androidx.health.connect:connect-client:1.1.0-alpha08")

    // Firebase (free-tier only)
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-config-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    // Google Sign-In
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set("${rootProject.projectDir}/docs/openapi.yaml")
    outputDir.set(layout.buildDirectory.dir("generated/openapi").get().asFile.absolutePath)
    apiPackage.set("com.mealplanplus.data.generated.api")
    modelPackage.set("com.mealplanplus.data.generated.model")
    library.set("jvm-retrofit2")
    configOptions.set(mapOf(
        "dateLibrary"          to "java8",
        "collectionType"       to "list",
        "gradleBuildFile"      to "false",
        "serializationLibrary" to "gson",
        "useCoroutines"        to "true",
        "omitGradleWrapper"    to "true",
    ))
    typeMappings.set(mapOf("DateTime" to "Instant"))
    importMappings.set(mapOf("Instant" to "java.time.Instant"))
    generateApiTests.set(false)
    generateModelTests.set(false)
    generateModelDocumentation.set(false)
    generateApiDocumentation.set(false)
}

tasks.named("preBuild") {
    dependsOn(tasks.named("openApiGenerate"))
}

tasks.register("verifyNoBillableFirebaseFeatures") {
    doLast {
        val forbidden = setOf(
            "com.google.firebase:firebase-firestore",
            "com.google.firebase:firebase-functions",
            "com.google.firebase:firebase-storage",
            "com.google.firebase:firebase-database"
        )
        val buildFile = file("build.gradle.kts").readText()
        forbidden.forEach { dep ->
            require(!buildFile.contains(dep)) {
                "ZERO_BILLING_MODE violation: $dep is forbidden (paid Firebase service)"
            }
        }
    }
}
tasks.named("build") { dependsOn("verifyNoBillableFirebaseFeatures") }
