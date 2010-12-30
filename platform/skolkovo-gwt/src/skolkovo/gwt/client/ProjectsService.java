package skolkovo.gwt.client;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.google.gwt.core.client.GWT;

@RemoteServiceRelativePath("ProjectsService")
public interface ProjectsService extends RemoteService {
    String[] getProjects();

    /**
     * Utility/Convenience class.
     * Use ProjectsService.App.getInstance() to access static instance of ProjectsServiceAsync
     */
    public static class App {
        private static final ProjectsServiceAsync ourInstance = (ProjectsServiceAsync) GWT.create(ProjectsService.class);

        public static ProjectsServiceAsync getInstance() {
            return ourInstance;
        }
    }
}
