package lsfusion.server.base.controller.lifecycle;

import java.util.Comparator;

import static java.lang.System.identityHashCode;
import static lsfusion.base.BaseUtils.compareInts;

public interface LifecycleListener {
    int LOGICS_ORDER = 100; // logical model
    int DBMANAGER_ORDER = 300; // physical model (execution)
    int SECURITYMANAGER_ORDER = 400; // physical model (management)
    
    int RMIMANAGER_ORDER = 500;    
    int BLLOADER_ORDER = 600; // onStarted is executed here to have all managers initialized, but just before exposing logics to the world, to guarantee that all system onStarted are called (however daemons are initialized later)
    
    int DAEMON_ORDER = 8000;

    int REFLECTION_ORDER = 9000; //the last because the most heavyweight

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
