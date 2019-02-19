package lsfusion.http.provider.logics;

import lsfusion.gwt.shared.exceptions.AppServerNotAvailableException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public interface LogicsProvider {

    ServerSettings getServerSettings(HttpServletRequest request);

    <R> R runRequest(String host, Integer port, String exportName, LogicsRunnable<R> runnable) throws IOException, AppServerNotAvailableException;

}
