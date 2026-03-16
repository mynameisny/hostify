pluginManagement {
    repositories {
        maven {
            url = uri("https://nexus.ctl.cloud.cnpc/repository/maven-public/")
            isAllowInsecureProtocol = true
            credentials {
                username = "ucmp-reader"
                password = "iO*Iu@iy@wsa"
            }
        }
        gradlePluginPortal()
    }
}

rootProject.name = "hostify"