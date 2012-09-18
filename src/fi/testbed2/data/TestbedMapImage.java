package fi.testbed2.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import fi.testbed2.app.MainApplication;
import fi.testbed2.exception.DownloadTaskException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Represents the image object read from the testbed website.
 */
public class TestbedMapImage {

    private String imageURL;
    private String timestamp;
    private Bitmap downloadedBitmapImage;
    private int index;

    public TestbedMapImage(String imageURL, String timestamp, int index) {
        this.imageURL = imageURL;
        this.timestamp = timestamp;
        this.index = index;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public boolean hasBitmapDataDownloaded() {
        return downloadedBitmapImage !=null;
    }

    /**
     * Returns the downloaded bitmap image. If image not yet downloaded, downloads it.
     * @return
     * @throws DownloadTaskException
     */
    public Bitmap getDownloadedBitmapImage() throws DownloadTaskException {

        if (downloadedBitmapImage==null) {
            try {

                //Log.e(MainApplication.LOG_IDENTIFIER, "downloading image: "+index);
                downloadedBitmapImage = BitmapFactory.decodeStream((InputStream) new URL(imageURL).getContent());

                if (downloadedBitmapImage==null) {
                    throw new DownloadTaskException("Could not download map image.");
                }

            } catch (IllegalStateException e) {
                throw new DownloadTaskException("IllegalStateException: " + e.getMessage(), e);
            } catch (IOException e) {
                throw new DownloadTaskException("IOException: " + e.getMessage(), e);
            }

        }

        return downloadedBitmapImage;
    }

    public String getLocalTimestamp() {
        String gmtTimestamp = this.getTimestamp();
        int year = Integer.parseInt(gmtTimestamp.substring(0, 4));
        int month = Integer.parseInt(gmtTimestamp.substring(4, 6));
        int day = Integer.parseInt(gmtTimestamp.substring(6, 8));
        int hour = Integer.parseInt(gmtTimestamp.substring(8, 10));
        int minute = Integer.parseInt(gmtTimestamp.substring(10, 12));
        int second = 0;

        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("Helsinki"));
        cal.set(year + 1900, month, day, hour, minute, second);
        cal.setTimeInMillis(cal.getTimeInMillis()); // XXX + Calendar.getInstance().get(Calendar.DST_OFFSET));

        String localStamp = String.format("%1$tH:%2$tM", cal.getTime(), cal.getTime());

        return localStamp;
    }

}
