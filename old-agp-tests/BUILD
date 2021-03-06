load("//tools/base/bazel:bazel.bzl", "iml_module")

# managed by go/iml_to_build
iml_module(
    name = "intellij.android.old-agp-tests",
    iml_files = ["intellij.android.old-agp-tests.iml"],
    test_class = "com.android.tools.idea.OldAgpTestSuite",
    # keep sorted
    test_data = [
        "//prebuilts/studio/jdk",
        "//prebuilts/studio/layoutlib:build.prop",
        "//prebuilts/studio/layoutlib/data:framework_res.jar",
        "//prebuilts/studio/layoutlib/data:native_libs",
        "//prebuilts/studio/layoutlib/data/fonts",
        "//prebuilts/studio/layoutlib/data/icu",
        "//prebuilts/studio/sdk:build-tools/28.0.3",
        "//prebuilts/studio/sdk:build-tools/latest",
        "//prebuilts/studio/sdk:cmake",
        "//prebuilts/studio/sdk:docs",
        "//prebuilts/studio/sdk:licenses",
        "//prebuilts/studio/sdk:ndk",
        "//prebuilts/studio/sdk:platform-tools",
        "//prebuilts/studio/sdk:platforms/android-28",
        "//prebuilts/studio/sdk:platforms/latest",
        "//prebuilts/studio/sdk:platforms/latest-preview",
        "//prebuilts/studio/sdk:sources",
        "//tools/adt/idea/android:test_deps.zip",
        "//tools/adt/idea/android/annotations",
        "//tools/adt/idea/android/lib:sampleData",
        "//tools/adt/idea/android/testData",
        "//tools/adt/idea/artwork:device-art-resources",
        "//tools/base/build-system:gradle-distrib",
        "//tools/base/build-system:gradle-distrib-5.3.1",
        "//tools/base/build-system:gradle-distrib-5.5",
        "//tools/base/build-system:gradle-distrib-6.5",
        "//tools/base/build-system:studio_repo.zip",
        "//tools/base/build-system/previous-versions:3.3.2.zip",
        "//tools/base/build-system/previous-versions:3.5.0.zip",
        "//tools/base/third_party/kotlin:kotlin-m2repository.zip",
        "//tools/data-binding:data_binding_runtime.zip",
        "//tools/external/gradle:gradle-distrib-2.6",
        "//tools/external/gradle:gradle-distrib-3.5",
        "//tools/external/gradle:gradle-distrib-4.5",
    ],
    test_shard_count = 2,
    test_srcs = ["testSrc"],
    test_tags = [
        "cpu:3",
        "block-network",
        "no_test_mac",
        "no_test_windows",
    ],
    test_timeout = "eternal",
    visibility = ["//visibility:public"],
    # do not sort: must match IML order
    deps = [
        "//prebuilts/studio/intellij-sdk:studio-sdk",
        "//prebuilts/studio/intellij-sdk:studio-sdk-plugin-gradle",
        "//tools/adt/idea/adt-testutils:intellij.android.adt.testutils[module, test]",
        "//tools/base/testutils:studio.android.sdktools.testutils[module, test]",
        "//tools/adt/idea/android:intellij.android.core.tests[module, test]",
        "//tools/adt/idea/android-test-framework:intellij.android.testFramework[module, test]",
        "//tools/adt/idea/android:intellij.android.core[module, test]",
        "//tools/adt/idea/.idea/libraries:truth[test]",
        "//tools/adt/idea/.idea/libraries:mockito[test]",
        "//tools/adt/idea/android-common:intellij.android.common[module, test]",
        "//tools/base/sdk-common:studio.android.sdktools.sdk-common[module, test]",
        "//tools/adt/idea/deploy:intellij.android.deploy[module, test]",
        "//tools/adt/idea/project-system-gradle-psd:intellij.android.projectSystem.gradle.psd[module, test]",
        "//tools/adt/idea/project-system:intellij.android.projectSystem[module, test]",
        "//tools/adt/idea/project-system-gradle:intellij.android.projectSystem.gradle[module, test]",
        "//tools/adt/idea/project-system-gradle-models:intellij.android.projectSystem.gradle.models[module, test]",
        "//tools/adt/idea/project-system-gradle-repository-search:intellij.android.projectSystem.gradle.repositorySearch[module, test]",
        "//tools/adt/idea/project-system-gradle:intellij.android.projectSystem.gradle.tests[module, test]",
        "//prebuilts/studio/intellij-sdk:studio-sdk-plugin-Kotlin",
    ],
)
