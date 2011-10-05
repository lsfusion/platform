package paas.manager.server;

import paas.PaasBusinessLogics;
import platform.server.logics.BusinessLogics;
import platform.server.lifecycle.LifecycleAdapter;
import platform.server.lifecycle.LifecycleEvent;

public class AppManagerLifecycleListener extends LifecycleAdapter {
    private final AppManager appManager;

    public AppManagerLifecycleListener(AppManager appManager) {
        this.appManager = appManager;
    }

    @Override
    protected void onStarted(LifecycleEvent event) {
        appManager.start();
    }

    @Override
    protected void onLogicsCreated(BusinessLogics logics) {
        PaasBusinessLogics paas = (PaasBusinessLogics) logics;
        appManager.setLogics(paas);
        paas.setAppManager(appManager);
    }

    @Override
    protected void onStopping(LifecycleEvent event) {
        appManager.stop();
    }
}
