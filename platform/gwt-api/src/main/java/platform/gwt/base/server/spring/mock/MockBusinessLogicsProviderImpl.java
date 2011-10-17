package platform.gwt.base.server.spring.mock;

import platform.gwt.base.server.spring.BusinessLogicsProvider;
import platform.interop.RemoteLogicsInterface;

public class MockBusinessLogicsProviderImpl implements BusinessLogicsProvider {
    private final RemoteLogicsInterface logics;

    public MockBusinessLogicsProviderImpl(RemoteLogicsInterface logics) {
        this.logics = logics;
    }

    @Override
    public RemoteLogicsInterface getLogics() {
        return logics;
    }

    @Override
    public void invalidate() {
        //ignore
    }
}
