plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    id("kotlin-kapt")

}

android {
    namespace = "com.isaac.souqalghiyar"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.isaac.souqalghiyar"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        compose = true
    }
}

dependencies {
    // 1. المكتبات الأساسية ونظام تشغيل التطبيق (Core & Lifecycle)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6") // مهمة لمراقبة الـ StateFlow في الكومبوز
    implementation("androidx.activity:activity-compose:1.9.2")

    // 2. حزمة حقول الواجهات (Jetpack Compose & Material 3)
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended") // مهمة جداً لأن كود الواجهة عندك يستخدم أيقونات متقدمة


    // 4. خدمات السيرفر وقاعدة البيانات (Firebase - تم تنظيف التكرار)
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-firestore-ktx") // قاعدة البيانات الرئيسية للطلبات والمستخدمين
    implementation("com.google.firebase:firebase-messaging-ktx") // مهمة جداً لإرسال واستقبال الإشعارات والـ fcm_token
    // implementation("com.google.firebase:firebase-database-ktx") // (اختياري) ارفع التعليق عنها فقط إذا كنت تستخدم الـ Realtime Database بجانب Firestore

    // 5. خدمات الخلفية والشبكة (Background Tasks & Network)
    implementation("androidx.work:work-runtime-ktx:2.9.0") // مفيدة لإدارة مهام الخلفية المستمرة
    implementation("com.squareup.okhttp3:okhttp:4.12.0")   // مفيدة إذا كنت ستتصل ببوابات رسائل SMS خارجية مستقبلاً

    // 6. أدوات الفحص والتجريب (Testing)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // 3. التنقل وحقن الاعتماديات (Navigation & Hilt)
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-compiler:2.52") // 👈 احذف الـ // من هنا ليعمل السطر
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Google ML Kit Text Recognition (لقراءة النصوص من الصورة داخل الهاتف)
    implementation("com.google.mlkit:text-recognition:16.0.0")

    // Coil (لتحميل وعرض الصور في واجهات Compose)
    implementation("io.coil-kt:coil-compose:2.6.0")
}