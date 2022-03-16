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
package com.android.tools.idea.explorer;

import static com.android.tools.idea.concurrency.AsyncTestUtils.pumpEventsAndWaitForFuture;
import static com.android.tools.idea.concurrency.AsyncTestUtils.pumpEventsAndWaitForFutureException;
import static com.android.tools.idea.concurrency.AsyncTestUtils.pumpEventsAndWaitForFutures;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.android.tools.idea.concurrency.FutureCallbackExecutor;
import com.android.tools.idea.ddms.DeviceNamePropertiesProvider;
import com.android.tools.idea.explorer.adbimpl.AdbShellCommandException;
import com.android.tools.idea.explorer.fs.DeviceFileEntry;
import com.android.tools.idea.explorer.fs.DeviceFileSystem;
import com.android.tools.idea.explorer.fs.DeviceFileSystemRenderer;
import com.android.tools.idea.explorer.fs.DeviceFileSystemService;
import com.android.tools.idea.explorer.mocks.MockDeviceExplorerFileManager;
import com.android.tools.idea.explorer.mocks.MockDeviceExplorerView;
import com.android.tools.idea.explorer.mocks.MockDeviceFileEntry;
import com.android.tools.idea.explorer.mocks.MockDeviceFileSystem;
import com.android.tools.idea.explorer.mocks.MockDeviceFileSystemRenderer;
import com.android.tools.idea.explorer.mocks.MockDeviceFileSystemService;
import com.android.tools.idea.explorer.mocks.MockFileOpener;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.intellij.ide.ClipboardSynchronizer;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.fileChooser.PathChooserDialog;
import com.intellij.openapi.fileChooser.impl.FileChooserFactoryImpl;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.TestDialog;
import com.intellij.openapi.ui.TestDialogManager;
import com.intellij.openapi.ui.TestInputDialog;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.ui.UIBundle;
import com.intellij.util.concurrency.EdtExecutorService;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.tree.TreeModelAdapter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import org.jetbrains.android.AndroidTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.PooledThreadExecutor;

public class DeviceExplorerControllerTest extends AndroidTestCase {

  private DeviceExplorerModel myModel;
  private MockDeviceExplorerView myMockView;
  private MockDeviceFileSystemService myMockService;
  private MockDeviceExplorerFileManager myMockFileManager;
  private Supplier<Path> myDownloadLocationSupplier;
  private MockDeviceFileSystem myDevice1;
  private MockDeviceFileEntry myFoo;
  private MockDeviceFileEntry myFooFile1;
  private MockDeviceFileEntry myFooFile2;
  private MockDeviceFileEntry myFooLink1;
  private MockDeviceFileEntry myFile1;
  private MockDeviceFileEntry myFile2;
  private MockDeviceFileSystem myDevice2;
  private RepaintManager myMockRepaintManager;
  private MockDeviceFileEntry myFooDir;
  private MockDeviceFileEntry myFooDirLink;
  private TestDialog myInitialTestDialog;
  private TestInputDialog myInitialTestInputDialog;
  private FutureCallbackExecutor myEdtExecutor;
  private FutureCallbackExecutor myTaskExecutor;
  private boolean myTearingDown;
  private MockFileOpener myMockFileOpener;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    myEdtExecutor = FutureCallbackExecutor.wrap(EdtExecutorService.getInstance());
    myTaskExecutor = FutureCallbackExecutor.wrap(PooledThreadExecutor.INSTANCE);
    myModel = new DeviceExplorerModel() {
      @Override
      public void setActiveDeviceTreeModel(@Nullable DeviceFileSystem device,
                                           @Nullable DefaultTreeModel treeModel,
                                           @Nullable DefaultTreeSelectionModel treeSelectionModel) {
        if (!myTearingDown) {
          // We notify the mock view before everything else to avoid having a dependency
          // on the order of registration of listeners registered with {@code DeviceExplorerModel.addListener()}
          assert myMockView != null;
          myMockView.deviceTreeModelUpdated(device, treeModel, treeSelectionModel);
        }
        super.setActiveDeviceTreeModel(device, treeModel, treeSelectionModel);
      }
    };
    myMockService = new MockDeviceFileSystemService(getProject(), myEdtExecutor, myTaskExecutor);
    myMockView = new MockDeviceExplorerView(getProject(), new MockDeviceFileSystemRendererFactory(), myModel);
    File downloadPath = FileUtil.createTempDirectory("device-explorer-temp", "", true);
    myDownloadLocationSupplier = mock(Supplier.class);
    when(myDownloadLocationSupplier.get()).thenReturn(downloadPath.toPath());
    myMockFileManager = new MockDeviceExplorerFileManager(getProject(), myEdtExecutor, myTaskExecutor, myDownloadLocationSupplier);

    myMockFileOpener = new MockFileOpener();

    myDevice1 = myMockService.addDevice("TestDevice-1");
    myFoo = myDevice1.getRoot().addDirectory("Foo");
    myFooDirLink = myDevice1.getRoot().addDirLink("fooDirLink", "fooDir");
    myFooFile1 = myFoo.addFile("fooFile1.txt");
    myFooFile2 = myFoo.addFile("fooFile2.txt");
    myFooLink1 = myFoo.addFileLink("fooLink1.txt", "fooFile1.txt");
    myFooDir = myFoo.addDirectory("fooDir");
    myFooDir.addFile("fooDirFile1.txt");
    myFooDir.addFile("fooDirFile2.txt");
    myFile1 = myDevice1.getRoot().addFile("file1.txt");
    myFile2 = myDevice1.getRoot().addFile("file2.txt");
    myDevice1.getRoot().addFile("file3.txt");

    myDevice2 = myMockService.addDevice("TestDevice-2");
    myDevice2.getRoot().addDirectory("Foo2");
    myDevice2.getRoot().addFile("foo2File1.txt");
    myDevice2.getRoot().addFile("foo2File2.txt");
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      myTearingDown = true;

      RepaintManager.setCurrentManager(null);
      myMockRepaintManager = null;

      if (myInitialTestDialog != null) {
        TestDialogManager.setTestDialog(myInitialTestDialog);
      }

      if (myInitialTestInputDialog != null) {
        TestDialogManager.setTestInputDialog(myInitialTestInputDialog);
      }

      if (myMockFileManager != null) {
        Disposer.dispose(myMockFileManager);
        myMockFileManager = null;
      }

      myFooLink1 = null;
      myFooFile1 = null;
      myFooFile2 = null;
      myFooDir = null;
      myFooDirLink = null;
      myFoo = null;
      myFile1 = null;
      myFile2 = null;

      if (myMockService != null) {
        myMockService = null;
      }

      myTaskExecutor = null;
      myEdtExecutor = null;
      ClipboardSynchronizer.getInstance().resetContent();
    }
    finally {
      myMockService = null;
      myMockView = null;
      myModel = null;
      myDevice1 = null;
      myDevice2 = null;
      super.tearDown();
    }
  }


  private void injectRepaintManagerMock() {
    RepaintManager current = RepaintManager.currentManager(null);
    assert current != null;
    myMockRepaintManager = spy(current);
    RepaintManager.setCurrentManager(myMockRepaintManager);
  }

  public void testControllerIsSetAsProjectKey() {
    // Prepare
    DeviceExplorerController controller = createController();

    // Assert
    assertEquals(controller, DeviceExplorerController.getProjectController(getProject()));
  }

  public void testStartController() throws InterruptedException, ExecutionException, TimeoutException {
    // Prepare
    DeviceExplorerController controller = createController();

    // Act
    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());

    // Assert
    checkMockViewInitialState(controller, myDevice1);
  }

  public void testStartControllerFailure() {
    // Prepare
    String setupErrorMessage = "<Unique error message>";
    DeviceFileSystemService service = mock(DeviceFileSystemService.class);
    when(service.start(any()))
      .thenReturn(Futures.immediateFailedFuture(new RuntimeException(setupErrorMessage)));
    DeviceExplorerController controller = createController(myMockView, service, myMockFileOpener, myMockFileManager);

    // Act
    controller.setup();
    String errorMessage = pumpEventsAndWaitForFuture(myMockView.getReportErrorRelatedToServiceTracker().consume());

    // Assert
    assertNotNull(errorMessage);
    assertTrue(errorMessage.contains(setupErrorMessage));
  }

  public void testStartControllerUnexpectedFailure() {
    // Prepare
    DeviceFileSystemService service = mock(DeviceFileSystemService.class);
    when(service.start(any()))
      .thenReturn(Futures.immediateFailedFuture(new RuntimeException()));
    DeviceExplorerController controller = createController(myMockView, service, myMockFileOpener, myMockFileManager);

    // Act
    controller.setup();
    String errorMessage = pumpEventsAndWaitForFuture(myMockView.getReportErrorRelatedToServiceTracker().consume());

    // Assert
    assertNotNull(errorMessage);
    assertTrue(errorMessage.contains("Error initializing ADB"));
  }

  public void testRestartController() throws InterruptedException, ExecutionException, TimeoutException {
    // Prepare
    DeviceExplorerController controller = createController();

    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());
    checkMockViewInitialState(controller, myDevice1);

    // Act
    controller.restartService();
    pumpEventsAndWaitForFuture(myMockView.getAllDevicesRemovedTracker().consume());

    // Assert
    checkMockViewInitialState(controller, myDevice1);
  }

  public void testRestartControllerFailure() {
    // Prepare
    String setupErrorMessage = "<Unique error message>";
    DeviceFileSystemService service = mock(DeviceFileSystemService.class);
    when(service.start(any())).thenReturn(Futures.immediateFuture(null));
    when(service.restart(any())).thenReturn(Futures.immediateFailedFuture(new RuntimeException(setupErrorMessage)));
    when(service.getDevices()).thenReturn(Futures.immediateFuture(new ArrayList<>()));
    DeviceExplorerController controller = createController(myMockView, service, myMockFileOpener, myMockFileManager);

    // Act
    controller.setup();
    controller.restartService();
    String errorMessage = pumpEventsAndWaitForFuture(myMockView.getReportErrorRelatedToServiceTracker().consume());

    // Assert
    assertNotNull(errorMessage);
    assertTrue(errorMessage.contains(setupErrorMessage));
  }

  public void testGetDevicesFailure() {
    // Prepare
    String setupErrorMessage = "<Unique error message>";
    DeviceFileSystemService service = mock(DeviceFileSystemService.class);
    when(service.start(any()))
      .thenReturn(Futures.immediateFuture(null));
    when(service.getDevices()).thenReturn(Futures.immediateFailedFuture(new RuntimeException(setupErrorMessage)));
    DeviceExplorerController controller = createController(myMockView, service, myMockFileOpener, myMockFileManager);

    // Act
    controller.setup();
    String errorMessage = pumpEventsAndWaitForFuture(myMockView.getReportErrorRelatedToServiceTracker().consume());

    // Assert
    assertNotNull(errorMessage);
    assertTrue(errorMessage.contains(errorMessage));
  }

  public void testGetRootDirectoryFailure() {
    // Prepare
    String setupErrorMessage = "<Unique error message>";
    myDevice1.setRootDirectoryError(new RuntimeException(setupErrorMessage));
    DeviceExplorerController controller = createController();

    // Act
    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());

    checkMockViewComboBox(controller);
    String errorMessage = pumpEventsAndWaitForFuture(myMockView.getReportErrorRelatedToDeviceTracker().consume());

    // Assert
    assertNotNull(errorMessage);
    assertTrue(errorMessage.contains(setupErrorMessage));
  }

  public void testExpandChildren() throws InterruptedException, ExecutionException, TimeoutException {
    // Prepare
    DeviceExplorerController controller = createController();

    // Act
    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());
    checkMockViewInitialState(controller, myDevice1);

    // Set timers to ensure the "loading..." animation code is hit
    controller.setShowLoadingNodeDelayMillis(10);
    controller.setTransferringNodeRepaintMillis(10);
    myFoo.setGetEntriesTimeoutMillis(1_000);

    // Listen to node expansion effect (structure changed event)
    TreePath fooPath = getFileEntryPath(myFoo);
    SettableFuture<TreePath> futureNodeExpanded = createNodeExpandedFuture(myFoo);
    SettableFuture<MyLoadingNode> futureTreeNodesChanged = SettableFuture.create();
    myMockView.getTree().getModel().addTreeModelListener(new TreeModelAdapter() {
      @Override
      public void treeNodesChanged(TreeModelEvent event) {
        if (Objects.equals(fooPath.getLastPathComponent(), event.getTreePath().getLastPathComponent())) {
          Object[] children = event.getChildren();
          if (children != null && children.length == 1) {
            Object child = children[0];
            if (child instanceof MyLoadingNode) {
              futureTreeNodesChanged.set((MyLoadingNode)child);
            }
          }
        }
      }
    });

    // Expand node
    myMockView.getTree().expandPath(fooPath);

    // Wait for tree node to be expanded
    MyLoadingNode myLoadingNode = pumpEventsAndWaitForFuture(futureTreeNodesChanged);
    TreePath nodeExpandedPath = pumpEventsAndWaitForFuture(futureNodeExpanded);

    // Assert
    assertTrue(myLoadingNode.getTick() > 1);
    assertEquals(fooPath.getLastPathComponent(), nodeExpandedPath.getLastPathComponent());
  }

  public void testExpandChildrenFailure() throws InterruptedException, ExecutionException, TimeoutException {
    // Prepare
    DeviceExplorerController controller = createController();
    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());
    checkMockViewInitialState(controller, myDevice1);
    String errorMessage = "<Expected test error>";
    myFoo.setGetEntriesError(new RuntimeException(errorMessage));
    SettableFuture<TreePath> nodeExpandedFuture = createNodeExpandedFuture(myFoo);

    // Act
    expandEntry(myFoo);
    pumpEventsAndWaitForFuture(nodeExpandedFuture);

    // Assert
    Object fooNode = getFileEntryPath(myFoo).getLastPathComponent();
    assertNotNull(fooNode);
    assertInstanceOf(fooNode, TreeNode.class);
    assertTrue(((TreeNode)fooNode).getChildCount() == 1);

    Object errorNode = ((TreeNode)fooNode).getChildAt(0);
    assertNotNull(errorNode);
    assertInstanceOf(errorNode, ErrorNode.class);
    assertEquals(errorMessage, ((ErrorNode)errorNode).getText());
  }

  public void testOpenNodeInEditorDoesNothingForSymlinkToDirectory() throws Exception {
    DeviceExplorerFileManager mockFileManager = spy(myMockFileManager);
    DeviceExplorerController controller = createController(myMockView, myMockService, myMockFileOpener, mockFileManager);

    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());
    checkMockViewInitialState(controller, myDevice1);

    DeviceExplorerViewListener listener = myMockView.getListeners().get(0);

    TreePath fooDirPath = getFileEntryPath(myFooDirLink);
    myMockView.getTree().setSelectionPath(fooDirPath);
    DeviceFileEntryNode node = (DeviceFileEntryNode)fooDirPath.getLastPathComponent();

    List<DeviceFileEntryNode> nodes = new ArrayList<>();
    nodes.add(node);

    listener.openNodesInEditorInvoked(nodes);

    verifyNoMoreInteractions(mockFileManager);
  }

  public void testDownloadFileWithEnterKey() throws Exception {
    downloadFile(myFile1, () -> {
      // Send a VK_ENTER key event
      fireEnterKey(myMockView.getTree());

      pumpEventsAndWaitForFuture(myMockView.getOpenNodesInEditorInvokedTracker().consume());
    });
    pumpEventsAndWaitForFuture(myMockFileOpener.getOpenFileTracker().consume());
  }

  public void testDownloadFileWithMouseClick() throws Exception {
    downloadFile(myFile1, () -> {
      TreePath path = getFileEntryPath(myFile1);
      Rectangle pathBounds = myMockView.getTree().getPathBounds(path);
      assert pathBounds != null;

      // Fire double-click event
      fireDoubleClick(myMockView.getTree(), pathBounds.x, pathBounds.y);

      pumpEventsAndWaitForFuture(myMockView.getOpenNodesInEditorInvokedTracker().consume());
    });
    pumpEventsAndWaitForFuture(myMockFileOpener.getOpenFileTracker().consume());
  }

  public void testDownloadFileLocationWithMouseClick() throws Exception {
    // This saves in the default location for test
    downloadFile(myFile1, () -> {
      TreePath path = getFileEntryPath(myFile1);
      Rectangle pathBounds = myMockView.getTree().getPathBounds(path);
      assert pathBounds != null;

      // Fire double-click event
      fireDoubleClick(myMockView.getTree(), pathBounds.x, pathBounds.y);

      pumpEventsAndWaitForFuture(myMockView.getOpenNodesInEditorInvokedTracker().consume());
    });
    Path downloadPath = pumpEventsAndWaitForFuture(myMockFileOpener.getOpenFileTracker().consume());
    assertTrue(FileUtil.toSystemIndependentName(downloadPath.toString())
                 .endsWith("device-explorer-temp/TestDevice-1/file1.txt"));

    // Change the setting to an alternate directory, ensure that changing during runtime works
    Path changedPath = FileUtil.createTempDirectory("device-explorer-temp-2","", true).toPath();
    when(myDownloadLocationSupplier.get()).thenReturn(changedPath);

    // Now try the alternate location
    downloadFile(myFile1, () -> {
      TreePath path = getFileEntryPath(myFile1);
      Rectangle pathBounds = myMockView.getTree().getPathBounds(path);
      assert pathBounds != null;

      // Fire double-click event
      fireDoubleClick(myMockView.getTree(), pathBounds.x, pathBounds.y);

      pumpEventsAndWaitForFuture(myMockView.getOpenNodesInEditorInvokedTracker().consume());
    });
    downloadPath = pumpEventsAndWaitForFuture(myMockFileOpener.getOpenFileTracker().consume());
    assertTrue(FileUtil.toSystemIndependentName(downloadPath.toString())
                 .endsWith("device-explorer-temp-2/TestDevice-1/file1.txt"));
  }

  public void testDownloadFileFailure() throws InterruptedException, ExecutionException, TimeoutException {
    // Prepare
    DeviceExplorerController controller = createController();

    // Act
    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());
    checkMockViewInitialState(controller, myDevice1);

    String errorMessage = "<Expected test error>";
    myDevice1.setDownloadError(new RuntimeException(errorMessage));

    // Select node
    myMockView.getTree().setSelectionPath(getFileEntryPath(myFile1));

    // Send a VK_ENTER key event
    fireEnterKey(myMockView.getTree());
    pumpEventsAndWaitForFuture(myMockView.getOpenNodesInEditorInvokedTracker().consume());

    pumpEventsAndWaitForFuture(myMockFileManager.getDownloadFileEntryTracker().consume());
    Throwable t = pumpEventsAndWaitForFutureException(myMockFileManager.getDownloadFileEntryCompletionTracker().consume());
    String loadingError = pumpEventsAndWaitForFuture(myMockView.getReportErrorRelatedToNodeTracker().consume());

    // Assert
    assertNotNull(t);
    assertNotNull(loadingError);
    assertTrue(loadingError.contains(errorMessage));
  }

  public void testChangeActiveDevice() throws InterruptedException, ExecutionException, TimeoutException {
    // Prepare
    DeviceExplorerController controller = createController();

    // Act
    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());
    checkMockViewInitialState(controller, myDevice1);

    myMockView.getDeviceCombo().setSelectedItem(myDevice2);

    // Assert
    checkMockViewActiveDevice(myDevice2);
  }

  public void testUpdateActiveDeviceState() throws InterruptedException, ExecutionException, TimeoutException {
    // Prepare
    DeviceExplorerController controller = createController();
    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());
    checkMockViewInitialState(controller, myDevice1);
    TreeModel model = myModel.getTreeModel();

    // Act
    injectRepaintManagerMock();
    Arrays.stream(myMockService.getListeners()).forEach(l -> l.deviceUpdated(myDevice1));
    pumpEventsAndWaitForFuture(myMockView.getDeviceUpdatedTracker().consume());

    // Assert
    // Check there was no update to the underlying model, and that only
    // the combo box UI has been invalidated.
    assertEquals(myDevice1, myModel.getActiveDevice());
    assertEquals(model, myModel.getTreeModel());
    verify(myMockRepaintManager).addDirtyRegion(myMockView.getDeviceCombo(),
                                                        0,
                                                        0,
                                                        myMockView.getDeviceCombo().getWidth(),
                                                        myMockView.getDeviceCombo().getHeight());
  }

  public void testSetActiveDeviceFromSerialNumber() throws InterruptedException, ExecutionException, TimeoutException {
    // Prepare
    DeviceExplorerController controller = createController();

    // Act
    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());
    checkMockViewInitialState(controller, myDevice1);

    controller.selectActiveDevice(myDevice2.getDeviceSerialNumber());

    // Assert
    checkMockViewActiveDevice(myDevice2);
  }

  public void testSetActiveDeviceFromSerialNumberNotFound() throws InterruptedException, ExecutionException, TimeoutException {
    // Prepare
    DeviceExplorerController controller = createController();

    // Act
    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());
    checkMockViewInitialState(controller, myDevice1);

    controller.selectActiveDevice("Nonexistent");
    String selectError = pumpEventsAndWaitForFuture(myMockView.getReportErrorGenericTracker().consume());

    // Assert
    assertNotNull(selectError);
    assertTrue(selectError.contains("Unable to find device with serial number"));
    checkMockViewActiveDevice(myDevice1);
  }

  public void testAddDevice() throws InterruptedException, ExecutionException, TimeoutException {
    // Prepare
    DeviceExplorerController controller = createController();

    // Act
    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());
    checkMockViewInitialState(controller, myDevice1);

    SettableFuture<Void> futureItemAdded = SettableFuture.create();
    myMockView.getDeviceCombo().getModel().addListDataListener(new ListDataListener() {
      @Override
      public void intervalAdded(ListDataEvent e) {
        futureItemAdded.set(null);
      }

      @Override
      public void intervalRemoved(ListDataEvent e) {
      }

      @Override
      public void contentsChanged(ListDataEvent e) {
      }
    });
    DeviceFileSystem newFileSystem = myMockService.addDevice("TestDevice-3");
    DeviceFileSystem addedFileSystem = pumpEventsAndWaitForFuture(myMockView.getDeviceAddedTracker().consume());
    pumpEventsAndWaitForFuture(futureItemAdded);

    // Assert
    assertEquals(newFileSystem, addedFileSystem);
    assertEquals(3, myMockView.getDeviceCombo().getItemCount());
  }

  public void testRemoveActiveDevice() throws InterruptedException, ExecutionException, TimeoutException {
    // Prepare
    DeviceExplorerController controller = createController();

    // Act
    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());
    checkMockViewInitialState(controller, myDevice1);

    SettableFuture<Void> futureItemRemoved = SettableFuture.create();
    myMockView.getDeviceCombo().getModel().addListDataListener(new ListDataListener() {
      @Override
      public void intervalAdded(ListDataEvent e) {
      }

      @Override
      public void intervalRemoved(ListDataEvent e) {
        futureItemRemoved.set(null);
      }

      @Override
      public void contentsChanged(ListDataEvent e) {
      }
    });
    assertTrue(myMockService.removeDevice(myDevice1));
    pumpEventsAndWaitForFuture(myMockView.getDeviceRemovedTracker().consume());
    pumpEventsAndWaitForFuture(futureItemRemoved);

    // Assert
    assertEquals(1, myMockView.getDeviceCombo().getItemCount());
    checkMockViewActiveDevice(myDevice2);
  }

  public void testFileSystemTree_ContextMenu_Items_Present() throws InterruptedException, ExecutionException, TimeoutException {
    // Prepare
    DeviceExplorerController controller = createController();

    // Act
    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());
    checkMockViewInitialState(controller, myDevice1);

    // Assert
    ActionGroup actionGroup = myMockView.getFileTreeActionGroup();
    assertEquals(9, actionGroup.getChildren(null).length);

    ActionGroup subGroup = getSubGroup(actionGroup, "New");
    assertNotNull(subGroup);
    assertEquals(2, subGroup.getChildren(null).length);

    // Act: Call "update" on each action, just to make sure the code is covered
    myMockView.getTree().setSelectionPath(getFileEntryPath(myFile1));
    List<AnAction> actions = Arrays.asList(actionGroup.getChildren(null));
    AnActionEvent e = createContentMenuItemEvent();
    actions.forEach(x -> x.update(e));
  }

  public void testFileSystemTree_ContextMenu_Open_Works() throws Exception {
    downloadFile(myFile1, () -> {
      ActionGroup actionGroup = myMockView.getFileTreeActionGroup();
      AnAction action = getActionByText(actionGroup, "Open");
      assertNotNull(action);
      AnActionEvent e = createContentMenuItemEvent();
      action.update(e);
      // Assert
      assertTrue(e.getPresentation().isVisible());
      assertTrue(e.getPresentation().isEnabled());

      // Act
      action.actionPerformed(e);

      // Assert
      pumpEventsAndWaitForFuture(myMockView.getOpenNodesInEditorInvokedTracker().consume());
    });
    pumpEventsAndWaitForFuture(myMockFileOpener.getOpenFileTracker().consume());
  }

  public void testFileSystemTree_ContextMenu_SaveFileAs_Works() throws Exception {
    File tempFile = FileUtil.createTempFile("foo", "bar");

    downloadFile(myFile1, () -> {
      // Prepare
      // The "Save As" dialog does not work in headless mode, so we register a custom
      // component that simply returns the tempFile we created above.
      FileChooserFactoryImpl factory = new FileChooserFactoryImpl() {
        @NotNull
        @Override
        public FileSaverDialog createSaveFileDialog(@NotNull FileSaverDescriptor descriptor, @Nullable Project project) {
          return new FileSaverDialog() {
            @Override
            public @Nullable VirtualFileWrapper save(@Nullable VirtualFile baseDir, @Nullable String filename) {
              return new VirtualFileWrapper(tempFile);
            }

            @Override
            public @Nullable VirtualFileWrapper save(@Nullable Path baseDir, @Nullable String filename) {
              return new VirtualFileWrapper(tempFile);
            }
          };
        }
      };
      ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), FileChooserFactory.class, factory, getTestRootDisposable());

      // Invoke "Save As..." content menu
      ActionGroup actionGroup = myMockView.getFileTreeActionGroup();
      AnAction action = getActionByText(actionGroup, "Save As...");
      assertNotNull(action);
      AnActionEvent e = createContentMenuItemEvent();
      action.update(e);

      // Assert
      assertTrue(e.getPresentation().isVisible());
      assertTrue(e.getPresentation().isEnabled());

      // Act
      action.actionPerformed(e);

      // Assert
      pumpEventsAndWaitForFuture(myMockView.getSaveNodesAsTracker().consume());
    });

    // Assert
    assertTrue(tempFile.exists());
    assertEquals(200_000, tempFile.length());
  }

  public void testFileSystemTree_ContextMenu_SaveDirectoryAs_Works() throws Exception {
    // Prepare
    DeviceExplorerController controller = createController();
    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());
    checkMockViewInitialState(controller, myDevice1);

    // Act
    // Select node
    TreePath file1Path = getFileEntryPath(myFoo);
    myMockView.getTree().setSelectionPath(file1Path);

    ActionGroup actionGroup = myMockView.getFileTreeActionGroup();
    AnAction action = getActionByText(actionGroup, "Save As...");
    assertNotNull(action);
    AnActionEvent e = createContentMenuItemEvent();
    action.update(e);

    // Assert
    assertTrue(e.getPresentation().isVisible());
    assertTrue(e.getPresentation().isEnabled());

    // Prepare
    // The "Choose file" dialog does not work in headless mode, so we register a custom
    // component that simply returns the tempFile we created above.
    File tempDirectory = FileUtil.createTempDirectory("saveAsDir", "");
    myDevice1.setDownloadFileChunkSize(1_000); // download chunks of 1000 bytes at a time
    myDevice1.setDownloadFileChunkIntervalMillis(10); // wait 10 millis between each 1000 bytes chunk

    FileChooserFactoryImpl factory = new FileChooserFactoryImpl() {
      @NotNull
      @Override
      public PathChooserDialog createPathChooser(@NotNull FileChooserDescriptor descriptor,
                                                 @Nullable Project project,
                                                 @Nullable Component parent) {
        return (toSelect, callback) -> {
          List<VirtualFile> list = Collections.singletonList(new VirtualFileWrapper(tempDirectory).getVirtualFile());
          callback.consume(list);
        };
      }
    };
    ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), FileChooserFactory.class, factory, getTestRootDisposable());

    // Act
    myMockView.getStartTreeBusyIndicatorTacker().clear();
    myMockView.getStopTreeBusyIndicatorTacker().clear();
    myMockView.getReportMessageRelatedToNodeTracker().clear();
    action.actionPerformed(e);

    // Assert
    pumpEventsAndWaitForFuture(myMockView.getStartTreeBusyIndicatorTacker().consume());
    pumpEventsAndWaitForFuture(myMockView.getStopTreeBusyIndicatorTacker().consume());
    String summaryMessage = pumpEventsAndWaitForFuture(myMockView.getReportMessageRelatedToNodeTracker().consume());

    assertNotNull(summaryMessage);
    System.out.println("SaveAs message: " + summaryMessage);
    assertTrue(summaryMessage.contains("Successfully downloaded"));

    File[] files = tempDirectory.listFiles();
    assertNotNull(files);

    List<File> createdFiles = Arrays.asList(files);
    assertEquals(4, createdFiles.size());
    assertTrue(createdFiles.stream().anyMatch(x -> Objects.equals(x.getName(), myFooFile1.getName())));
    assertTrue(createdFiles.stream().anyMatch(x -> Objects.equals(x.getName(), myFooFile2.getName())));
    assertTrue(createdFiles.stream().anyMatch(x -> Objects.equals(x.getName(), myFooLink1.getName())));
    assertTrue(createdFiles.stream().anyMatch(x -> Objects.equals(x.getName(), myFooDir.getName())));
  }

  public void testFileSystemTree_ContextMenu_SaveMultipleFilesAs_Works() throws Exception {
    // Prepare
    DeviceExplorerController controller = createController();
    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());
    checkMockViewInitialState(controller, myDevice1);

    // Act
    // Select nodes
    expandEntry(myFoo);
    myMockView.getTree().setSelectionPath(getFileEntryPath(myFooFile1));
    myMockView.getTree().addSelectionPath(getFileEntryPath(myFooFile2));

    ActionGroup actionGroup = myMockView.getFileTreeActionGroup();
    AnAction action = getActionByText(actionGroup, "Save As...");
    assertNotNull(action);
    AnActionEvent e = createContentMenuItemEvent();
    action.update(e);

    // Assert
    assertTrue(e.getPresentation().isVisible());
    assertTrue(e.getPresentation().isEnabled());

    // Prepare
    // The "Choose file" dialog does not work in headless mode, so we register a custom
    // component that simply returns the tempFile we created above.
    File tempDirectory = FileUtil.createTempDirectory("saveAsDir", "");
    myDevice1.setDownloadFileChunkSize(1_000); // download chunks of 1000 bytes at a time
    myDevice1.setDownloadFileChunkIntervalMillis(10); // wait 10 millis between each 1000 bytes chunk

    FileChooserFactoryImpl factory = new FileChooserFactoryImpl() {
      @NotNull
      @Override
      public PathChooserDialog createPathChooser(@NotNull FileChooserDescriptor descriptor,
                                                 @Nullable Project project,
                                                 @Nullable Component parent) {
        return (toSelect, callback) -> {
          List<VirtualFile> list = Collections.singletonList(new VirtualFileWrapper(tempDirectory).getVirtualFile());
          callback.consume(list);
        };
      }
    };
    ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), FileChooserFactory.class, factory, getTestRootDisposable());

    // Act
    myMockView.getStartTreeBusyIndicatorTacker().clear();
    myMockView.getStopTreeBusyIndicatorTacker().clear();
    myMockView.getReportMessageRelatedToNodeTracker().clear();
    action.actionPerformed(e);

    // Assert
    pumpEventsAndWaitForFuture(myMockView.getStartTreeBusyIndicatorTacker().consume());
    pumpEventsAndWaitForFuture(myMockView.getStopTreeBusyIndicatorTacker().consume());
    String summaryMessage = pumpEventsAndWaitForFuture(myMockView.getReportMessageRelatedToNodeTracker().consume());

    assertNotNull(summaryMessage);
    System.out.println("SaveAs message: " + summaryMessage);
    assertTrue(summaryMessage.contains("Successfully downloaded"));

    File[] files = tempDirectory.listFiles();
    assertNotNull(files);
    List<File> createdFiles = Arrays.asList(files);
    assertEquals(2, createdFiles.size());
    assertTrue(createdFiles.stream().anyMatch(x -> Objects.equals(x.getName(), myFooFile1.getName())));
    assertTrue(createdFiles.stream().anyMatch(x -> Objects.equals(x.getName(), myFooFile2.getName())));
  }

  public void testFileSystemTree_ContextMenu_SaveDirectoryAs_ShowsProblems() throws Exception {
    // Prepare
    DeviceExplorerController controller = createController();
    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());
    checkMockViewInitialState(controller, myDevice1);

    // Act
    // Select node
    TreePath file1Path = getFileEntryPath(myFoo);
    myMockView.getTree().setSelectionPath(file1Path);

    ActionGroup actionGroup = myMockView.getFileTreeActionGroup();
    AnAction action = getActionByText(actionGroup, "Save As...");
    assertNotNull(action);
    AnActionEvent e = createContentMenuItemEvent();
    action.update(e);

    // Assert
    assertTrue(e.getPresentation().isVisible());
    assertTrue(e.getPresentation().isEnabled());

    // Prepare
    // The "Choose file" dialog does not work in headless mode, so we register a custom
    // component that simply returns the tempFile we created above.
    File tempDirectory = FileUtil.createTempDirectory("saveAsDir", "");
    myDevice1.setDownloadFileChunkSize(1_000); // download chunks of 1000 bytes at a time
    myDevice1.setDownloadFileChunkIntervalMillis(10); // wait 10 millis between each 1000 bytes chunk
    String downloadErrorMessage = "[test] Error downloading file";
    myDevice1.setDownloadError(new Exception(downloadErrorMessage));

    FileChooserFactoryImpl factory = new FileChooserFactoryImpl() {
      @NotNull
      @Override
      public PathChooserDialog createPathChooser(@NotNull FileChooserDescriptor descriptor,
                                                 @Nullable Project project,
                                                 @Nullable Component parent) {
        return (toSelect, callback) -> {
          List<VirtualFile> list = Collections.singletonList(new VirtualFileWrapper(tempDirectory).getVirtualFile());
          callback.consume(list);
        };
      }
    };
    ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), FileChooserFactory.class, factory, getTestRootDisposable());

    // Act
    myMockView.getStartTreeBusyIndicatorTacker().clear();
    myMockView.getStopTreeBusyIndicatorTacker().clear();
    myMockView.getReportMessageRelatedToNodeTracker().clear();
    action.actionPerformed(e);

    // Assert
    pumpEventsAndWaitForFuture(myMockView.getStartTreeBusyIndicatorTacker().consume());
    pumpEventsAndWaitForFuture(myMockView.getStopTreeBusyIndicatorTacker().consume());
    String summaryMessage = pumpEventsAndWaitForFuture(myMockView.getReportErrorRelatedToNodeTracker().consume());

    assertNotNull(summaryMessage);
    System.out.println("SaveAs message: " + summaryMessage);
    assertTrue(summaryMessage.contains("There were errors"));
    assertTrue(summaryMessage.contains(downloadErrorMessage));

    // Note: Even though downloading files failed, empty directories are still created during
    //       a directory tree download, in this case we should have exactly one.
    File[] files = tempDirectory.listFiles();
    assertNotNull(files);
    List<File> createdFiles = Arrays.asList(files);
    assertEquals(1, createdFiles.size());
    assertTrue(createdFiles.stream().anyMatch(x -> Objects.equals(x.getName(), myFooDir.getName())));
  }

  public void testFileSystemTree_ContextMenu_New_IsHiddenForFiles() throws Exception {
    // Prepare
    DeviceExplorerController controller = createController();
    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());
    checkMockViewInitialState(controller, myDevice1);

    // Act
    myMockView.getTree().setSelectionPath(getFileEntryPath(myFile1));

    // Assert
    checkContextMenuItemVisible("New/File", false);
    checkContextMenuItemVisible("New/Directory", false);
  }

  public void testFileSystemTree_ContextMenu_New_IsVisibleForDirectories() throws Exception {
    // Prepare
    DeviceExplorerController controller = createController();
    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());
    checkMockViewInitialState(controller, myDevice1);

    // Act
    expandEntry(myFoo);
    myMockView.getTree().setSelectionPath(getFileEntryPath(myFooDir));

    // Assert
    checkContextMenuItemVisible("New/File", true);
    checkContextMenuItemVisible("New/Directory", true);
  }

  public void testFileSystemTree_ContextMenu_NewFile_Works() throws Exception {
    // Prepare
    DeviceExplorerController controller = createController();
    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());
    checkMockViewInitialState(controller, myDevice1);

    expandEntry(myFoo);
    TreePath fooDirPath = getFileEntryPath(myFooDir);
    myMockView.getTree().setSelectionPath(fooDirPath);
    SettableFuture<TreePath> fooDirExpandedFuture = createNodeExpandedFuture(myFooDir);
    String newFileName = "foobar.txt";
    replaceTestInputDialog(newFileName);

    // Act
    AnAction action = getContextMenuAction("New/File");
    AnActionEvent e = createContentMenuItemEvent();
    action.update(e);
    action.actionPerformed(e);

    // Assert
    pumpEventsAndWaitForFuture(fooDirExpandedFuture);

    // Look for the new file entry in the tree view
    DeviceFileEntryNode newChild = enumerationAsList(((DeviceFileEntryNode)fooDirPath.getLastPathComponent()).children())
      .stream()
      .filter(x -> x instanceof DeviceFileEntryNode)
      .map(x -> (DeviceFileEntryNode)x)
      .filter(x -> Objects.equals(newFileName, x.getEntry().getName()) && x.getEntry().isFile())
      .findFirst()
      .orElse(null);
    assertNotNull(newChild);
  }

  public void testFileSystemTree_ContextMenu_NewDirectory_Works() throws Exception {
    // Prepare
    DeviceExplorerController controller = createController();
    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());
    checkMockViewInitialState(controller, myDevice1);

    expandEntry(myFoo);
    TreePath fooDirPath = getFileEntryPath(myFooDir);
    myMockView.getTree().setSelectionPath(fooDirPath);
    SettableFuture<TreePath> fooDirExpandedFuture = createNodeExpandedFuture(myFooDir);
    String newDirectoryName = "foobar.txt";
    replaceTestInputDialog(newDirectoryName);

    // Act
    AnAction action = getContextMenuAction("New/Directory");
    AnActionEvent e = createContentMenuItemEvent();
    action.update(e);
    action.actionPerformed(e);

    // Assert
    pumpEventsAndWaitForFuture(fooDirExpandedFuture);

    // Look for the new file entry in the tree view
    DeviceFileEntryNode newChild = enumerationAsList(((DeviceFileEntryNode)fooDirPath.getLastPathComponent()).children())
      .stream()
      .filter(x -> x instanceof DeviceFileEntryNode)
      .map(x -> (DeviceFileEntryNode)x)
      .filter(x -> Objects.equals(newDirectoryName, x.getEntry().getName()) && x.getEntry().isDirectory())
      .findFirst()
      .orElse(null);
    assertNotNull(newChild);
  }

  public void testFileSystemTree_ContextMenu_NewDirectory_ExistingPath_Fails() throws Exception {
    // Prepare
    DeviceExplorerController controller = createController();
    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());
    checkMockViewInitialState(controller, myDevice1);

    TreePath fooPath = getFileEntryPath(myFoo);
    myMockView.getTree().setSelectionPath(fooPath);
    String newDirectoryName = myFooDir.getName(); // Existing name to create conflict
    replaceTestInputDialog(newDirectoryName);
    SettableFuture<String> futureMessageDialog = SettableFuture.create();
    replaceTestDialog(s -> {
      futureMessageDialog.set(s);

      // Simulate a "Cancel" dialog in the "New Folder Name" dialog, since the controller
      // shows the "New Folder Name" dialog as long as an error is detected when
      // creating the new folder.
      replaceTestInputDialog(null);
      return 0;
    });

    // Act
    AnAction action = getContextMenuAction("New/Directory");
    AnActionEvent e = createContentMenuItemEvent();
    action.update(e);
    action.actionPerformed(e);

    // Assert
    String message = pumpEventsAndWaitForFuture(futureMessageDialog);
    assertNotNull(message);
    assertTrue(message.contains(UIBundle.message("create.new.folder.could.not.create.folder.error.message", newDirectoryName)));

    // Ensure entry does not exist in tree view
    DeviceFileEntryNode newChild = enumerationAsList(((DeviceFileEntryNode)fooPath.getLastPathComponent()).children())
      .stream()
      .filter(x -> x instanceof DeviceFileEntryNode)
      .map(x -> (DeviceFileEntryNode)x)
      .filter(x -> Objects.equals(newDirectoryName, x.getEntry().getName()) && x.getEntry().isDirectory())
      .findFirst()
      .orElse(null);
    assertNull(newChild);
  }

  public void testFileSystemTree_ContextMenu_CopyPath_Works() throws Exception {
    // Prepare
    DeviceExplorerController controller = createController();

    // Act
    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());
    checkMockViewInitialState(controller, myDevice1);

    myMockView.getTree().setSelectionPath(getFileEntryPath(myFile1));

    ActionGroup actionGroup = myMockView.getFileTreeActionGroup();
    AnAction action = getActionByText(actionGroup, "Copy Path");
    assertNotNull(action);
    AnActionEvent e = createContentMenuItemEvent();
    action.update(e);

    // Assert
    assertTrue(e.getPresentation().isVisible());
    assertTrue(e.getPresentation().isEnabled());

    // Act
    action.actionPerformed(e);
    pumpEventsAndWaitForFuture(myMockView.getCopyNodePathsTracker().consume());

    // Assert
    Transferable contents = CopyPasteManager.getInstance().getContents();
    assertNotNull(contents);
    assertEquals("/" + myFile1.getName(), contents.getTransferData(DataFlavor.stringFlavor));
  }

  public void testFileSystemTree_ContextMenu_CopyPaths_Works() throws Exception {
    // Prepare
    DeviceExplorerController controller = createController();

    // Act
    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());
    checkMockViewInitialState(controller, myDevice1);

    myMockView.getTree().setSelectionPath(getFileEntryPath(myFile1));
    myMockView.getTree().addSelectionPath(getFileEntryPath(myFile2));

    ActionGroup actionGroup = myMockView.getFileTreeActionGroup();
    AnAction action = getActionByText(actionGroup, "Copy Path");
    assertNotNull(action);
    AnActionEvent e = createContentMenuItemEvent();
    action.update(e);

    // Assert
    assertTrue(e.getPresentation().isVisible());
    assertTrue(e.getPresentation().isEnabled());

    // Act
    action.actionPerformed(e);
    pumpEventsAndWaitForFuture(myMockView.getCopyNodePathsTracker().consume());

    // Assert
    Transferable contents = CopyPasteManager.getInstance().getContents();
    assertNotNull(contents);
    assertEquals(myFile1.getFullPath() + "\n" + myFile2.getFullPath(), contents.getTransferData(DataFlavor.stringFlavor));
  }

  public void testFileSystemTree_ContextMenu_Delete_Works() throws Exception {
    // Prepare
    DeviceExplorerController controller = createController();

    // Act
    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());
    checkMockViewInitialState(controller, myDevice1);

    expandEntry(myFoo);
    myMockView.getTree().setSelectionPath(getFileEntryPath(myFooFile1));
    myMockView.getTree().addSelectionPath(getFileEntryPath(myFooFile2));

    SettableFuture<TreePath> futureTreeChanged = createNodeExpandedFuture(myFoo);
    ActionGroup actionGroup = myMockView.getFileTreeActionGroup();
    AnAction action = getActionByText(actionGroup, "Delete...");
    assertNotNull(action);
    AnActionEvent e = createContentMenuItemEvent();
    action.update(e);
    replaceTestDialog(s -> 0); // "OK" button

    // Assert
    assertTrue(e.getPresentation().isVisible());
    assertTrue(e.getPresentation().isEnabled());

    // Act
    action.actionPerformed(e);
    pumpEventsAndWaitForFuture(myMockView.getDeleteNodesTracker().consume());
    pumpEventsAndWaitForFuture(futureTreeChanged);

    // Assert
    DeviceFileEntryNode fooNode = DeviceFileEntryNode.fromNode(getFileEntryPath(myFoo).getLastPathComponent());
    assertNotNull(fooNode);
    assertEquals(2, fooNode.getChildCount());
  }

  public void testFileSystemTree_ContextMenu_Delete_ShowProblems() throws Exception {
    // Prepare
    DeviceExplorerController controller = createController();

    // Act
    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());
    checkMockViewInitialState(controller, myDevice1);

    expandEntry(myFoo);
    myFooFile1.setDeleteError(new AdbShellCommandException("Error deleting file"));
    myMockView.getTree().setSelectionPath(getFileEntryPath(myFooFile1));
    myMockView.getTree().addSelectionPath(getFileEntryPath(myFooFile2));

    SettableFuture<TreePath> futureTreeChanged = createNodeExpandedFuture(myFoo);
    ActionGroup actionGroup = myMockView.getFileTreeActionGroup();
    AnAction action = getActionByText(actionGroup, "Delete...");
    assertNotNull(action);
    AnActionEvent e = createContentMenuItemEvent();
    action.update(e);
    SettableFuture<Void> showProblemsFuture = SettableFuture.create();
    replaceTestDialog(s -> {
      if (s.contains("Could not erase")) {
        showProblemsFuture.set(null);
      }
      return 0;  // "OK" button
    });

    // Assert
    assertTrue(e.getPresentation().isVisible());
    assertTrue(e.getPresentation().isEnabled());

    // Act
    action.actionPerformed(e);
    pumpEventsAndWaitForFuture(myMockView.getDeleteNodesTracker().consume());
    pumpEventsAndWaitForFuture(showProblemsFuture);
    pumpEventsAndWaitForFuture(futureTreeChanged);

    // Assert
    // One entry has been deleted
    DeviceFileEntryNode fooNode = DeviceFileEntryNode.fromNode(getFileEntryPath(myFoo).getLastPathComponent());
    assertNotNull(fooNode);
    assertEquals(3, fooNode.getChildCount());
  }

  public void testFileSystemTree_ContextMenu_Upload_SingleFile_Works() throws Exception {
    // Prepare
    DeviceExplorerController controller = createController();
    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());
    checkMockViewInitialState(controller, myDevice1);

    expandEntry(myFoo);
    myMockView.getTree().setSelectionPath(getFileEntryPath(myFoo));

    SettableFuture<TreePath> futureTreeChanged = createNodeExpandedFuture(myFoo);
    ActionGroup actionGroup = myMockView.getFileTreeActionGroup();
    AnAction action = getActionByText(actionGroup, "Upload...");
    assertNotNull(action);
    AnActionEvent e = createContentMenuItemEvent();
    action.update(e);

    // The "Choose file" dialog does not work in headless mode, so we register a custom
    // component that simply returns the tempFile we created above.
    File tempFile = FileUtil.createTempFile("foo", "bar.txt");
    Files.write(tempFile.toPath(), new byte[10_000]);
    myDevice1.setUploadFileChunkSize(500);
    myDevice1.setUploadFileChunkIntervalMillis(20);

    FileChooserFactoryImpl factory = new FileChooserFactoryImpl() {
      @NotNull
      @Override
      public PathChooserDialog createPathChooser(@NotNull FileChooserDescriptor descriptor,
                                                 @Nullable Project project,
                                                 @Nullable Component parent) {
        return (toSelect, callback) -> {
          List<VirtualFile> files = Stream.of(tempFile)
            .map(x -> new VirtualFileWrapper(x).getVirtualFile())
            .collect(Collectors.toList());
          callback.consume(files);
        };
      }
    };
    ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), FileChooserFactory.class, factory, getTestRootDisposable());

    // Assert
    assertTrue(e.getPresentation().isVisible());
    assertTrue(e.getPresentation().isEnabled());

    // Act
    myMockView.getStartTreeBusyIndicatorTacker().clear();
    myMockView.getStopTreeBusyIndicatorTacker().clear();
    action.actionPerformed(e);
    pumpEventsAndWaitForFuture(myMockView.getStartTreeBusyIndicatorTacker().consume());
    pumpEventsAndWaitForFuture(myMockView.getUploadFilesTracker().consume());
    pumpEventsAndWaitForFuture(futureTreeChanged);
    pumpEventsAndWaitForFuture(myMockView.getStopTreeBusyIndicatorTacker().consume());

    // Assert
    // One node has been added
    DeviceFileEntryNode fooNode = DeviceFileEntryNode.fromNode(getFileEntryPath(myFoo).getLastPathComponent());
    assertNotNull(fooNode);
    assertEquals(5, fooNode.getChildCount());
  }

  public void testFileSystemTree_ContextMenu_Upload_DirectoryAndFile_Works() throws Exception {
    // Prepare
    DeviceExplorerController controller = createController();

    // Act
    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());
    checkMockViewInitialState(controller, myDevice1);

    expandEntry(myFoo);
    myMockView.getTree().setSelectionPath(getFileEntryPath(myFoo));

    SettableFuture<TreePath> futureTreeChanged = createNodeExpandedFuture(myFoo);
    ActionGroup actionGroup = myMockView.getFileTreeActionGroup();
    AnAction action = getActionByText(actionGroup, "Upload...");
    assertNotNull(action);
    AnActionEvent e = createContentMenuItemEvent();
    action.update(e);

    File tempFile = FileUtil.createTempFile("foo", "bar.txt");
    Files.write(tempFile.toPath(), new byte[10_000]);

    File tempDirectory = FileUtil.createTempDirectory("foo", "dir");
    File foobar2File = FileUtil.createTempFile(tempDirectory, "foobar2", ".txt");
    Files.write(foobar2File.toPath(), new byte[10_000]);

    File foobar3File = FileUtil.createTempFile(tempDirectory, "foobar3", ".txt");
    Files.write(foobar3File.toPath(), new byte[10_000]);

    File foobar4File = FileUtil.createTempFile(tempDirectory, "foobar4", ".txt");
    Files.write(foobar4File.toPath(), new byte[10_000]);

    // The "Choose file" dialog does not work in headless mode, so we register a custom
    // component that simply returns the tempFile we created above.
    FileChooserFactoryImpl factory = new FileChooserFactoryImpl() {
      @NotNull
      @Override
      public PathChooserDialog createPathChooser(@NotNull FileChooserDescriptor descriptor,
                                                 @Nullable Project project,
                                                 @Nullable Component parent) {
        return (toSelect, callback) -> {
          List<VirtualFile> files = Stream.of(tempFile, tempDirectory)
            .map(x -> new VirtualFileWrapper(x).getVirtualFile())
            .collect(Collectors.toList());
          callback.consume(files);
        };
      }
    };
    ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), FileChooserFactory.class, factory, getTestRootDisposable());

    // Assert
    assertTrue(e.getPresentation().isVisible());
    assertTrue(e.getPresentation().isEnabled());

    // Act
    myMockView.getStartTreeBusyIndicatorTacker().clear();
    myMockView.getStopTreeBusyIndicatorTacker().clear();
    action.actionPerformed(e);
    pumpEventsAndWaitForFuture(myMockView.getStartTreeBusyIndicatorTacker().consume());
    pumpEventsAndWaitForFuture(myMockView.getUploadFilesTracker().consume());
    pumpEventsAndWaitForFuture(futureTreeChanged);
    pumpEventsAndWaitForFuture(myMockView.getStopTreeBusyIndicatorTacker().consume());

    // Assert
    // Two nodes have been added
    DeviceFileEntryNode fooNode = DeviceFileEntryNode.fromNode(getFileEntryPath(myFoo).getLastPathComponent());
    assertNotNull(fooNode);
    assertEquals(6, fooNode.getChildCount());
  }

  public void testFileSystemTree_ContextMenu_Upload_ShowsProblems() throws Exception {
    // Prepare
    DeviceExplorerController controller = createController();

    // Act
    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());
    checkMockViewInitialState(controller, myDevice1);

    expandEntry(myFoo);
    myMockView.getTree().setSelectionPath(getFileEntryPath(myFoo));

    SettableFuture<TreePath> futureTreeChanged = createNodeExpandedFuture(myFoo);
    ActionGroup actionGroup = myMockView.getFileTreeActionGroup();
    AnAction action = getActionByText(actionGroup, "Upload...");
    assertNotNull(action);
    AnActionEvent e = createContentMenuItemEvent();
    action.update(e);

    // Create 15 temporary files, so that we hit the "limit # of problems to display to 10" code path
    List<File> tempFiles = IntStream.range(0, 15)
      .mapToObj(i -> {
        try {
          return FileUtil.createTempFile("foo", ".txt");
        }
        catch (IOException ignored) {
          return null;
        }
      })
      .filter(Objects::nonNull)
      .collect(Collectors.toList());

    // The "Choose file" dialog does not work in headless mode, so we register a custom
    // component that simply returns the tempFile we created above.
    FileChooserFactoryImpl factory = new FileChooserFactoryImpl() {
      @NotNull
      @Override
      public PathChooserDialog createPathChooser(@NotNull FileChooserDescriptor descriptor,
                                                 @Nullable Project project,
                                                 @Nullable Component parent) {
        return (toSelect, callback) -> {
          List<VirtualFile> files = ContainerUtil.map(tempFiles, x -> new VirtualFileWrapper(x).getVirtualFile());
          callback.consume(files);
        };
      }
    };
    ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), FileChooserFactory.class, factory, getTestRootDisposable());

    // Ensure file upload fails
    myDevice1.setUploadError(new AdbShellCommandException("Permission error"));

    // Assert
    assertTrue(e.getPresentation().isVisible());
    assertTrue(e.getPresentation().isEnabled());

    // Act
    myMockView.getStartTreeBusyIndicatorTacker().clear();
    myMockView.getStopTreeBusyIndicatorTacker().clear();
    action.actionPerformed(e);
    pumpEventsAndWaitForFuture(myMockView.getStartTreeBusyIndicatorTacker().consume());
    pumpEventsAndWaitForFuture(myMockView.getUploadFilesTracker().consume());
    pumpEventsAndWaitForFuture(futureTreeChanged);
    pumpEventsAndWaitForFuture(myMockView.getStopTreeBusyIndicatorTacker().consume());
    pumpEventsAndWaitForFuture(myMockView.getReportErrorRelatedToNodeTracker().consume());

    // Assert
    // No node has been added
    DeviceFileEntryNode fooNode = DeviceFileEntryNode.fromNode(getFileEntryPath(myFoo).getLastPathComponent());
    assertNotNull(fooNode);
    assertEquals(4, fooNode.getChildCount());
  }

  public void testFileSystemTree_ContextMenu_Synchronize_Works() throws Exception {
    // Prepare
    DeviceExplorerController controller = createController();
    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());
    checkMockViewInitialState(controller, myDevice1);

    // Expand 2 directories
    expandEntry(myFoo);
    expandEntry(myFooDir);

    // Select 2 nodes, but do not select the "myFooDir" subdirectory, as synchronizing
    // its parent ("myFoo") show implicitly expand all its children too.
    myMockView.getTree().setSelectionPath(getFileEntryPath(myFoo));
    myMockView.getTree().addSelectionPath(getFileEntryPath(myFooFile2));

    ActionGroup actionGroup = myMockView.getFileTreeActionGroup();
    AnAction action = getActionByText(actionGroup, "Synchronize");
    assertNotNull(action);
    AnActionEvent e = createContentMenuItemEvent();
    action.update(e);
    assertTrue(e.getPresentation().isVisible());
    assertTrue(e.getPresentation().isEnabled());

    // Add 1 files in each expanded directory, check the tree does not show them yet
    myFoo.addFile("NewFile.txt");
    myFooDir.addFile("NewFile.txt");
    assertEquals(myFoo.getMockEntries().size() - 1,
                 DeviceFileEntryNode.fromNode(getFileEntryPath(myFoo).getLastPathComponent()).getChildCount());
    assertEquals(myFooDir.getMockEntries().size() - 1,
                 DeviceFileEntryNode.fromNode(getFileEntryPath(myFooDir).getLastPathComponent()).getChildCount());

    SettableFuture<TreePath> futureMyFooChanged = createNodeExpandedFuture(myFoo);
    SettableFuture<TreePath> futureMyFooDirChanged = createNodeExpandedFuture(myFooDir);

    // Act
    action.actionPerformed(e);

    // Assert
    pumpEventsAndWaitForFuture(myMockView.getSynchronizeNodesTracker().consume());
    pumpEventsAndWaitForFuture(futureMyFooChanged);
    pumpEventsAndWaitForFuture(futureMyFooDirChanged);
    assertEquals(myFoo.getMockEntries().size(),
                 DeviceFileEntryNode.fromNode(getFileEntryPath(myFoo).getLastPathComponent()).getChildCount());
    assertEquals(myFooDir.getMockEntries().size(),
                 DeviceFileEntryNode.fromNode(getFileEntryPath(myFooDir).getLastPathComponent()).getChildCount());
  }

  private static <V> List<V> enumerationAsList(Enumeration e) {
    //noinspection unchecked
    return Collections.list(e);
  }

  @SuppressWarnings("SameParameterValue")
  private void replaceTestDialog(@NotNull Function<String, Integer> showFunction) {
    TestDialog previousDialog = TestDialogManager.setTestDialog(showFunction::apply);
    if (myInitialTestDialog == null) {
      myInitialTestDialog = previousDialog;
    }
  }

  @SuppressWarnings("SameParameterValue")
  private void replaceTestInputDialog(@Nullable String returnValue) {
    TestInputDialog previousDialog = TestDialogManager.setTestInputDialog(new TestInputDialog() {
      @Override
      public String show(String message) {
        return show(message, null);
      }

      @Override
      public String show(String message, @Nullable InputValidator validator) {
        if (validator != null) {
          validator.checkInput(message);
        }
        return returnValue;
      }
    });

    if (myInitialTestInputDialog == null) {
      myInitialTestInputDialog = previousDialog;
    }
  }

  @NotNull
  private static AnActionEvent createContentMenuItemEvent() {
    return AnActionEvent.createFromDataContext(ActionPlaces.UNKNOWN, null, dataId -> null);
  }

  @Nullable
  private static AnAction getActionByText(@NotNull ActionGroup actionGroup, @NotNull String text) {
    AnActionEvent e = createContentMenuItemEvent();
    return Arrays.stream(actionGroup.getChildren(null))
      .filter(x -> {
        x.update(e);
        return Objects.equals(text, e.getPresentation().getText());
      })
      .findFirst()
      .orElse(null);
  }

  @SuppressWarnings("SameParameterValue")
  @Nullable
  private static ActionGroup getSubGroup(@NotNull ActionGroup actionGroup, @NotNull String name) {
    return Arrays.stream(actionGroup.getChildren(null))
      .filter(x -> x instanceof ActionGroup)
      .map(x -> (ActionGroup)x)
      .filter(x -> Objects.equals(name, x.getTemplatePresentation().getText()))
      .findFirst()
      .orElse(null);
  }

  private void checkContextMenuItemVisible(@NotNull String menuPath, boolean visible) {
    AnAction action = getContextMenuAction(menuPath);
    AnActionEvent e = createContentMenuItemEvent();
    action.update(e);

    // Assert
    assertEquals(visible, e.getPresentation().isVisible());
    assertEquals(visible, e.getPresentation().isEnabled());
  }

  @NotNull
  private AnAction getContextMenuAction(@NotNull String menuPath) {
    ActionGroup actionGroup = myMockView.getFileTreeActionGroup();
    List<String> menuNames = StringUtil.split(menuPath, "/");
    for (int i = 0; i < menuNames.size() - 1; i++) {
      ActionGroup subGroup = getSubGroup(actionGroup, menuNames.get(i));
      assertNotNull(subGroup);
      actionGroup = subGroup;
    }
    AnAction action = getActionByText(actionGroup, menuNames.get(menuNames.size() - 1));
    assertNotNull(action);
    return action;
  }

  @NotNull
  private SettableFuture<TreePath> createNodeExpandedFuture(@NotNull final MockDeviceFileEntry entry) {
    assert entry.isDirectory();

    TreePath entryPath = getFileEntryPath(entry);
    SettableFuture<TreePath> isNodeExpandedFuture = SettableFuture.create();
    TreeModelListener treeModelAdapter = new TreeModelAdapter() {
      @Override
      protected void process(@NotNull TreeModelEvent event, @NotNull EventType type) {
        if (isNodeFullyUpdated(event)) {
          isNodeExpandedFuture.set(event.getTreePath());
        }
      }

      private boolean isNodeFullyUpdated(TreeModelEvent event) {
        DeviceFileEntryNode entryNode = DeviceFileEntryNode.fromNode(entryPath.getLastPathComponent());
        assertNotNull(entryNode);

        // Ensure this is the final event where we have all children (and not just the
        // "Loading..." child)
        if (!Objects.equals(entryNode, event.getTreePath().getLastPathComponent())) {
          return false;
        }

        if (entryNode.getChildCount() == 1 && entryNode.getChildAt(0) instanceof ErrorNode) {
          return true;
        }

        if (entryNode.getChildCount() != entry.getMockEntries().size()) {
          return false;
        }

        Set<String> nodes = entryNode.getChildEntryNodes().stream().map(x -> x.getEntry().getName()).collect(Collectors.toSet());
        Set<String> entries = entry.getMockEntries().stream().map(MockDeviceFileEntry::getName).collect(Collectors.toSet());
        return nodes.equals(entries);
      }
    };
    myMockView.getTree().getModel().addTreeModelListener(treeModelAdapter);
    myEdtExecutor.addConsumer(isNodeExpandedFuture,
                              (path, throwable) -> myMockView.getTree().getModel().removeTreeModelListener(treeModelAdapter));
    return isNodeExpandedFuture;
  }

  private void checkMockViewInitialState(DeviceExplorerController controller, MockDeviceFileSystem activeDevice)
    throws InterruptedException, ExecutionException, TimeoutException {
    checkMockViewComboBox(controller);
    checkMockViewActiveDevice(activeDevice);
  }

  private void checkMockViewComboBox(DeviceExplorerController controller) {
    // Check we have 2 devices available
    List<DeviceFileSystem> devices = pumpEventsAndWaitForFutures(myMockView.getDeviceAddedTracker().consumeMany(2));
    assertEquals(2, devices.size());
    assertEquals(1, devices.stream().filter(x -> x.getName() == "TestDevice-1").count());
    assertEquals(1, devices.stream().filter(x -> x.getName() == "TestDevice-2").count());

    // The device combo box should contain both devices, and the first one should be selected
    assertEquals(2, myMockView.getDeviceCombo().getItemCount());

    // The first device should be selected automatically
    pumpEventsAndWaitForFuture(myMockView.getDeviceSelectedTracker().consume());
    assertEquals(0, myMockView.getDeviceCombo().getSelectedIndex());
    assertTrue(controller.hasActiveDevice());
  }

  private void checkMockViewActiveDevice(MockDeviceFileSystem activeDevice) {

    pumpEventsAndWaitForFuture(myMockView.getStartTreeBusyIndicatorTacker().consume());
    pumpEventsAndWaitForFuture(myMockView.getStopTreeBusyIndicatorTacker().consume());

    // The root node should have been expanded to show the first level of children
    pumpEventsAndWaitForFuture(myMockView.getTreeNodeExpandingTracker().consume());

    // Check the file system tree is displaying the file system of the first device
    DeviceFileEntryNode rootEntry = DeviceFileEntryNode.fromNode(myMockView.getTree().getModel().getRoot());
    assertNotNull(rootEntry);
    assertEquals(activeDevice.getRoot(), rootEntry.getEntry());

    // Check the file system tree is showing the first level of entries of the file system
    pumpEventsAndWaitForFuture(myMockView.getTreeModelChangedTracker().consume());
    pumpEventsAndWaitForFuture(myMockView.getTreeStructureChangedTacker().consume());
    assertEquals("mock: " + activeDevice.getRoot().getMockEntries() + " rootEntry: " + rootEntry.getChildEntryNodes(),
                 activeDevice.getRoot().getMockEntries().size(), rootEntry.getChildCount());
  }

  public VirtualFile downloadFile(MockDeviceFileEntry file, Runnable trigger) throws Exception {
    // Prepare
    DeviceExplorerController controller = createController();

    // Act
    controller.setup();
    pumpEventsAndWaitForFuture(myMockView.getStartRefreshTracker().consume());
    checkMockViewInitialState(controller, myDevice1);

    myDevice1.setDownloadFileChunkSize(1_000); // download chunks of 1000 bytes at a time
    myDevice1.setDownloadFileChunkIntervalMillis(10); // wait 10 millis between each 1000 bytes chunk
    // Setting the size to 200_000 bytes should force the download to take ~2 seconds,
    // i.e. 200 chunks of 1000 bytes at 100 chunks per second.
    // This allows use to cover the code that animates nodes UI during download.
    file.setSize(200_000);

    // Select node
    TreePath file1Path = getFileEntryPath(file);
    myMockView.getTree().setSelectionPath(file1Path);

    trigger.run();

    // Assert
    pumpEventsAndWaitForFuture(myMockFileManager.getDownloadFileEntryTracker().consume());
    return pumpEventsAndWaitForFuture(myMockFileManager.getDownloadFileEntryCompletionTracker().consume());
  }

  private void expandEntry(@NotNull MockDeviceFileEntry entry) {
    // Attach listener for node expansion completion
    SettableFuture<TreePath> futureNodeExpanded = createNodeExpandedFuture(entry);

    // Expand node
    myMockView.getTree().expandPath(getFileEntryPath(entry));

    // Wait for tree node to be expanded
    pumpEventsAndWaitForFuture(futureNodeExpanded);
  }

  private DeviceExplorerController createController() {
    return createController(myMockView, myMockService, myMockFileOpener, myMockFileManager);
  }

  private DeviceExplorerController createController(DeviceExplorerView view,
                                                    DeviceFileSystemService service,
                                                    DeviceExplorerController.FileOpener fileOpener,
                                                    DeviceExplorerFileManager deviceExplorerFileManager) {
    return new DeviceExplorerController(getProject(), myModel, view, service, deviceExplorerFileManager, fileOpener, myEdtExecutor, myTaskExecutor);
  }

  /**
   * Returns the {@link TreePath} corresponding to a given {@link DeviceFileEntry}.
   * Throws an exception if the file entry is not found.
   */
  @NotNull
  private TreePath getFileEntryPath(@NotNull final MockDeviceFileEntry entry) {
    Stack<MockDeviceFileEntry> entries = getEntryStack(entry);

    List<Object> nodes = new ArrayList<>();
    DeviceFileEntryNode currentNode = DeviceFileEntryNode.fromNode(myMockView.getTree().getModel().getRoot());
    assertNotNull(currentNode);
    MockDeviceFileEntry currentEntry = entries.pop();
    assertEquals(currentNode.getEntry(), currentEntry);
    nodes.add(currentNode);
    while (!entries.isEmpty()) {
      MockDeviceFileEntry newEntry = entries.pop();

      currentEntry = null;
      for (int i = 0; i < myMockView.getTree().getModel().getChildCount(currentNode); i++) {
        DeviceFileEntryNode newNode = DeviceFileEntryNode.fromNode(myMockView.getTree().getModel().getChild(currentNode, i));
        assertNotNull(newNode);
        if (newNode.getEntry() == newEntry) {
          currentNode = newNode;
          currentEntry = newEntry;
          break;
        }
      }
      assertNotNull(String.format("File System Tree does not contain node \"%s\"", entry.getFullPath()), currentEntry);
      nodes.add(currentNode);
    }

    return new TreePath(nodes.toArray());
  }

  @NotNull
  private static Stack<MockDeviceFileEntry> getEntryStack(@NotNull MockDeviceFileEntry entry) {
    Stack<MockDeviceFileEntry> entries = new Stack<>();
    while (entry != null) {
      entries.add(entry);
      entry = (MockDeviceFileEntry)entry.getParent();
    }
    return entries;
  }

  private static void fireEnterKey(@NotNull JComponent component) {
    KeyEvent event = new KeyEvent(component, 0, 0, 0, KeyEvent.VK_ENTER, '\0');
    for (KeyListener listener : component.getKeyListeners()) {
      listener.keyPressed(event);
    }
    for (KeyListener listener : component.getKeyListeners()) {
      listener.keyTyped(event);
    }
    for (KeyListener listener : component.getKeyListeners()) {
      listener.keyReleased(event);
    }
  }

  private static void fireDoubleClick(@NotNull JComponent component, int x, int y) {
    MouseEvent event = new MouseEvent(component, 0, 0, 0, x, y, 2, false, MouseEvent.BUTTON1);
    for (MouseListener listener : component.getMouseListeners()) {
      listener.mouseClicked(event);
    }
    for (MouseListener listener : component.getMouseListeners()) {
      listener.mousePressed(event);
    }
  }

  public static class MockDeviceFileSystemRendererFactory implements DeviceFileSystemRendererFactory {
    @Override
    public DeviceFileSystemRenderer create(DeviceNamePropertiesProvider deviceNamePropertiesProvider) {
      return new MockDeviceFileSystemRenderer();
    }
  }
}
