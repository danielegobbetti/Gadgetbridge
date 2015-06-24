package nodomain.freeyourgadget.gadgetbridge;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Build.VERSION;
import android.preference.PreferenceManager;
import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import nodomain.freeyourgadget.gadgetbridge.database.ActivityDatabaseHandler;

public class GBApplication extends Application {
    private static GBApplication context;
    private static ActivityDatabaseHandler mActivityDatabaseHandler;

    public GBApplication() {
        context = this;
        mActivityDatabaseHandler = new ActivityDatabaseHandler(context);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        setupLogging();
//        For debugging problems with the logback configuration
//        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
//         print logback's internal status
//        StatusPrinter.print(lc);
//        Logger logger = LoggerFactory.getLogger(GBApplication.class);

// for testing DB stuff
//        SQLiteDatabase db = mActivityDatabaseHandler.getWritableDatabase();
//        db.close();
    }

    public static boolean isFileLoggingEnabled() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GBApplication.getContext());
        return prefs.getBoolean("log_to_file", false);
    }

    private void setupLogging() {
        if (isFileLoggingEnabled()) {
            File dir = getExternalFilesDir(null);
            if (dir != null) {
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                // used by assets/logback.xml since the location cannot be statically determined
                System.setProperty("GB_LOGFILES_DIR", dir.getAbsolutePath());
            } else {
                Log.e("GBApplication", "External files dir is null, cannot log to file");
                System.setProperty("GB_LOGFILES_DIR", "/dev/null");

            }
        } else {
            System.setProperty("GB_LOGFILES_DIR", "/dev/null"); // just to please logback configuration, not used at all
            try {
                ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
                root.detachAppender("FILE");
            } catch (Throwable ex) {
                System.out.println("Error removing logger FILE appender");
                ex.printStackTrace();
            }
        }
    }

    public static Context getContext() {
        return context;
    }

    public static ActivityDatabaseHandler getActivityDatabaseHandler() {
        return mActivityDatabaseHandler;
    }

    public static boolean isRunningLollipopOrLater() {
        return VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }
}
