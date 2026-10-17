pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Velum"

// -PmodelOnly=true configura só o módulo puro de Kotlin.
// Serve para testar o catálogo sem Android SDK.
val modelOnly = providers.gradleProperty("modelOnly").orNull == "true"

include(":core:model")
if (!modelOnly) {
    include(":core:designsystem")
    include(":feature:home")
    include(":feature:details")
    include(":feature:player")
    include(":feature:addons")
    include(":app")
}
