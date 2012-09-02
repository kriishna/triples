package com.antsapps.triples;

import java.util.Arrays;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.View;

import com.antsapps.triples.backend.Game;
import com.antsapps.triples.backend.Statistics;

public class HistogramView extends View {

  private static final int COLUMN_PADDING = 2;
  private static final int MAX_TIME = 30;
  private static final int NUM_TIME_LABELS = 5;
  private static final int NUM_GRIDLINES = 4;

  private static final float TEXT_HEIGHT = 24f;
  private static final float HEIGHT_OVER_WIDTH = (float) ((Math.sqrt(5) - 1) / 2);
  private static final String X_AXIS_TITLE = "Time (minutes)";
  private Statistics mStatistics;
  private float mYLabelWidth;

  private static final Paint TEXT_PAINT = new Paint();
  static {
    TEXT_PAINT.setTextSize(TEXT_HEIGHT);
  }

  private static final float BUFFER = 8;

  private final int[] mBins = new int[MAX_TIME + 1];
  private int mMaxMinutes;
  private int mMaxBinSize;
  private float mGraphWidth;
  private float mGraphHeight;

  public HistogramView(Context context) {
    super(context);
  }

  public HistogramView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public void setStatistics(Statistics statistics) {
    mStatistics = statistics;
    calcBins();
    invalidate();
  }

  private void calcBins() {
    mMaxMinutes = 0;
    mMaxBinSize = 0;
    Arrays.fill(mBins, 0);
    for (Game game : mStatistics.getData()) {
      int bin = Math.min(convertTimeToMinutes(game.getTimeElapsed()), MAX_TIME);
      mBins[bin]++;
      mMaxMinutes = Math.max(mMaxMinutes, bin);
      mMaxBinSize = Math.max(mMaxBinSize, mBins[bin]);
    }
    mMaxMinutes = roundUpToNearestMultiple(mMaxMinutes, NUM_TIME_LABELS);
    mMaxBinSize = roundUpToNearestMultiple(mMaxBinSize, NUM_GRIDLINES);
  }

  private static int roundUpToNearestMultiple(float number, int factor) {
    return (int) (FloatMath.ceil(number / factor)) * factor;
  }

  private int convertTimeToMinutes(long time) {
    return (int) time / (60 * 1000);
  }

  @Override
  protected void onMeasure(final int widthMeasureSpec,
      final int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    updateMeasuredDimensions(widthMeasureSpec, heightMeasureSpec);
  }

  private void updateMeasuredDimensions(final int widthMeasureSpec,
      final int heightMeasureSpec) {
    int widthOfCards = getDefaultSize(getMeasuredWidth(), widthMeasureSpec);
    int heightOfCards = (int) (widthOfCards * HEIGHT_OVER_WIDTH);
    setMeasuredDimension(
        widthOfCards,
        getDefaultSize(heightOfCards, heightMeasureSpec));
  }

  @Override
  protected void onDraw(Canvas canvas) {
    if (mStatistics != null) {
      if (mStatistics.getNumGames() == 0) {
        drawNoGames(canvas);
      } else {
        calcDimens();
        drawLabels(canvas);
        drawGridlines(canvas);
        drawPlot(canvas);
        drawAxes(canvas);
        drawAxesTitles(canvas);
      }
    }
  }

  private void drawNoGames(Canvas canvas) {
    Paint paint = new Paint(TEXT_PAINT);
    paint.setTextAlign(Align.CENTER);
    canvas.drawText(
        "No games completed",
        getWidth() / 2,
        getHeight() / 2,
        paint);
  }

  private void calcDimens() {
    generateLabels();
    mYLabelWidth = 0;
    for (int i = 0; i <= NUM_GRIDLINES; i++) {
      mYLabelWidth = Math.max(
          mYLabelWidth,
          TEXT_PAINT.measureText(String.valueOf(calcNumForGridline(i))));
    }
    mGraphWidth = getWidth() - getYLabelWidth() - BUFFER;
    mGraphHeight = getHeight() - getXLabelHeight() - getXTitleHeight() - BUFFER;
  }

  private void generateLabels() {

  }

  private void drawAxes(Canvas canvas) {
    Paint axesPaint = new Paint();
    axesPaint.setStrokeWidth(4);
    axesPaint.setColor(Color.BLACK);
    axesPaint.setStrokeCap(Paint.Cap.SQUARE);
    canvas.drawLine(
        calcXForMinutes(0),
        calcYForNumber(0),
        calcXForMinutes(mMaxMinutes + 1),
        calcYForNumber(0),
        axesPaint);
  }

  private float getXTitleHeight() {
    return TEXT_HEIGHT + BUFFER;
  }

  private float getXLabelHeight() {
    return TEXT_PAINT.getTextSize() + BUFFER;
  }

  private float getYLabelWidth() {
    return BUFFER + mYLabelWidth;
  }

  private void drawAxesTitles(Canvas canvas) {
    Paint xTitlePaint = new Paint(TEXT_PAINT);
    xTitlePaint.setTextAlign(Paint.Align.CENTER);
    canvas.drawText(
        X_AXIS_TITLE,
        mYLabelWidth + BUFFER + mGraphWidth / 2,
        getHeight(),
        xTitlePaint);
  }

  private void drawLabels(Canvas canvas) {
    // Y Axis Labels (number)
    Paint yTextPaint = new Paint(TEXT_PAINT);
    yTextPaint.setTextAlign(Paint.Align.RIGHT);
    for (int i = 0; i <= NUM_GRIDLINES; i++) {
      int number = calcNumForGridline(i);
      canvas.drawText(
          String.valueOf(number),
          mYLabelWidth,
          calcYForNumber(number) + TEXT_HEIGHT / 3,
          yTextPaint);
    }

    // X Axis Labels (minutes)
    Paint xTextPaint = new Paint(TEXT_PAINT);
    xTextPaint.setTextAlign(Paint.Align.CENTER);
    for (int i = 0; i <= NUM_TIME_LABELS; i++) {
      int minutes = mMaxMinutes / NUM_TIME_LABELS * i;
      canvas.drawText(
          String.valueOf(minutes),
          calcXForMinutes(minutes),
          getHeight() - getXTitleHeight(),
          xTextPaint);
    }
    if (mMaxMinutes == MAX_TIME) {
      // Draw a + for the last label.
      xTextPaint.setTextAlign(Align.RIGHT);
      canvas.drawText(
          "+",
          calcXForMinutes(MAX_TIME + 1),
          getHeight() - getXTitleHeight(),
          xTextPaint);
    }
  }

  private float calcXForMinutes(float minutes) {
    return mYLabelWidth + BUFFER + mGraphWidth / (mMaxMinutes + 1) * minutes;
  }

  private void drawGridlines(Canvas canvas) {
    Paint gridlinePaint = new Paint();
    gridlinePaint.setColor(0x40000000);
    gridlinePaint.setStrokeWidth(2);
    gridlinePaint.setStrokeCap(Paint.Cap.SQUARE);
    for (int i = 1; i <= NUM_GRIDLINES; i++) {
      int num = calcNumForGridline(i);
      canvas.drawLine(
          calcXForMinutes(0),
          calcYForNumber(num),
          calcXForMinutes(mMaxMinutes + 1),
          calcYForNumber(num),
          gridlinePaint);
    }
  }

  private int calcNumForGridline(int i) {
    return mMaxBinSize / NUM_GRIDLINES * i;
  }

  private void drawPlot(Canvas canvas) {
    Paint columnPaint = new Paint();
    columnPaint.setColor(0xFF33B5E5);
    columnPaint.setStrokeWidth(2);
    columnPaint.setStyle(Paint.Style.FILL);
    for (int i = 0; i <= mMaxMinutes; i++) {
      canvas.drawRect(
          calcXForMinutes(i) + COLUMN_PADDING,
          calcYForNumber(mBins[i]),
          calcXForMinutes(i + 1) - COLUMN_PADDING,
          calcYForNumber(0),
          columnPaint);
    }
  }

  private float calcYForNumber(int number) {
    return getHeight()
        - (getXLabelHeight() + getXTitleHeight() + mGraphHeight / mMaxBinSize
            * number);
  }
}
