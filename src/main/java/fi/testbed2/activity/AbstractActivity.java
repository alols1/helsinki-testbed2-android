package fi.testbed2.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;
import com.google.ads.AdView;
import com.google.inject.Inject;
import fi.testbed2.R;
import fi.testbed2.dialog.DialogBuilder;
import fi.testbed2.dialog.DialogType;
import fi.testbed2.service.PreferenceService;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import roboguice.activity.RoboActivity;


/**
 * Base activity class for all activities.
 * Handles options menu, for instance.
 */
@EActivity
public abstract class AbstractActivity extends Activity {

    @Inject
    DialogBuilder dialogBuilder;

    @Inject
    PreferenceService preferenceService;

    private String currentErrorMsg;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (preferenceService.isShowWhatsNewDialog()) {
            showDialog(DialogType.WHATS_NEW);
        }
    }

    /** Called when resuming, BUT after onActivityResult! */
    @Override
    protected void onResume() {
        super.onResume();

        AdView adView = (AdView)this.findViewById(R.id.adView);

        if (adView!=null) {
            if (preferenceService.showAds()) {
                adView.setVisibility(AdView.VISIBLE);
            } else {
                adView.setVisibility(AdView.GONE);
            }
        }

    }

    public abstract void onRefreshButtonSelected();

    @OptionsItem(R.id.main_menu_refresh)
    public void onRefreshMenuItemSelected() {
        onRefreshButtonSelected();
    }

    @OptionsItem(R.id.main_menu_preferences)
    public void onPreferencesMenuItemSelected() {
        startActivity(new Intent(this, TestbedPreferenceActivity.class));
    }

    @OptionsItem(R.id.main_menu_about)
    public void onAboutMenuItemSelected() {
        showDialog(DialogType.ABOUT);
    }

    protected void showErrorDialog(String errorMsg) {
        currentErrorMsg = errorMsg;
        showDialog(DialogType.ERROR);
    }

    protected void showDialog(DialogType dialogType) {

        AlertDialog alertDialog = null;

        switch(dialogType) {
            case ABOUT:
                alertDialog = dialogBuilder.getAboutAlertDialog();
                break;
            case WHATS_NEW:
                alertDialog = dialogBuilder.getWhatsNewAlertDialog();
                break;
            case ERROR:
                alertDialog = dialogBuilder.getErrorDialog(currentErrorMsg);
                break;
        }

        alertDialog.show();

    }

    protected void showShortMessage(String msg) {
        Toast toast = Toast.makeText(getApplicationContext(), msg,
                Toast.LENGTH_SHORT);
        toast.show();
    }

    /**
     * Possibly helps freeing up memory
     * @param view
     */
    protected void unbindDrawables(View view) {
        if (view==null) {
            return;
        }
        if (view.getBackground() != null) {
            view.getBackground().setCallback(null);
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                unbindDrawables(((ViewGroup) view).getChildAt(i));
            }
            ((ViewGroup) view).removeAllViews();
        }
    }

}
