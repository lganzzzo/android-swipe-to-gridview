package com.lganzzzo.ui.widget.swipetogridview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.OverScroller;

import com.lganzzzo.ui.widget.PrimitiveLayout;

import java.util.LinkedList;

import test.lganzzzo.com.test1.R;

/**
 * Created by leonid on 3/31/15.
 */
public class SwipeToGridViewLayout extends ViewGroup{

  public static final int MODE_SWIPE = 0;
  public static final int MODE_GRID = 1;

  private int currMode = MODE_SWIPE;
  private int nextMode = MODE_GRID;
  private int scrollingMode = -1;

  private float modeTransitionTime = 0f;
  private float modeTransitionSpeed = 2f;

  private int SWIPE_CARD_MARGIN_VERTICAL = 50;
  private int SWIPE_CARD_MARGIN_HORIZONTAL = 50;

  private int GRID_CARD_MARGIN_VERTICAL = 50;
  private int GRID_CARD_MARGIN_HORIZONTAL = 100;

  private int SWIPE_DIV_WIDTH = 20;
  private int GRID_DIV_HORIZONTAL_WIDTH = 50;
  private int GRID_DIV_VERTICAL_WIDTH = 50;

  private float cardAspectRatio = 4f / 3f;

  private int GRID_COLS = 4;

  private boolean hasScroll = false;

  private SwipeToGridViewAdapter adapter;
  private ViewPool viewPool = new ViewPool();
  private CardRecordManager cardRecordManager = new CardRecordManager();

  private float aspectRatio = 1;

  private float fullCardWidth = 0;
  private float fullCardHeight = 0;

  private float gridCardWidth;
  private float gridCardHeight;

  private float swipeFrameWidth;
  private float gridFrameWidth;
  private float gridFrameHeight;

  private float swipeScrollX = 0;
  private float swipeScrollY = 0;
  private float gridScrollX = 0;
  private float gridScrollY = 0;

  private float swipeMinScrollX;
  private float swipeMaxScrollX;

  private float gridMinScrollY;
  private float gridMaxScrollY;

  private float touchPointX;
  private float touchPointY;

  private float touchInterceptPointX;
  private float touchInterceptPointY;

  private float touchSwipeScrollX;
  private float touchGrisScrollY;

  private int selectedCardIndex = -1;

  /**
   * needed because MotionEvent.ACTION_POINTER_DOWN not working
   */
  private int lastTouchPointerCount = 0;
  private float initialTouchDistance;

  private boolean isTouched = false;
  private boolean wasFling = false;

  private long lastTickTime = 0;

  private OverScroller scroller;
  private GestureDetector gestureDetector;

  private GestureDetector.OnGestureListener gestureListener = new GestureDetector.OnGestureListener() {
    @Override
    public boolean onDown(MotionEvent e) {
      return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
      return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
      return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
      wasFling = true;
      SwipeToGridViewLayout.this.onFling(velocityX, velocityY);
      return false;
    }
  };

  public SwipeToGridViewLayout(Context context) {
    super(context);
    init();
  }

  public SwipeToGridViewLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    readAttrs(attrs);
    init();
  }

  public SwipeToGridViewLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    readAttrs(attrs);
    init();
  }

  private void init(){
    setWillNotDraw(false);
    scroller = new OverScroller(getContext());
    gestureDetector = new GestureDetector(getContext(), gestureListener);
    hasScroll = false;
  }

  private void readAttrs(AttributeSet attrs){
    TypedArray a = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.SwipeToGridViewLayout, 0, 0);

    try {
      cardAspectRatio = a.getFloat(R.styleable.SwipeToGridViewLayout_card_aspect_ratio, 1.33333f);
      GRID_COLS = a.getInteger(R.styleable.SwipeToGridViewLayout_grid_view_columns, 3);

      SWIPE_CARD_MARGIN_HORIZONTAL = a.getDimensionPixelOffset(R.styleable.SwipeToGridViewLayout_swipe_view_padding_horizontal, 100);
      SWIPE_CARD_MARGIN_VERTICAL = a.getDimensionPixelOffset(R.styleable.SwipeToGridViewLayout_swipe_view_padding_vertical, 50);

      GRID_CARD_MARGIN_HORIZONTAL = a.getDimensionPixelOffset(R.styleable.SwipeToGridViewLayout_grid_view_padding_horizontal, 50);
      GRID_CARD_MARGIN_VERTICAL = a.getDimensionPixelOffset(R.styleable.SwipeToGridViewLayout_grid_view_padding_vertical, 50);

      SWIPE_DIV_WIDTH = a.getDimensionPixelOffset(R.styleable.SwipeToGridViewLayout_swipe_view_div_width, 50);
      GRID_DIV_HORIZONTAL_WIDTH = a.getDimensionPixelOffset(R.styleable.SwipeToGridViewLayout_grid_view_div_horizontal_width, 50);
      GRID_DIV_VERTICAL_WIDTH = a.getDimensionPixelOffset(R.styleable.SwipeToGridViewLayout_grid_view_div_vertical_width, 50);


    } finally {
      a.recycle();
    }

  }

  public void setAdapter(SwipeToGridViewAdapter adapter){
    this.adapter = adapter;
  }

  public void precacheViews(int number){
    viewPool.fillPool(number);
  }

  private int getXPositionForSwipePage(int pageIndex){
    int shift = (int) ((getWidth() / 2) - fullCardWidth / 2);
    return (int) (- pageIndex * swipeFrameWidth + shift);
  }

  private void computeScoll(int width, int height, int itemsCount){

    swipeMinScrollX = getXPositionForSwipePage(0);
    swipeMaxScrollX = getXPositionForSwipePage(itemsCount - 1);


    int rows = itemsCount / GRID_COLS;
    if(rows * GRID_COLS < itemsCount){
      rows++;
    }

    float gridHeight = rows * gridFrameHeight;

    if(gridHeight < height){
      gridMinScrollY = (height - gridHeight) / 2;
      gridMaxScrollY = gridMinScrollY;
    } else {
      gridMinScrollY = GRID_CARD_MARGIN_VERTICAL;
      gridMaxScrollY = -(gridHeight - height);
    }

  }

  @Override
  public void onMeasure(int widthMeasureSpec, int heightMeasureSpec){

    int itemsCount = adapter.getCount();

    int fullWidth = MeasureSpec.getSize(widthMeasureSpec);
    int fullHeight = MeasureSpec.getSize(heightMeasureSpec);

    float width = fullWidth - SWIPE_CARD_MARGIN_HORIZONTAL * 2;
    float height = fullHeight - SWIPE_CARD_MARGIN_VERTICAL * 2;

    if(width > 0 && height > 0){

      aspectRatio = height / width;

      if(cardAspectRatio > aspectRatio){
        fullCardHeight = height;
        fullCardWidth = height / cardAspectRatio;
      }else{
        fullCardWidth = width;
        fullCardHeight = width * cardAspectRatio;
      }

      swipeFrameWidth = fullCardWidth + SWIPE_DIV_WIDTH;
      gridFrameWidth = (fullWidth - GRID_CARD_MARGIN_HORIZONTAL * 2f) / GRID_COLS;

      gridCardWidth = gridFrameWidth - GRID_DIV_HORIZONTAL_WIDTH;
      gridCardHeight = gridCardWidth * cardAspectRatio;

      gridFrameHeight = gridCardHeight + GRID_DIV_VERTICAL_WIDTH;

      swipeScrollY = (fullHeight - fullCardHeight) / 2f;

      float gridWidth;
      if(adapter.getCount() < GRID_COLS){
        gridWidth = itemsCount * gridFrameWidth - GRID_DIV_HORIZONTAL_WIDTH;
      }else{
        gridWidth = fullWidth - GRID_CARD_MARGIN_HORIZONTAL * 2f - GRID_DIV_HORIZONTAL_WIDTH;
      }
      gridScrollX = (fullWidth - gridWidth) / 2f;

      computeScoll(fullWidth, fullHeight, itemsCount);

      if(hasScroll == false && getWidth() > 0) {
        swipeScrollX = swipeMinScrollX;
        gridScrollY = gridMinScrollY;
        hasScroll = true;
      }

    }else{
      fullCardWidth = 0;
      fullCardHeight = 0;
    }

    setMeasuredDimension(fullWidth, fullHeight);

    cardRecordManager.update();
    cardRecordManager.measureCards();
    cardRecordManager.requestOrRecycleViews();

    viewPool.measureCards();

    int widthSpec = MeasureSpec.makeMeasureSpec(fullWidth, MeasureSpec.EXACTLY);
    int heightSpec = MeasureSpec.makeMeasureSpec(fullHeight, MeasureSpec.EXACTLY);

    for(int i = 0; i < getChildCount(); i++){
      View view = getChildAt(i);
      view.measure(widthSpec, heightSpec);
    }

  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {

    for(int i = 0; i < getChildCount(); i++){
      View view = getChildAt(i);
      view.layout(0, 0, getWidth(), getHeight());
    }

    if(currMode == MODE_SWIPE) {
      scrollingMode = MODE_SWIPE;
      scrollToPage(getCurrentSwipePage());
    }

  }

  @Override
  public void onDraw(Canvas canvas){

    if(!scroller.isFinished()){
      if(scrollingMode == MODE_GRID){
        scroller.computeScrollOffset();
        setGridScrollY(scroller.getCurrY());
      }

      if(scrollingMode == MODE_SWIPE){
        scroller.computeScrollOffset();
        setSwipeScrollX(scroller.getCurrX());
      }

      postInvalidate();
    }

    processAnimation();

    cardRecordManager.calcCardRects();
    //cardRecordManager.debugDrawCards(canvas);

    cardRecordManager.requestOrRecycleViews();

  }

  private void processAnimation(){

    long tick = System.currentTimeMillis();
    float deltaTime = (float)(tick - lastTickTime) / 1000f;

    boolean isAnimating = false;

    if(lastTickTime > 0) {
      isAnimating = processModeTransition(deltaTime);
    }else{
      postInvalidate();
      isAnimating = true;
    }

    if(isAnimating) {
      lastTickTime = tick;
    }else{
      lastTickTime = 0;
    }

  }

  private boolean processModeTransition(float deltaTime){

    if(initialTouchDistance != 0){
      return false;
    }

    if(modeTransitionTime > 0){

      if(getWidth() > 0) {
        modeTransitionTime = modeTransitionTime + deltaTime * modeTransitionSpeed;
      }

      if(modeTransitionTime >= 1){
        modeTransitionTime = 0f;
        if(currMode == MODE_SWIPE){
          currMode = MODE_GRID;
        }else{
          currMode = MODE_SWIPE;
        }
      }

      postInvalidate();

      return true;

    }else{
      return false;
    }

  }

  private void setSwipeScrollX(float x){

    if(x > swipeMinScrollX){
      x = swipeMinScrollX;
    }

    if(x < swipeMaxScrollX){
      x = swipeMaxScrollX;
    }

    swipeScrollX = x;

  }

  private void setGridScrollY(float y){

    if(y > gridMinScrollY){
      y = gridMinScrollY;
    }

    if(y < gridMaxScrollY){
      y = gridMaxScrollY;
    }

    gridScrollY = y;

  }

  private int getCardFrameIndexAtXY(float x, float y){

    if(modeTransitionTime != 0){
      return -1;
    }

    int result = - 1;

    if(currMode == MODE_SWIPE){
      result = (int)((x - swipeScrollX) / swipeFrameWidth);
    }else if(currMode == MODE_GRID){
      int row = (int) ((y - gridScrollY) / gridFrameHeight);

      float cx = x - gridScrollX;

      if(cx >= gridFrameWidth * GRID_COLS){
        cx = gridFrameWidth * (GRID_COLS - 1);
      }

      if(cx < 0){
        cx = 0;
      }

      int col = (int) (cx / gridFrameWidth);
      result = col + row * GRID_COLS;
    }

    if(result >= adapter.getCount()){
      return -1;
    }

    return result;

  }

  private void syncScroll(float x, float y){

    int cardIndex = getCardFrameIndexAtXY(x, y);

    if(cardIndex >= 0){

      if(currMode == MODE_SWIPE){

        int row = cardIndex / GRID_COLS;
        setGridScrollY(- (row * gridFrameHeight) + getHeight() / 2f - gridCardHeight / 2f);

      }else if(currMode == MODE_GRID){

        setSwipeScrollX(getXPositionForSwipePage(cardIndex));

      }

    }

  }

  private float modeTransitionFunction(float x){

    if(x < 0){
      x = 0;
    }

    if(x > 1){
      x = 1;
    }

    return x;
  }

  private void updateTouchPoint(float x, float y){
    touchPointX = x;
    touchPointY = y;
    touchSwipeScrollX = swipeScrollX;
    touchGrisScrollY = gridScrollY;
  }

  private void onTouchBegin(float x, float y){
    scroller.abortAnimation();
    isTouched = true;
    wasFling = false;

    updateTouchPoint(x, y);

    int newIndex = getCardFrameIndexAtXY(x, y);
    if(newIndex != selectedCardIndex){
      selectedCardIndex = newIndex;
      postInvalidate();
    }

  }

  private void onTouchContinue(MotionEvent event){

    float x;
    float y;

    float sx = 0;
    float sy = 0;

    int pointCount = event.getPointerCount();

    for(int i = 0; i < pointCount; i++){
      MotionEvent.PointerCoords pointerCoords = new MotionEvent.PointerCoords();
      event.getPointerCoords(i, pointerCoords);

      sx = sx + pointerCoords.x;
      sy = sy + pointerCoords.y;
    }

    x = sx / (float)pointCount;
    y = sy / (float)pointCount;

    if(lastTouchPointerCount != pointCount) {

      updateTouchPoint(x, y);

      if (lastTouchPointerCount < pointCount && pointCount == 2) {
        onZoomBegin(event, x, y);
      } else if (lastTouchPointerCount > pointCount && pointCount < 2) {
        onZoomEnd(event, x, y);
      }

    }

    processOnePointMove(x, y);

    if(pointCount == 2 && initialTouchDistance > 0){
      onZoomContinue(event);
    }

    postInvalidate();

    lastTouchPointerCount = pointCount;

  }

  private float calcTouchDistance(MotionEvent event){
    MotionEvent.PointerCoords pointerCoords = new MotionEvent.PointerCoords();
    event.getPointerCoords(0, pointerCoords);

    float x1 = pointerCoords.x;
    float y1 = pointerCoords.y;

    event.getPointerCoords(1, pointerCoords);

    float x2 = pointerCoords.x;
    float y2 = pointerCoords.y;

    return (float)Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
  }

  private void onZoomBegin(MotionEvent event, float x, float y){
    initialTouchDistance = calcTouchDistance(event);

    touchPointX = x;
    touchPointY = y;

    syncScroll(x, y);

    if(currMode == MODE_SWIPE) {
      touchGrisScrollY = gridScrollY;
    }else if(currMode == MODE_GRID){
      touchSwipeScrollX = swipeScrollX;
    }

  }

  private void onZoomContinue(MotionEvent event){

    float zoomRation = 1;

    if(currMode == MODE_SWIPE){
      float td = calcTouchDistance(event);
      if(td <= 0){
        td = 0.001f;
      }
      zoomRation = initialTouchDistance / td;
    }else{
      zoomRation = calcTouchDistance(event) / initialTouchDistance;
    }

    if(zoomRation > 2){
      zoomRation = 2;
    }else if(zoomRation < 1){
      zoomRation = 1;
    }

    modeTransitionTime = zoomRation - 1;

  }

  private void onZoomEnd(MotionEvent event, float x, float y){
    initialTouchDistance = 0;
    postInvalidate();
  }

  private void onTouchEnd(MotionEvent event){
    initialTouchDistance = 0;
    isTouched = false;

    if(wasFling == false && currMode == MODE_SWIPE){
      scrollingMode = MODE_SWIPE;
      scrollToPage(getCurrentSwipePage());
    }

    wasFling = false;

    postInvalidate();
  }

  private int getCurrentSwipePage(){
    float shift = getWidth() / 2 - fullCardWidth / 2;
    return Math.round((- swipeScrollX + shift) / swipeFrameWidth);
  }

  private void scrollToPage(int pageIndex){
    float dx = getXPositionForSwipePage(pageIndex) - swipeScrollX;
    scroller.startScroll((int)swipeScrollX, 0, (int) dx, 0);
  }

  private void onFling(float vx, float vy){

    if(modeTransitionTime > 0){
      return;
    }

    scrollingMode = currMode;

    if(currMode == MODE_GRID){
      scroller.fling(0, (int) gridScrollY, 0, (int)vy, 0, 0, (int)gridMaxScrollY, (int)gridMinScrollY, 100, 100);
    }

    if(currMode == MODE_SWIPE){

      int index = getCurrentSwipePage();
      float needScrollX = getXPositionForSwipePage(index);

      float neededVx = fullCardWidth / 5;
      if(vx > neededVx && needScrollX < swipeScrollX){
        scrollToPage(index - 1);
      } else if(vx < -neededVx && needScrollX > swipeScrollX){
        scrollToPage(index + 1);
      }else{
        scrollToPage(index);
      }

    }

  }

  private void logTouchEvent(MotionEvent event){

    Log.e("AAA", "event:" + event.toString());

  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent event){

    Log.e("AAA", "From Intercept");
    logTouchEvent(event);
    gestureDetector.onTouchEvent(event);

    if(event.getPointerCount() >= 2){
      scroller.abortAnimation();
      return true;
    }

    switch (event.getAction()){

      case MotionEvent.ACTION_DOWN:
        scroller.abortAnimation();
        touchInterceptPointX = event.getX();
        touchInterceptPointY = event.getY();

        int newIndex = getCardFrameIndexAtXY(event.getX(), event.getY());
        if(newIndex != selectedCardIndex){
          selectedCardIndex = newIndex;
          postInvalidate();
        }

        break;

      case MotionEvent.ACTION_MOVE:
        if(currMode == MODE_SWIPE && Math.abs(event.getX() - touchInterceptPointX) > 20){
          onTouchBegin(event.getX(), event.getY());
          return true;
        } else if(currMode == MODE_GRID && Math.abs(event.getY() - touchInterceptPointY) > 20){
          onTouchBegin(event.getX(), event.getY());
          return true;
        }

        break;

      case MotionEvent.ACTION_UP:
        onTouchEnd(event);
        break;

      default:
        break;

    }

    return false;

  }

  @Override
  public boolean onTouchEvent(MotionEvent event){
    Log.e("AAA", "From touch");
    logTouchEvent(event);
    gestureDetector.onTouchEvent(event);

    switch (event.getAction()){

      case MotionEvent.ACTION_DOWN:
        if(isTouched){
          return true;
        }
        onTouchBegin(event.getX(), event.getY());
        return true;

      case MotionEvent.ACTION_MOVE:
        onTouchContinue(event);
        return true;

      case MotionEvent.ACTION_UP:
        onTouchEnd(event);
        return true;

      default:
        break;

    }

    return false;



  }

  private void processOnePointMove(float x, float y){

    if(currMode == MODE_SWIPE) {
      if(modeTransitionTime == 0){
        processSwipeMove(x, y, 1);
      } else {
        float f = modeTransitionFunction(modeTransitionTime);
        float fi = 1 - f;
        processSwipeMove(x, y, fi);
        processGridMove(x, y, f);
      }
    } else {
      if(modeTransitionTime == 0){
        processGridMove(x, y, 1);
      } else {
        float fi = modeTransitionFunction(modeTransitionTime);
        float f = 1 - fi;
        processSwipeMove(x, y, fi);
        processGridMove(x, y, f);
      }
    }

  }

  private void processSwipeMove(float x, float y, float k){
    setSwipeScrollX(touchSwipeScrollX + k * (x - touchPointX));
  }

  private void processGridMove(float x, float y, float k){
    setGridScrollY(touchGrisScrollY + k * (y - touchPointY));
  }

  private boolean processZoomingTouch(MotionEvent event){
    return false;
  }




  //---------------------------------------------------------------------------
  // CardRecord

  private class CardRecord{

    private int index;

    private RectF swipeRect;
    private RectF gridRect;
    private RectF calculatedRect = null;

    private ViewRecord viewRecord;

    public CardRecord(int index){
      this.index = index;
    }

    public boolean isVisibleOnScreen(){
      RectF r = calculatedRect;
      if(r == null){
        calcRect();
        r = calculatedRect;
      }
      return r.right >= 0 && r.left < getMeasuredWidth() && r.bottom >= 0 && r.top < getMeasuredHeight();
    }

    private void requestView(){
      if(viewRecord == null) {
        viewRecord = viewPool.requestView(index);
      }
    }

    private void recycleView(){
      if(viewRecord != null) {
        viewPool.recycleView(viewRecord);
        viewRecord = null;
      }
    }

    public void requestViewIfNeeded(){
      if(isVisibleOnScreen()){
        requestView();
        viewRecord.scroll(- calculatedRect.left, - calculatedRect.top);
        float scale = (calculatedRect.width()) / fullCardWidth;
        viewRecord.getView().setScaleY(scale);
        viewRecord.getView().setScaleX(scale);
        viewRecord.getView().setPivotX(0);
        viewRecord.getView().setPivotY(0);
      }
    }

    public void recycleViewIfNeeded(){
      if(!isVisibleOnScreen()){
        recycleView();
      }
    }

    public void placeAndCalcRects(){

      PointF swipePos = calcSwipePosition();
      PointF gridPos = calcGridPosition();

      swipeRect = new RectF(swipePos.x, swipePos.y, swipePos.x + fullCardWidth, swipePos.y + fullCardHeight);
      gridRect = new RectF(gridPos.x, gridPos.y, gridPos.x + gridCardWidth, gridPos.y + gridCardHeight);

    }

    public RectF getCalculatedRect(){
      if(calculatedRect == null){
        calculatedRect = calcRect();
      }
      return calculatedRect;
    }

    public RectF calcRect(){

      if(modeTransitionTime == 0){
        if(currMode == MODE_SWIPE){
          calculatedRect = new RectF(swipeRect.left + swipeScrollX, swipeRect.top + swipeScrollY, swipeRect.right + swipeScrollX, swipeRect.bottom + swipeScrollY);
          return calculatedRect;
        }else{
          calculatedRect = new RectF(gridRect.left + gridScrollX, gridRect.top + gridScrollY, gridRect.right + gridScrollX, gridRect.bottom + gridScrollY);
          return calculatedRect;
        }
      }else{

        float f;
        float fi;

        if(currMode == MODE_SWIPE){
          f = modeTransitionFunction(modeTransitionTime);
          fi = 1 - f;
        }else{
          fi = modeTransitionFunction(modeTransitionTime);
          f = 1 - fi;
        }

        RectF sr = new RectF(swipeRect.left + swipeScrollX, swipeRect.top + swipeScrollY, swipeRect.right + swipeScrollX, swipeRect.bottom + swipeScrollY);
        RectF gr = new RectF(gridRect.left + gridScrollX, gridRect.top + gridScrollY, gridRect.right + gridScrollX, gridRect.bottom + gridScrollY);

        calculatedRect = new RectF( sr.left * fi + gr.left * f, sr.top * fi + gr.top * f,
          sr.right * fi + gr.right * f, sr.bottom * fi + gr.bottom * f);
        return calculatedRect;

      }

    }

    private PointF calcGridPosition(){

      int row = index / GRID_COLS;
      int col = index - row * GRID_COLS;

      return new PointF(col * gridFrameWidth, row * gridFrameHeight);

    }

    private PointF calcSwipePosition(){
      return new PointF(index * swipeFrameWidth, 0);
    }

  }

  //---------------------------------------------------------------------------
  // CardRecordManager

  private class CardRecordManager{

    private Paint debugPaintStroke;
    private Paint debugPaintStrokeSelected;
    private Paint debugPaintFill;
    private TextPaint debugTextPaint;
    private LinkedList<CardRecord> records = new LinkedList<CardRecord>();

    public CardRecordManager(){
      debugPaintStroke = new Paint();
      debugPaintStroke.setColor(0xFF444444);
      debugPaintStroke.setStyle(Paint.Style.STROKE);
      debugPaintStroke.setStrokeWidth(5);

      debugPaintStrokeSelected = new Paint();
      debugPaintStrokeSelected.setColor(0xFFFF0000);
      debugPaintStrokeSelected.setStyle(Paint.Style.STROKE);
      debugPaintStrokeSelected.setStrokeWidth(5);

      debugPaintFill = new Paint();
      debugPaintFill.setColor(0xFFAAAAAA);
      debugPaintFill.setStyle(Paint.Style.FILL);

      debugTextPaint = new TextPaint();
      debugTextPaint.setColor(0xFF444444);
      debugTextPaint.setAntiAlias(true);

    }

    public void update(){

      int newCount = adapter.getCount();
      int count = records.size();

      if(newCount > count){
        for(int i = 0; i < newCount - count; i++){
          CardRecord record = new CardRecord(count + i);
          records.add(record);
        }
      }else if(newCount < count){

        if(newCount < 0){
          throw new IndexOutOfBoundsException();
        }

        for(int i = 0; i < count - newCount; i++){
          CardRecord record = records.removeLast();
          record.recycleView();
        }

      }

    }

    public void measureCards(){
      for(CardRecord record : records){
        record.placeAndCalcRects();
      }
    }

    public void requestOrRecycleViews(){
      for(CardRecord record : records){
        record.recycleViewIfNeeded();
      }

      for(CardRecord record : records){
        record.requestViewIfNeeded();
      }

    }

    public void calcCardRects(){
      for(CardRecord record : records){
        record.calcRect();
      }
    }

    public void debugDrawCards(Canvas canvas){

      int i = 0;

      for(CardRecord record : records){

        RectF rect = record.calcRect();

        float w = rect.right - rect.left;
        float h = rect.bottom - rect.top;

        float cx = (rect.right + rect.left) / 2f;
        float cy = (rect.bottom + rect.top) / 2f;

        canvas.drawRect(rect, debugPaintFill);
        if(selectedCardIndex == i){
          canvas.drawRect(rect, debugPaintStrokeSelected);
        }else {
          canvas.drawRect(rect, debugPaintStroke);
        }

        debugTextPaint.setTextSize(w / 5);

        canvas.drawText(String.valueOf(i), cx, cy, debugTextPaint);

        i++;
      }

    }

  }

  //---------------------------------------------------------------------------
  // ViewRecord

  private class ViewRecord{

    private int index;

    private View view;
    private PrimitiveLayout subView;

    public ViewRecord(int index, View view){
      this.index = index;
      this.view = view;
      subView = new PrimitiveLayout(getContext());
      subView.addView(view, new LayoutParams((int)fullCardWidth, (int)fullCardHeight));
      addViewInLayout(subView, -1, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
      subView.requestLayout();
    }

    public void setIndex(int index){
      this.index = index;
    }

    public int getIndex(){
      return index;
    }

    public View getView(){
      return view;
    }

    public void scroll(float x, float y){
      subView.scrollTo((int)x, (int)y);
    }

  }

  //---------------------------------------------------------------------------
  // ViewPool

  private class ViewPool{

    private LinkedList<ViewRecord> records = new LinkedList<ViewRecord>();
    private LinkedList<ViewRecord> availableRecords = new LinkedList<ViewRecord>();

    public ViewRecord requestView(int index){

      Log.e("AAA", "requesting view... viewPoolSize = " + availableRecords.size() + "/" + records.size());

      if(availableRecords.size() > 0) {

        for (ViewRecord rec : availableRecords) {
          if (rec.getIndex() == index) {
            availableRecords.remove(rec);
            attachViewToParent(rec.subView, -1, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            return rec;
          }
        }

        ViewRecord rec = availableRecords.removeFirst();
        rec.setIndex(index);
        updateRecordView(rec);
        attachViewToParent(rec.subView, -1, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        return rec;

      }else{
        return createNewRecord(index);
      }

    }

    public void recycleView(ViewRecord record){
      availableRecords.add(record);
      detachViewFromParent(record.subView);
    }

    private ViewRecord createNewRecord(int index){
      ViewRecord record = new ViewRecord(index, adapter.createView());
      record.getView().setLayoutParams(new LayoutParams((int)fullCardWidth, (int)fullCardHeight));
      records.add(record);
      updateRecordView(record);
      return record;
    }

    private void updateRecordView(ViewRecord record){
      adapter.updateView(record.getIndex(), record.getView());
    }

    public void fillPool(int number){
      for(int i = 0; i < number; i++) {
        ViewRecord record = new ViewRecord(-100, adapter.createView());
        record.getView().setLayoutParams(new LayoutParams((int) fullCardWidth, (int) fullCardHeight));
        records.add(record);
        availableRecords.add(record);
        detachViewFromParent(record.subView);
      }
    }

    public void measureCards(){

      for(ViewRecord rec : records){
        rec.getView().setLayoutParams(new LayoutParams((int)fullCardWidth, (int)fullCardHeight));
      }

    }

  }

}
