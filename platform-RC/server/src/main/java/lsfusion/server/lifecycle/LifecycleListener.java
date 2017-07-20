package lsfusion.server.lifecycle;

import java.util.Comparator;

import static java.lang.System.identityHashCode;
import static lsfusion.base.BaseUtils.compareInts;

public interface LifecycleListener {
    int LOGICS_ORDER = 100;
    int SYSTEM_ORDER = 200;
    int DBMANAGER_ORDER = SYSTEM_ORDER + 100;
    int SECURITYMANAGER_ORDER = SYSTEM_ORDER + 200;
    int RMIMANAGER_ORDER = SYSTEM_ORDER + 300;
    int BLLOADER_ORDER = SYSTEM_ORDER + 400;

    //более высокий order ради onStarted
    int HIGH_DAEMON_ORDER = 8000;
    int REFLECTION_ORDER = 9000;
    int DAEMON_ORDER = 10000;

    Comparator<LifecycleListener> ORDER_COMPARATOR = new Comparator<LifecycleListener>() {
        @Override
        public int compare(LifecycleListener o1, LifecycleListener o2) {
            int p1 = o1.getOrder();
            int p2 = o2.getOrder();
            int cmp = compareInts(p1, p2);
            return cmp != 0 ? cmp : compareInts(identityHashCode(o1), identityHashCode(o2));
        }
    };

    int getOrder();

    void lifecycleEvent(LifecycleEvent event);
}
