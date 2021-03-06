android {
  signingConfigs {
    create("release") {
      storeFile = file("release.keystore")
      storePassword = "password"
      storeType = "type1"
      keyAlias = "myReleaseKey"
      keyPassword = "releaseKeyPassword"
    }
    getByName("debug") {
      storeFile = file("debug.keystore")
      storePassword = "debug_password"
      storeType = "type2"
      keyAlias = "myDebugKey"
      keyPassword = "debugKeyPassword"
    }
  }
}