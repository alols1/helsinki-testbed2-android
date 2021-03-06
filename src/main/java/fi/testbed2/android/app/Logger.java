package fi.testbed2.android.app;

import fi.testbed2.Environment;
import roboguice.util.Ln;

/**
 * Logger class used in testbed app.
 */
public final class Logger {

    public static void debug(String s) {
        if (Environment.DEBUG) {
            Ln.e(s);
        }
    }

}
