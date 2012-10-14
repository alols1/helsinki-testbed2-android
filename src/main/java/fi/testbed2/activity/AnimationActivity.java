package fi.testbed2.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.location.LocationListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import com.google.inject.Inject;
import fi.testbed2.R;
import fi.testbed2.app.MainApplication;
import fi.testbed2.service.*;
import fi.testbed2.data.Municipality;
import fi.testbed2.data.TestbedParsedPage;
import fi.testbed2.task.DownloadImagesTask;
import fi.testbed2.util.SeekBarUtil;
import fi.testbed2.view.AnimationView;
import roboguice.inject.ContentView;
import roboguice.inject.InjectResource;
import roboguice.inject.InjectView;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: this class has grown quite large. Refactor it to smaller classes
 */
@ContentView(R.layout.animation)
public class AnimationActivity extends AbstractActivity implements OnClickListener, SeekBar.OnSeekBarChangeListener {

    @Inject
    MunicipalityService municipalityService;

    @Inject
    LocationService locationService;

    @Inject
    CoordinateService coordinateService;

    @Inject
    PreferenceService preferenceService;

    @Inject
    BitmapService bitmapService;

    @Inject
    PageService pageService;

    @InjectView(R.id.animation_view)
    AnimationView animationView;

    @InjectView(R.id.playpause_button)
    ImageButton playPauseButton;

    @InjectView(R.id.timestamp_view)
    TextView timestampView;

    @InjectView(R.id.seek)
    SeekBar seekBar;

    boolean isPlaying = true;

    private DownloadImagesTask task;

    private int orientation;

    private boolean allImagesDownloaded;

    public AnimationActivity() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!compulsoryDataIsAvailable()) {
            returnToMainActivity();
            return;
        }

        playPauseButton.setOnClickListener(this);
        seekBar.setOnSeekBarChangeListener(this);

        animationView.setAllImagesDownloaded(false);

        animationView.municipalities = preferenceService.getSavedMunicipalities();
        animationView.userLocationService = locationService;
        animationView.bitmapService = bitmapService;
        animationView.pageService = pageService;

        animationView.init(this);
        initAnimation();

        orientation = this.getResources().getConfiguration().orientation;

    }


    private void initAnimation() {

        animationView.post(new Runnable() {
            @Override
            public void run() {
                final Rect bounds = preferenceService.getSavedMapBounds(orientation);
                final float scale = preferenceService.getSavedScaleFactor(orientation);
                updatePlayingState(true);
                if (bounds==null) {
                    animationView.start(timestampView, seekBar, scale);
                } else {
                    animationView.start(timestampView, seekBar, bounds, scale);
                }

            }
        });

    }


    /**
     * Updates the Downloading text in top left corner
     * @param text
     */
    public void updateDownloadProgressInfo(final String text) {
        runOnUiThread(new Runnable() {
            public void run() {
                animationView.setDownloadProgressText(text);
                timestampView.invalidate();
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        saveMapBoundsAndScaleFactor();
        orientation = newConfig.orientation;
        updateBoundsToView();
    }

    /**
     * Saves the bounds of the map user has previously viewed to persistent storage.
     */
    private void saveMapBoundsAndScaleFactor() {
        preferenceService.saveMapBoundsAndScaleFactor(animationView.getBounds(),
                animationView.getScaleFactor(), orientation);
    }

    private void updateBoundsToView() {
        animationView.setScaleFactor(preferenceService.getSavedScaleFactor(orientation));
        animationView.updateBounds(preferenceService.getSavedMapBounds(orientation));
    }


    @Override
	protected void onPause() {
        super.onPause();
        if (task!=null) {
            task.kill();
        }
        this.pauseAnimation();
        this.saveMapBoundsAndScaleFactor();
        locationService.stopListeningLocationChanges();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!compulsoryDataIsAvailable()) {
            returnToMainActivity();
            return;
        }

        if (preferenceService.showUserLocation()) {
            locationService.startListeningLocationChanges();
        }

        updateBoundsToView();
        animationView.municipalities = preferenceService.getSavedMunicipalities();
        animationView.setFrameDelay(preferenceService.getSavedFrameDelay());

        if (!allImagesDownloaded) {
            task = new DownloadImagesTask(this);
            task.setActivity(this);
            task.execute();
        }
    }

    /**
     * Check that we have all compulsory data before starting the activity.
     * Android system might kill the app after onPause() and when resuming the
     * app after it has been killed, all static data is null.
     *
     * @return
     */
    private boolean compulsoryDataIsAvailable() {

        TestbedParsedPage page = pageService.getTestbedParsedPage();

        /*
        * Parsed page should always be non-null.
        */
        if (page==null) {
            return false;
        }

        boolean noImagesExist = page.getAllTestbedImages()==null || page.getAllTestbedImages().isEmpty();
        boolean allImagesDownloadedButSomeAreMissing = allImagesDownloaded && pageService.getNotDownloadedCount()>0;

        if (noImagesExist || allImagesDownloadedButSomeAreMissing) {
            return false;
        }

        return true;

    }

    private void returnToMainActivity() {
        this.allImagesDownloaded = false;
        pageService.evictAll();
        bitmapService.evictAll();
        System.gc();
        Intent intent = new Intent();
        this.setResult(MainApplication.RESULT_OK, intent);
        this.finish();
    }

    public void onAllImagesDownloaded() {
        allImagesDownloaded = true;
        animationView.setAllImagesDownloaded(true);
        animationView.setDownloadProgressText(null);
        animationView.refresh(getApplicationContext());
        animationView.previous();
        updatePlayingState(false);
        if (preferenceService.isStartAnimationAutomatically()) {
            playAnimation();
        }
    }

    private void playAnimation() {
        updatePlayingState(true);
        animationView.play();
    }

    private void pauseAnimation() {
        updatePlayingState(false);
        animationView.pause();
    }

    @Override
	public void onClick(View v) {
        if (!allImagesDownloaded) {
            return;
        }
		switch(v.getId()) {
            case R.id.playpause_button:
                animationView.playpause();
                updatePlayingState(!isPlaying);
                break;
            default:
                return;
		}
	}

    private void updatePlayingState(boolean animationIsPlaying) {

        isPlaying = animationIsPlaying;

        if (isPlaying) {
            playPauseButton.setImageResource(R.drawable.ic_media_pause);
        } else {
            playPauseButton.setImageResource(R.drawable.ic_media_play);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindDrawables(findViewById(R.id.AnimationRootView));
    }

    @Override
    public void onRefreshButtonSelected() {
        this.pauseAnimation();
        this.allImagesDownloaded = false;
        Intent intent = new Intent();
        this.setResult(MainApplication.RESULT_REFRESH, intent);
        this.finish();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            this.pauseAnimation();
            animationView.goToFrame(SeekBarUtil.getFrameIndexFromSeekBarValue(progress,
                    pageService.getTestbedParsedPage().getAllTestbedImages().size()));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        this.pauseAnimation();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // Don't do anything
    }
}
