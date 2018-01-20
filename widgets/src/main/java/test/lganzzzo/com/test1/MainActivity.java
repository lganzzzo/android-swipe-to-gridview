package test.lganzzzo.com.test1;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.lganzzzo.ui.widget.swipetogridview.SwipeToGridViewAdapter;
import com.lganzzzo.ui.widget.swipetogridview.SwipeToGridViewLayout;

import java.util.Random;

public class MainActivity extends Activity {

  private SwipeToGridViewLayout swipeToGridViewLayout;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);

    swipeToGridViewLayout = (SwipeToGridViewLayout)findViewById(R.id.card_view);
    swipeToGridViewLayout.setAdapter(new CardsAdapter(this, 100));
    swipeToGridViewLayout.precacheViews(50);

  }

  private static class CardsAdapter extends SwipeToGridViewAdapter {

    private Random random = new Random(256);
    private int[] colors;

    public CardsAdapter(Context context, int count) {
      super(context);
      colors = new int[count];
      for(int i = 0; i < count; i++){
        colors[i] = 0x66000000 + random.nextInt(0xFFFFFF);
      }
    }

    @Override
    public View createView() {
      return LayoutInflater.from(getContext()).inflate(R.layout.card_view_item_layout, null);
    }

    @Override
    public void updateView(int index, View view) {
      View frame = view.findViewById(R.id.frame_layout);
      frame.setBackgroundColor(colors[index]);
      TextView textView = (TextView)view.findViewById(R.id.textView);
      textView.setText("#" + index);
    }

    @Override
    public int getCount() {
      return colors.length;
    }

  }

}
