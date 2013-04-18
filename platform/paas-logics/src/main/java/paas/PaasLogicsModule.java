package paas;

import org.antlr.runtime.RecognitionException;
import platform.server.classes.ConcreteCustomClass;
import platform.server.logics.linear.LAP;
import platform.server.logics.linear.LCP;
import platform.server.logics.scripted.ScriptingLogicsModule;

import java.io.IOException;

public class PaasLogicsModule extends ScriptingLogicsModule {

    public ConcreteCustomClass project;
    public ConcreteCustomClass module;
    public ConcreteCustomClass configuration;
    public ConcreteCustomClass database;
    public ConcreteCustomClass status;

    public LCP projectDescription;
    public LCP projectOwnerLogin;
    public LCP projectOwner;

    public LCP moduleInProject;
    public LCP moduleSource;

    public LCP configurationProject;
    public LCP configurationDatabase;
    public LCP configurationDatabaseName;
    public LCP configurationPort;
    public LCP configurationExportName;
    public LCP configurationStatus;
    public LAP configurationStop;

    public LCP databaseConfiguration;

    public PaasLogicsModule(PaasBusinessLogics paas) throws IOException {
        super(PaasLogicsModule.class.getResourceAsStream("/scripts/Paas.lsf"), paas.LM, paas);
        setBaseLogicsModule(paas.LM);
    }

    @Override
    public void initClasses() throws RecognitionException {
        super.initClasses();

        project = (ConcreteCustomClass) getClassByName("Project");
        module = (ConcreteCustomClass) getClassByName("Module");
        configuration = (ConcreteCustomClass) getClassByName("Configuration");
        database = (ConcreteCustomClass) getClassByName("Database");
        status = (ConcreteCustomClass) getClassByName("Status");
    }

    @Override
    public void initProperties() throws RecognitionException {
        super.initProperties();

        projectDescription = getLCPByName("projectDescription");
        projectOwner = getLCPByName("projectOwner");
        projectOwnerLogin = getLCPByName("projectOwnerLogin");

        moduleInProject = getLCPByName("moduleInProject");
        moduleSource = getLCPByName("moduleSource");

        configurationProject = getLCPByName("configurationProject");
        configurationDatabase = getLCPByName("configurationDatabase");
        configurationDatabaseName = getLCPByName("configurationDatabaseName");
        configurationPort = getLCPByName("configurationPort");
        configurationExportName = getLCPByName("configurationExportName");
        configurationStatus = getLCPByName("configurationStatus");

        databaseConfiguration = getLCPByName("databaseConfiguration");

        configurationStop = getLAPByName("configurationStop");
    }
}
