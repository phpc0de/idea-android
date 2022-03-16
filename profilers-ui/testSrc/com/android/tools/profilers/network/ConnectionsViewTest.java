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
package com.android.tools.profilers.network;

import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.Assert.assertThat;

import com.android.tools.adtui.model.FakeTimer;
import com.android.tools.idea.transport.faketransport.FakeGrpcChannel;
import com.android.tools.idea.transport.faketransport.FakeTransportService;
import com.android.tools.profilers.FakeIdeProfilerComponents;
import com.android.tools.profilers.FakeIdeProfilerServices;
import com.android.tools.profilers.FakeProfilerService;
import com.android.tools.profilers.ProfilerClient;
import com.android.tools.profilers.StudioProfilers;
import com.android.tools.profilers.StudioProfilersView;
import com.android.tools.profilers.network.httpdata.HttpData;
import com.google.common.collect.ImmutableList;
import com.intellij.testFramework.EdtRule;
import com.intellij.testFramework.RunsInEdt;
import java.awt.Color;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@RunsInEdt
public class ConnectionsViewTest {
  private static final ImmutableList<HttpData> FAKE_DATA =
    new ImmutableList.Builder<HttpData>()
      .add(TestHttpData.newBuilder(1, 1, 2).build())
      .add(TestHttpData.newBuilder(2, 3, 5).build())
      .add(TestHttpData.newBuilder(3, 8, 13)
             .setResponsePayloadSize(TestHttpData.fakeContentSize(3))
             .build())
      .add(TestHttpData.newBuilder(4, 21, 34)
             .setResponseFields(TestHttpData.fakeResponseFields(4, "bmp"))
             .build())
      .build();

  private final FakeTimer myTimer = new FakeTimer();
  @Rule public FakeGrpcChannel myGrpcChannel =
    new FakeGrpcChannel("ConnectionsViewTest", new FakeTransportService(myTimer, false), new FakeProfilerService(myTimer),
                        FakeNetworkService.newBuilder().setHttpDataList(FAKE_DATA).build());
  @Rule public final EdtRule myEdtRule = new EdtRule();

  private NetworkProfilerStageView myStageView;

  @Before
  public void setUp() {
    StudioProfilers profilers = new StudioProfilers(new ProfilerClient(myGrpcChannel.getChannel()), new FakeIdeProfilerServices(), myTimer);
    StudioProfilersView profilersView = new StudioProfilersView(profilers, new FakeIdeProfilerComponents());
    myStageView = new NetworkProfilerStageView(profilersView, new NetworkProfilerStage(profilers));
  }

  @Test
  public void logicToExtractColumnValuesFromDataWorks() throws Exception {
    HttpData data = FAKE_DATA.get(2); // Request: id = 3, time = 8->13
    long id = data.getId();
    assertThat(id, is(3L));

    // ID is set as the URL name, e.g. example.com/{id}, by TestHttpData
    assertThat(ConnectionsView.Column.NAME.getValueFrom(data), is(Long.toString(id)));
    assertThat(ConnectionsView.Column.SIZE.getValueFrom(data), is(TestHttpData.fakeContentSize(id)));
    assertThat(TestHttpData.FAKE_CONTENT_TYPE, endsWith((String)ConnectionsView.Column.TYPE.getValueFrom(data)));
    assertThat(ConnectionsView.Column.STATUS.getValueFrom(data), is(TestHttpData.FAKE_RESPONSE_CODE));
    assertThat(ConnectionsView.Column.TIME.getValueFrom(data), is(TimeUnit.SECONDS.toMicros(5)));
    assertThat(ConnectionsView.Column.TIMELINE.getValueFrom(data), is(TimeUnit.SECONDS.toMicros(8)));
  }

  @Test
  public void mimeTypeContainingMultipleComponentsIsTruncated() throws Exception {
    HttpData data = FAKE_DATA.get(0); // Request: id = 1
    long id = data.getId();
    assertThat(id, is(1L));
    assertThat(TestHttpData.FAKE_CONTENT_TYPE, allOf(endsWith((String)ConnectionsView.Column.TYPE.getValueFrom(data)),
                                                     not((String)ConnectionsView.Column.TYPE.getValueFrom(data))));
  }

  @Test
  public void simpleMimeTypeIsCorrectlyDisplayed() throws Exception {
    HttpData data = FAKE_DATA.get(3); // Request: id = 4
    long id = data.getId();
    assertThat(id, is(4L));
    assertThat("bmp", is((String)ConnectionsView.Column.TYPE.getValueFrom(data)));
  }

  @Test
  public void dataRangeControlsVisibleConnections() throws Exception {
    ConnectionsView view = new ConnectionsView(myStageView);
    JTable table = getConnectionsTable(view);

    assertThat(table.getRowCount(), is(0));

    myStageView.getStage().getTimeline().getSelectionRange().set(TimeUnit.SECONDS.toMicros(3), TimeUnit.SECONDS.toMicros(10));
    assertThat(table.getRowCount(), is(2));

    myStageView.getStage().getTimeline().getSelectionRange().set(0, 0);
    assertThat(table.getRowCount(), is(0));
  }

  @Test
  public void activeConnectionIsAutoFocusedByTable() throws Exception {
    ConnectionsView view = new ConnectionsView(myStageView);

    JTable table = getConnectionsTable(view);
    final int[] selectedRow = {-1};

    // We arbitrarily select one of the fake data instances and sanity check that the table
    // auto-selects it, which is checked for below.
    int arbitraryIndex = 1;
    HttpData activeData = FAKE_DATA.get(arbitraryIndex);
    myStageView.getStage().setSelectedConnection(activeData);

    CountDownLatch latchSelected = new CountDownLatch(1);
    table.getSelectionModel().addListSelectionListener(e -> {
      selectedRow[0] = e.getFirstIndex();
      latchSelected.countDown();
    });

    myStageView.getStage().getTimeline().getSelectionRange().set(0, TimeUnit.SECONDS.toMicros(100));
    latchSelected.await();
    assertThat(selectedRow[0], is(arbitraryIndex));
  }

  @Test
  public void tableCanBeSorted() throws Exception {
    myStageView.getStage().getTimeline().getSelectionRange().set(0, TimeUnit.SECONDS.toMicros(100));
    ConnectionsView view = new ConnectionsView(myStageView);

    JTable table = getConnectionsTable(view);

    // Times: 1, 2, 5, 13. Should sort numerically, not alphabetically (e.g. not 1, 13, 2, 5)
    // Toggle once for ascending, twice for descending
    table.getRowSorter().toggleSortOrder(ConnectionsView.Column.TIME.ordinal());
    table.getRowSorter().toggleSortOrder(ConnectionsView.Column.TIME.ordinal());

    // After reverse sorting, data should be backwards
    assertThat(table.getRowCount(), is(4));
    assertThat(table.convertRowIndexToView(0), is(3));
    assertThat(table.convertRowIndexToView(1), is(2));
    assertThat(table.convertRowIndexToView(2), is(1));
    assertThat(table.convertRowIndexToView(3), is(0));

    myStageView.getStage().getTimeline().getSelectionRange().set(0, 0);
    assertThat(table.getRowCount(), is(0));

    // Include middle two requests: 3->5 (time = 2), and 8->13 (time=5)
    // This should still be shown in reverse sorted over
    myStageView.getStage().getTimeline().getSelectionRange().set(TimeUnit.SECONDS.toMicros(3), TimeUnit.SECONDS.toMicros(10));
    assertThat(table.getRowCount(), is(2));
    assertThat(table.convertRowIndexToView(0), is(1));
    assertThat(table.convertRowIndexToView(1), is(0));
  }

  @Test
  public void testTableRowHighlight() {
    myStageView.getStage().getTimeline().getSelectionRange().set(0, TimeUnit.SECONDS.toMicros(100));
    ConnectionsView view = new ConnectionsView(myStageView);
    int timelineColumn = ConnectionsView.Column.TIMELINE.ordinal();
    JTable table = getConnectionsTable(view);

    Color backgroundColor = Color.YELLOW;
    Color selectionColor = Color.BLUE;
    table.setBackground(backgroundColor);
    table.setSelectionBackground(selectionColor);

    TableCellRenderer renderer = table.getCellRenderer(1, timelineColumn);
    assertThat(table.prepareRenderer(renderer, 1, timelineColumn).getBackground(), is(backgroundColor));

    table.setRowSelectionInterval(1, 1);
    assertThat(table.prepareRenderer(renderer, 1, timelineColumn).getBackground(), is(selectionColor));
  }

  /**
   * The underlying table in ConnectionsView is intentionally not exposed to regular users of the
   * class. However, for tests, it is useful to inspect the contents of the table to verify it was
   * updated.
   */
  private static JTable getConnectionsTable(ConnectionsView view) {
    return (JTable)view.getComponent();
  }
}