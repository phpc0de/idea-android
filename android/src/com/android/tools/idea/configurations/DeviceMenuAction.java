/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.android.tools.idea.configurations;

import com.android.sdklib.devices.Device;
import com.android.sdklib.devices.State;
import com.android.tools.adtui.actions.DropDownAction;
import com.android.tools.adtui.device.DeviceArtPainter;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import icons.StudioIcons;
import org.jetbrains.android.actions.RunAndroidAvdManagerAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

import static com.android.ide.common.rendering.HardwareConfigHelper.*;

public class DeviceMenuAction extends DropDownAction {
  private static final boolean LIST_RECENT_DEVICES = false;
  private final ConfigurationHolder myRenderContext;

  public DeviceMenuAction(@NotNull ConfigurationHolder renderContext) {
    super("Device for Preview", "Device for Preview", StudioIcons.LayoutEditor.Toolbar.VIRTUAL_DEVICES);
    myRenderContext = renderContext;
    Presentation presentation = getTemplatePresentation();
    updatePresentation(presentation);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    updatePresentation(e.getPresentation());
  }

  @Override
  public boolean displayTextInToolbar() {
    return true;
  }

  private void updatePresentation(Presentation presentation) {
    Configuration configuration = myRenderContext.getConfiguration();
    boolean visible = configuration != null;
    if (visible) {
      Device device = configuration.getCachedDevice();
      String label = getDeviceLabel(device, true);
      presentation.setText(label, false);
    }
    if (visible != presentation.isVisible()) {
      presentation.setVisible(visible);
    }
  }

  /**
   * Returns a suitable label to use to display the given device
   *
   * @param device the device to produce a label for
   * @param brief  if true, generate a brief label (suitable for a toolbar
   *               button), otherwise a fuller name (suitable for a menu item)
   * @return the label
   */
  public static String getDeviceLabel(@Nullable Device device, boolean brief) {
    if (device == null) {
      return "";
    }
    String name = device.getDisplayName();

    if (brief) {
      // Produce a really brief summary of the device name, suitable for
      // use in the narrow space available in the toolbar for example
      int nexus = name.indexOf("Nexus"); //$NON-NLS-1$
      if (nexus != -1) {
        int begin = name.indexOf('(');
        if (begin != -1) {
          begin++;
          int end = name.indexOf(')', begin);
          if (end != -1) {
            if (name.equals("Nexus 7 (2012)")) {
              return "Nexus 7";
            }
            else {
              return name.substring(begin, end).trim();
            }
          }
        }
      }

      String skipPrefix = "Android ";
      name = StringUtil.trimStart(name, skipPrefix);
    }

    return name;
  }

  private static Icon getDeviceClassIcon(@Nullable Device device) {
    if (device != null) {
      if (isWear(device)) {
        return StudioIcons.LayoutEditor.Toolbar.DEVICE_WEAR;
      }
      else if (isTv(device)) {
        return StudioIcons.LayoutEditor.Toolbar.DEVICE_TV;
      }
      else if (isAutomotive(device)) {
        return StudioIcons.LayoutEditor.Toolbar.DEVICE_AUTOMOTIVE;
      }

      // Glass not yet in the device list

      if (DeviceArtPainter.isTablet(device)) {
        return StudioIcons.LayoutEditor.Toolbar.DEVICE_TABLET;
      }
    }

    return StudioIcons.LayoutEditor.Toolbar.DEVICE_PHONE;
  }

  @Override
  protected boolean updateActions(@NotNull DataContext context) {
    removeAll();
    Configuration configuration = myRenderContext.getConfiguration();
    if (configuration == null) {
      return true;
    }
    Device current = configuration.getCachedDevice();
    ConfigurationManager configurationManager = configuration.getConfigurationManager();

    if (LIST_RECENT_DEVICES) {
      List<Device> recent = configurationManager.getDevices();
      if (recent.size() > 1) {
        for (Device device : recent) {
          String label = getLabel(device, isNexus(device));
          Icon icon = getDeviceClassIcon(device);
          add(new SetDeviceAction(myRenderContext, label, device, icon, device == current));
        }
        addSeparator();
      }
    }

    createDeviceMenuList(configuration, current);

    return true;
  }

  private void createDeviceMenuList(@NotNull Configuration configuration, @Nullable Device currentDevice) {
    Map<DeviceGroup, List<Device>> groupedDevices = DeviceUtils.getSuitableDevices(configuration);

    // We don't add DeviceGroup.NEXUS because all Nexus devices with small screen size are legacy devices.
    addDeviceSection(groupedDevices, DeviceGroup.NEXUS_XL, currentDevice);
    addDeviceSection(groupedDevices, DeviceGroup.NEXUS_TABLET, currentDevice);
    addDeviceSection(groupedDevices, DeviceGroup.WEAR, currentDevice);
    addDeviceSection(groupedDevices, DeviceGroup.TV, currentDevice);
    addDeviceSection(groupedDevices, DeviceGroup.AUTOMOTIVE, currentDevice);
    addCustomDeviceSection(currentDevice);
    addAvdDeviceSection(DeviceUtils.getAvdDevices(configuration), currentDevice);
    addGenericDeviceSection(groupedDevices.getOrDefault(DeviceGroup.GENERIC, Collections.emptyList()), currentDevice);
    add(ActionManager.getInstance().getAction(RunAndroidAvdManagerAction.ID));
  }

  private void addDeviceSection(@NotNull Map<DeviceGroup, List<Device>> groupedDevices,
                                @NotNull DeviceGroup group,
                                @Nullable Device current) {
    List<Device> devices = groupedDevices.getOrDefault(group, Collections.emptyList());
    if (!devices.isEmpty()) {
      add(new DeviceCategory(getGroupTitle(group), null, getDeviceClassIcon(devices.get(0))));
      for (final Device device : devices) {
        String label = getLabel(device, isNexus(device));
        add(new SetDeviceAction(myRenderContext, label, device, null, current == device));
      }
      addSeparator();
    }
  }

  @NotNull
  private static String getGroupTitle(@NotNull DeviceGroup group) {
    switch (group) {
      case NEXUS:
      case NEXUS_XL:
        return "Phone";
      case NEXUS_TABLET:
        return "Tablet";
      case WEAR:
        return "Wear";
      case TV:
        return "TV";
      case AUTOMOTIVE:
        return "Automotive";
      case GENERIC:
        return "Generic";
      case OTHER:
        return "Other";
      default:
        return "Device";
    }
  }

  private void addCustomDeviceSection(@Nullable Device currentDevice) {
    add(new SetCustomDeviceAction(myRenderContext, currentDevice));
    addSeparator();
  }

  private void addAvdDeviceSection(@NotNull List<Device> devices, @Nullable Device current) {
    if (!devices.isEmpty()) {
      add(new DeviceCategory("Virtual Device", null, StudioIcons.LayoutEditor.Toolbar.VIRTUAL_DEVICES));
      for (final Device device : devices) {
        boolean selected = current != null && current.getId().equals(device.getId());

        String avdDisplayName = "AVD: " + device.getDisplayName();
        add(new SetAvdAction(myRenderContext, device, avdDisplayName, selected));
      }
      addSeparator();
    }
  }

  private void addGenericDeviceSection(@NotNull List<Device> devices, @Nullable Device current) {
    if (!devices.isEmpty()) {
      DefaultActionGroup genericGroup = DefaultActionGroup.createPopupGroup(() -> "_Generic Phones and Tablets");
      for (final Device device : devices) {
        String label = getLabel(device, isNexus(device));
        genericGroup.add(new SetDeviceAction(myRenderContext, label, device, null, current == device));
      }
      add(genericGroup);
    }
  }

  @NotNull
  public static ImmutableList<Device> getSortedDevicesInMenu(@NotNull Configuration configuration) {
    Map<DeviceGroup, List<Device>> groupedDevices = DeviceUtils.getSuitableDevices(configuration);

    // TODO: Refactor to have same device order as #createDeviceMenuList() function.
    ImmutableList.Builder<Device> builder = new ImmutableList.Builder<>();
    builder.addAll(groupedDevices.getOrDefault(DeviceGroup.NEXUS_XL, Collections.emptyList()));
    builder.addAll(groupedDevices.getOrDefault(DeviceGroup.NEXUS_TABLET, Collections.emptyList()));
    builder.addAll(groupedDevices.getOrDefault(DeviceGroup.WEAR, Collections.emptyList()));
    builder.addAll(groupedDevices.getOrDefault(DeviceGroup.TV, Collections.emptyList()));
    builder.addAll(groupedDevices.getOrDefault(DeviceGroup.AUTOMOTIVE, Collections.emptyList()));
    builder.addAll(DeviceUtils.getAvdDevices(configuration));
    builder.addAll(groupedDevices.getOrDefault(DeviceGroup.GENERIC, Collections.emptyList()));

    return builder.build();
  }

  private String getLabel(Device device, boolean isNexus) {
    // See if there is a better match, and if so, display it in the menu action
    Configuration configuration = myRenderContext.getConfiguration();
    if (configuration != null) {
      VirtualFile better = ConfigurationMatcher.getBetterMatch(configuration, device, null, null, null);
      if (better != null) {
        return ConfigurationAction.getBetterMatchLabel(device.getDisplayName(), better, configuration.getFile());
      }
    }

    return isNexus ? getNexusMenuLabel(device) : getGenericLabel(device);
  }

  private static final class DeviceCategory extends AnAction {

    private Icon myIcon;

    public DeviceCategory(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
      super(text, description, null);
      myIcon = icon;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      Presentation p = e.getPresentation();
      p.setEnabled(false);
      p.setDisabledIcon(myIcon);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      // Always disable, do nothing
    }
  }

  protected abstract class DeviceAction extends ConfigurationAction {

    DeviceAction(@NotNull ConfigurationHolder renderContext,
                 @Nullable String title) {
      super(renderContext, title);
    }

    @Override
    protected final void updatePresentation(@NotNull Presentation presentation) {
      DeviceMenuAction.this.updatePresentation(presentation);
    }

    @Nullable
    abstract public Device getDevice();
  }

  private class SetDeviceAction extends DeviceAction {
    private final Device myDevice;

    public SetDeviceAction(@NotNull ConfigurationHolder renderContext,
                           @NotNull final String title,
                           @NotNull final Device device,
                           @Nullable Icon defaultIcon,
                           final boolean select) {
      super(renderContext, null);
      myDevice = device;
      // The name of AVD device may contain underline character, but they should not be recognized as the mnemonic.
      getTemplatePresentation().setText(title, false);
      if (select) {
        getTemplatePresentation().putClientProperty(SELECTED_PROPERTY, true);
      }
      else if (ConfigurationAction.isBetterMatchLabel(title)) {
        getTemplatePresentation().setIcon(ConfigurationAction.getBetterMatchIcon());
      }
      else if (defaultIcon != null) {
        getTemplatePresentation().setIcon(defaultIcon);
      }
    }

    @Override
    protected void updateConfiguration(@NotNull Configuration configuration, boolean commit) {
      // Attempt to jump to the default orientation of the new device; for example, if you're viewing a layout in
      // portrait orientation on a Nexus 4 (its default), and you switch to a Nexus 10, we jump to landscape orientation
      // (its default) unless of course there is a different layout that is the best fit for that device.
      Device prevDevice = configuration.getCachedDevice();
      State prevState = configuration.getDeviceState();
      String newState = prevState != null ? prevState.getName() : null;
      if (prevDevice != null && prevState != null && prevState.isDefaultState() &&
          !myDevice.getDefaultState().getName().equals(prevState.getName()) &&
          configuration.getEditedConfig().getScreenOrientationQualifier() == null) {
        VirtualFile file = configuration.getFile();
        if (file != null) {
          String name = myDevice.getDefaultState().getName();
          if (ConfigurationMatcher.getBetterMatch(configuration, myDevice, name, null, null) == null) {
            newState = name;
          }
        }
      }

      if (newState != null) {
        configuration.setDeviceStateName(newState);
      }
      if (commit) {
        configuration.getConfigurationManager().selectDevice(myDevice);
      }
      configuration.setDevice(myDevice, true);
    }

    @NotNull
    @Override
    public Device getDevice() {
      return myDevice;
    }
  }

  private class SetCustomDeviceAction extends DeviceAction {
    private static final String CUSTOM_DEVICE_NAME = "Custom";
    @Nullable private final Device myDevice;
    @Nullable private Device myCustomDevice;

    public SetCustomDeviceAction(@NotNull ConfigurationHolder renderContext, @Nullable Device device) {
      super(renderContext, CUSTOM_DEVICE_NAME);
      myDevice = device;
      if (myDevice != null && Configuration.CUSTOM_DEVICE_ID.equals(myDevice.getId())) {
        getTemplatePresentation().putClientProperty(SELECTED_PROPERTY, true);
      }
    }

    @Override
    protected void updateConfiguration(@NotNull Configuration configuration, boolean commit) {
      if (myDevice != null) {
        Device.Builder customBuilder = new Device.Builder(myDevice);
        customBuilder.setTagId(myDevice.getTagId());
        customBuilder.setName(CUSTOM_DEVICE_NAME);
        customBuilder.setId(Configuration.CUSTOM_DEVICE_ID);
        myCustomDevice = customBuilder.build();
        configuration.setEffectiveDevice(myCustomDevice, myDevice.getDefaultState());
      }
    }

    @Nullable
    @Override
    public Device getDevice() {
      return myCustomDevice;
    }
  }

  private class SetAvdAction extends ConfigurationAction {
    @NotNull private final Device myAvdDevice;

    public SetAvdAction(@NotNull ConfigurationHolder renderContext,
                        @NotNull Device avdDevice,
                        @NotNull String displayName,
                        final boolean select) {
      super(renderContext, displayName);
      myAvdDevice = avdDevice;
      getTemplatePresentation().putClientProperty(SELECTED_PROPERTY, select);
    }

    @Override
    protected void updatePresentation(@NotNull Presentation presentation) {
      DeviceMenuAction.this.updatePresentation(presentation);
    }

    @Override
    protected void updateConfiguration(@NotNull Configuration configuration, boolean commit) {
      configuration.setEffectiveDevice(myAvdDevice, myAvdDevice.getDefaultState());
    }
  }
}
