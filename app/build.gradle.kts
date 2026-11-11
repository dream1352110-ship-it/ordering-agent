plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.sm.orderingagent"

    // 1. PDF 明确要求 SDK 适配的编译版本
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sm.orderingagent"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 2. 编译目标统一使用 Java 17
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // 3. 启用 Compose (如果模板已生成则保持)
    buildFeatures {
        compose = true
    }

    testOptions { unitTests.isIncludeAndroidResources = true }

    flavorDimensions += "integration"
    productFlavors {
        create("function") { dimension = "integration" }
        create("cli") { dimension = "integration" }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.compose.material:material-icons-extended")
    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.16.1")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(files("libs/capability-contract-1.1.0.aar"))
    implementation(files("libs/capability-provider-sdk-1.1.0.aar"))
}