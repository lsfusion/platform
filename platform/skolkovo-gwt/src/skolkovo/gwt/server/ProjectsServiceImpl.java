package skolkovo.gwt.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import skolkovo.gwt.client.ProjectsService;

public class ProjectsServiceImpl extends RemoteServiceServlet implements ProjectsService {
    public String[] getProjects() {
        return new String[] {"aurora", "gannimed", "mustang"};
    }
}