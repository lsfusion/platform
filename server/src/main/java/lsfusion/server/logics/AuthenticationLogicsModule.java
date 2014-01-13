package lsfusion.server.logics;

import lsfusion.server.classes.AbstractCustomClass;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CurrentComputerFormulaProperty;
import lsfusion.server.logics.property.CurrentUserFormulaProperty;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;


public class AuthenticationLogicsModule extends ScriptingLogicsModule{
    BusinessLogics BL;

    public ConcreteCustomClass computer;
    public AbstractCustomClass user;
    public ConcreteCustomClass systemUser;
    public ConcreteCustomClass customUser;

    public LCP isLockedCustomUser;
    public LCP loginCustomUser;
    public LCP customUserLogin;
    public LCP sha256PasswordCustomUser;
    public LCP calculatedHash;
    public LCP currentUser;
    public LCP currentUserName;

    public LCP hostnameComputer;
    public LCP scannerComPortComputer;
    public LCP scannerSingleReadComputer;
    
    public LCP currentComputer;
    public LCP hostnameCurrentComputer;

    public LCP useLDAP;
    public LCP serverLDAP;
    public LCP portLDAP;

    public LAP generateLoginPassword;

    public AuthenticationLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(AuthenticationLogicsModule.class.getResourceAsStream("/lsfusion/system/Authentication.lsf"), "/lsfusion/system/Authentication.lsf", baseLM, BL);
        this.BL = BL;
        setBaseLogicsModule(baseLM);
    }

    @Override
    public void initClasses() throws RecognitionException {
        super.initClasses();

        computer = (ConcreteCustomClass) getClassByName("Computer");
        user = (AbstractCustomClass) getClassByName("User");
        systemUser = (ConcreteCustomClass) getClassByName("SystemUser");
        customUser = (ConcreteCustomClass) getClassByName("CustomUser");
    }

    @Override
    public void initProperties() throws RecognitionException {
        // Текущий пользователь
        currentUser = addProperty(null, new LCP<PropertyInterface>(new CurrentUserFormulaProperty("currentUser", user)));
        currentComputer = addProperty(null, new LCP<PropertyInterface>(new CurrentComputerFormulaProperty("currentComputer", computer)));

        super.initProperties();

        currentUserName = getLCPByOldName("currentUserName");

        // Компьютер
        hostnameComputer = getLCPByOldName("hostnameComputer");
        scannerComPortComputer = getLCPByOldName("scannerComPortComputer");
        scannerSingleReadComputer = getLCPByOldName("scannerSingleReadComputer");

        hostnameCurrentComputer = getLCPByOldName("hostnameCurrentComputer");

        isLockedCustomUser = getLCPByOldName("isLockedCustomUser");

        loginCustomUser = getLCPByOldName("loginCustomUser");
        customUserLogin = getLCPByOldName("customUserLogin");

        sha256PasswordCustomUser = getLCPByOldName("sha256PasswordCustomUser");
        sha256PasswordCustomUser.setEchoSymbols(true);

        calculatedHash = getLCPByOldName("calculatedHash");

        useLDAP = getLCPByOldName("useLDAP");
        serverLDAP = getLCPByOldName("serverLDAP");
        portLDAP =  getLCPByOldName("portLDAP");

        generateLoginPassword = getLAPByOldName("generateLoginPassword");


    }
}
