/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * <p>This source code is licensed under the MIT license found in the LICENSE file in the root
 * directory of this source tree.
 */
package com.reactnativecommunity.slider;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatSeekBar;

import com.reactnativecommunity.slider.ReactSliderDrawableHelper.ProgressDrawableHandler;
import com.reactnativecommunity.slider.ReactSliderDrawableHelper.ThumbDrawableHandler;

import javax.annotation.Nullable;

/**
 * Slider that behaves more like the iOS one, for consistency.
 *
 * <p>On iOS, the value is 0..1. Android SeekBar only supports integer values. For consistency, we
 * pretend in JS that the value is 0..1 but set the SeekBar value to 0..100.
 *
 * <p>Note that the slider is _not_ a controlled component (setValue isn't called during dragging).
 */
public class ReactSlider extends AppCompatSeekBar {

  /**
   * If step is 0 (unset) we default to this total number of steps. Don't use 100 which leads to
   * rounding errors (0.200000000001).
   */
  private static int DEFAULT_TOTAL_STEPS = 128;

  /**
   * We want custom min..max range. Android only supports 0..max range so we implement this
   * ourselves.
   */
  private double mMinValue = 0;

  private double mMaxValue = 0;

  /**
   * Value sent from JS (setState). Doesn't get updated during drag (slider is not a controlled
   * component).
   */
  private double mValue = 0;

  /** If zero it's determined automatically. */
  private double mStep = 0;

  private double mStepCalculated = 0;

  private final ProgressDrawableHandler mProgressDrawableHandler;

  private final ThumbDrawableHandler mThumbDrawableHandler;

  public ReactSlider(Context context, @Nullable AttributeSet attrs, int style) {
    super(context, attrs, style);
    disableStateListAnimatorIfNeeded();
    mProgressDrawableHandler = new ProgressDrawableHandler(this);
    mThumbDrawableHandler = new ThumbDrawableHandler(this);
  }

  private void disableStateListAnimatorIfNeeded() {
    // We disable the state list animator for Android 6 and 7; this is a hack to prevent T37452851
    // and https://github.com/facebook/react-native/issues/9979
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      super.setStateListAnimator(null);
    }
  }

  /* package */ void setMaxValue(double max) {
    mMaxValue = max;
    updateAll();
  }

  /* package */ void setMinValue(double min) {
    mMinValue = min;
    updateAll();
  }

  /* package */ void setValue(double value) {
    mValue = value;
    updateValue();
  }

  /* package */ void setStep(double step) {
    mStep = step;
    updateAll();
  }

  /**
   * Convert SeekBar's native progress value (e.g. 0..100) to a value passed to JS (e.g. -1.0..2.5).
   */
  public double toRealProgress(int seekBarProgress) {
    if (seekBarProgress == getMax()) {
      return mMaxValue;
    }
    return seekBarProgress * getStepValue() + mMinValue;
  }

  /** Update underlying native SeekBar's values. */
  private void updateAll() {
    if (mStep == 0) {
      mStepCalculated = (mMaxValue - mMinValue) / (double) DEFAULT_TOTAL_STEPS;
    }
    setMax(getTotalSteps());
    updateValue();
  }

  /** Update value only (optimization in case only value is set). */
  private void updateValue() {
    setProgress((int) Math.round((mValue - mMinValue) / (mMaxValue - mMinValue) * getTotalSteps()));
  }

  private int getTotalSteps() {
    return (int) Math.ceil((mMaxValue - mMinValue) / getStepValue());
  }

  private double getStepValue() {
    return mStep > 0 ? mStep : mStepCalculated;
  }

  void setProgressDrawable(int tag) {
    mProgressDrawableHandler.setView(tag);
  }

  void setThumbDrawable(int tag) {
    mThumbDrawableHandler.setView(tag);
  }

  void tearDown() {
    mProgressDrawableHandler.tearDown();
    mThumbDrawableHandler.tearDown();
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    boolean retVal = super.onTouchEvent(event);
    int action = event.getActionMasked();
    if (mThumbDrawableHandler.isCustomDrawable()) {
      if (action == MotionEvent.ACTION_DOWN) {
        mThumbDrawableHandler.start();
      } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
        mThumbDrawableHandler.end();
      }
    }
    return retVal;
  }

}
