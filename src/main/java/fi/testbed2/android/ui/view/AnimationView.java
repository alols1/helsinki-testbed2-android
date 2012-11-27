package fi.testbed2.android.ui.view;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.*;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.googlecode.androidannotations.annotations.EView;
import com.jhlabs.map.Point2D;
import com.larvalabs.svgandroid.SVGParser;
import fi.testbed2.R;
import fi.testbed2.android.app.Logger;
import fi.testbed2.android.ui.svg.LocationMarkerSVG;
import fi.testbed2.android.ui.svg.MunicipalityMarkerSVG;
import fi.testbed2.android.ui.util.CanvasUtil;
import fi.testbed2.domain.MapLocationXY;
import fi.testbed2.domain.Municipality;
import fi.testbed2.domain.TestbedMapImage;
import fi.testbed2.service.BitmapService;
import fi.testbed2.service.LocationService;
import fi.testbed2.service.PageService;
import fi.testbed2.service.SettingsService;
import fi.testbed2.util.SeekBarUtil;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * View which shows the map animation.
 */
@EView
public class AnimationView extends View {

    /**
     * Original map image dimensions.
     */
    public static final double MAP_IMAGE_ORIG_WIDTH = 600d;
    public static final double MAP_IMAGE_ORIG_HEIGHT = 508d;

    /**
     * Defines how many pixels the user is allowed to click
     * off the municipality point. If the touch is withing this
     * threshold, the municipality info is shown.
     */
    public static final float MUNICIPALITY_SEARCH_THRESHOLD = 15.0f;

    /**
     * Used for bounds calculation
     */
    private static final float GESTURE_THRESHOLD_DIP = 16.0f;

    // Scaling related
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;
    @Getter @Setter
    private MapScaleInfo scaleInfo = new MapScaleInfo();
    private boolean doNotMoveMap;

    // Animation frame related
    private int frameWidth;
    private int frameHeight;

    // Bounds calculation related
    @Getter
    private Rect bounds;
    private float boundsStartY;
    private float boundsStartX;
    private float boundsDistanceY;
	private float boundsDistanceX;
	private boolean boundsMoveMap;

    // Views and texts
	private TextView timestampView;
    private SeekBar seekBar;
    @Setter
    private String downloadProgressText;

    // Marker image caching
    private Picture locationMarkerImage;
    private Picture municipalityMarkerImage;
    private Toast municipalityToast;

    // Data
    private List<Municipality> municipalities;
    private Map<Municipality, Point2D.Double> municipalitiesOnScreen;

    // Services
    public LocationService userLocationService;
    public BitmapService bitmapService;
    public PageService pageService;
    public SettingsService settingsService;

    @Setter
    private boolean allImagesDownloaded;

    @Getter
    private AnimationViewPlayer player;

    /**
     * All three constructors are needed!!
     */

	public AnimationView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public AnimationView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AnimationView(Context context) {
		super(context);
	}

    public void initView(Context context) {

        Logger.debug("Initializing AnimationView");

        player = new AnimationViewPlayer(this);

        gestureDetector = new GestureDetector(context, new GestureListener());
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());

        BitmapDrawable firstMap = new BitmapDrawable(bitmapService.getBitmap(getMapImagesToBeDrawn().get(0)));

        frameWidth = firstMap.getMinimumWidth();
        frameHeight = firstMap.getMinimumHeight();

        player.setFrameDelay(settingsService.getSavedFrameDelay());
        player.setCurrentFrame(0);
        player.setFrames(getMapImagesToBeDrawn().size() - 1);

    }

    public void startAnimation(TextView timestampView, SeekBar seekBar, Rect bounds, MapScaleInfo scaleInfo) {
        Logger.debug("Start animation");
        player.play();
        this.scaleInfo = scaleInfo;
        this.timestampView = timestampView;
        this.seekBar = seekBar;
        if (bounds==null) {
            initializeBounds();
        } else {
            this.bounds = bounds;
        }
		next();
	}

    public void next() {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                if(player.isPlaying()) {
                    player.goToNextFrame();
                }
            }
        }, player.getFrameDelay());
    }

    private void initializeBounds() {

        // scale bounds
        int measuredHeight = getMeasuredHeight();
        int measuredWidth = getMeasuredWidth();

        Logger.debug("Initializing bounds...");
        Logger.debug("measuredHeight: " + measuredHeight);
        Logger.debug("measuredWidth: " + measuredWidth);

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

    private List<TestbedMapImage> getMapImagesToBeDrawn() {

        if (allImagesDownloaded) {
            return pageService.getTestbedParsedPage().getAllTestbedImages();
        } else {
            List<TestbedMapImage> list = new ArrayList<TestbedMapImage>();
            list.add(pageService.getTestbedParsedPage().getLatestTestbedImage());
            return list;
        }

    }

    @Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

        canvas.save();
        canvas.scale(scaleInfo.getScaleFactor(), scaleInfo.getScaleFactor(),
                scaleInfo.getPivotX(), scaleInfo.getPivotY());

        TestbedMapImage currentMap = getMapImagesToBeDrawn().get(player.getCurrentFrame());

        updateTimestampView(currentMap);
        updateSeekBar();

        BitmapDrawable frame = new BitmapDrawable(bitmapService.getBitmap(currentMap));
        frame.setBounds(bounds);
        frame.draw(canvas);

        drawMunicipalities(canvas);
        drawUserLocation(canvas);

        canvas.restore();

    }

    private void updateTimestampView(TestbedMapImage currentMap) {

        String timestamp = currentMap.getLocalTimestamp();
        String text = String.format("%1$02d/%2$02d @ ",
                player.getCurrentFrame() + 1 , player.getFrames() + 1) + timestamp;

        if (downloadProgressText!=null) {
            text="@ "+timestamp+"  "+downloadProgressText;
        }

        timestampView.setText(text);
        timestampView.invalidate();

    }

    private void updateSeekBar() {
        seekBar.setProgress(SeekBarUtil.getSeekBarValueFromFrameNumber(player.getCurrentFrame(),
                pageService.getTestbedParsedPage().getAllTestbedImages().size()));
    }

    private void drawUserLocation(Canvas canvas) {
        MapLocationXY userLocation = userLocationService.getUserLocationXY();
        if (userLocation!=null) {
            drawPoint(userLocation, Color.BLACK, canvas, null);
        }
    }

    private void drawMunicipalities(Canvas canvas) {

        for (Municipality municipality : municipalities) {
            if (municipality!=null) {
                drawPoint(municipality.getXyPos(), Color.BLACK, canvas, municipality);
            }
        }

    }

    private void drawPoint(MapLocationXY point, int color, Canvas canvas, Municipality municipality) {

        Paint paint = new Paint();
        paint.setColor(color);
        paint.setAntiAlias(true);
        paint.setAlpha(200); // 0...255, 255 = no transparency, does not affect SVG transparency!

        double imgScaledWidth = bounds.width();
        double imgScaledHeight = bounds.height();

        double widthRatio = imgScaledWidth / MAP_IMAGE_ORIG_WIDTH;
        double heightRatio = imgScaledHeight / MAP_IMAGE_ORIG_HEIGHT;

        float xScaled = Double.valueOf(bounds.left + point.getX()*widthRatio).floatValue();
        float yScaled = Double.valueOf(bounds.top + point.getY()*heightRatio).floatValue();

        int xInt = Float.valueOf(xScaled).intValue();
        int yInt = Float.valueOf(yScaled).intValue();

        if (municipality==null) {

            Picture pic = getLocationMarkerImage();

            int markerImageHeight = settingsService.getMapMarkerSize();

            // Scale width a bit larger than original width (otherwise looks a bit too thin marker)
            float ratio = pic.getWidth()/pic.getHeight();
            int width = Float.valueOf(markerImageHeight*ratio+markerImageHeight/5).intValue();
            int height = markerImageHeight;

            /*
            * x, y coordinates are image's top left corner,
            * so position the marker to the bottom center
            */
            int left = xInt-width/2;
            int top = yInt-height;
            int right = xInt+width/2;
            int bottom = yInt;

            drawPicture(pic, canvas, new Rect(left,top,right,bottom));

        } else {

            // Save canvas coordinates for info toast
            this.municipalitiesOnScreen.put(municipality,
                    new Point2D.Double(xInt,yInt));

            Picture pic = getMunicipalityMarkerImage();

            int circleDiameter = settingsService.getMapPointSize();

            /*
            * x, y coordinates are image's top left corner,
            * so position the marker to the center
            */

            int left = xInt-circleDiameter/2;
            int top = yInt-circleDiameter/2;
            int right = xInt+circleDiameter/2;
            int bottom = yInt+circleDiameter/2;

            drawPicture(pic, canvas, new Rect(left, top, right, bottom));
        }

    }

    private void drawPicture(Picture pic, Canvas canvas, Rect rect) {

        try {
            canvas.drawPicture(pic, rect);
        } catch (Throwable e) {
            /*
             * Some user's seem to use Force GPU setting which does not support
             * the drawPicture method (throws java.lang.UnsupportedOperationException).
             * Do not show anything to those users but an alert dialog
             * that they should disabled Force GPU settings.
             */
        }
    }

    private Picture getLocationMarkerImage() {

        if (locationMarkerImage ==null) {
            String color = settingsService.getMapMarkerColor();
            locationMarkerImage = SVGParser.getSVGFromString(new LocationMarkerSVG(color).getXmlContent()).getPicture();
        }
        return locationMarkerImage;

    }

    private Picture getMunicipalityMarkerImage() {

        if (municipalityMarkerImage ==null) {
            String color = settingsService.getMapPointColor();
            municipalityMarkerImage = SVGParser.getSVGFromString(new MunicipalityMarkerSVG(color).getXmlContent()).getPicture();
        }
        return municipalityMarkerImage;

    }

    public void resetMarkerAndPointImageCache() {
        locationMarkerImage = null;
        municipalityMarkerImage = null;
    }

	@Override
	public boolean onTouchEvent(MotionEvent event) {

        gestureDetector.onTouchEvent(event);
        scaleDetector.onTouchEvent(event);
        calculateNewBounds(event);

        return true;
	}


    /**
     * This method is quite a mess. It is hard to understand how the bounds
     * are calculated. Do not touch this if you don't know what you are doing.
     *
     * @param event
     * @return
     */
    private boolean calculateNewBounds(MotionEvent event) {

        int action = event.getAction();
        boolean isMultiTouchEvent = event.getPointerCount()>1;

        // Do not move the map in multi-touch
        if (isMultiTouchEvent) {
            doNotMoveMap = true;
        }

        // Allow map movement again when new non-multi-touch down event occurs
        if (!isMultiTouchEvent && action==MotionEvent.ACTION_DOWN) {
            doNotMoveMap = false;
        }

        if (doNotMoveMap) {
            return true;
        }

        switch(action) {
            case MotionEvent.ACTION_CANCEL:
                break;
            case MotionEvent.ACTION_UP:
                boundsMoveMap = false;
                break;
            case MotionEvent.ACTION_DOWN:
                boundsStartY = event.getY();
                boundsDistanceY = 0.0f;
                boundsStartX = event.getX();
                boundsDistanceX = 0.0f;
                break;
            case MotionEvent.ACTION_MOVE:
                boundsDistanceY = -(boundsStartY - event.getY());
                boundsDistanceX = -(boundsStartX - event.getX());

                Rect savedBounds = new Rect(bounds);

                Logger.debug("Calculating new bounds...");

                // Convert the dips to pixels
                final float scale = getContext().getResources().getDisplayMetrics().density;
                int mGestureThreshold = (int) (GESTURE_THRESHOLD_DIP * scale / scaleInfo.getScaleFactor() + 0.5f);

                if(!boundsMoveMap && (Math.abs(boundsDistanceY) > mGestureThreshold || Math.abs(boundsDistanceX) > mGestureThreshold)) {
                    boundsMoveMap = true;
                }

                if(boundsMoveMap) {

                    hideMunicipalityToast();

                    float mDistance_y_dip = boundsDistanceY * scale / scaleInfo.getScaleFactor() + 0.5f;
                    float mDistance_x_dip = boundsDistanceX * scale / scaleInfo.getScaleFactor() + 0.5f;

                    bounds.offset((int)mDistance_x_dip, (int)mDistance_y_dip);

                    Logger.debug("New bounds left: " + bounds.left);
                    Logger.debug("New bounds top: " + bounds.top);
                    Logger.debug("New bounds right: " + bounds.right);
                    Logger.debug("New bounds bottom: " + bounds.bottom);

                    // restart measuring distances
                    boundsStartY = event.getY();
                    boundsDistanceY = 0.0f;
                    boundsStartX = event.getX();
                    boundsDistanceX = 0.0f;

                    invalidate();
                }


                break;
            default:
                return super.onTouchEvent(event);

        }

        return true;
    }

    /*
    * ============
    * Listeners for pinch zooming
    * ============
    */

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {

            if (detector.getScaleFactor()>1.0+MapScaleInfo.MIN_SCALE_STEP_WHEN_PINCHING ||
                    detector.getScaleFactor()<1.0-MapScaleInfo.MIN_SCALE_STEP_WHEN_PINCHING) {

                float newScaleFactor = scaleInfo.getScaleFactor() * detector.getScaleFactor();

                // Don't let the map to get too small or too large.
                newScaleFactor = Math.max(MapScaleInfo.MIN_SCALE_FACTOR,
                        Math.min(newScaleFactor, MapScaleInfo.MAX_SCALE_FACTOR));
                scaleInfo.setScaleFactor(newScaleFactor);
                scaleInfo.setPivotX(getMeasuredWidth()/2);
                scaleInfo.setPivotY(getMeasuredHeight()/2);

                hideMunicipalityToast();
                invalidate();
                return true;

            }

            return false;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {

            scaleInfo.setPivotX(e.getX());
            scaleInfo.setPivotY(e.getY());
            float newScaleFactor = scaleInfo.getScaleFactor() *
                    MapScaleInfo.SCALE_STEP_WHEN_DOUBLE_TAPPING;

            if (newScaleFactor >= MapScaleInfo.MAX_SCALE_FACTOR) {
                newScaleFactor = MapScaleInfo.DEFAULT_SCALE_FACTOR;
            }

            scaleInfo.setScaleFactor(newScaleFactor);

            hideMunicipalityToast();
            invalidate();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {

            float xCanvas = CanvasUtil.convertRawXCoordinateToScaledCanvasCoordinate(e.getX(), scaleInfo);
            float yCanvas = CanvasUtil.convertRawYCoordinateToScaledCanvasCoordinate(e.getY(), scaleInfo);

            Logger.debug("Long pressed x: " + e.getX());
            Logger.debug("Long pressed y: " + e.getY());
            Logger.debug("Long pressed x (canvas): " + xCanvas);
            Logger.debug("Long pressed y (canvas): " + yCanvas);
            Logger.debug("Scale factor: " + scaleInfo.getScaleFactor());
            Logger.debug("Scale pivotX: " + scaleInfo.getPivotX());
            Logger.debug("Scale pivotY: " + scaleInfo.getPivotY());

            hideMunicipalityToast(); // always hide previous toast
            showInfoForMunicipality(xCanvas, yCanvas, e.getX(), e.getY());
        }

    }

    /**
     * Shows info toast about selected municipality if there is
     * a municipality close to the given x,y coordinates.
     *
     * @param canvasX X coordinate in canvas coordinate
     * @param canvasY Y coordinate in canvas coordinate
     * @param rawX Raw X coordinate from the touch event
     * @param rawY Raw Y coordinate from the touch event
     * @return True if the toast was shown, false if not shown.
     */
    private boolean showInfoForMunicipality(float canvasX, float canvasY, float rawX, float rawY) {

        for (Municipality municipality : municipalities) {

            if (municipality!=null) {

                Point2D.Double pos = municipalitiesOnScreen.get(municipality);

                if (pos!=null) {

                    Logger.debug("Pos: " + pos + ", for: " + municipality.getName());

                    final float scale = getContext().getResources().getDisplayMetrics().density;
                    int threshold = (int) ((settingsService.getMapPointSize()/2)*scale +
                                            (MUNICIPALITY_SEARCH_THRESHOLD/scaleInfo.getScaleFactor())*scale);

                    if (Math.abs(pos.x-canvasX)<=threshold && Math.abs(pos.y-canvasY)<=threshold) {

                        Logger.debug("Show info for municipality: " + municipality.getName());

                        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                        View layout = inflater.inflate(R.layout.toast_municipality,
                                (ViewGroup) findViewById(R.id.toast_layout_root));

                        TextView text = (TextView) layout.findViewById(R.id.text);
                        text.setText(municipality.getName());

                        municipalityToast = new Toast(getContext());
                        municipalityToast.setGravity(Gravity.TOP| Gravity.LEFT,
                                Double.valueOf(rawX+20*scale).intValue(),
                                Double.valueOf(rawY-40*scale).intValue());
                        municipalityToast.setDuration(Toast.LENGTH_LONG);
                        municipalityToast.setView(layout);
                        municipalityToast.show();
                        return true;

                    }

                }

            }

        }

        return false;

    }

    private void hideMunicipalityToast() {
        if (municipalityToast!=null) {
            municipalityToast.cancel();
        }
    }

    /*
    * ============
    * Setters and getters
    * ============
    */

    public void updateBounds(Rect bounds) {
        if (bounds==null) {
            initializeBounds();
        } else {
            this.bounds = bounds;
        }
    }

    public void setMunicipalities(List<Municipality> municipalities) {
        this.municipalities = municipalities;
        this.municipalitiesOnScreen = new HashMap<Municipality, Point2D.Double>();
    }
}