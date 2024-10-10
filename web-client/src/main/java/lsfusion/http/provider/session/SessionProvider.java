package lsfusion.http.provider.session;

import lsfusion.interop.session.ExternalUtils;

public interface SessionProvider {

    ExternalUtils.SessionContainer getContainer();
}
