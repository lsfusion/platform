package skolkovo.gwt.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ProjectsServiceAsync {
    void getProjects(AsyncCallback<String[]> asyncCallback);
}
