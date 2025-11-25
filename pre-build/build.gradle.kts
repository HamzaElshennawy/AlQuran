plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        create("androidLibraryConvention") {
            id = "com.hifnawy.pre.build"
            implementationClass = "com.hifnawy.pre.build.PreBuildPlugin"
        }
    }
}

dependencies {
    implementation(libs.android.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
}
