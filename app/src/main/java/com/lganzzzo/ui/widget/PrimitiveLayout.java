package com.lganzzzo.ui.widget;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by leonid on 3/31/15.
 */
public class PrimitiveLayout extends ViewGroup{

  public PrimitiveLayout(Context context) {
    super(context);
  }

  @Override
  protected LayoutParams generateDefaultLayoutParams(){
    return new LayoutParams(0, 0);
  }

  @Override
  public void onMeasure(int widthMeasureSpec, int heightMeasureSpec){

    for(int i = 0; i < getChildCount(); i++){

      View view = getChildAt(i);
      LayoutParams params = view.getLayoutParams();

      if(params == null){
        params = generateDefaultLayoutParams();
        view.setLayoutParams(params);
      }

      int widthSpec = MeasureSpec.makeMeasureSpec(params.width, MeasureSpec.EXACTLY);
      int heightSpec = MeasureSpec.makeMeasureSpec(params.height, MeasureSpec.EXACTLY);

      view.measure(widthSpec, heightSpec);

    }

    int width = MeasureSpec.getSize(widthMeasureSpec);
    int height = MeasureSpec.getSize(heightMeasureSpec);

    setMeasuredDimension(width, height);

  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {

    for(int i = 0; i < getChildCount(); i++){
      View view = getChildAt(i);
      LayoutParams params = view.getLayoutParams();
      view.layout(0, 0, params.width, params.height);
    }

  }

}