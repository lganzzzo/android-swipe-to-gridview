package com.lganzzzo.ui.widget.swipetogridview;

import android.content.Context;
import android.view.View;

/**
 * Created by leonid on 4/1/15.
 */
public abstract class SwipeToGridViewAdapter {

  private Context context;

  public SwipeToGridViewAdapter(Context context){
    this.context = context;
  }

  public Context getContext(){
    return context;
  }

  public abstract View createView();
  public abstract void updateView(int index, View view);
  public abstract int getCount();

}