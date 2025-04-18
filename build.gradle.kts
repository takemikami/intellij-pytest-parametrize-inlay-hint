import org.jetbrains.intellij.platform.gradle.TestFrameworkType
plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intelliJPlatform)
}
group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()
kotlin {
    jvmToolchain(21)
}
repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}
dependencies {
    testImplementation(libs.junit)
    testImplementation("org.opentest4j:opentest4j:1.3.0")
    intellijPlatform {
        create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })
        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })
        testFramework(TestFrameworkType.Platform)
    }
}
intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            untilBuild = provider { null }
        }
    }
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        channels = providers.gradleProperty("pluginVersion").map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }
    pluginVerification {
        ides {
            recommended()
        }
    }
}
intellijPlatformTesting {
    runIde {
        register("runIdeForUiTests") {
            task {
                jvmArgumentProviders += CommandLineArgumentProvider {
                    listOf(
                        "-Drobot-server.port=8082",
                        "-Dide.mac.message.dialogs.as.sheets=false",
                        "-Djb.privacy.policy.text=<!--999.999-->",
                        "-Djb.consents.confirmation.enabled=false",
                    )
                }
            }
            plugins {
                robotServerPlugin()
            }
        }
    }
}
