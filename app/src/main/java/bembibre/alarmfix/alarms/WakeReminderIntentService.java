package bembibre.alarmfix.alarms;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

/**
 * Created by Max Power on 12/08/2017.
 */
public abstract class WakeReminderIntentService extends IntentService {
    public static final String LOCK_NAME_STATIC="com.dummies.android.taskreminder.Static";
    private static PowerManager.WakeLock lockStatic=null;

    public WakeReminderIntentService(String name) {
        super(name);
    }

    public static void acquireStaticLock(Context context) {
        getLock(context).acquire();
    }

    synchronized private static PowerManager.WakeLock getLock(Context context) {
        if (lockStatic == null) {
            PowerManager mgr = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
            lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, LOCK_NAME_STATIC);
            lockStatic.setReferenceCounted(true);
        }
        return(lockStatic);
    }

    @Override
    final protected void onHandleIntent(Intent intent) {
        try {
            doReminderWork(intent);
        } finally {
            getLock(this).release();
        }
    }

    protected abstract void doReminderWork(Intent intent);
}