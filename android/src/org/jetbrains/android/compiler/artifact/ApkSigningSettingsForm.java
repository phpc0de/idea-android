package org.jetbrains.android.compiler.artifact;

import com.intellij.openapi.ui.TextFieldWithBrowseButton;

import javax.swing.*;

public interface ApkSigningSettingsForm {
  JButton getLoadKeyStoreButton();

  JTextField getKeyStorePathField();

  JPanel getPanel();

  JButton getCreateKeyStoreButton();

  JPasswordField getKeyStorePasswordField();

  TextFieldWithBrowseButton getKeyAliasField();

  JPasswordField getKeyPasswordField();
}
