@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    compilerOptions {
        freeCompilerArgs = listOf(
            "-Xexpect-actual-classes", // remove warnings for expect classes
            "-Xskip-prerelease-check",
            "-opt-in=kotlinx.cinterop.BetaInteropApi",
            "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
            "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
            "-opt-in=org.jetbrains.compose.resources.InternalResourceApi",
        )
    }
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmTarget.get()))
    }
    applyDefaultHierarchyTemplate()
    androidTarget {
        publishLibraryVariants("release")
    }

    listOf(
        iosX64(), iosArm64(), iosSimulatorArm64()
    ).forEach {
        it.binaries {
            framework {
                baseName = "imagepicker"
                isStatic = false
                export(project(":composeApp"))
            }
        }
        val path = projectDir.resolve("src/nativeInterop/cinterop/ImageObserver")
        it.binaries.all {
            linkerOpts("-F $path")
            linkerOpts("-ObjC")
        }
        it.compilations.getByName("main") {
            cinterops.create("ImageObserver") {
                defFile("src/nativeInterop/cinterop/ImageObserver.def")
                compilerOpts("-F $path")
            }
        }
    }
    sourceSets {
        commonMain.get().apply {
            resources.srcDir("src/commonMain/composeResources")
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material)
            implementation(compose.materialIconsExtended)
            implementation(compose.material3)
            implementation(compose.components.resources)

            implementation(project.dependencies.platform(libs.compose.bom))
            implementation(project.dependencies.platform(libs.coroutines.bom))
            implementation(project.dependencies.platform(libs.coil.bom))

            implementation(libs.kotlinx.datetime)
            implementation(libs.compose.navigation)
            implementation(libs.compose.lifecycle.runtime)

            compileOnly(libs.compose.filepicker)
            compileOnly(libs.compose.sonner)


            implementation(libs.coil.core)
            api(libs.coil.compose)

            implementation(libs.voyager.navigator)
            implementation(libs.voyager.screenmodel)
            implementation(libs.voyager.navigator.bottomsheet)
            implementation(libs.voyager.navigator.tab)
            implementation(libs.voyager.transitions)

            compileOnly("dev.chrisbanes.haze:haze:0.9.0-rc03")
            compileOnly("dev.chrisbanes.haze:haze-materials:0.9.0-rc03")
        }
        androidMain.dependencies {
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.ui)
            implementation(libs.accompanist.permissions)
            implementation(libs.coil.video)
            implementation(libs.coil.gif)
        }
    }

}


compose.resources{
    publicResClass = false
    packageOfResClass = "compose_image_picker.imagepicker.generated.resources"
    generateResClass = always
}
android {
    namespace = "com.huhx.picker"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    lint {
        targetSdk = libs.versions.android.targetSdk.get().toInt()
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
    }
}