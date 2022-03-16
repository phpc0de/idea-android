# C++ Manual Tests

## Project System Header Files

### Simple test
1. From Android Studio choose `File/New/New Project...`
1. Click "Include C++ Support"
1. Click through the wizard `Next->Next->Basic Activity->Next->Next`
1. In Android project view open app/cpp/includes.

**Expect to see 'includes' node**


![New Project Enhanced Includes][new-project]

NOTE: On Windows the slashes in paths displayed should be back slashes.

#### Add a new user header file
1. Right-click on `cpp` and choose `New C/C++ Header File`
1. Name the new header file `my-header-file.h`

**Expect to see 'my-header-file.h' node under `cpp`**

![New Project Enhanced Includes][add-new-header-file]

NOTE: On Windows the slashes in paths displayed should be back slashes.

### Endless Tunnel -- Viewing NDK sub components

1. From Android Studio choose `File/New/Import Sample...`
1. Type `NDK` and choose Endless Tunnel
1. Open `app/cpp/game/includes/NDK Components`

**Expect to see several sub-nodes under 'NDK Components' node**


![New Project Enhanced Includes][endless-tunnel-ndk-components]

NOTE 1: On Windows the slashes in paths displayed should be back slashes.

NOTE 2: The exact content of the sub-nodes depends on the version of the NDK installed.


### Viewing CDep sub components

1. Clone this project from github: `git clone https://github.com/jomof/cdep-android-studio-freetype-sample.git`
1. `cd cdep-android-studio-freetype-sample`
1. On Mac/Linux `./cdep`, on Windows `cdep`
1. Open project `File/Open..` choose `/path/to/projects/cdep-android-studio-freetype-sample/build.gradle`
1. Open `app/cpp/includes/CDep Packages`

**Expect to see two sub-nodes under 'CDep Packages' node: freetype and SDL**


![New Project Enhanced Includes][res/cxx/enhanced-header-files/cdep-free-type-example.png]


[new-project]: res/cxx/enhanced-header-files/new-project.png
[add-new-header-file]: res/cxx/enhanced-header-files/add-new-header-file.png
[endless-tunnel-ndk-components]: res/cxx/enhanced-header-files/endless-tunnel-ndk-components.png
[cdep-free-type-example]: res/cxx/enhanced-header-files/cdep-free-type-example..png

## CMake Version Pinning

### Prepare C++ Project

1. From Android Studio choose `File/New/New Project...`
1. Select `Native C++` from the wizard.
1. Expect gradle sync to succeed without errors.

### Pin and use Quick Fix to install CMake from SDK
1. Use SDK Manager (Main/Preferences/Android SDK) to uninstall all CMake versions from SDK Tools.
1. Edit the main module's `build.gradle` file and add `android.externalNativeBuild.cmake.version "3.10.2"`.
1. Click `Sync now`.
1. Expect a gradle sync error with link: `Install CMake 3.10.2`. Click on the link.
1. Expect installation to complete successfully. Click `Finish`.
1. Expect gradle sync to automatically restart and succeed.

Verify the same behavior using version "3.10.2-rc2" and "3.10.2+".

### Pin and auto-install fork version from SDK
1. Use SDK Manager (Main/Preferences/Android SDK) to uninstall all CMake versions from SDK Tools.
1. Edit the main module's `build.gradle` file and add `android.externalNativeBuild.cmake.version "3.6.0"`.
1. Click `Sync now`.
1. Expect CMake 3.6 to be auto-downloaded.
1. Expect gradle sync to automatically restart and succeed.

Verify the same behavior using version "3.6.0-rc2" and "3.6.0+".

### Pin to non-existing version
1. Use SDK Manager (Main/Preferences/Android SDK) to uninstall all CMake versions from SDK Tools.
1. Edit the main module's `build.gradle` file and add `android.externalNativeBuild.cmake.version "3.12.0"`.
1. Expect a banner to appear at the top: `Gradle files have changed since last project sync...`.
1. Click `Sync now`.
1. Expect a gradle sync error with the following message, and no link:

```
CMake '3.12' was not found in PATH or by cmake.dir property.
- CMake ... (these lines are not important)
```
