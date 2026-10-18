plugins { java }

group = "dev.zelevar"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Если сборка не находит версию, укажите точную, например 26.3.build.169-beta
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }

tasks.withType<JavaCompile> { options.encoding = "UTF-8" }
