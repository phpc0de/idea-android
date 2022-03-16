
apply { plugin("kotlin") }

apply { plugin("jps-compatible") }

dependencies {
    compile(project(":compiler:util"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("guava", "android-base-common", rootProject = rootProject) }
    compileOnly(intellijPluginDep("gradle")) { includeJars("gradle-api", rootProject = rootProject) }
    compileOnly(intellijPluginDep("android")) { includeJars("android", "android-common", "sdk-common") }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

