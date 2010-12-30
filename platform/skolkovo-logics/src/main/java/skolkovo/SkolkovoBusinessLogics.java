package skolkovo;

import net.sf.jasperreports.engine.JRException;
import platform.server.data.sql.DataAdapter;
import platform.server.logics.BusinessLogics;
import skolkovo.remote.SkolkovoRemoteInterface;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;

public class SkolkovoBusinessLogics extends BusinessLogics<SkolkovoBusinessLogics> implements SkolkovoRemoteInterface {
    public SkolkovoBusinessLogics(DataAdapter adapter, int exportPort) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super(adapter, exportPort);
    }

    @Override
    protected void initGroups() {
        //todo:

    }

    @Override
    protected void initClasses() {
        //todo:

    }

    @Override
    protected void initProperties() {
        //todo:

    }

    @Override
    protected void initTables() {
        //todo:

    }

    @Override
    protected void initIndexes() {
        //todo:

    }

    @Override
    protected void initNavigators() throws JRException, FileNotFoundException {
        //todo:

    }

    @Override
    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        //todo:

    }

    public String[] getProjectNames(int expertId) {
        return new String[] {"aurora", "gannimed", "mustang"};
    }
}
