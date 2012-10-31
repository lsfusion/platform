package platform.gwt.base.server.spring.mock;

import platform.gwt.base.server.spring.BusinessLogicsProvider;
import platform.gwt.base.server.spring.InvalidateListener;
import platform.interop.RemoteLogicsInterface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MockBusinessLogicsProviderImpl implements BusinessLogicsProvider {
    private final RemoteLogicsInterface logics;
    private final List<InvalidateListener> invalidateListeners = Collections.synchronizedList(new ArrayList<InvalidateListener>());

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

    @Override
    public void addInvlidateListener(InvalidateListener listener) {
        invalidateListeners.add(listener);
    }

    @Override
    public void removeInvlidateListener(InvalidateListener listener) {
        invalidateListeners.remove(listener);
    }
}
