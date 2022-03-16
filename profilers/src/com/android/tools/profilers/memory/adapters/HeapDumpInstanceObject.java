/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.tools.profilers.memory.adapters;

import com.google.common.annotations.VisibleForTesting;
import com.android.tools.perflib.heap.*;
import com.android.tools.perflib.heap.ClassInstance.FieldValue;
import com.android.tools.profiler.proto.Memory.AllocationStack;
import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.android.tools.profilers.memory.adapters.ValueObject.ValueType.*;

/**
 * A UI representation of a {@link ClassInstance}.
 */
class HeapDumpInstanceObject implements InstanceObject {
  private static final String NAME_FORMATTER = "%s@%d (0x%x)";
  private static final int MAX_VALUE_TEXT_LENGTH = 1024;
  private static final Comparator<Instance> DEPTH_THEN_ID_COMPARATOR =
    // compare by id after depth to enforce more deterministic order
    Comparator.comparingInt(Instance::getDistanceToGcRoot).thenComparingLong(Instance::getId);
  private static final String INVALID_STRING_VALUE = " ...<invalid string value>...";
  private static final Map<Type, ValueType> VALUE_TYPE_MAP = ImmutableMap.<Type, ValueObject.ValueType>builder()
    .put(Type.BOOLEAN, BOOLEAN)
    .put(Type.BYTE, BYTE)
    .put(Type.CHAR, CHAR)
    .put(Type.SHORT, SHORT)
    .put(Type.INT, INT)
    .put(Type.LONG, LONG)
    .put(Type.FLOAT, FLOAT)
    .put(Type.DOUBLE, DOUBLE)
    .put(Type.OBJECT, OBJECT)
    .build();

  @NotNull protected ValueType myValueType;

  @NotNull private final HeapDumpCaptureObject myCaptureObject;
  @NotNull private final Instance myInstance;
  @NotNull private final ClassDb.ClassEntry myClassEntry;

  HeapDumpInstanceObject(@NotNull HeapDumpCaptureObject captureObject,
                         @NotNull Instance instance,
                         @NotNull ClassDb.ClassEntry classEntry,
                         @Nullable ValueType precomputedValueType) {
    myCaptureObject = captureObject;
    myInstance = instance;
    myClassEntry = classEntry;

    if (precomputedValueType != null) {
      myValueType = precomputedValueType;
      return;
    }

    ClassObj classObj = instance.getClassObj();
    if (instance instanceof ClassObj) {
      myValueType = CLASS;
    }
    else if (instance instanceof ClassInstance && classObj.getClassName().equals(ClassDb.JAVA_LANG_STRING)) {
      myValueType = STRING;
    }
    else if (classObj.getClassName().endsWith("[]")) {
      myValueType = ARRAY;
    }
    else {
      myValueType = OBJECT;
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof HeapDumpInstanceObject)) {
      return false;
    }

    HeapDumpInstanceObject otherInstance = (HeapDumpInstanceObject)obj;
    return myInstance == otherInstance.myInstance;
  }

  @Override
  public int hashCode() {
    // cheap hashcode implementation in sync with `equals` as defined above
    return System.identityHashCode(myInstance);
  }

  @NotNull
  @Override
  public String getName() {
    return "";
  }

  @NotNull
  @Override
  public String getValueText() {
    // TODO show length of array instance
    return String.format(NAME_FORMATTER,
                         myClassEntry.getSimpleClassName(),
                         myInstance.getUniqueId(),
                         myInstance.getUniqueId());
  }

  @NotNull
  @Override
  public String getToStringText() {
    if (myValueType == STRING) {
      String text = ((ClassInstance)myInstance).getAsString(MAX_VALUE_TEXT_LENGTH);
      if (text != null) {
        int textLength = text.length();
        StringBuilder builder = new StringBuilder(6 + textLength);
        builder.append("\"");
        if (textLength == MAX_VALUE_TEXT_LENGTH) {
          builder.append(text, 0, textLength - 1).append("...");
        }
        else {
          builder.append(text);
        }
        builder.append("\"");
        return builder.toString();
      }
      else {
        return INVALID_STRING_VALUE;
      }
    }
    return "";
  }

  @Override
  public int getHeapId() {
    return myInstance.getHeap().getId();
  }

  @NotNull
  @Override
  public ClassDb.ClassEntry getClassEntry() {
    return myClassEntry;
  }

  @Override
  public int getDepth() {
    return myInstance.getDistanceToGcRoot();
  }

  @Override
  public long getNativeSize() {
    return myInstance.getNativeSize();
  }

  @Override
  public int getShallowSize() {
    return myInstance.getSize();
  }

  @Override
  public long getRetainedSize() {
    return myInstance.getTotalRetainedSize();
  }

  @Override
  public int getFieldCount() {
    if (myInstance instanceof ClassInstance) {
      ClassInstance classInstance = (ClassInstance)myInstance;
      return classInstance.getValues().size();
    }
    else if (myInstance instanceof ArrayInstance) {
      ArrayInstance arrayInstance = (ArrayInstance)myInstance;
      return arrayInstance.getLength();
    }
    else if (myInstance instanceof ClassObj) {
      ClassObj classObj = (ClassObj)myInstance;
      return classObj.getStaticFieldValues().size();
    }
    return 0;
  }

  @NotNull
  @Override
  public List<FieldObject> getFields() {
    List<FieldObject> fields = new ArrayList<>();
    if (myInstance instanceof ClassInstance) {
      ClassInstance classInstance = (ClassInstance)myInstance;
      for (FieldValue field : classInstance.getValues()) {
        fields.add(new HeapDumpFieldObject(myCaptureObject, myInstance, field));
      }
    }
    else if (myInstance instanceof ArrayInstance) {
      ArrayInstance arrayInstance = (ArrayInstance)myInstance;
      Type arrayType = arrayInstance.getArrayType();
      int arrayIndex = 0;
      for (Object value : arrayInstance.getValues()) {
        FieldValue field = new FieldValue(new Field(arrayType, Integer.toString(arrayIndex)), value);
        fields.add(new HeapDumpFieldObject(myCaptureObject, myInstance, field));
        arrayIndex++;
      }
    }
    else if (myInstance instanceof ClassObj) {
      ClassObj classObj = (ClassObj)myInstance;
      for (Map.Entry<Field, Object> entry : classObj.getStaticFieldValues().entrySet()) {
        FieldValue field = new FieldValue(entry.getKey(), entry.getValue());
        fields.add(new HeapDumpFieldObject(myCaptureObject, myInstance, field));
      }
    }

    return fields;
  }

  @Nullable
  @Override
  public ArrayObject getArrayObject() {
    if (!(myInstance instanceof ArrayInstance)) {
      return null;
    }

    ArrayInstance arrayInstance = (ArrayInstance)myInstance;
    return new ArrayObject() {
      @NotNull
      @Override
      public ValueType getArrayElementType() {
        return VALUE_TYPE_MAP.get(arrayInstance.getArrayType());
      }

      @Nullable
      @Override
      public byte[] getAsByteArray() {
        if (getArrayElementType() == BYTE) {
          return arrayInstance.asRawByteArray(0, arrayInstance.getLength());
        }
        return null;
      }

      @Nullable
      @Override
      public char[] getAsCharArray() {
        if (getArrayElementType() == CHAR) {
          return arrayInstance.asCharArray(0, arrayInstance.getLength());
        }
        return null;
      }

      @NotNull
      @Override
      public Object[] getAsArray() {
        return arrayInstance.getValues();
      }

      @Override
      public int getArrayLength() {
        return arrayInstance.getLength();
      }
    };
  }

  @Override
  @NotNull
  public ValueType getValueType() {
    return myValueType;
  }

  @Nullable
  @Override
  public AllocationStack getAllocationCallStack() {
    if (myInstance.getStack() == null) {
      return null;
    }

    AllocationStack.Builder builder = AllocationStack.newBuilder();
    AllocationStack.StackFrameWrapper.Builder frameBuilder = AllocationStack.StackFrameWrapper.newBuilder();
    for (StackFrame stackFrame : myInstance.getStack().getFrames()) {
      String fileName = stackFrame.getFilename();
      String guessedClassName = fileName.endsWith(".java") ? fileName.substring(0, fileName.length() - ".java".length()) : fileName;
      frameBuilder.addFrames(
        AllocationStack.StackFrame.newBuilder().setClassName(guessedClassName).setMethodName(stackFrame.getMethodName())
          .setLineNumber(stackFrame.getLineNumber()).setFileName(fileName).build());
    }
    builder.setFullStack(frameBuilder);
    return builder.build();
  }

  @Override
  public boolean isCallStackEmpty() {
    return myInstance.getStack() == null || myInstance.getStack().getFrames().length == 0;
  }

  @Override
  public boolean getIsRoot() {
    return myInstance instanceof RootObj;
  }

  @NotNull
  @Override
  public List<ReferenceObject> getReferences() {
    return getIsRoot() ? Collections.EMPTY_LIST : extractReferences();
  }

  @VisibleForTesting
  @NotNull
  public List<ReferenceObject> extractReferences() {
    // Sort hard referrers to appear first.
    List<Instance> sortedReferences = new ArrayList<>(myInstance.getHardReverseReferences());
    sortedReferences.sort(DEPTH_THEN_ID_COMPARATOR);

    // Sort soft referrers to appear second.
    if (myInstance.getSoftReverseReferences() != null) {
      List<Instance> sortedSoftReferences = new ArrayList<>(myInstance.getSoftReverseReferences());
      sortedSoftReferences.sort(DEPTH_THEN_ID_COMPARATOR);
      sortedReferences.addAll(sortedSoftReferences);
    }

    List<ReferenceObject> referrers = new ArrayList<>(sortedReferences.size());

    // Determine the variable names of references.
    for (Instance reference : sortedReferences) {
      // Note that each instance can have multiple references to the same object.
      List<String> referencingFieldNames = new ArrayList<>(3);
      if (reference instanceof ClassInstance) {
        ClassInstance classInstance = (ClassInstance)reference;
        for (ClassInstance.FieldValue entry : classInstance.getValues()) {
          // This instance is referenced by a field of the referrer class
          if (entry.getField().getType() == Type.OBJECT && entry.getValue() == myInstance) {
            referencingFieldNames.add(entry.getField().getName());
          }
        }
      }
      else if (reference instanceof ArrayInstance) {
        ArrayInstance arrayInstance = (ArrayInstance)reference;
        assert arrayInstance.getArrayType() == Type.OBJECT;
        Object[] values = arrayInstance.getValues();
        for (int i = 0; i < values.length; ++i) {
          // This instance is referenced by an array
          if (values[i] == myInstance) {
            referencingFieldNames.add(String.valueOf(i));
          }
        }
      }
      else if (reference instanceof ClassObj) {
        ClassObj classObj = (ClassObj)reference;
        Map<Field, Object> staticValues = classObj.getStaticFieldValues();
        for (Map.Entry<Field, Object> entry : staticValues.entrySet()) {
          // This instance is referenced by a static field of a Class object.
          if (entry.getKey().getType() == Type.OBJECT && entry.getValue() == myInstance) {
            referencingFieldNames.add(entry.getKey().getName());
          }
        }
      }

      InstanceObject referencingInstance = myCaptureObject.findInstanceObject(reference);
      assert referencingInstance != null;
      referrers.add(new ReferenceObject(referencingFieldNames, referencingInstance));
    }

    return referrers;
  }
}
