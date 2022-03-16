/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.imports

import com.android.support.AndroidxNameUtils

/**
 *  Lookup from key class names to well known maven.google.com artifacts.
 */
object MavenClassRegistryFromHardcodedMap : MavenClassRegistryBase() {

  /**
   * Given a class name, returns the likely collection of [MavenClassRegistryBase.Library] objects for the maven.google.com
   * artifacts containing that class.
   *
   * Here, the passed in [className] can be either short class name or fully qualified class name.
   *
   * This implementation only returns results for a few important classes, not an exhaustive search from a full index.
   *
   * NOTE: This MavenClassRegistry.Library should be returning the pre-AndroidX MavenClassRegistry.Library names when
   * applicable; the caller is responsible for mapping this to the corresponding AndroidX [MavenClassRegistryBase.Library]
   * artifact if the project is using it. (The reason for this is practical: we have an artifact map to map forwards,
   * not backwards.)
   */
  override fun findLibraryData(className: String, useAndroidX: Boolean): Collection<Library> {
    val index = className.lastIndexOf('.')
    val shortName = className.substring(index + 1)
    val packageName = if (index == -1) "" else className.substring(0, index)

    val foundArtifact = findLocalArtifactData(shortName) ?: return emptyList()
    return if (packageName.isEmpty() ||
               foundArtifact.packageName == packageName ||
               checkAndroidX(className, "${foundArtifact.packageName}.${shortName}")) {
      listOf(foundArtifact)
    }
    else {
      emptyList()
    }
  }

  /**
   * For the given runtime artifact, if Kotlin is the adopted language, the corresponding ktx library is provided.
   */
  override fun findKtxLibrary(artifact: String): String? {
    return when (artifact) {
      "android.arch.work:work-runtime" -> "android.arch.work:work-runtime-ktx"
      "android.arch.navigation:navigation-runtime" -> "android.arch.navigation:navigation-runtime-ktx"
      "android.arch.navigation:navigation-fragment" -> "android.arch.navigation:navigation-fragment-ktx"
      "android.arch.navigation:navigation-common" -> "android.arch.navigation:navigation-common-ktx"
      "android.arch.navigation:navigation-ui" -> "android.arch.navigation:navigation-ui-ktx"
      "androidx.activity:activity" -> "androidx.activity:activity-ktx"
      "androidx.collection:collection" -> "androidx.collection:collection-ktx"
      "androidx.core:core" -> "androidx.core:core-ktx"
      "androidx.dynamicanimation:dynamicanimation" -> "androidx.dynamicanimation:dynamicanimation-ktx"
      "androidx.fragment:fragment" -> "androidx.fragment:fragment-ktx"
      "androidx.lifecycle:lifecycle-livedata" -> "androidx.lifecycle:lifecycle-livedata-ktx"
      "androidx.lifecycle:lifecycle-livedata-core" -> "androidx.lifecycle:lifecycle-livedata-core-ktx"
      "androidx.lifecycle:lifecycle-reactivestreams" -> "androidx.lifecycle:lifecycle-reactivestreams-ktx"
      "androidx.lifecycle:lifecycle-viewmodel" -> "androidx.lifecycle:lifecycle-viewmodel-ktx"
      "androidx.navigation:navigation-runtime" -> "androidx.navigation:navigation-runtime-ktx"
      "androidx.navigation:navigation-fragment" -> "androidx.navigation:navigation-fragment-ktx"
      "androidx.navigation:navigation-ui" -> "androidx.navigation:navigation-ui-ktx"
      "androidx.paging:paging-runtime" -> "androidx.paging:paging-runtime-ktx"
      "androidx.paging:paging-common" -> "androidx.paging:paging-common-ktx"
      "androidx.paging:paging-rxjava2" -> "androidx.paging:paging-rxjava2-ktx"
      "androidx.palette:palette" -> "androidx.palette:palette-ktx"
      "androidx.preference:preference" -> "androidx.preference:preference-ktx"
      "androidx.slice:slice-builders" -> "androidx.slice:slice-builders-ktx"
      "androidx.sqlite:sqlite" -> "androidx.sqlite:sqlite-ktx"
      "androidx.work:work-runtime" -> "androidx.work:work-runtime-ktx"
      "com.google.android.play:core" -> "com.google.android.play:core-ktx"
      "com.google.firebase:firebase-common" -> "com.google.firebase:firebase-common-ktx"
      "com.google.firebase:firebase-config" -> "com.google.firebase:firebase-config-ktx"
      "com.google.firebase:firebase-crashlytics" -> "com.google.firebase:firebase-crashlytics-ktx"
      "com.google.firebase:firebase-database" -> "com.google.firebase:firebase-database-ktx"
      "com.google.firebase:firebase-dynamic-links" -> "com.google.firebase:firebase-dynamic-links-ktx"
      "com.google.firebase:firebase-firestore" -> "com.google.firebase:firebase-firestore-ktx"
      "com.google.firebase:firebase-functions" -> "com.google.firebase:firebase-functions-ktx"
      "com.google.firebase:firebase-inappmessaging" -> "com.google.firebase:firebase-inappmessaging-ktx"
      "com.google.firebase:firebase-inappmessaging-display" -> "com.google.firebase:firebase-inappmessaging-display-ktx"
      "com.google.firebase:firebase-perf" -> "com.google.firebase:firebase-perf-ktx"
      "com.google.firebase:firebase-storage" -> "com.google.firebase:firebase-storage-ktx"
      else -> null
    }
  }

  private fun checkAndroidX(className: String, preAndroidXClassName: String): Boolean {
    return AndroidxNameUtils.getNewName(preAndroidXClassName) == className
  }

  private fun findLocalArtifactData(className: String): Library? {
    return when (className) {
      "Nullable",
      "NonNull" ->
        // In AndroidX this is androidx.annotation
        Library(artifact = "com.android.support:support-annotations", packageName = "android.support.annotation")

      "RecyclerView",
      "LinearLayoutManager" ->
        // In AndroidX this is androidx.recyclerview.widget
        Library(artifact = "com.android.support:recyclerview-v7", packageName = "android.support.v7.widget")
      "DiffUtil" ->
        // In AndroidX this is androidx.recyclerview.widget
        Library(artifact = "com.android.support:recyclerview-v7", packageName = "android.support.v7.util")

      "NavHost",
      "NavController" ->
        // Not available prior to AndroidX
        Library(artifact = "android.arch.navigation:navigation-runtime", packageName = "androidx.navigation")

      "NavHostFragment" ->
        // Not available prior to AndroidX
        Library(artifact = "android.arch.navigation:navigation-fragment",
                packageName = "androidx.navigation.fragment")

      "TextClassifier" ->
        // Not available prior to AndroidX
        Library(artifact = "com.android.support:textclassifier", packageName = "androidx.textclassifier")

      "PrintHelper" ->
        // In AndroidX this is androidx.print
        Library(artifact = "com.android.support:print", packageName = "android.support.v4.print")

      "CoordinatorLayout" ->
        // In AndroidX this is androidx.coordinatorlayout.widget
        Library(artifact = "com.android.support:coordinatorlayout", packageName = "android.support.design.widget")

      "MediaPlayer" ->
        // Not available prior to AndroidX
        Library(artifact = "com.android.support:media2", packageName = "androidx.media2.player")

      "ExoPlayer",
      "ExoPlayerFactory" ->
        Library(artifact = "com.android.support:media2-exoplayer", packageName = "com.google.android.exoplayer2")

      "Loader",
      "CursorLoader",
      "AsyncTaskLoader" ->
        // In AndroidX this is androidx.loader.content
        Library(artifact = "com.android.support:loader", packageName = "android.support.v4.content")

      "DrawerLayout" ->
        // In AndroidX this is androidx.drawerlayout.widget
        Library(artifact = "com.android.support:drawerlayout", packageName = "android.support.v4.widget")

      "SupportSQLiteQuery",
      "SupportSQLiteQueryBuilder",
      "SupportSQLiteOpenHelper",
      "SupportSQLiteDatabase" ->
        // In AndroidX this is androidx.sqlite.db
        Library(artifact = "android.arch.persistence:db", packageName = "android.arch.persistence.db")

      "Palette" ->
        // In AndroidX this is androidx.palette.graphics
        Library(artifact = "com.android.support:palette-v7", packageName = "android.support.v7.graphics")

      "WebViewCompat" ->
        // Not available prior to AndroidX
        Library(artifact = "com.android.support:webkit", packageName = "androidx.webkit")

      "Transition" ->
        // In AndroidX this is androidx.transition
        Library(artifact = "com.android.support:transition", packageName = "android.support.transition")

      "VersionedParcel",
      "VersionedParcelable" ->
        // Not available prior to AndroidX
        Library(artifact = "com.android.support:versionedparcelable", packageName = "androidx.versionedparcelable")

      "ViewPager" ->
        // In AndroidX this is androidx.viewpager.widget
        Library(artifact = "com.android.support:viewpager", packageName = "android.support.v4.view")

      "ViewPager2" ->
        // Not available prior to AndroidX
        Library(artifact = "androidx.viewpager2:viewpager2", packageName = "androidx.viewpager2.widget")

      "SwipeRefreshLayout" ->
        // In AndroidX this is androidx.swiperefreshlayout.widget
        Library(artifact = "com.android.support:swiperefreshlayout", packageName = "android.support.v4.widget")

      "MediaRouter" ->
        // In AndroidX this is "androidx.mediarouter.media
        Library(artifact = "com.android.support:mediarouter-v7", packageName = "android.support.v7.media")

      "MediaRouteButton",
      "MediaRouteActionProvider" ->
        // In AndroidX this is androidx.mediarouter.app
        Library(artifact = "com.android.support:mediarouter-v7", packageName = "android.support.v7.app")

      "FingerprintDialogFragment",
      "BiometricPrompt",
      "BiometricFragment" ->
        // Not available prior to AndroidX
        Library(artifact = "androidx.biometric:biometric", packageName = "androidx.biometric")

      "HeifWriter",
      "HeifEncoder" ->
        // Not available prior to AndroidX
        Library(artifact = "com.android.support:heifwriter", packageName = "androidx.heifwriter")

      "ExifInterface" ->
        // In AndroidX this is androidx.exifinterface.media
        Library(artifact = "com.android.support:exifinterface", packageName = "android.support.media")

      "PagedList",
      "PagedListBuilder",
      "PagedListAdapter" ->
        // In AndroidX this is androidx.paging
        Library(artifact = "android.arch.paging:runtime", packageName = "android.arch.paging")

      "VectorDrawableCompat" ->
        // In AndroidX this is androidx.vectordrawable.graphics.drawable
        Library(artifact = "com.android.support:support-vector-drawable",
                packageName = "android.support.graphics.drawable")

      "PathInterpolatorCompat" ->
        // In AndroidX this is androidx.core.view.animation
        Library(artifact = "com.android.support:support-vector-drawable",
                packageName = "android.support.v4.view.animation")

      "AnimatedVectorDrawableCompat" ->
        // In AndroidX this is androidx.vectordrawable.graphics.drawable
        Library(artifact = "com.android.support:animated-vector-drawable",
                packageName = "android.support.graphics.drawable")

      "ItemKeyProvider",
      "ItemDetailsLookup",
      "MutableSelection",
      "Selection",
      "SelectionTracker",
      "StorageStrategy" ->
        // Not available prior to AndroidX
        Library(artifact = "com.android.support:recyclerview-selection",
                packageName = "androidx.recyclerview.selection")

      "LiveData",
      "MutableLiveData",
      "Observer",
      "ComputableLiveData" ->
        // In AndroidX this is androidx.lifecycle
        Library(artifact = "android.arch.lifecycle:livedata", packageName = "android.arch.lifecycle")

      "ViewModelProviders" ->
        // In AndroidX this is androidx.lifecycle
        Library(artifact = "android.arch.lifecycle:extensions", packageName = "android.arch.lifecycle")

      "ViewModel",
      "ViewModelProvider" ->
        // In AndroidX this is androidx.lifecycle
        Library(artifact = "android.arch.lifecycle:viewmodel", packageName = "android.arch.lifecycle")

      "Lifecycle" ->
        // In AndroidX this is androidx.lifecycle
        Library(artifact = "android.arch.lifecycle:common", packageName = "android.arch.lifecycle")

      "ProcessLifecycleOwner" ->
        // In AndroidX this is androidx.lifecycle
        Library(artifact = "androidx.lifecycle:lifecycle-process", packageName = "android.arch.lifecycle")

      "ConstraintLayout" ->
        // In AndroidX this is androidx.constraintlayout.widget
        Library(artifact = "com.android.support.constraint:constraint-layout",
                packageName = "android.support.constraint")

      "MotionLayout" ->
        // Not available prior to AndroidX
        Library(artifact = "com.android.support.constraint:constraint-layout",
                packageName = "androidx.constraintlayout.motion.widget.MotionLayout")

      "LocalBroadcastManager" ->
        // In AndroidX this is androidx.localbroadcastmanager.content
        Library(artifact = "com.android.support:localbroadcastmanager", packageName = "android.support.v4.content")

      "ContentRecommendation" ->
        // In AndroidX this is androidx.recommendation.app
        Library(artifact = "com.android.support:recommendation", packageName = "android.support.app.recommendation")

      "Preference",
      "ListPreference",
      "PreferenceGroup",
      "PreferenceGroupAdapter",
      "PreferenceScreen",
      "PreferenceViewHolder" ->
        // In AndroidX this is androidx.preference
        Library(artifact = "com.android.support:preference-v7", packageName = "android.support.v14.preference")

      "AppCompatActivity" ->
        // In AndroidX this is androidx.appcompat.app
        Library(artifact = "com.android.support:appcompat-v7", packageName = "android.support.v7.app")

      "Toolbar" ->
        // In AndroidX this is androidx.appcompat.widget
        Library(artifact = "com.android.support:appcompat-v7", packageName = "android.support.v7.widget")

      "Data",
      "ListenableWorker",
      "OneTimeWorkRequest",
      "PeriodicWorkRequest",
      "Worker",
      "WorkManager",
      "WorkRequest" ->
        // Not available prior to AndroidX
        Library(artifact = "android.arch.work:work-runtime", packageName = "androidx.work")

      "RxWorker" ->
        // Not available prior to AndroidX
        Library(artifact = "android.arch.work:work-rxjava2", packageName = "androidx.work")

      "CoroutineWorker" ->
        // Not available prior to AndroidX
        Library(artifact = "android.arch.work.work-runtime-ktx", packageName = "androidx.work")

      "CardView" ->
        // In AndroidX this is androidx.cardview.widget
        Library(artifact = "com.android.support:cardview-v7", packageName = "android.support.v7.widget")

      "ContentPager" ->
        // In AndroidX this is androidx.contentpager.content
        Library(artifact = "com.android.support:support-content", packageName = "android.support.content")

      "CursorAdapter",
      "SimpleCursorAdapter",
      "CursorFilter" ->
        // In AndroidX this is androidx.cursoradapter.widget
        Library(artifact = "com.android.support:cursoradapter", packageName = "android.support.v4.widget")

      "DocumentFile",
      "RawDocumentFile",
      "SingleDocumentFile",
      "TreeDocumentFile" ->
        // In AndroidX this is androidx.documentfile.provider
        Library(artifact = "com.android.support:documentfile", packageName = "android.support.v4.provider")

      "EmojiCompat",
      "FontRequestEmojiCompatConfig" ->
        // In AndroidX this is androidx.emoji.text
        Library(artifact = "com.android.support:support-emoji", packageName = "android.support.text.emoji")

      "EmojiButton",
      "EmojiTextView" ->
        // In AndroidX this is androidx.emoji.widget
        Library(artifact = "com.android.support:support-emoji", packageName = "android.support.text.emoji.widget")

      "DialogFragment",
      "Fragment",
      "ListFragment",
      "FragmentManager",
      "FragmentActivity",
      "FragmentTransaction" ->
        // In AndroidX this is androidx.fragment.app
        Library(artifact = "androidx.fragment:fragment", packageName = "android.support.v4.app")

      "FragmentContainerView" ->
        // Not available prior to AndroidX
        Library(artifact = "androidx.fragment:fragment", packageName = "androidx.fragment.app")

      "GridLayout" ->
        // In AndroidX this is androidx.gridlayout.widget
        Library(artifact = "com.android.support:gridlayout-v7", packageName = "android.support.v7.widget")

      "FastOutLinearInInterpolator",
      "FastOutSlowInInterpolator",
      "LinearOutSlowInInterpolator",
      "LookupTableInterpolator" ->
        // In AndroidX this is androidx.interpolator.view.animation
        Library(artifact = "com.android.support:interpolator", packageName = "android.support.v4.view.animation")

      "Query",
      "Entity",
      "Dao",
      "Insert",
      "Delete",
      "Database",
      "Room",
      "RoomDatabase" ->
        // In AndroidX this is androidx.room
        Library(artifact = "android.arch.persistence.room:runtime", packageName = "android.arch.persistence.room")

      "SliceAction",
      "ListBuilder",
      "GridRowBuilder" ->
        // Not available prior to AndroidX
        Library(artifact = "com.android.support:slices-builders", packageName = "androidx.slice.builders")

      "SliceManager" ->
        // Not available prior to AndroidX
        Library(artifact = "com.android.support:slices-core", packageName = "androidx.slice")

      "SliceView" ->
        // Not available prior to AndroidX
        Library(artifact = "com.android.support:slices-view", packageName = "androidx.slice.widget")

      "SliceUtils" ->
        // Not available prior to AndroidX
        Library(artifact = "com.android.support:slices-view", packageName = "androidx.slice")

      "SlidingPaneLayout" ->
        // In AndroidX this is androidx.slidingpanelayout.widget
        Library(artifact = "com.android.support:slidingpanelayout", packageName = "android.support.v4.widget")

      "CustomTabsIntent" ->
        // In AndroidX this is androidx.browser.customtabs
        Library(artifact = "com.android.support:customtabs", packageName = "android.support.customtabs")

      // com.android.support:support-compat: Most compat classes are here
      "MenuCompat",
      "MenuItemCompat",
      "GravityCompat" ->
        // In AndroidX this is androidx.core.view
        Library(artifact = "com.android.support:support-compat", packageName = "android.support.v4.view")

      "NotificationCompat",
      "NotificationManagerCompat",
      "ServiceCompat",
      "JobIntentService" ->
        // In AndroidX this is androidx.core.app
        Library(artifact = "com.android.support:support-compat", packageName = "android.support.v4.app")

      "FileProvider",
      "ContextCompat" ->
        // In AndroidX this is androidx.core.content
        Library(artifact = "com.android.support:support-compat", packageName = "android.support.v4.content")

      "DrawableCompat" ->
        // In AndroidX this is androidx.core.graphics.drawable
        Library(artifact = "com.android.support:support-compat",
                packageName = "android.support.v4.graphics.drawable")

      "BitmapCompat" ->
        // In AndroidX this is androidx.core.graphics
        Library(artifact = "com.android.support:support-compat", packageName = "android.support.v4.graphics")

      "AdvertisingIdClient",
      "AdvertisingIdInfo" ->
        // Not available prior to AndroidX
        Library(artifact = "androidx.ads:ads-identifier", packageName = "androidx.ads.identifier")

      "AsyncLayoutInflater" ->
        // In AndroidX this is androidx.asynclayoutinflater.view
        Library(artifact = "androidx.asynclayoutinflater:asynclayoutinflater",
                packageName = "android.support.v4.view")

      "HintConstants" ->
        // Not available prior to AndroidX
        Library(artifact = "androidx.autofill:autofill", packageName = "androidx.autofill")

      "BenchmarkRule" ->
        // Not available prior to AndroidX
        Library(artifact = "androidx.benchmark:benchmark-junit4", packageName = "androidx.benchmark.junit4")

      "Preview" ->
        Library(artifact = "androidx.compose.ui:ui-tooling", packageName = "androidx.compose.ui.tooling.preview")

      "CameraX",
      "ImageAnalysis",
      "ImageAnalysisConfig",
      "PreviewConfig",
      "ImageCapture",
      "ImageCaptureConfig" ->
        // Not available prior to AndroidX
        Library(artifact = "androidx.camera:camera-camera2", packageName = "androidx.camera.core")

      "AbsSavedState",
      "ExploreByTouchHelper",
      "FocusStrategy",
      "ViewDragHelper" ->
        // In AndroidX this is androidx.customview.widget
        Library(artifact = "androidx.customview:customview", packageName = "android.support.v4.view")

      "KeyedAppState",
      "KeyedAppStatesReporter",
      "KeyedAppStatesService",
      "ReceivedKeyedAppState" ->
        // Not available prior to AndroidX
        Library(artifact = "androidx.enterprise:enterprise-feedback", packageName = "androidx.enterprise.feedback")

      "RemoteCallback",
      "CallbackReceiver",
      "BroadcastReceiverWithCallbacks",
      "AppWidgetProviderWithCallbacks",
      "ContentProviderWithCallbacks",
      "ExternalInput" ->
        // Not available prior to AndroidX
        Library(artifact = "androidx.remotecallback:remotecallback", packageName = "androidx.remotecallback")

      "SavedStateRegistryOwner",
      "SavedStateRegistry",
      "SavedStateProvider",
      "SavedStateRegistryController" ->
        // Not available prior to AndroidX
        Library(artifact = "androidx.savedstate:savedstate", packageName = "androidx.savedstate")

      "EncryptedFile",
      "EncryptedSharedPreferences",
      "MasterKeys" ->
        // Not available prior to AndroidX
        Library(artifact = "androidx.security:security-crypto", packageName = "androidx.security.crypto")

      "CallbackToFutureAdapter" ->
        Library(artifact = "androidx.concurrent:concurrent-listenablefuture-callback",
                packageName = "androidx.concurrent.futures")

      "Composable" ->
        Library(artifact = "androidx.compose.runtime:runtime", packageName = "androidx.compose.runtime")

      // Material Design MavenClassRegistry.Library
      "Snackbar",
      "FloatingActionButton",
      "BottomAppBar",
      "TabLayout",
      "AppBarLayout",
      "BottomNavigationView",
      "CheckableImageButton" ->
        // In AndroidX this has a number of different package names based on each widget
        Library(artifact = "com.android.support:design", packageName = "android.support.design.widget")

      // Firebase Libraries

      "AppInviteInvitation" ->
        Library(artifact = "com.google.firebase:firebase-invites", packageName = "com.google.android.gms.appinvite")
      "AdView",
      "AdRequest" ->
        Library(artifact = "com.google.firebase:firebase-ads", packageName = "com.google.android.gms.ads")
      "FirebaseAnalytics" ->
        Library(artifact = "com.google.firebase:firebase-core", packageName = "com.google.firebase.analytics")
      "FirebaseMessagingService" ->
        Library(artifact = "com.google.firebase:firebase-messaging", packageName = "com.google.firebase.messaging")
      "FirebaseAuth" ->
        Library(artifact = "com.google.firebase:firebase-auth", packageName = "com.google.firebase.auth")
      "FirebaseDatabase" ->
        Library(artifact = "com.google.firebase:firebase-database", packageName = "com.google.firebase.database")
      "FirebaseStorage",
      "StorageReference" ->
        Library(artifact = "com.google.firebase:firebase-storage", packageName = "com.google.firebase.storage")
      "FirebaseRemoteConfig" ->
        Library(artifact = "com.google.firebase:firebase-config", packageName = "com.google.firebase.remoteconfig")
      "FirebaseCrash" ->
        Library(artifact = "com.google.firebase:firebase-crash", packageName = "com.google.firebase.crash")
      "FirebaseDynamicLinks" ->
        Library(artifact = "com.google.firebase:firebase-invites", packageName = "com.google.firebase.dynamiclinks")
      "FirebaseFirestore" ->
        Library(artifact = "com.google.firebase:firebase-firestore", packageName = "com.google.firebase.firestore")

      // ML

      "FirebaseVisionFace",
      "FirebaseVisionFaceDetectorOptions" ->
        Library(artifact = "com.google.firebase:firebase-ml-vision",
                packageName = "com.google.firebase.ml.vision.face")
      "FirebaseVisionLabelDetector" ->
        Library(artifact = "com.google.firebase:firebase-ml-vision",
                packageName = "com.google.firebase.ml.vision.label")
      "FirebaseVisionImage" ->
        Library(artifact = "com.google.firebase:firebase-ml-vision",
                packageName = "com.google.firebase.ml.vision.common")
      "FirebaseVisionText" ->
        Library(artifact = "com.google.firebase:firebase-ml-vision",
                packageName = "com.google.firebase.ml.vision.text")
      "FirebaseVisionBarcode" ->
        Library(artifact = "com.google.firebase:firebase-ml-vision",
                packageName = "com.google.firebase.ml.vision.barcode")
      "FirebaseVision" ->
        Library(artifact = "com.google.firebase:firebase-ml-vision", packageName = "com.google.firebase.ml.vision")
      "FirebaseVisionCloudDetectorOptions" ->
        Library(artifact = "com.google.firebase:firebase-ml-vision",
                packageName = "com.google.firebase.ml.vision.cloud")

      // Play Services Libraries

      "SignInButton" ->
        Library(artifact = "com.google.android.gms:play-services-auth",
                packageName = "com.google.android.gms.common")

      "GoogleSignIn",
      "GoogleSignInAccount",
      "GoogleSignInOptions" ->
        Library(artifact = "com.google.android.gms:play-services-auth",
                packageName = "com.google.android.gms.auth.api.signin")

      "Awareness" ->
        Library(artifact = "com.google.android.gms:play-services-awareness",
                packageName = "com.google.android.gms.awareness")

      "CastOptions",
      "CastContext" ->
        Library(artifact = "com.google.android.gms:play-services-cast",
                packageName = "com.google.android.gms.cast.framework")

      "Fitness",
      "FitnessOptions" ->
        Library(artifact = "com.google.android.gms:play-services-fitness",
                packageName = "com.google.android.gms.fitness")

      "LocationRequest",
      "LocationServices" ->
        Library(artifact = "com.google.android.gms:play-services-location",
                packageName = "com.google.android.gms.location")

      // https://developers.google.com.android.reference.com.google.android.gms.maps.OnMapReadyCallback
      "GoogleMap",
      "CameraUpdateFactory",
      "OnMapReadyCallback",
      "SupportMapFragment" ->
        Library(artifact = "com.google.android.gms:play-services-maps", packageName = "com.google.android.gms.maps")

      "LatLng",
      "MarkerOptions" ->
        Library(artifact = "com.google.android.gms:play-services-maps",
                packageName = "com.google.android.gms.maps.model")

      "Nearby" ->
        Library(artifact = "com.google.android.gms:play-services-nearby",
                packageName = "com.google.android.gms.nearby")

      "Games" ->
        Library(artifact = "com.google.android.gms:play-services-games",
                packageName = "com.google.android.gms.games")

      "PaymentsClient",
      "Wallet" ->
        Library(artifact = "com.google.android.gms:play-services-wallet",
                packageName = "com.google.android.gms.wallet")

      "BillingClient" ->
        Library(artifact = "com.android.billingclient:billing", packageName = "com.android.billingclient.api")

      "AppUpdateInfo",
      "AppUpdateManager",
      "AppUpdateManagerFactory" ->
        Library(artifact = "com.google.android.play:core", packageName = "com.google.android.play.core.appupdate")

      else -> null
    }
  }
}
