plugins {
    java
    application
    id("org.springframework.boot") version "3.2.4"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "me.ningyu.app"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    maven {
        name = "cnpc-nexus"
        url = uri("https://nexus.ctl.cloud.cnpc/repository/maven-public/")
        isAllowInsecureProtocol = true
        credentials {
            username = "ucmp-reader"
            password = "iO*Iu@iy@wsa"
        }
    }
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    
    // OpenAPI/Swagger - 使用与Spring Boot 3.2.4兼容的版本
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
    
    // Database
    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("com.h2database:h2")
    
    // Development Tools
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    
    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

application {
    mainClass.set("me.ningyu.app.hostify.Application")
}

// 复制Markdown文件到静态资源目录的任务
val copyMarkdownFiles = tasks.register<Copy>("copyMarkdownFiles") {
    from(projectDir) {
        include("arch.md", "BATCH_IMPORT_GUIDE.md", "DOCKER_GUIDE.md")
    }
    into(layout.buildDirectory.dir("resources/main/static"))
    dependsOn("processResources")
}

// 确保resolveMainClassName任务在copyMarkdownFiles之后运行
tasks.named("resolveMainClassName") {
    dependsOn(copyMarkdownFiles)
}

tasks.named("processResources") {
    finalizedBy(copyMarkdownFiles)
}

tasks.named("bootJar") {
    dependsOn(copyMarkdownFiles)
}

tasks.named("jar") {
    dependsOn(copyMarkdownFiles)
}