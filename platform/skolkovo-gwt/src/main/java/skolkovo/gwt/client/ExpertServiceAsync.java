package skolkovo.gwt.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ExpertServiceAsync {
    void getProjects(AsyncCallback<String[]> asyncCallback);
}
