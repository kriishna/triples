package com.antsapps.triples;

import java.util.List;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.CycleInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;

import com.antsapps.triples.backend.Card;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;

public class CardDrawable extends Drawable implements Comparable<CardDrawable> {

  private static final Rect EMPTY_RECT = new Rect(0, 0, 0, 0);

  interface OnAnimationFinishedListener {
    void onAnimationFinished(boolean stillVisible);
  }

  private int mDrawOrder;

  private final Card mCard;

  private final SymbolDrawable mSymbol;
  private final CardBackgroundDrawable mCardBackground;

  private Rect mBounds;

  private boolean mSelected;

  private Animation mAnimation;
  private final Transformation mTransformation = new Transformation();

  public CardDrawable(Card card) {
    mCard = card;

    mSymbol = new SymbolDrawable(mCard);
    mCardBackground = new CardBackgroundDrawable();
  }

  private static List<Rect> getBoundsForNumId(int id, Rect bounds) {
    List<Rect> rects = Lists.newArrayList();

    int halfSideLength = bounds.width() / 10;
    int gap = halfSideLength / 2;
    switch (id) {
      case 0:
        rects.add(squareFromCenterAndRadius(
            bounds.centerX(),
            bounds.centerY(),
            halfSideLength));
        break;
      case 1:
        rects.add(squareFromCenterAndRadius(bounds.centerX() - gap / 2
            - halfSideLength, bounds.centerY(), halfSideLength));
        rects.add(squareFromCenterAndRadius(bounds.centerX() + gap / 2
            + halfSideLength, bounds.centerY(), halfSideLength));
        break;
      case 2:
        rects.add(squareFromCenterAndRadius(bounds.centerX() - gap
            - halfSideLength * 2, bounds.centerY(), halfSideLength));
        rects.add(squareFromCenterAndRadius(
            bounds.centerX(),
            bounds.centerY(),
            halfSideLength));
        rects.add(squareFromCenterAndRadius(bounds.centerX() + gap
            + halfSideLength * 2, bounds.centerY(), halfSideLength));
        break;
    }
    return rects;
  }

  private static Rect squareFromCenterAndRadius(int centerX, int centerY,
      int radius) {
    return new Rect(centerX - radius, centerY - radius, centerX + radius,
        centerY + radius);
  }

  public boolean isAnimating() {
    return mAnimation != null && mAnimation.hasStarted()
        && !mAnimation.hasEnded();
  }

  @Override
  public void draw(Canvas canvas) {
    Rect bounds = mBounds;
    Log.v("CD", "drawing with bounds = " + bounds);
    if (bounds == null || bounds == EMPTY_RECT) {
      return;
    }
    int sc = canvas.save();
    Animation anim = mAnimation;
    if (anim != null) {
      anim.getTransformation(
          AnimationUtils.currentAnimationTimeMillis(),
          mTransformation);

      canvas.concat(mTransformation.getMatrix());
      mCardBackground.setAlpha((int) (mTransformation.getAlpha() * 255));
      mSymbol.setAlpha((int) (mTransformation.getAlpha() * 255));

      if (!anim.isInitialized()) {
        anim.initialize(bounds.width(), bounds.height(), 0, 0);
      }
    }
    drawInternal(canvas, bounds);
    canvas.restoreToCount(sc);
  }

  private void drawInternal(Canvas canvas, Rect bounds) {
    mCardBackground.setBounds(bounds);
    mCardBackground.draw(canvas);
    for (Rect rect : getBoundsForNumId(mCard.mNumber, bounds)) {
      mSymbol.setBounds(rect);
      mSymbol.draw(canvas);
    }
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }

  @Override
  public void setAlpha(int alpha) {
    mSymbol.setAlpha(alpha);
    mCardBackground.setAlpha(alpha);
  }

  @Override
  public void setColorFilter(ColorFilter cf) {
    mSymbol.setColorFilter(cf);
    mCardBackground.setColorFilter(cf);
  }

  public Card getCard() {
    return mCard;
  }

  public void onTap() {
    if (mSelected) {
      setSelected(false);
    } else {
      setSelected(true);
    }
  }

  private void setSelected(boolean selected) {
    mSelected = selected;
    mCardBackground.setSelected(selected);
  }

  @Override
  public void setBounds(Rect bounds) {
    Log.i("CD", "setBounds.  mBounds = " + mBounds + ", bounds = " + bounds);
    mBounds = bounds;
  }

  public void onIncorrectTriple() {
    // Shake animation
    Animation shakeAnimation = new RotateAnimation(0, 5, mBounds.centerX(),
        mBounds.centerY());
    shakeAnimation.setInterpolator(new CycleInterpolator(4));
    shakeAnimation.setDuration(800);
    shakeAnimation.setStartTime(Animation.START_ON_FIRST_FRAME);
    shakeAnimation.setAnimationListener(new AnimationListener() {

      @Override
      public void onAnimationEnd(Animation animation) {
        setSelected(false);
        if (mAnimation == animation) {
          mAnimation = null;
        }
      }

      @Override
      public void onAnimationRepeat(Animation animation) {
      }

      @Override
      public void onAnimationStart(Animation animation) {
      }

    });
    mAnimation = shakeAnimation;
  }

  /**
   * @return whether the card should be drawn on a top level.
   */
  public void setNewPosition(final Rect bounds,
      final OnAnimationFinishedListener listener) {
    if(bounds == mBounds) {
      return;
    }
    Animation transitionAnimation = null;
    if (mBounds == null) {
      // This CardDrawable is new.
      Log.i("CD", "setBounds fade in.  mBounds = " + mBounds + ", bounds = "
          + bounds);
      mBounds = bounds;
      transitionAnimation = new AlphaAnimation(0, 1);
    } else {
      // This CardDrawable is old
      if (bounds == null) {
        // This CardDrawable is being removed
        transitionAnimation = new TranslateAnimation(0, 1000, 0, 0);
        mDrawOrder = 2;
      } else {
        // This CardDrawable is being moved to a new place
        transitionAnimation = new TranslateAnimation(0, bounds.centerX()
            - mBounds.centerX(), 0, bounds.centerY() - mBounds.centerY());
        mDrawOrder = 1;
      }
    }
    transitionAnimation.setInterpolator(new AccelerateInterpolator());
    transitionAnimation.setDuration(800);
    transitionAnimation.setStartTime(Animation.START_ON_FIRST_FRAME);

    transitionAnimation.setAnimationListener(new AnimationListener() {

      @Override
      public void onAnimationEnd(Animation animation) {
        if (bounds == null) {
          Log.i("CD", "setBounds slide off.  mBounds = " + mBounds
              + ", bounds = " + bounds);
          mBounds = null;
          listener.onAnimationFinished(false);
        } else {
          Log.i("CD", "setBounds slide to new.  mBounds = " + mBounds
              + ", bounds = " + bounds);
          mBounds = new Rect(bounds);
          listener.onAnimationFinished(true);
        }
        mDrawOrder = 0;

        if (mAnimation == animation) {
          mAnimation = null;
        }
      }

      @Override
      public void onAnimationRepeat(Animation animation) {
      }

      @Override
      public void onAnimationStart(Animation animation) {
      }

    });
    mAnimation = transitionAnimation;
  }

  public int getDrawOrder() {
    return mDrawOrder;
  }

  @Override
  public int compareTo(CardDrawable another) {
    return Ints.compare(mDrawOrder, another.mDrawOrder);
  }
}
