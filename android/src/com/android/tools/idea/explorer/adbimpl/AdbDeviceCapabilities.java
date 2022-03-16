/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tools.idea.explorer.adbimpl;

import static com.android.tools.idea.explorer.adbimpl.AdbPathUtil.DEVICE_TEMP_DIRECTORY;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.SyncException;
import com.android.ddmlib.SyncService;
import com.android.ddmlib.TimeoutException;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Helper class used to detect various capabilities/features supported by a {@link IDevice}
 * so callers can make decisions about which adb commands to use.
 */
public class AdbDeviceCapabilities {
  @NotNull private static final Logger LOGGER = Logger.getInstance(AdbDeviceCapabilities.class);
  @NotNull private static final String PROBE_FILES_TEMP_PATH = AdbPathUtil.resolve(DEVICE_TEMP_DIRECTORY, "device-explorer");

  @NotNull private static final String ESCAPING_LS_ESCAPED_PATH = AdbPathUtil.resolve(DEVICE_TEMP_DIRECTORY, "oyX2HCKL\\ acuauQGJ");
  @NotNull private static final String ESCAPING_LS_NOT_ESCAPED_PATH = AdbPathUtil.resolve(DEVICE_TEMP_DIRECTORY, "oyX2HCKL acuauQGJ");

  @NotNull private final IDevice myDevice;
  @Nullable private Boolean mySupportsTestCommand;
  @Nullable private Boolean mySupportsRmForceFlag;
  @Nullable private Boolean mySupportsTouchCommand;
  @Nullable private Boolean mySupportsSuRootCommand;
  @Nullable private Boolean myIsRoot;
  @Nullable private Boolean mySupportsCpCommand;
  @Nullable private Boolean myEscapingLs;
  @Nullable private Boolean mySupportsMkTempCommand;

  public AdbDeviceCapabilities(@NotNull IDevice device) {
    myDevice = device;
  }

  public synchronized boolean supportsTestCommand()
    throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException, SyncException {
    assertNotDispatchThread();

    if (mySupportsTestCommand == null) {
      mySupportsTestCommand = supportsTestCommandWorker();
    }
    return mySupportsTestCommand;
  }

  public synchronized boolean supportsRmForceFlag()
    throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException, SyncException {
    assertNotDispatchThread();

    if (mySupportsRmForceFlag == null) {
      mySupportsRmForceFlag = supportsRmForceFlagWorker();
    }
    return mySupportsRmForceFlag;
  }

  public synchronized boolean supportsTouchCommand()
    throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
    assertNotDispatchThread();

    if (mySupportsTouchCommand == null) {
      mySupportsTouchCommand = supportsTouchCommandWorker();
    }
    return mySupportsTouchCommand;
  }

  public synchronized boolean supportsSuRootCommand()
    throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
    assertNotDispatchThread();

    if (mySupportsSuRootCommand == null) {
      mySupportsSuRootCommand = supportsSuRootCommandWorker();
    }
    return mySupportsSuRootCommand;
  }

  public synchronized boolean isRoot()
    throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
    assertNotDispatchThread();

    if (myIsRoot == null) {
      myIsRoot = isRootWorker();
    }
    return myIsRoot;
  }

  public synchronized boolean supportsCpCommand()
    throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException, SyncException {
    assertNotDispatchThread();

    if (mySupportsCpCommand == null) {
      mySupportsCpCommand = supportsCpCommandWorker();
    }
    return mySupportsCpCommand;
  }

  synchronized boolean hasEscapingLs()
    throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
    assertNotDispatchThread();

    if (myEscapingLs == null) {
      myEscapingLs = hasEscapingLsWorker();
    }

    return myEscapingLs;
  }

  @SuppressWarnings("unused")
  public synchronized boolean supportsMkTempCommand()
    throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
    assertNotDispatchThread();

    if (mySupportsMkTempCommand == null) {
      mySupportsMkTempCommand = supportsMkTempCommandWorker();
    }
    return mySupportsMkTempCommand;
  }

  private static void assertNotDispatchThread() {
    Application application = ApplicationManager.getApplication();
    assert application == null || !application.isDispatchThread();
  }

  @NotNull
  private static String getCommandOutputExtract(@NotNull AdbShellCommandResult commandResult) {
    List<String> output = commandResult.getOutput();
    if (output.isEmpty()) {
      return "[command output is empty]";
    }
    return output.stream().limit(5).collect(Collectors.joining("\n  ", "\n  ", ""));
  }

  private boolean supportsTestCommandWorker()
    throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException, SyncException {

    try (ScopedRemoteFile tempFile = new ScopedRemoteFile(AdbPathUtil.resolve(PROBE_FILES_TEMP_PATH, ".__temp_test_test__file__.tmp"))) {
      // Create the remote file used for testing capability
      tempFile.create();

      // Try the "test" command on it (it should succeed if the command is supported)
      String command = new AdbShellCommandBuilder().withText("test -e ").withEscapedPath(tempFile.getRemotePath()).build();
      AdbShellCommandResult commandResult = AdbShellCommandsUtil.executeCommand(myDevice, command);
      try {
        commandResult.throwIfError();
        return true;
      }
      catch (AdbShellCommandException e) {
        LOGGER.info(String.format("Device \"%s\" does not seem to support the \"test\" command: %s",
                                  DeviceUtil.toDebugString(myDevice),
                                  getCommandOutputExtract(commandResult)),
                    e);
        return false;
      }
    }
  }

  public boolean supportsRmForceFlagWorker()
    throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException, SyncException {

    try (ScopedRemoteFile tempFile = new ScopedRemoteFile(AdbPathUtil.resolve(PROBE_FILES_TEMP_PATH, ".__temp_rm_test_file__.tmp"))) {
      // Create the remote file used for testing capability
      tempFile.create();

      // Try to delete it with "rm -f" (it should work if th command is supported)
      String command = new AdbShellCommandBuilder().withText("rm -f ").withEscapedPath(tempFile.getRemotePath()).build();
      AdbShellCommandResult commandResult = AdbShellCommandsUtil.executeCommand(myDevice, command);
      try {
        commandResult.throwIfError();
        // If no error, "rm -f" is supported and test file has been deleted, so no need to delete it again.
        tempFile.setDeleteOnClose(false);
        return true;
      }
      catch (AdbShellCommandException e) {
        LOGGER.info(String.format("Device \"%s\" does not seem to support \"-f\" flag for rm: %s",
                                  DeviceUtil.toDebugString(myDevice),
                                  getCommandOutputExtract(commandResult)),
                    e);
        return false;
      }
    }
  }

  private boolean supportsTouchCommandWorker()
    throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {

    try (ScopedRemoteFile tempFile = new ScopedRemoteFile(AdbPathUtil.resolve(PROBE_FILES_TEMP_PATH, ".__temp_touch_test_file__.tmp"))) {

      // Try the create the file with the "touch" command
      String command = new AdbShellCommandBuilder().withText("touch ").withEscapedPath(tempFile.getRemotePath()).build();
      AdbShellCommandResult commandResult = AdbShellCommandsUtil.executeCommand(myDevice, command);
      try {
        commandResult.throwIfError();

        // If "touch" did not work, we want to delete the temporary file
        tempFile.setDeleteOnClose(true);
        return true;
      }
      catch (AdbShellCommandException e) {
        LOGGER.info(String.format("Device \"%s\" does not seem to support \"touch\" command: %s",
                                  DeviceUtil.toDebugString(myDevice),
                                  getCommandOutputExtract(commandResult)),
                    e);
        return false;
      }
    }
  }

  private boolean supportsSuRootCommandWorker()
    throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {

    // Try a "su" command ("id") that should always succeed, unless "su" is not supported
    String command = new AdbShellCommandBuilder().withSuRootPrefix().withText("id").build();
    AdbShellCommandResult commandResult = AdbShellCommandsUtil.executeCommand(myDevice, command);
    try {
      commandResult.throwIfError();
      return true;
    }
    catch (AdbShellCommandException e) {
      LOGGER.info(String.format("Device \"%s\" does not seem to support the \"su 0\" command: %s",
                                DeviceUtil.toDebugString(myDevice),
                                getCommandOutputExtract(commandResult)),
                  e);
      return false;
    }
  }

  private boolean isRootWorker() throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
    // Note: The "isRoot" method below does not cache its results in case of negative answer.
    //       This means a round-trip to the device at each call when the device is not root.
    //       By caching the value in this class, we avoid these extra round trips.
    return myDevice.isRoot();
  }

  private boolean supportsCpCommandWorker()
    throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException, SyncException {

    try (ScopedRemoteFile srcFile = new ScopedRemoteFile(AdbPathUtil.resolve(PROBE_FILES_TEMP_PATH, ".__temp_cp_test_file__.tmp"));
         ScopedRemoteFile dstFile = new ScopedRemoteFile(AdbPathUtil.resolve(PROBE_FILES_TEMP_PATH, ".__temp_cp_test_file_dst__.tmp"))) {
      // Create the remote file used for testing capability
      srcFile.create();

      // Copy source file to destination file
      String command = new AdbShellCommandBuilder()
        .withText("cp ")
        .withEscapedPath(srcFile.getRemotePath())
        .withText(" ")
        .withEscapedPath(dstFile.getRemotePath())
        .build();
      AdbShellCommandResult commandResult = AdbShellCommandsUtil.executeCommand(myDevice, command);
      try {
        commandResult.throwIfError();

        // If "cp" succeeded, we need to delete the destination file
        dstFile.setDeleteOnClose(true);
        return true;
      }
      catch (AdbShellCommandException e) {
        LOGGER.info(String.format("Device \"%s\" does not seem to support the \"cp\" command: %s",
                                  DeviceUtil.toDebugString(myDevice),
                                  getCommandOutputExtract(commandResult)),
                    e);
        return false;
      }
    }
  }

  private boolean hasEscapingLsWorker()
    throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {

    try {
      touchEscapedPath();
    }
    catch (AdbShellCommandException exception) {
      LOGGER.info("Device \"" + DeviceUtil.toDebugString(myDevice) + "\" does not seem to support the touch command", exception);
      return false;
    }

    try (ScopedRemoteFile file = new ScopedRemoteFile(ESCAPING_LS_NOT_ESCAPED_PATH)) {
      file.setDeleteOnClose(true);
      return lsEscapedPath();
    }
    catch (AdbShellCommandException exception) {
      LOGGER.info("Device \"" + DeviceUtil.toDebugString(myDevice) + "\" does not seem to support the ls command", exception);
      return false;
    }
  }

  private void touchEscapedPath()
    throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException, AdbShellCommandException {

    String command = new AdbShellCommandBuilder()
      .withText("touch " + ESCAPING_LS_ESCAPED_PATH)
      .build();

    AdbShellCommandResult result = AdbShellCommandsUtil.executeCommand(myDevice, command);
    result.throwIfError();

    if (!result.getOutput().isEmpty()) {
      throw new AdbShellCommandException("Unexpected output from touch");
    }
  }

  private boolean lsEscapedPath()
    throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException, AdbShellCommandException {

    String command = new AdbShellCommandBuilder()
      .withText("ls " + ESCAPING_LS_ESCAPED_PATH)
      .build();

    AdbShellCommandResult result = AdbShellCommandsUtil.executeCommand(myDevice, command);
    result.throwIfError();

    String output = result.getOutput().get(0);

    if (output.equals(ESCAPING_LS_ESCAPED_PATH)) {
      return true;
    }
    else if (output.equals(ESCAPING_LS_NOT_ESCAPED_PATH)) {
      return false;
    }
    else {
      throw new AdbShellCommandException("Unexpected output from ls");
    }
  }

  private boolean supportsMkTempCommandWorker()
    throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {

    // Copy source file to destination file
    String command = new AdbShellCommandBuilder().withText("mktemp -p ").withEscapedPath(DEVICE_TEMP_DIRECTORY).build();
    AdbShellCommandResult commandResult = AdbShellCommandsUtil.executeCommand(myDevice, command);
    try {
      commandResult.throwIfError();
      if (commandResult.getOutput().isEmpty()) {
        throw new AdbShellCommandException("Unexpected output from mktemp, assuming not supported");
      }

      // If "mktemp" succeeded, we need to delete the destination file
      String remotePath = commandResult.getOutput().get(0);
      try (ScopedRemoteFile tempFile = new ScopedRemoteFile(remotePath)) {
        tempFile.setDeleteOnClose(true);
      }
      return true;
    }
    catch (AdbShellCommandException e) {
      LOGGER.info(String.format("Device \"%s\" does not seem to support the \"cp\" command: %s",
                                DeviceUtil.toDebugString(myDevice),
                                getCommandOutputExtract(commandResult)),
                  e);
      return false;
    }
  }

  /**
   * An {@link AutoCloseable} wrapper around a temporary file on a remote device.
   * The {@link #close()} method attempts to delete the file from the remote device
   * unless the {@link #setDeleteOnClose(boolean) setDeletedOnClose(false)} is called.
   */
  private class ScopedRemoteFile implements AutoCloseable {
    @NotNull private final String myRemotePath;
    private boolean myDeleteOnClose;

    public ScopedRemoteFile(@NotNull String remotePath) {
      myRemotePath = remotePath;
    }

    public void setDeleteOnClose(boolean value) {
      myDeleteOnClose = value;
    }

    @NotNull
    public String getRemotePath() {
      return myRemotePath;
    }

    public void create() throws TimeoutException, AdbCommandRejectedException, SyncException, IOException {
      assert !myDeleteOnClose;
      myDeleteOnClose = createRemoteTemporaryFile();
    }

    @Override
    public void close() {
      if (!myDeleteOnClose) {
        return;
      }

      try {
        String command = new AdbShellCommandBuilder().withText("rm ").withEscapedPath(myRemotePath).build();
        AdbShellCommandResult commandResult = AdbShellCommandsUtil.executeCommand(myDevice, command);
        try {
          commandResult.throwIfError();
        }
        catch (AdbShellCommandException e) {
          // There is not much we can do if we can't delete the test file other than logging the error.
          LOGGER.warn(String.format("Device \"%s\": Error deleting temporary test file \"%s\": %s",
                                    DeviceUtil.toDebugString(myDevice),
                                    myRemotePath,
                                    getCommandOutputExtract(commandResult)),
                      e);
        }
      }
      catch (Exception e) {
        // There is not much we can do if we can't delete the test file other than logging the error.
        LOGGER.warn(String.format("Device \"%s\": Error deleting temporary test file \"%s\"",
                                  DeviceUtil.toDebugString(myDevice),
                                  myRemotePath),
                    e);
      }
    }

    /**
     * Create an empty file on the remote device by first creating a local empty temporary file,
     * then pushing it to the remote device.
     */
    private boolean createRemoteTemporaryFile()
      throws IOException, TimeoutException, AdbCommandRejectedException, SyncException {

      File file = FileUtil.createTempFile(myRemotePath, "", true);
      try {
        SyncService sync = myDevice.getSyncService();
        if (sync == null) {
          throw new IOException(String.format("Device \"%s\": Unable to open sync connection",
                                              DeviceUtil.toDebugString(myDevice)));
        }

        try {
          LOGGER.trace(String.format("Device \"%s\": Uploading temporary file \"%s\" to remote file \"%s\"",
                                     DeviceUtil.toDebugString(myDevice),
                                     file,
                                     myRemotePath));
          sync.pushFile(file.getPath(), myRemotePath, SyncService.getNullProgressMonitor());
          return true;
        }
        finally {
          sync.close();
        }
      }
      finally {
        try {
          FileUtil.delete(file);
        }
        catch (Exception e) {
          LOGGER.warn(String.format("Device \"%s\": Error deleting temporary file \"%s\"",
                                    DeviceUtil.toDebugString(myDevice),
                                    file),
                      e);
        }
      }
    }
  }
}
