android {
  defaultConfig {
    externalNativeBuild {
      cmake {
        abiFilters += setOf("abiFilter2")
        arguments += listOf("argument2")
        cFlags += listOf("cFlag2")
        cppFlags += listOf("cppFlag2")
        targets += setOf("target2")
      }
      ndkBuild {
        abiFilters += setOf("abiFilter4")
        arguments += listOf("argument4")
        cFlags += listOf("cFlag4")
        cppFlags += listOf("cppFlag4")
        targets += setOf("target4")
      }
    }
    ndk {
      abiFilters("abiFilter5")
      abiFilters("abiFilter7")
    }
  }
}
