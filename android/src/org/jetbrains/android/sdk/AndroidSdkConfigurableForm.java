// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.android.sdk;

import com.android.sdklib.IAndroidTarget;
import com.android.tools.idea.io.FilePaths;
import com.android.tools.idea.sdk.AndroidSdks;
import com.android.tools.idea.sdk.IdeSdks;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModel;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.ui.OrderRoot;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.util.ArrayUtil;
import java.awt.event.ItemEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class AndroidSdkConfigurableForm {
  private JComboBox myInternalJdkComboBox;
  private JPanel myContentPanel;
  private JComboBox<IAndroidTarget> myBuildTargetComboBox;

  private final DefaultComboBoxModel myJdksModel = new DefaultComboBoxModel();
  private final SdkModel mySdkModel;

  private final DefaultComboBoxModel myBuildTargetsModel = new DefaultComboBoxModel();
  private String mySdkLocation;

  private boolean myFreeze = false;

  public AndroidSdkConfigurableForm(@NotNull SdkModel sdkModel, @NotNull final SdkModificator sdkModificator) {
    mySdkModel = sdkModel;
    myInternalJdkComboBox.setModel(myJdksModel);
    myInternalJdkComboBox.setRenderer(SimpleListCellRenderer.create("", Sdk::getName));
    myBuildTargetComboBox.setModel(myBuildTargetsModel);

    myBuildTargetComboBox.setRenderer(SimpleListCellRenderer.create((label, value, index) -> {
      if (value != null) {
        label.setText(AndroidSdkUtils.getTargetPresentableName(value));
      }
      else {
        label.setText("<html><font color='#" + ColorUtil.toHex(JBColor.RED) + "'>[none]</font></html>");
      }
    }));

    myBuildTargetComboBox.addItemListener(e -> {
      if (myFreeze) {
        return;
      }
      final IAndroidTarget target = (IAndroidTarget)e.getItem();

      List<OrderRoot> roots = AndroidSdks.getInstance().getLibraryRootsForTarget(target, FilePaths.stringToFile(mySdkLocation), true);
      Map<OrderRootType, String[]> configuredRoots = new HashMap<>();

      for (OrderRootType type : OrderRootType.getAllTypes()) {
        if (!AndroidSdkType.getInstance().isRootTypeApplicable(type)) {
          continue;
        }

        final VirtualFile[] oldRoots = sdkModificator.getRoots(type);
        final String[] oldRootPaths = new String[oldRoots.length];

        for (int i = 0; i < oldRootPaths.length; i++) {
          oldRootPaths[i] = oldRoots[i].getPath();
        }

        configuredRoots.put(type, oldRootPaths);
      }

      for (OrderRoot root : roots) {
        if (e.getStateChange() == ItemEvent.DESELECTED) {
          sdkModificator.removeRoot(root.getFile(), root.getType());
        }
        else {
          String[] configuredRootsForType = configuredRoots.get(root.getType());
          if (ArrayUtil.find(configuredRootsForType, root.getFile().getPath()) == -1) {
            sdkModificator.addRoot(root.getFile(), root.getType());
          }
        }
      }
    });
  }

  @NotNull
  public JPanel getContentPanel() {
    return myContentPanel;
  }

  @Nullable
  public Sdk getSelectedSdk() {
    return (Sdk)myInternalJdkComboBox.getSelectedItem();
  }

  @Nullable
  public IAndroidTarget getSelectedBuildTarget() {
    return (IAndroidTarget)myBuildTargetComboBox.getSelectedItem();
  }

  public void init(@Nullable Sdk jdk, Sdk androidSdk, IAndroidTarget buildTarget) {
    updateJdks();

    final String jdkName = jdk != null ? jdk.getName() : null;

    if (androidSdk != null) {
      for (int i = 0; i < myJdksModel.getSize(); i++) {
        if (Comparing.strEqual(((Sdk)myJdksModel.getElementAt(i)).getName(), jdkName)) {
          myInternalJdkComboBox.setSelectedIndex(i);
          break;
        }
      }
    }

    mySdkLocation = androidSdk != null ? androidSdk.getHomePath() : null;
    AndroidSdkData androidSdkData = mySdkLocation != null ? AndroidSdkData.getSdkData(mySdkLocation) : null;

    myFreeze = true;
    updateBuildTargets(androidSdkData, buildTarget);
    myFreeze = false;
  }

  private void updateJdks() {
    myJdksModel.removeAllElements();
    for (Sdk sdk : mySdkModel.getSdks()) {
      if (IdeSdks.getInstance().isJdkCompatible(sdk)) {
        myJdksModel.addElement(sdk);
      }
    }
  }

  private void updateBuildTargets(AndroidSdkData androidSdkData, IAndroidTarget buildTarget) {
    myBuildTargetsModel.removeAllElements();

    if (androidSdkData != null) {
      for (IAndroidTarget target : androidSdkData.getTargets()) {
        myBuildTargetsModel.addElement(target);
      }
    }

    if (buildTarget != null) {
      for (int i = 0; i < myBuildTargetsModel.getSize(); i++) {
        IAndroidTarget target = (IAndroidTarget)myBuildTargetsModel.getElementAt(i);
        if (buildTarget.hashString().equals(target.hashString())) {
          myBuildTargetComboBox.setSelectedIndex(i);
          return;
        }
      }
    }
    myBuildTargetComboBox.setSelectedItem(null);
  }

  public void addJavaSdk(Sdk sdk) {
    myJdksModel.addElement(sdk);
  }

  public void removeJavaSdk(Sdk sdk) {
    myJdksModel.removeElement(sdk);
  }

  public void updateJdks(Sdk sdk, String previousName) {
    Sdk[] sdks = mySdkModel.getSdks();
    for (Sdk currentSdk : sdks) {
      if (currentSdk != null && AndroidSdks.getInstance().isAndroidSdk(currentSdk)) {
        AndroidSdkAdditionalData data = AndroidSdks.getInstance().getAndroidSdkAdditionalData(currentSdk);
        Sdk internalJava = data != null ? data.getJavaSdk() : null;
        if (internalJava != null && Objects.equals(internalJava.getName(), previousName)) {
          data.setJavaSdk(sdk);
        }
      }
    }
    updateJdks();
  }

  public void internalJdkUpdate(@NotNull Sdk sdk) {
    AndroidSdkAdditionalData data = AndroidSdks.getInstance().getAndroidSdkAdditionalData(sdk);
    if (data == null) {
      return;
    }
    final Sdk javaSdk = data.getJavaSdk();
    if (myJdksModel.getIndexOf(javaSdk) == -1) {
      myJdksModel.addElement(javaSdk);
    }
    else {
      myJdksModel.setSelectedItem(javaSdk);
    }
  }
}
