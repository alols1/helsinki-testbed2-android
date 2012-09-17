package fi.testbed2.view;

import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import fi.testbed2.app.MainApplication;
import fi.testbed2.data.MapImage;

public class AnimationView extends View {

	// The gesture threshold expressed in dip
	private static final float GESTURE_THRESHOLD_DIP = 16.0f;
	private int frameDelay;
	private int currentFrame;
	private int frames;
	private List<MapImage> mapImageList;
	private int frameWidth;
	private int frameHeight;
	private Rect bounds;
	private float mStart_y;
	private float mDistance_y;
	private float mStart_x;
	private float mDistance_x;
	private boolean moveMap;
	private TextView timestampView;
	private boolean play;
	
	
	public AnimationView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public AnimationView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public AnimationView(Context context) {
		super(context);
		init(context);
	}
	
	public void start(TextView timestampView) {
		this.play = true;
		this.timestampView = timestampView;
		initializeBounds();
		next();
	}
	
	public void start(TextView timestampView, Rect bounds) {
		this.play = true;
		this.timestampView = timestampView;
		this.bounds = bounds;
		next();
	}
	
	public void previous() {
		this.play = false;
		
		currentFrame--;
		
		if(currentFrame < 0)
			currentFrame = Math.abs((frames - 1) - currentFrame);
		
		invalidate();
		
	}

	public void playpause() {
		if(play) {
			play = false;
		} else {
			play = true;
			next();
		}
		invalidate();
	}

	public void forward() {
		this.play = false;
		currentFrame++;
		
		if(currentFrame > frames)
			currentFrame = 0;
		
		invalidate();
	}
	
	public void stop() {
		this.play = false;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		String text = String.format("%1$2d/%2$2d @ ", currentFrame + 1 , frames + 1) + mapImageList.get(currentFrame).timestamp;

		timestampView.setText(text);
		timestampView.invalidate();

		BitmapDrawable frame = mapImageList.get(currentFrame).bitmapDrawable;
		frame.setBounds(bounds);
		frame.draw(canvas);

	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		int action = event.getAction();
		Rect viewBounds = new Rect(0, 0, getWidth(), getHeight());
		
		switch(action)
		{
		case MotionEvent.ACTION_CANCEL:
			break;
		case MotionEvent.ACTION_UP:
			moveMap = false;
			break;
		case MotionEvent.ACTION_DOWN:
			mStart_y = event.getY();
			mDistance_y = 0.0f;
			mStart_x = event.getX();
			mDistance_x = 0.0f;
			break;
		case MotionEvent.ACTION_MOVE:
			mDistance_y = -(mStart_y - event.getY());
			mDistance_x = -(mStart_x - event.getX());
			
			Rect savedBounds = new Rect(bounds);
			
			// Convert the dips to pixels
			final float scale = getContext().getResources().getDisplayMetrics().density;
			int mGestureThreshold = (int) (GESTURE_THRESHOLD_DIP * scale + 0.5f);


			if(!moveMap && (Math.abs(mDistance_y) > mGestureThreshold || Math.abs(mDistance_x) > mGestureThreshold)) {
				moveMap = true;
			}

			
			if(moveMap) {
				
				float mDistance_y_dip = mDistance_y * scale + 0.5f;
				float mDistance_x_dip = mDistance_x * scale + 0.5f;
				
				bounds.offset((int)mDistance_x_dip, (int)mDistance_y_dip);
				
				if(!bounds.contains(viewBounds)) {
					// overflow was on top or bottom y-axis, invalidate y
					if(bounds.top > viewBounds.top || bounds.bottom < viewBounds.bottom)
						mDistance_y = 0.0f;
					// if overflow was on left or right x-axis, invalidate x
					if(bounds.left > viewBounds.left || bounds.right < viewBounds.right)
						mDistance_x = 0.0f;
					savedBounds.offset((int)mDistance_x, (int)mDistance_y);
					bounds = savedBounds;
				}
				
				// restart measuring distances
				mStart_y = event.getY();
				mDistance_y = 0.0f;
				mStart_x = event.getX();
				mDistance_x = 0.0f;
				
				invalidate();
			}

			
			break;
		default:
			return super.onTouchEvent(event);
		}

		return true;
	}

    public void refresh(Context context) {
        this.init(context);
    }

	private void init(Context context) {
        
		// get default frame delay
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		frameDelay = Integer.parseInt(sharedPreferences.getString("PREF_ANIM_FRAME_DELAY", "1000"));
		
        mapImageList = MainApplication.getMapImageList();
        
    	// Assume all frames have same dimensions
    	frameWidth = mapImageList.get(0).bitmapDrawable.getMinimumWidth();
    	frameHeight = mapImageList.get(0).bitmapDrawable.getMinimumHeight();
    	
		currentFrame = 0;
		frames = mapImageList.size() - 1;
	}
	
	private void initializeBounds() {
		
		// scale bounds
		int measuredHeight = getMeasuredHeight();
		int measuredWidth = getMeasuredWidth();
		
		if(measuredHeight > measuredWidth) {
			int scaleBy = measuredHeight-frameHeight;
			if(scaleBy > 0)
				bounds = new Rect(0, 0, frameWidth + scaleBy, frameHeight + scaleBy);
			else
				bounds = new Rect(0, 0, frameWidth, frameHeight);
		} else {
			int scaleBy = measuredWidth-frameWidth;
			if(scaleBy > 0)
				bounds = new Rect(0, 0, frameWidth + scaleBy, frameHeight + scaleBy);
			else
				bounds = new Rect(0, 0, frameWidth, frameHeight);
		}
	}
	
	private void next() {
		postDelayed(new Runnable() {
			
			@Override
			public void run() {
				
				if(play) {
					currentFrame++;
					
					if(currentFrame > frames)
						currentFrame = 0;
					
					invalidate();
					next();
				}

			}
		}, frameDelay);
	}

	public Rect getBounds() {
		return bounds;
	}
	
}