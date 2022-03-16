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
package com.android.tools.idea.uibuilder.model;

import com.android.tools.idea.common.api.InsertType;
import com.android.tools.idea.common.model.ItemTransferable;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import org.jetbrains.annotations.NotNull;

/**
 * This class encapsulates either a {@see DropTargetDragEvent} or a {@see DropTargetDropEvent}
 * such that we can access either instance in code common for the 2 cases.
 * <p/>
 * Also ensure that accept is called before data is retrieved from a source that has text flavor but no designer flavor.
 */
public class NlDropEvent {
  private final DropTargetDragEvent myDragEvent;
  private final DropTargetDropEvent myDropEvent;
  private boolean myStatusSpecified;

  public NlDropEvent(@NotNull DropTargetDragEvent dragEvent) {
    myDragEvent = dragEvent;
    myDropEvent = null;
  }

  public NlDropEvent(@NotNull DropTargetDropEvent dropEvent) {
    myDragEvent = null;
    myDropEvent = dropEvent;
  }

  @NotNull
  public Point getLocation() {
    if (myDragEvent != null) {
      return myDragEvent.getLocation();
    }
    else {
      return myDropEvent.getLocation();
    }
  }

  public boolean isDataFlavorSupported(@NotNull DataFlavor flavor) {
    if (myDragEvent != null) {
      return myDragEvent.isDataFlavorSupported(flavor);
    }
    else {
      return myDropEvent.isDataFlavorSupported(flavor);
    }
  }

  public int getDropAction() {
    if (myDragEvent != null) {
      return myDragEvent.getDropAction();
    }
    else {
      return myDropEvent.getDropAction();
    }
  }

  @NotNull
  public Transferable getTransferable() {
    if (!myStatusSpecified &&
        !isDataFlavorSupported(ItemTransferable.DESIGNER_FLAVOR) &&
        isDataFlavorSupported(DataFlavor.stringFlavor)) {
      accept(DnDConstants.ACTION_COPY);
    }
    if (myDragEvent != null) {
      return myDragEvent.getTransferable();
    }
    else {
      return myDropEvent.getTransferable();
    }
  }

  public void accept(@NotNull InsertType insertType) {
    // This determines how the DnD source acts to a completed drop.
    // If we set the accepted drop action to DndConstants.ACTION_MOVE then the source should delete the source component.
    // When we move a component within the current designer the addComponents call will remove the source component in the transaction.
    // Only when we move a component from a different designer (handled as a InsertType.COPY) would we mark this as a ACTION_MOVE.
    accept(insertType == InsertType.COPY ? getDropAction() : DnDConstants.ACTION_COPY);
  }


  private void accept(int dropAction) {
    if (!myStatusSpecified) {
      if (myDragEvent != null) {
        myDragEvent.acceptDrag(dropAction);
      }
      else {
        myDropEvent.acceptDrop(dropAction);
      }
      myStatusSpecified = true;
    }
  }

  public void reject() {
    if (!myStatusSpecified) {
      if (myDragEvent != null) {
        myDragEvent.rejectDrag();
      }
      else {
        myDropEvent.rejectDrop();
      }
      myStatusSpecified = true;
    }
  }

  public void complete() {
    if (myDropEvent != null) {
      myDropEvent.dropComplete(true);
    }
  }
}
