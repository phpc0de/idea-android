/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.tools.idea.avdmanager;

import static com.android.sdklib.internal.avd.AvdManager.AVD_INI_DISPLAY_NAME;
import static com.google.common.base.Strings.nullToEmpty;

import com.android.repository.Revision;
import com.android.resources.Density;
import com.android.resources.ScreenOrientation;
import com.android.resources.ScreenSize;
import com.android.sdklib.ISystemImage;
import com.android.sdklib.devices.Device;
import com.android.sdklib.devices.DeviceManager;
import com.android.sdklib.devices.Storage;
import com.android.sdklib.internal.avd.AvdCamera;
import com.android.sdklib.internal.avd.AvdInfo;
import com.android.sdklib.internal.avd.AvdManager;
import com.android.sdklib.internal.avd.AvdNetworkLatency;
import com.android.sdklib.internal.avd.AvdNetworkSpeed;
import com.android.sdklib.internal.avd.EmulatedProperties;
import com.android.sdklib.internal.avd.GpuMode;
import com.android.sdklib.internal.avd.HardwareProperties;
import com.android.tools.idea.log.LogWrapper;
import com.android.tools.idea.observable.core.BoolProperty;
import com.android.tools.idea.observable.core.BoolValueProperty;
import com.android.tools.idea.observable.core.ObjectProperty;
import com.android.tools.idea.observable.core.ObjectValueProperty;
import com.android.tools.idea.observable.core.ObservableObject;
import com.android.tools.idea.observable.core.ObservableString;
import com.android.tools.idea.observable.core.OptionalProperty;
import com.android.tools.idea.observable.core.OptionalValueProperty;
import com.android.tools.idea.observable.core.StringProperty;
import com.android.tools.idea.observable.core.StringValueProperty;
import com.android.tools.idea.sdk.AndroidSdks;
import com.android.tools.idea.sdk.progress.StudioLoggerProgressIndicator;
import com.android.tools.idea.wizard.model.WizardModel;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * {@link WizardModel} containing useful configuration settings for defining an AVD image.
 *
 * See also {@link AvdDeviceData}, which these options supplement.
 */
public final class AvdOptionsModel extends WizardModel {

  private static final Storage minGeneralInternalMemSize = new Storage(200, Storage.Unit.MiB);
  private static final Storage minPlayStoreInternalMemSize = new Storage(2, Storage.Unit.GiB);
  private static final Storage minGeneralSdSize = new Storage(10, Storage.Unit.MiB);
  private static final Storage minPlayStoreSdSize = new Storage(100, Storage.Unit.MiB);
  private static final Storage defaultSdSize = new Storage(512, Storage.Unit.MiB);
  private static final Storage zeroSdSize = new Storage(0, Storage.Unit.MiB);

  private final AvdInfo myAvdInfo;
  @Nullable private final Runnable myAvdCreatedCallback;

  /**
   * The 'myUseQemu2' is used to name the family of virtual hardware boards
   * supported by the QEMU2 engines (which is different from the one for the classic engines, called 'goldfish').
   */
  private BoolProperty myUseQemu2 = new BoolValueProperty(true);

  private StringProperty myAvdId = new StringValueProperty();
  private StringProperty myAvdDisplayName = new StringValueProperty();

  private ObjectProperty<Storage> myInternalStorage = new ObjectValueProperty<>(EmulatedProperties.DEFAULT_INTERNAL_STORAGE);
  private ObjectProperty<ScreenOrientation> mySelectedAvdOrientation =
    new ObjectValueProperty<>(ScreenOrientation.PORTRAIT);
  private ObjectProperty<AvdCamera> mySelectedAvdFrontCamera;
  private ObjectProperty<AvdCamera> mySelectedAvdBackCamera;

  private BoolProperty myHasDeviceFrame = new BoolValueProperty(true);
  private BoolProperty myUseExternalSdCard = new BoolValueProperty(false);
  private BoolProperty myUseBuiltInSdCard = new BoolValueProperty(true);
  private ObjectProperty<AvdNetworkSpeed> mySelectedNetworkSpeed =
    new ObjectValueProperty<>(EmulatedProperties.DEFAULT_NETWORK_SPEED);
  private ObjectProperty<AvdNetworkLatency> mySelectedNetworkLatency =
    new ObjectValueProperty<>(EmulatedProperties.DEFAULT_NETWORK_LATENCY);

  private StringProperty mySystemImageName = new StringValueProperty();
  private StringProperty mySystemImageDetails = new StringValueProperty();

  private OptionalProperty<Integer> myCpuCoreCount = new OptionalValueProperty<>(EmulatedProperties.RECOMMENDED_NUMBER_OF_CORES);
  private ObjectProperty<Storage> myVmHeapStorage = new ObjectValueProperty<>(EmulatedProperties.DEFAULT_HEAP);

  private StringProperty myExternalSdCardLocation = new StringValueProperty();
  private OptionalProperty<Storage> mySdCardStorage = new OptionalValueProperty<>(defaultSdSize);

  private BoolProperty myUseHostGpu = new BoolValueProperty(true);
  private BoolProperty myColdBoot = new BoolValueProperty(false);
  private BoolProperty myFastBoot = new BoolValueProperty(true);
  private BoolProperty myChosenSnapshotBoot = new BoolValueProperty(false);
  private StringProperty myChosenSnapshotFile = new StringValueProperty();
  private OptionalProperty<GpuMode> myHostGpuMode = new OptionalValueProperty<>(GpuMode.AUTO);
  private BoolProperty myEnableHardwareKeyboard = new BoolValueProperty(true);

  private BoolProperty myIsInEditMode = new BoolValueProperty();
  private BoolProperty myRemovePreviousAvd = new BoolValueProperty(true); // Assume 'rename', not 'duplicate'

  private OptionalProperty<File> myBackupSkinFile = new OptionalValueProperty<>();
  private OptionalProperty<SystemImageDescription> mySystemImage = new OptionalValueProperty<>();
  private OptionalProperty<Device> myDevice = new OptionalValueProperty<>();

  private ObservableString existingSdLocation = new StringValueProperty();
  private ObservableObject<Storage> myOriginalSdCard;

  private AvdDeviceData myAvdDeviceData;
  private AvdInfo myCreatedAvd;

  public void setAsCopy() {
    // Copying this AVD. Adjust its name.
    String originalName = myAvdDisplayName.get();
    String newName = "Copy_of_" + originalName;
    for (int copyNum = 2;
         AvdManagerConnection.getDefaultAvdManagerConnection().findAvdWithDisplayName(newName);
         copyNum++) {
      // Dang, that name's already in use. Try again.
      newName = "Copy_" + copyNum + "_of_" + originalName;
    }
    myAvdDisplayName.set(newName);
    // Don't remove the original AVD
    myRemovePreviousAvd.set(false);
  }

  public AvdOptionsModel(@Nullable AvdInfo avdInfo) {
    this(avdInfo, null);
  }

  public AvdOptionsModel(@Nullable AvdInfo avdInfo, @Nullable Runnable avdCreatedCallback) {
    myAvdInfo = avdInfo;
    myAvdCreatedCallback = avdCreatedCallback;
    myAvdDeviceData = new AvdDeviceData();

    boolean supportsVirtualCamera = EmulatorAdvFeatures.emulatorSupportsVirtualScene(
            AndroidSdks.getInstance().tryToChooseSdkHandler(),
            new StudioLoggerProgressIndicator(AvdOptionsModel.class),
            new LogWrapper(Logger.getInstance(AvdOptionsModel.class)));
    mySelectedAvdFrontCamera = new ObjectValueProperty<>(AvdCamera.EMULATED);
    mySelectedAvdBackCamera = new ObjectValueProperty<>(
            supportsVirtualCamera ? AvdCamera.VIRTUAL_SCENE : AvdCamera.EMULATED);

    if (myAvdInfo != null) {
      updateValuesWithAvdInfo(myAvdInfo);
    }
    else {
      updateValuesFromHardwareProperties();
    }
    myDevice.addListener(() -> {
      if (myDevice.get().isPresent()) {
        myAvdDeviceData.updateValuesFromDevice(myDevice.getValue(), mySystemImage.getValueOrNull());

        ScreenSize size = ScreenSize.getScreenSize(myAvdDeviceData.diagonalScreenSize().get());
        Density density = AvdScreenData.getScreenDensity(myAvdDeviceData.deviceId().get(),
                                                         myAvdDeviceData.isTv().get(),
                                                         myAvdDeviceData.screenDpi().get(),
                                                         myAvdDeviceData.screenResolutionHeight().get());
        Storage vmHeapSize = EmulatedProperties.calculateDefaultVmHeapSize(size, density, myAvdDeviceData.isTv().get());
        myVmHeapStorage.set(vmHeapSize);
        if (myAvdDeviceData.getHasSdCard().get()) {
          // has sdcard in device, go with default setting
          myUseBuiltInSdCard.set(true);
        } else {
          myUseBuiltInSdCard.set(false);
        }
        myUseExternalSdCard.set(false);
      }
    });
    mySystemImage.addListener(() -> {
      if (myDevice.get().isPresent()) {
        myAvdDeviceData.updateSkinFromDeviceAndSystemImage(myDevice.getValue(), mySystemImage.getValueOrNull());
      }
    });
  }

  public boolean isPlayStoreCompatible() {
    return myDevice != null &&
           myDevice.isPresent().get() &&
           myDevice.getValue().hasPlayStore() &&
           mySystemImage.isPresent().get() &&
           mySystemImage.getValue().getSystemImage().hasPlayStore();
  }

  public Storage minSdCardSize() {
    if (!myUseBuiltInSdCard.get() && !myUseExternalSdCard.get()) {
      return zeroSdSize;
    }
    return isPlayStoreCompatible() ? minPlayStoreSdSize : minGeneralSdSize;
  }

  public Storage minInternalMemSize() {
    return isPlayStoreCompatible() ? minPlayStoreInternalMemSize : minGeneralInternalMemSize;
  }

  /**
   * Ensure that the SD card size and internal memory
   * size are large enough. (If a device is Play Store
   * enabled, a larger size is required.)
   */
  public void ensureMinimumMemory() {
    if (mySdCardStorage.getValue().lessThan(minSdCardSize())) {
      mySdCardStorage.setValue(minSdCardSize());
    }
    if (myInternalStorage.get().lessThan(minInternalMemSize())) {
      myInternalStorage.set(minInternalMemSize());
    }
  }

  /**
   * Decodes the given string from the INI file and returns a {@link Storage} of
   * corresponding size.
   */
  @Nullable
  private static Storage getStorageFromIni(@Nullable String iniString, boolean isInternalStorage) {
    if (iniString == null) {
      return null;
    }
    String numString = iniString.substring(0, iniString.length() - 1);
    char unitChar = iniString.charAt(iniString.length() - 1);
    Storage.Unit selectedUnit = null;
    for (Storage.Unit u : Storage.Unit.values()) {
      if (u.toString().charAt(0) == unitChar) {
        selectedUnit = u;
        break;
      }
    }
    if (selectedUnit == null) {
      selectedUnit = isInternalStorage ? Storage.Unit.B : Storage.Unit.MiB; // Values expressed without a unit read as B for internal storage
      numString = iniString;
    }
    try {
      long numLong = Long.parseLong(numString);
      return new Storage(numLong, selectedUnit);
    }
    catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * Encode the given value as a string that can be placed in the AVD's INI file.
   */
  @NotNull
  private static String toIniString(@NotNull Double value) {
    return String.format(Locale.US, "%f", value);
  }

  /**
   * Encode the given value as a string that can be placed in the AVD's INI file.
   */
  @NotNull
  private static String toIniString(@NotNull File value) {
    return value.getPath();
  }

  /**
   * Encode the given value as a string that can be placed in the AVD's INI file.
   * Example: 10M or 1G
   */
  @NotNull
  public static String toIniString(@NotNull Storage storage, boolean convertToMb) {
    Storage.Unit unit = convertToMb ? Storage.Unit.MiB : storage.getAppropriateUnits();
    String unitString = convertToMb ? "" : unit.toString().substring(0, 1);
    return String.format(Locale.US, "%1$d%2$s", storage.getSizeAsUnit(unit), unitString);
  }

  /**
   * Encode the given value as a string that can be placed in the AVD's INI file.
   */
  @NotNull
  private static String toIniString(@NotNull Boolean b) {
    return b ? "yes" : "no";
  }

  /**
   * Decode the given value from an AVD's INI file.
   */
  private static boolean fromIniString(@Nullable String s) {
    return "yes".equals(s);
  }

  @NotNull
  private static String calculateAvdName(@Nullable AvdInfo avdInfo,
                                         @NotNull Map<String, String> hardwareProperties,
                                         @NotNull Device device) {
    if (avdInfo != null) {
      return avdInfo.getName();
    }
    String candidateBase = hardwareProperties.get(AVD_INI_DISPLAY_NAME);
    if (candidateBase == null || candidateBase.isEmpty()) {
      String deviceName = device.getDisplayName().replace(' ', '_');
      String manufacturer = device.getManufacturer().replace(' ', '_');
      candidateBase = String.format("AVD_for_%1$s_by_%2$s", deviceName, manufacturer);
    }
    return AvdWizardUtils.cleanAvdName(AvdManagerConnection.getDefaultAvdManagerConnection(), candidateBase, true);
  }

  @NotNull
  public BoolProperty useQemu2() {
    return myUseQemu2;
  }

  @NotNull
  public StringProperty avdId() {
    return myAvdId;
  }

  @NotNull
  public StringProperty avdDisplayName() {
    return myAvdDisplayName;
  }

  @NotNull
  public ObjectProperty<ScreenOrientation> selectedAvdOrientation() {
    return mySelectedAvdOrientation;
  }

  @NotNull
  public ObjectProperty<AvdCamera> selectedFrontCamera() {
    return mySelectedAvdFrontCamera;
  }

  @NotNull
  public ObjectProperty<AvdCamera> selectedBackCamera() {
    return mySelectedAvdBackCamera;
  }

  @NotNull
  public BoolProperty hasDeviceFrame() {
    return myHasDeviceFrame;
  }

  @NotNull
  public ObjectProperty<AvdNetworkSpeed> selectedNetworkSpeed() {
    return mySelectedNetworkSpeed;
  }

  @NotNull
  public ObjectProperty<AvdNetworkLatency> selectedNetworkLatency() {
    return mySelectedNetworkLatency;
  }

  @NotNull
  public ObjectProperty<Storage> internalStorage() {
    return myInternalStorage;
  }

  @NotNull
  public BoolProperty useExternalSdCard() {
    return myUseExternalSdCard;
  }

  @NotNull
  public BoolProperty useBuiltInSdCard() {
    return myUseBuiltInSdCard;
  }

  @NotNull
  public StringProperty systemImageName() {
    return mySystemImageName;
  }

  @NotNull
  public StringProperty systemImageDetails() {
    return mySystemImageDetails;
  }

  @NotNull
  public ObjectProperty<Storage> vmHeapStorage() {
    return myVmHeapStorage;
  }

  @NotNull
  public OptionalProperty<Integer> cpuCoreCount() {
    return myCpuCoreCount;
  }

  @NotNull
  public OptionalProperty<Storage> sdCardStorage() {
    return mySdCardStorage;
  }

  @NotNull
  public StringProperty externalSdCardLocation() {
    return myExternalSdCardLocation;
  }

  @NotNull
  public BoolProperty useHostGpu() {
    return myUseHostGpu;
  }

  @NotNull
  public BoolProperty useColdBoot() {
    return myColdBoot;
  }

  @NotNull
  public BoolProperty useFastBoot() {
    return myFastBoot;
  }

  @NotNull
  public BoolProperty useChosenSnapshotBoot() {
    return myChosenSnapshotBoot;
  }

  @NotNull
  public StringProperty chosenSnapshotFile() {
    return myChosenSnapshotFile;
  }

  @NotNull
  public OptionalProperty<GpuMode> hostGpuMode() {
    return myHostGpuMode;
  }

  @NotNull
  public BoolProperty enableHardwareKeyboard(){
    return myEnableHardwareKeyboard;
  }

  @NotNull
  public OptionalProperty<File> backupSkinFile() {
    return myBackupSkinFile;
  }

  @NotNull
  public OptionalProperty<Device> device() {
    return myDevice;
  }

  @NotNull
  public BoolProperty isInEditMode() {
    return myIsInEditMode;
  }

  @NotNull
  public OptionalProperty<SystemImageDescription> systemImage() {
    return mySystemImage;
  }

  @NotNull
  public AvdDeviceData getAvdDeviceData() {
    return myAvdDeviceData;
  }

  private void updateValuesWithAvdInfo(@NotNull AvdInfo avdInfo) {
    List<Device> devices = DeviceManagerConnection.getDefaultDeviceManagerConnection().getDevices();
    Device selectedDevice = null;
    String manufacturer = avdInfo.getDeviceManufacturer();
    String deviceId = avdInfo.getDeviceName();
    for (Device device : devices) {
      if (manufacturer.equals(device.getManufacturer()) && deviceId.equals(device.getId())) {
        selectedDevice = device;
        break;
      }
    }

    myDevice.setNullableValue(selectedDevice);
    SystemImageDescription systemImageDescription = null;
    ISystemImage selectedImage = avdInfo.getSystemImage();
    if (selectedImage != null) {
      systemImageDescription = new SystemImageDescription(selectedImage);
      mySystemImage.setValue(systemImageDescription);
    }
    myAvdDeviceData = new AvdDeviceData(selectedDevice, systemImageDescription);

    Map<String, String> properties = avdInfo.getProperties();

    myUseQemu2.set(properties.containsKey(AvdWizardUtils.CPU_CORES_KEY));
    String cpuCoreCount = properties.get(AvdWizardUtils.CPU_CORES_KEY);
    myCpuCoreCount.setValue(cpuCoreCount==null ? 1 : Integer.parseInt(cpuCoreCount));

    Storage storage = getStorageFromIni(properties.get(AvdWizardUtils.RAM_STORAGE_KEY), false);
    if (storage != null) {
      myAvdDeviceData.ramStorage().set(storage);
    }
    storage = getStorageFromIni(properties.get(AvdWizardUtils.VM_HEAP_STORAGE_KEY), false);
    if (storage != null) {
      myVmHeapStorage.set(storage);
    }
    storage = getStorageFromIni(properties.get(AvdWizardUtils.INTERNAL_STORAGE_KEY), true);
    if (storage != null) {
      myInternalStorage.set(storage);
    }

    // check if avd has sdcard first, then decided the kind of sdcard
    if (properties.getOrDefault(HardwareProperties.HW_SDCARD, toIniString(true)).equals(toIniString(true))) {
      String sdCardLocation = null;
      if (properties.get(AvdWizardUtils.EXISTING_SD_LOCATION) != null) {
        sdCardLocation = properties.get(AvdWizardUtils.EXISTING_SD_LOCATION);
      }
      else if (properties.get(AvdWizardUtils.SD_CARD_STORAGE_KEY) != null) {
        sdCardLocation = FileUtil.join(avdInfo.getDataFolderPath(), "sdcard.img");
      }
      existingSdLocation = new StringValueProperty(nullToEmpty(sdCardLocation));

      String dataFolderPath = avdInfo.getDataFolderPath();
      File sdLocationFile = null;
      if (sdCardLocation != null) {
        sdLocationFile = new File(sdCardLocation);
      }
      if (sdLocationFile != null) {
        if (Objects.equal(sdLocationFile.getParent(), dataFolderPath)) {
          // the image is in the AVD folder, consider it to be internal
          File sdFile = new File(sdCardLocation);
          Storage sdCardSize = new Storage(sdFile.length());
          myUseExternalSdCard.set(false);
          myUseBuiltInSdCard.set(true);
          myOriginalSdCard = new ObjectValueProperty<>(sdCardSize);
          mySdCardStorage.setValue(sdCardSize);
        }
        else {
          // the image is external
          myUseExternalSdCard.set(true);
          myUseBuiltInSdCard.set(false);
          externalSdCardLocation().set(sdCardLocation);
        }
      }
    } else {//if avd doesn't have sdcard set it to no sdcard
      myUseExternalSdCard.set(false);
      myUseBuiltInSdCard.set(false);
    }

    myUseHostGpu.set(fromIniString(properties.get(AvdWizardUtils.USE_HOST_GPU_KEY)));
    mySelectedAvdFrontCamera.set(AvdCamera.fromName(properties.get(AvdWizardUtils.FRONT_CAMERA_KEY)));
    mySelectedAvdBackCamera.set(AvdCamera.fromName(properties.get(AvdWizardUtils.BACK_CAMERA_KEY)));
    mySelectedNetworkLatency.set(AvdNetworkLatency.fromName(properties.get(AvdWizardUtils.NETWORK_LATENCY_KEY)));
    mySelectedNetworkSpeed.set(AvdNetworkSpeed.fromName(properties.get(AvdWizardUtils.NETWORK_SPEED_KEY)));
    myEnableHardwareKeyboard.set(fromIniString(properties.get(AvdWizardUtils.HAS_HARDWARE_KEYBOARD_KEY)));
    myAvdDisplayName.set(avdInfo.getDisplayName());
    myHasDeviceFrame.set(fromIniString(properties.get(AvdWizardUtils.DEVICE_FRAME_KEY)));
    myColdBoot.set(fromIniString(properties.get(AvdWizardUtils.USE_COLD_BOOT)));
    myFastBoot.set(fromIniString(properties.get(AvdWizardUtils.USE_FAST_BOOT)));
    myChosenSnapshotBoot.set(fromIniString(properties.get(AvdWizardUtils.USE_CHOSEN_SNAPSHOT_BOOT)));
    final String chosenFile = properties.get(AvdWizardUtils.CHOSEN_SNAPSHOT_FILE);
    myChosenSnapshotFile.set(StringUtil.notNullize(chosenFile));

    ScreenOrientation screenOrientation = null;
    String orientation = properties.get(HardwareProperties.HW_INITIAL_ORIENTATION);
    if (!Strings.isNullOrEmpty(orientation)) {
      screenOrientation = ScreenOrientation.getByShortDisplayName(orientation);
    }
    mySelectedAvdOrientation.set((screenOrientation == null) ? ScreenOrientation.PORTRAIT : screenOrientation);

    String skinPath = properties.get(AvdWizardUtils.CUSTOM_SKIN_FILE_KEY);
    if (skinPath != null) {
      File skinFile = (skinPath.equals(AvdWizardUtils.NO_SKIN.getPath())) ? AvdWizardUtils.NO_SKIN : new File(skinPath);

      if (skinFile.isDirectory() || FileUtil.filesEqual(skinFile, AvdWizardUtils.NO_SKIN)) {
        myAvdDeviceData.customSkinFile().setValue(skinFile);
      }
    }
    String backupSkinPath = properties.get(AvdWizardUtils.BACKUP_SKIN_FILE_KEY);
    if (backupSkinPath != null) {
      File skinFile = new File(backupSkinPath);
      if (skinFile.isDirectory() || FileUtil.filesEqual(skinFile, AvdWizardUtils.NO_SKIN)) {
        backupSkinFile().setValue(skinFile);
      }
    }
    String modeString = properties.get(AvdWizardUtils.HOST_GPU_MODE_KEY);
    myHostGpuMode.setValue(GpuMode.fromGpuSetting(modeString));

    myIsInEditMode.set(true);
  }

  /**
   * Set the initial internal storage size and sd card storage size, using values from hardware-properties.ini
   */
  private void updateValuesFromHardwareProperties() {
    AvdManagerConnection conn = AvdManagerConnection.getDefaultAvdManagerConnection();
    Storage storage = getStorageFromIni(conn.getSdCardSizeFromHardwareProperties(), false);
    if (storage != null) {
      mySdCardStorage.setValue(storage);
    }
    storage = getStorageFromIni(conn.getInternalStorageSizeFromHardwareProperties(), true);
    // TODO (b/65811265) Currently, internal storage size in hardware-properties.ini is defaulted
    // to 0. In this case, We will skip this default value. When the hardware-properties.ini is
    // updated, we will delete the redundant value check.
    if (storage != null && storage.getSize() != 0) {
      myInternalStorage.set(storage);
    }
  }

  /**
   * Returns a map containing all of the properties editable on this wizard to be passed on to the AVD prior to serialization
   */
  private Map<String, Object> generateUserEditedPropertiesMap() {
    HashMap<String, Object> map = new HashMap<>();
    map.put(AvdWizardUtils.DEVICE_DEFINITION_KEY, myDevice);
    map.put(AvdWizardUtils.SYSTEM_IMAGE_KEY, mySystemImage);
    map.put(AvdWizardUtils.AVD_ID_KEY, myAvdId.get());
    map.put(AvdWizardUtils.VM_HEAP_STORAGE_KEY, myVmHeapStorage.get());
    map.put(AvdWizardUtils.DISPLAY_NAME_KEY, myAvdDisplayName.get());
    map.put(AvdWizardUtils.DEFAULT_ORIENTATION_KEY, mySelectedAvdOrientation.get());
    map.put(AvdWizardUtils.RAM_STORAGE_KEY, myAvdDeviceData.ramStorage().get());
    map.put(AvdWizardUtils.IS_IN_EDIT_MODE_KEY, myIsInEditMode.get());
    map.put(AvdWizardUtils.HAS_HARDWARE_KEYBOARD_KEY, myEnableHardwareKeyboard.get());
    map.put(HardwareProperties.HW_INITIAL_ORIENTATION, mySelectedAvdOrientation.get().getShortDisplayValue());
    map.put(AvdWizardUtils.USE_HOST_GPU_KEY, myUseHostGpu.get());
    map.put(AvdWizardUtils.DEVICE_FRAME_KEY, myHasDeviceFrame.get());
    map.put(AvdWizardUtils.HOST_GPU_MODE_KEY, myHostGpuMode.getValue());
    map.put(AvdWizardUtils.USE_COLD_BOOT, myColdBoot.get());
    map.put(AvdWizardUtils.USE_FAST_BOOT, myFastBoot.get());
    map.put(AvdWizardUtils.USE_CHOSEN_SNAPSHOT_BOOT, myChosenSnapshotBoot.get());
    map.put(AvdWizardUtils.CHOSEN_SNAPSHOT_FILE, myChosenSnapshotFile.get());

    if (myUseQemu2.get()) {
      if (myCpuCoreCount.get().isPresent()) {
        map.put(AvdWizardUtils.CPU_CORES_KEY, myCpuCoreCount.getValue());
      }
      else {
        // Force the use the new emulator (qemu2)
        map.put(AvdWizardUtils.CPU_CORES_KEY, 1);
      }
    }
    else {
      // Do NOT use the new emulator (qemu2)
      map.remove(AvdWizardUtils.CPU_CORES_KEY);
    }

    if (myOriginalSdCard != null) {
      map.put(AvdWizardUtils.SD_CARD_STORAGE_KEY, myOriginalSdCard);
    }

    if (!Strings.isNullOrEmpty(existingSdLocation.get())) {
      map.put(AvdWizardUtils.EXISTING_SD_LOCATION, existingSdLocation.get());
    }
    if (!Strings.isNullOrEmpty(myExternalSdCardLocation.get())) {
      map.put(AvdWizardUtils.EXISTING_SD_LOCATION, myExternalSdCardLocation.get());
      map.put(AvdWizardUtils.DISPLAY_SD_LOCATION_KEY, myExternalSdCardLocation.get());
    }
    map.put(AvdWizardUtils.DISPLAY_USE_EXTERNAL_SD_KEY, myUseExternalSdCard.get());
    map.put(AvdWizardUtils.INTERNAL_STORAGE_KEY, myInternalStorage.get());
    map.put(AvdWizardUtils.NETWORK_SPEED_KEY, mySelectedNetworkSpeed.get().getAsParameter());
    map.put(AvdWizardUtils.NETWORK_LATENCY_KEY, mySelectedNetworkLatency.get().getAsParameter());
    map.put(AvdWizardUtils.FRONT_CAMERA_KEY, myAvdDeviceData.hasFrontCamera().get() ? mySelectedAvdFrontCamera.get().getAsParameter()
                                                                                    : AvdCamera.NONE);
    map.put(AvdWizardUtils.BACK_CAMERA_KEY, myAvdDeviceData.hasBackCamera().get() ? mySelectedAvdBackCamera.get().getAsParameter()
                                                                                   : AvdCamera.NONE);

    if(myAvdDeviceData.customSkinFile().get().isPresent()){
      map.put(AvdWizardUtils.CUSTOM_SKIN_FILE_KEY, myAvdDeviceData.customSkinFile().getValue());
    }

    if (myBackupSkinFile.get().isPresent()) {
      map.put(AvdWizardUtils.BACKUP_SKIN_FILE_KEY, myBackupSkinFile.getValue());
    }

    if (mySdCardStorage.get().isPresent()) {
      map.put(AvdWizardUtils.DISPLAY_SD_SIZE_KEY, mySdCardStorage.getValue());
    }
    return map;
  }

  @Override
  public void handleFinished() {
    // By this point we should have both a Device and a SystemImage
    Device device = myDevice.getValue();
    SystemImageDescription systemImage = mySystemImage.getValue();

    Map<String, String> hardwareProperties = DeviceManager.getHardwareProperties(device);
    Map<String, Object> userEditedProperties = generateUserEditedPropertiesMap();

    String sdCard = null;
    boolean hasSdCard = false;
    if (myUseExternalSdCard.get()) {
      sdCard = myExternalSdCardLocation.get();
      // Remove SD card storage size because it will use external file
      userEditedProperties.remove(AvdWizardUtils.SD_CARD_STORAGE_KEY);
      hasSdCard = true;
    } else if (myUseBuiltInSdCard.get()){
      if (sdCardStorage().get().isPresent() && myOriginalSdCard != null && sdCardStorage().getValue().equals(myOriginalSdCard.get())) {
        // unchanged, use existing card
        sdCard = existingSdLocation.get();
        hasSdCard = true;
      } else {
        // Remove existing sd card because we will create a new one
        userEditedProperties.remove(AvdWizardUtils.EXISTING_SD_LOCATION);
        Storage storage = null;
        myOriginalSdCard = new ObjectValueProperty<>(mySdCardStorage.getValue());
        if (mySdCardStorage.get().isPresent()) {
          storage = mySdCardStorage.getValue();
          sdCard = toIniString(storage, false);
        }
        hasSdCard = storage != null && storage.getSize() > 0;
      }
    } else {
      hasSdCard = false;
      // Remove existing sd card, since device doesn't have sdcard
      userEditedProperties.remove(AvdWizardUtils.EXISTING_SD_LOCATION);
    }

    hardwareProperties.put(HardwareProperties.HW_SDCARD, toIniString(hasSdCard));
    // Remove any internal keys from the map
    userEditedProperties = Maps.filterEntries(
      userEditedProperties,
      input -> !input.getKey().startsWith(AvdWizardUtils.WIZARD_ONLY) && input.getValue() != null);

    // Call toIniString() on all remaining values
    hardwareProperties.putAll(Maps.transformEntries(userEditedProperties, (key, value) -> {
      if (value instanceof Storage) {
        if (key.equals(AvdWizardUtils.RAM_STORAGE_KEY) || key.equals(AvdWizardUtils.VM_HEAP_STORAGE_KEY)) {
          return toIniString((Storage)value, true);
        }
        else {
          return toIniString((Storage)value, false);
        }
      }
      else if (value instanceof Boolean) {
        return toIniString((Boolean)value);
      }
      else if (value instanceof File) {
        return toIniString((File)value);
      }
      else if (value instanceof Double) {
        return toIniString((Double)value);
      }
      else if (value instanceof GpuMode) {
        GpuMode gpuMode = (GpuMode)value;
        if (gpuMode == GpuMode.SWIFT &&
            !AvdManagerConnection.getDefaultAvdManagerConnection().
              emulatorVersionIsAtLeast(new Revision(27, 1, 6))) {
          // Older Emulator versions expect "guest" when SWIFT is selected on the UI
          return "guest";
        }
        return gpuMode.getGpuSetting();
      }
      else {
        return value.toString();
      }
    }));

    File skinFile = (myAvdDeviceData.customSkinFile().get().isPresent())
                    ? myAvdDeviceData.customSkinFile().getValue()
                    : AvdWizardUtils.pathToUpdatedSkins(device.getDefaultHardware().getSkinFile().toPath(), systemImage);

    if (myBackupSkinFile.get().isPresent()) {
      hardwareProperties.put(AvdManager.AVD_INI_BACKUP_SKIN_PATH, myBackupSkinFile.getValue().getPath());
    }

    // Add defaults if they aren't already set differently
    if (!hardwareProperties.containsKey(AvdManager.AVD_INI_SKIN_DYNAMIC)) {
      hardwareProperties.put(AvdManager.AVD_INI_SKIN_DYNAMIC, toIniString(true));
    }
    if (!hardwareProperties.containsKey(HardwareProperties.HW_KEYBOARD)) {
      hardwareProperties.put(HardwareProperties.HW_KEYBOARD, toIniString(false));
    }

    boolean isCircular = myAvdDeviceData.isScreenRound().get();

    String tempAvdName = myAvdId.get();
    final String avdName = tempAvdName.isEmpty()
                           ? calculateAvdName(myAvdInfo, hardwareProperties, device)
                           : tempAvdName;

    // If we're editing an AVD and we downgrade a system image, wipe the user data with confirmation
    if (myAvdInfo != null) {
      ISystemImage image = myAvdInfo.getSystemImage();
      if (image != null) {
        int oldApiLevel = image.getAndroidVersion().getFeatureLevel();
        int newApiLevel = systemImage.getVersion().getFeatureLevel();
        final String oldApiName = image.getAndroidVersion().getApiString();
        final String newApiName = systemImage.getVersion().getApiString();
        if (oldApiLevel > newApiLevel ||
            (oldApiLevel == newApiLevel && image.getAndroidVersion().isPreview() && !systemImage.getVersion().isPreview())) {
          final AtomicReference<Boolean> shouldContinue = new AtomicReference<>();
          ApplicationManager.getApplication().invokeAndWait(() -> {
            String message =
              String.format(Locale.getDefault(), "You are about to downgrade %1$s from API level %2$s to API level %3$s.\n" +
                                                 "This requires a wipe of the userdata partition of the AVD.\nDo you wish to " +
                                                 "continue with the data wipe?", avdName, oldApiName, newApiName);
            int result = Messages.showYesNoDialog((Project)null, message, "Confirm Data Wipe", AllIcons.General.QuestionDialog);
            shouldContinue.set(result == Messages.YES);
          }, ModalityState.any());
          if (shouldContinue.get()) {
            AvdManagerConnection.getDefaultAvdManagerConnection().wipeUserData(myAvdInfo);
          }
          else {
            return;
          }
        }
      }
    }

    final String sdCardFinal = sdCard;
    AvdManagerConnection connection = AvdManagerConnection.getDefaultAvdManagerConnection();

    ProgressManager.getInstance().runProcessWithProgressSynchronously(
      () -> {
        myCreatedAvd = connection.createOrUpdateAvd(
          myAvdInfo, avdName, device, systemImage, mySelectedAvdOrientation.get(),
          isCircular, sdCardFinal, skinFile == null ? null : skinFile.toPath(), hardwareProperties, myRemovePreviousAvd.get());
        if (myAvdCreatedCallback != null) {
          myAvdCreatedCallback.run();
        }
      },
      "Creating Android Virtual Device", false, null
    );

    if (myCreatedAvd == null) {
      ApplicationManager.getApplication().invokeAndWait(() -> Messages.showErrorDialog(
        (Project)null, "An error occurred while creating the AVD. See idea.log for details.", "Error Creating AVD"), ModalityState.any());
    }
  }

  @NotNull
  public AvdInfo getCreatedAvd() {
    return myCreatedAvd;
  }

  @Nullable
  public String getAvdLocation() {
    return (myAvdInfo == null) ? null : myAvdInfo.getDataFolderPath();
  }
}
