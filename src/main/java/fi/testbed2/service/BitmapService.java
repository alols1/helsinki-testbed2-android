package fi.testbed2.service;

import android.graphics.Bitmap;
import fi.testbed2.android.task.exception.DownloadTaskException;
import fi.testbed2.domain.TestbedMapImage;

/**
 * Service which provides downloaded Bitmap images
 * for testbed images.
 */
public interface BitmapService {

    public boolean bitmapIsDownloaded(TestbedMapImage image);
    public Bitmap downloadBitmap(TestbedMapImage image) throws DownloadTaskException;
    public Bitmap getBitmap(TestbedMapImage image);
    public void evictBitmap(TestbedMapImage image);
    public void evictAll();

}
