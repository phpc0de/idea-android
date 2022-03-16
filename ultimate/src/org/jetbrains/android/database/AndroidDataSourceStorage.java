// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.android.database;

import com.intellij.database.Dbms;
import com.intellij.database.util.DbImplUtilCore;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.annotations.XCollection;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

@State(name = "AndroidDataSourceStorage", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class AndroidDataSourceStorage implements PersistentStateComponent<AndroidDataSourceStorage.State> {
  private final List<AndroidDataSource> myDataSources = ContainerUtil.createLockFreeCopyOnWriteList();

  public static AndroidDataSourceStorage getInstance(Project project) {
    return project.getService(AndroidDataSourceStorage.class);
  }

  @Override
  public State getState() {
    State state = new State();
    for (AndroidDataSource dataSource : myDataSources) {
      state.myDataSources.add(dataSource.buildFullState());
    }
    return state;
  }

  @Override
  public void loadState(@NotNull State state) {
    myDataSources.clear();
    for (AndroidDataSource.State dataSourceState : state.myDataSources) {
      AndroidDataSource o = new AndroidDataSource();
      o.loadState(dataSourceState);
      addDataSource(o);
    }
    for (AndroidDataSource o : myDataSources) {
      o.ensureDriverConfigured();
    }
  }

  public List<AndroidDataSource> getDataSources() {
    return new ArrayList<>(myDataSources);
  }

  public void removeDataSource(AndroidDataSource dataSource) {
    myDataSources.remove(dataSource);
  }

  public void addDataSource(AndroidDataSource dataSource) {
    dataSource.setDatabaseDriver(DbImplUtilCore.guessDatabaseDriver(Dbms.SQLITE));
    myDataSources.add(dataSource);
  }

  public static class State {
    @XCollection(propertyElementName = "data-sources")
    public final List<AndroidDataSource.State> myDataSources = new SmartList<>();
  }
}
