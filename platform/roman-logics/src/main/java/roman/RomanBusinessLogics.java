package roman;

import net.sf.jasperreports.engine.JRException;
import platform.server.auth.User;
import platform.server.data.sql.DataAdapter;
import platform.server.logics.BusinessLogics;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;

@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration", "DuplicateThrows"})
public class RomanBusinessLogics extends BusinessLogics<RomanBusinessLogics> {
    RomanLogicsModule RomanLM;

    public RomanBusinessLogics(DataAdapter adapter, int exportPort) throws IOException, ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException, FileNotFoundException, JRException {
        super(adapter, exportPort);
    }

    @Override
    protected void createModules() {
        super.createModules();
        RomanLM = new RomanLogicsModule(LM, this);
        addLogicsModule(RomanLM);
    }

    @Override
    protected void initAuthentication() throws ClassNotFoundException, SQLException, IllegalAccessException, InstantiationException {
        User admin = addUser("admin", "fusion");
        admin.addSecurityPolicy(permitAllPolicy);
    }


}
