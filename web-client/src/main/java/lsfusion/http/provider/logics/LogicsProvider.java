package lsfusion.http.provider.logics;

import lsfusion.gwt.shared.exceptions.AppServerNotAvailableException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public interface LogicsProvider {

    JSONObject getServerSettings(HttpServletRequest request);

    <R> R runRequest(String host, Integer port, String exportName, LogicsRunnable<R> runnable) throws IOException, AppServerNotAvailableException;

}
