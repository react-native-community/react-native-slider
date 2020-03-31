/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * <p>This source code is licensed under the MIT license found in the LICENSE file in the root
 * directory of this source tree.
 */
package com.reactnativecommunity.slider;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.IntDef;
import androidx.appcompat.widget.AppCompatSeekBar;

import com.facebook.react.uimanager.ReactStylesDiffMap;
import com.reactnativecommunity.slider.ReactInformantViewManager.InformantRegistry.InformantTarget;
import com.reactnativecommunity.slider.ReactSliderDrawableHelper.BackgroundDrawableHandler;
import com.reactnativecommunity.slider.ReactSliderDrawableHelper.DrawableHandler;
import com.reactnativecommunity.slider.ReactSliderDrawableHelper.ForegroundDrawableHandler;
import com.reactnativecommunity.slider.ReactSliderDrawableHelper.ThumbDrawableHandler;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

/**
 * Slider that behaves more like the iOS one, for consistency.
 *
 * <p>On iOS, the value is 0..1. Android SeekBar only supports integer values. For consistency, we
 * pretend in JS that the value is 0..1 but set the SeekBar value to 0..100.
 *
 * <p>Note that the slider is _not_ a controlled component (setValue isn't called during dragging).
 */
public class ReactSlider extends AppCompatSeekBar implements InformantTarget<ReactStylesDiffMap> {

  @IntDef({
      SliderDrawable.MAXIMUM_TRACK,
      SliderDrawable.MINIMUM_TRACK,
      SliderDrawable.THUMB
  })
  @Retention(RetentionPolicy.SOURCE)
  @interface SliderDrawable {
    int MAXIMUM_TRACK = 0;
    int MINIMUM_TRACK = 1;
    int THUMB = 2;
  }

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

  private boolean mIsInverted = false;

  final ForegroundDrawableHandler mProgressDrawableHandler;
  final BackgroundDrawableHandler mBackgroundDrawableHandler;
  final ThumbDrawableHandler mThumbDrawableHandler;

  public ReactSlider(Context context, @Nullable AttributeSet attrs, int style) {
    super(context, attrs, style);
    disableStateListAnimatorIfNeeded();
    setViewBackgroundDrawable();
    mProgressDrawableHandler = new ForegroundDrawableHandler(this);
    mBackgroundDrawableHandler = new BackgroundDrawableHandler(this);
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

  /**
   * this fixes the thumb's ripple drawable and preserves it even when a background color is applied
   */
  private void setViewBackgroundDrawable() {
    int color = Color.TRANSPARENT;
    if (getBackground() instanceof ColorDrawable) {
      color = ((ColorDrawable) getBackground()).getColor();
    }
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
      RippleDrawable rippleDrawable = new RippleDrawable(ColorStateList.valueOf(Color.LTGRAY), null, null);
      LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{new ColorDrawable(color), rippleDrawable});
      setBackground(layerDrawable);
    }
  }

  /**
   * {@link #setViewBackgroundDrawable()}
   * @param color
   */
  @Override
  public void setBackgroundColor(int color) {
    ((ColorDrawable) ((LayerDrawable) getBackground()).getDrawable(0)).setColor(color);
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

  boolean isInverted() {
    return mIsInverted;
  }

  void setInverted(boolean inverted) {
    mIsInverted = inverted;
    if (inverted) setScaleX(-1f);
    else setScaleX(1f);
    invalidate();
  }

  /**
   * Convert SeekBar's native progress value (e.g. 0..{@link ReactSlider#getMax()}) to a value passed to JS (e.g. -1.0..2.5).
   */
  public double toRealProgress(int seekBarProgress) {
    double progress = seekBarProgress;
    if (progress == getMax()) {
      return mMaxValue;
    }
    return progress * getStepValue() + mMinValue;
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

  DrawableHandler getDrawableHandler(@SliderDrawable int type) {
    switch (type) {
      case SliderDrawable.MAXIMUM_TRACK:
        return mBackgroundDrawableHandler;
      case SliderDrawable.MINIMUM_TRACK:
        return mProgressDrawableHandler;
      case SliderDrawable.THUMB:
        return mThumbDrawableHandler;
      default:
        throw new Error("bad drawable type");
    }
  }

  @Override
  synchronized public void receiveFromInformant(int informantID, int recruiterID, ReactStylesDiffMap context) {
    DrawableHandler[] handlers = new DrawableHandler[]{mBackgroundDrawableHandler, mProgressDrawableHandler, mThumbDrawableHandler};
    for (DrawableHandler handler: handlers) {
      int id = handler.getView() != null ? handler.getView().getId() : View.NO_ID;
      if (id != View.NO_ID) {
        if (id == informantID) {
          handler.updateFromProps(context);
        }
        if (id == recruiterID) {
          handler.dispatchDraw();
          break;
        }
      }
    }
  }

  void tearDown() {
    mProgressDrawableHandler.tearDown();
    mBackgroundDrawableHandler.tearDown();
    mThumbDrawableHandler.tearDown();
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    boolean retVal = super.onTouchEvent(event);
    mThumbDrawableHandler.onTouchEvent(event);
    return retVal;
  }

  private BitmapDrawable getBitmapDrawable(final String uri) {
    BitmapDrawable bitmapDrawable = null;
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    Future<BitmapDrawable> future = executorService.submit(new Callable<BitmapDrawable>() {
      @Override
      public BitmapDrawable call() {
        BitmapDrawable bitmapDrawable = null;
        try {
          Bitmap bitmap = null;
          if (uri.startsWith("http://") || uri.startsWith("https://") ||
              uri.startsWith("file://") || uri.startsWith("asset://") || uri.startsWith("data:")) {
            bitmap = BitmapFactory.decodeStream(new URL(uri).openStream());
          } else {
            int drawableId = getResources()
                .getIdentifier(uri, "drawable", getContext()
                    .getPackageName());
            bitmap = BitmapFactory.decodeResource(getResources(), drawableId);
          }

          bitmapDrawable = new BitmapDrawable(getResources(), bitmap);
        } catch (Exception e) {
          e.printStackTrace();
        }
        return bitmapDrawable;
      }
    });
    try {
      bitmapDrawable = future.get();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return bitmapDrawable;
  }

  public void setThumbImage(final String uri) {
    if (uri != null) {
      setThumb(getBitmapDrawable(uri));
      // Enable alpha channel for the thumbImage
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        setSplitTrack(false);
      }
    } else {
      setThumb(getThumb());
    }
  }

}
