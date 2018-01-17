package test.lganzzzo.com.test1;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.lganzzzo.ui.widget.swipetogridview.SwipeToGridViewAdapter;
import com.lganzzzo.ui.widget.swipetogridview.SwipeToGridViewLayout;


public class MainActivity extends Activity {

  private SwipeToGridViewLayout swipeToGridViewLayout;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);

    swipeToGridViewLayout = (SwipeToGridViewLayout)findViewById(R.id.card_view);
    swipeToGridViewLayout.setAdapter(new CardsAdapter(this));
    swipeToGridViewLayout.precacheViews(50);

  }

  private static class CardsAdapter extends SwipeToGridViewAdapter {

    private int[] images = {R.drawable.image_1, R.drawable.image_2, R.drawable.image_3};

    public CardsAdapter(Context context) {
      super(context);
    }

    @Override
    public View createView() {
      return LayoutInflater.from(getContext()).inflate(R.layout.card_view_item_layout, null);
    }

    @Override
    public void updateView(int index, View view) {
      ImageView imageView = (ImageView)view.findViewById(R.id.imageView);
      imageView.setImageResource(images[index % images.length]);

      TextView textView = (TextView)view.findViewById(R.id.textView);
      textView.setText("#" + index);

    }

    @Override
    public int getCount() {
      return 2 * 100;
    }

  }

}
