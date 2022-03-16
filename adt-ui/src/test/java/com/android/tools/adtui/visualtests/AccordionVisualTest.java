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

package com.android.tools.adtui.visualtests;

import static com.google.common.util.concurrent.MoreExecutors.newDirectExecutorService;

import com.android.tools.adtui.AccordionLayout;
import com.android.tools.adtui.AnimatedTimeRange;
import com.android.tools.adtui.chart.linechart.LineChart;
import com.android.tools.adtui.common.AdtUiUtils;
import com.android.tools.adtui.model.*;
import com.android.tools.adtui.model.updater.Updatable;
import com.intellij.ui.components.JBPanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AccordionVisualTest extends VisualTest {

  private static final int DOUBLE_CLICK = 2;
  private static final int LINECHART_DATA_DELAY = 100; // milliseconds.

  private static final int MIN_SIZE = 50;
  private static final int PREFERRED_SIZE = 100;
  private static final int MAX_SIZE = 200;

  private int mChartCountX;

  private int mChartCountY;

  private long mStartTimeUs;

  private AccordionLayout mAccordionX;

  private AccordionLayout mAccordionY;

  private JBPanel mPanelX;

  private JBPanel mPanelY;

  private AnimatedTimeRange mAnimatedTimeRange;

  private ArrayList<RangedContinuousSeries> mRangedData;

  private ArrayList<DefaultDataSeries<Long>> mData;

  @Override
  protected List<Updatable> createModelList() {
    mStartTimeUs = TimeUnit.NANOSECONDS.toMicros(System.nanoTime());
    Range timeGlobalRangeUs = new Range(0, 0);
    mAnimatedTimeRange = new AnimatedTimeRange(timeGlobalRangeUs, mStartTimeUs);

    mPanelX = new JBPanel();
    mPanelY = new JBPanel();
    mAccordionX = new AccordionLayout(mPanelX, AccordionLayout.Orientation.HORIZONTAL);
    mAccordionY = new AccordionLayout(mPanelY, AccordionLayout.Orientation.VERTICAL);
    mPanelX.setLayout(mAccordionX);
    mPanelY.setLayout(mAccordionY);
    List<Updatable> componentsList = new ArrayList<>();

    // Add the scene components to the list
    componentsList.add(mAccordionX);
    componentsList.add(mAccordionY);
    componentsList.add(mAnimatedTimeRange);

    mRangedData = new ArrayList<>();
    mData = new ArrayList<>();

    Range yRange = new Range(0.0, 100.0);
    for (int i = 0; i < 4; i++) {
      if (i % 2 == 0) {
        yRange = new Range(0.0, 100.0);
      }
      DefaultDataSeries<Long> series = new DefaultDataSeries<>();
      RangedContinuousSeries ranged = new RangedContinuousSeries("Widgets #" + i, timeGlobalRangeUs, yRange, series);
      mRangedData.add(ranged);
      mData.add(series);
    }
    return componentsList;
  }

  @Override
  protected void reset() {
    super.reset();

    mChartCountY = 0;
    mChartCountX = 0;
  }

  @Override
  public String getName() {
    return "Accordion";
  }

  @Override
  protected void populateUi(@NotNull JPanel panel) {
    panel.setLayout(new GridLayout(0, 1));

    Thread mUpdateDataThread = new Thread(() -> {
      try {
        while (true) {
          long nowUs = TimeUnit.NANOSECONDS.toMicros(System.nanoTime()) - mStartTimeUs;
          for (DefaultDataSeries<Long> series : mData) {
            List<SeriesData<Long>> data = series.getAllData();
            long last = data.isEmpty() ? 0 : data.get(data.size() - 1).value;
            float delta = 10 * ((float)Math.random() - 0.45f);
            series.add(nowUs, last + (long)delta);
          }
          Thread.sleep(LINECHART_DATA_DELAY);
        }
      }
      catch (InterruptedException e) {
      }
    }, "AccordionVisualTest#populateUi");
    mUpdateDataThread.start();
    // Creates the vertical accordion at the top half.
    JBPanel yPanel = new JBPanel();
    panel.add(yPanel);
    yPanel.setBorder(BorderFactory.createLineBorder(AdtUiUtils.DEFAULT_BORDER_COLOR));
    final JPanel controlsY = VisualTest.createControlledPane(yPanel, mPanelY);

    controlsY.add(VisualTest.createButton("Reset Weights", listener -> mAccordionY.resetComponents()));
    controlsY.add(VisualTest.createButton("Add Chart", listener -> {
      final LineChart chart = generateChart(mAccordionY, AccordionLayout.Orientation.VERTICAL,
                                            0, PREFERRED_SIZE, Integer.MAX_VALUE);
      mPanelY.add(chart);
      mChartCountY++;
    }));
    controlsY.add(VisualTest.createButton("Add Chart With Min", listener -> {
      final LineChart chart = generateChart(mAccordionY, AccordionLayout.Orientation.VERTICAL,
                                            MIN_SIZE, PREFERRED_SIZE, Integer.MAX_VALUE);
      mPanelY.add(chart);
      mChartCountY++;
    }));
    controlsY.add(VisualTest.createButton("Add Chart With Small Max", listener -> {
      final LineChart chart = generateChart(mAccordionY, AccordionLayout.Orientation.VERTICAL,
                                            0, PREFERRED_SIZE, MAX_SIZE);
      mPanelY.add(chart);
      mChartCountY++;
    }));
    controlsY.add(VisualTest.createButton("Remove Last Chart", listener -> {
      mPanelY.remove(--mChartCountY);
    }));

    controlsY.add(
      new Box.Filler(new Dimension(0, 0), new Dimension(300, Integer.MAX_VALUE),
                     new Dimension(300, Integer.MAX_VALUE)));

    // Creates the horizontal accordion at the bottom half.
    JBPanel xPanel = new JBPanel();
    panel.add(xPanel);
    xPanel.setBorder(BorderFactory.createLineBorder(AdtUiUtils.DEFAULT_BORDER_COLOR));
    final JPanel controlsX = VisualTest.createControlledPane(xPanel, mPanelX);
    controlsX.add(VisualTest.createButton("Reset Weights", listener -> {
      mAccordionX.resetComponents();
    }));
    controlsX.add(VisualTest.createButton("Add Chart", listener -> {
      final LineChart chart = generateChart(mAccordionX, AccordionLayout.Orientation.HORIZONTAL,
                                            0, PREFERRED_SIZE, Integer.MAX_VALUE);
      mPanelX.add(chart);
      mChartCountX++;
    }));
    controlsX.add(VisualTest.createButton("Add Chart With Min", listener -> {
      final LineChart chart = generateChart(mAccordionX, AccordionLayout.Orientation.HORIZONTAL,
                                            MIN_SIZE, PREFERRED_SIZE, Integer.MAX_VALUE);
      mPanelX.add(chart);
      mChartCountX++;
    }));
    controlsX.add(VisualTest.createButton("Add Chart With Small Max", listener -> {
      final LineChart chart = generateChart(mAccordionX, AccordionLayout.Orientation.HORIZONTAL,
                                            0, PREFERRED_SIZE, MAX_SIZE);
      mPanelX.add(chart);
      mChartCountX++;
    }));
    controlsX.add(VisualTest.createButton("Remove Last Chart", listener -> mPanelX.remove(--mChartCountX)));

    controlsX.add(
      new Box.Filler(new Dimension(0, 0), new Dimension(300, Integer.MAX_VALUE),
                     new Dimension(300, Integer.MAX_VALUE)));
  }

  @NotNull
  private LineChart generateChart(AccordionLayout layout, AccordionLayout.Orientation direction,
                                  int minSize, int preferredSize, int maxSize) {
    ArrayList<RangedContinuousSeries> data = mRangedData;
    LineChartModel model = new LineChartModel(newDirectExecutorService());
    model.addAll(data);
    LineChart chart = new LineChart(model);
    if (direction == AccordionLayout.Orientation.VERTICAL) {
      chart.setMinimumSize(new Dimension(0, minSize));
      chart.setPreferredSize(new Dimension(0, preferredSize));
      chart.setMaximumSize(new Dimension(0, maxSize));
    }
    else {
      chart.setMinimumSize(new Dimension(minSize, 0));
      chart.setPreferredSize(new Dimension(preferredSize, 0));
      chart.setMaximumSize(new Dimension(maxSize, 0));
    }
    chart.setBorder(BorderFactory.createLineBorder(Color.GRAY));
    chart.setToolTipText("Double-click to maximize. Ctrl+Double-click to minimize.");
    chart.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == DOUBLE_CLICK) {
          if (e.isControlDown()) {
            layout.toggleMinimize(chart);
          }
          else {
            layout.toggleMaximize(chart);
          }
        }
      }
    });
    addToUpdater(model);
    return chart;
  }
}
