package lsfusion.server.logics;

import lsfusion.server.classes.AbstractCustomClass;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CurrentComputerFormulaProperty;
import lsfusion.server.logics.property.CurrentUserFormulaProperty;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;
import java.util.ArrayList;


public class AuthenticationLogicsModule extends ScriptingLogicsModule{
    BusinessLogics BL;

    public ConcreteCustomClass computer;
    public AbstractCustomClass user;
    public ConcreteCustomClass systemUser;
    public ConcreteCustomClass customUser;

    public LCP isLockedCustomUser;
    public LCP loginCustomUser;
    public LCP customUserLogin;
    public LCP customUserUpcaseLogin;
    public LCP sha256PasswordCustomUser;
    public LCP calculatedHash;
    public LCP lastActivityCustomUser;
    public LCP ignorePrintTypeCustomUser;
    public LCP currentUser;
    public LCP currentUserName;
    public LCP needRestartCustomUser;
    public LCP needShutdownCustomUser;

    public LCP hostnameComputer;
    public LCP scannerComPortComputer;
    public LCP scannerSingleReadComputer;
    public LCP useDiscountCardReaderComputer;

    public LCP currentComputer;
    public LCP hostnameCurrentComputer;

    public LCP useLDAP;
    public LCP serverLDAP;
    public LCP portLDAP;
    public LCP baseDNLDAP;
    public LCP userDNSuffixLDAP;

    public LAP generateLoginPassword;

    public AuthenticationLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(AuthenticationLogicsModule.class.getResourceAsStream("/lsfusion/system/Authentication.lsf"), "/lsfusion/system/Authentication.lsf", baseLM, BL);
        this.BL = BL;
        setBaseLogicsModule(baseLM);
    }

    @Override
    public void initClasses() throws RecognitionException {
        super.initClasses();

        computer = (ConcreteCustomClass) findClass("Computer");
        user = (AbstractCustomClass) findClass("User");
        systemUser = (ConcreteCustomClass) findClass("SystemUser");
        customUser = (ConcreteCustomClass) findClass("CustomUser");
    }

    @Override
    public void initProperties() throws RecognitionException {
        // Текущий пользователь
        currentUser = addProperty(null, new LCP<PropertyInterface>(new CurrentUserFormulaProperty(user)));
        makePropertyPublic(currentUser, "currentUser", new ArrayList<ResolveClassSet>());
        currentComputer = addProperty(null, new LCP<PropertyInterface>(new CurrentComputerFormulaProperty(computer)));
        makePropertyPublic(currentComputer, "currentComputer", new ArrayList<ResolveClassSet>());

        super.initProperties();

        currentUserName = findProperty("currentUserName");

        // Компьютер
        hostnameComputer = findProperty("hostnameComputer");
        scannerComPortComputer = findProperty("scannerComPortComputer");
        scannerSingleReadComputer = findProperty("scannerSingleReadComputer");
        useDiscountCardReaderComputer = findProperty("useDiscountCardReaderComputer");

        hostnameCurrentComputer = findProperty("hostnameCurrentComputer");

        isLockedCustomUser = findProperty("isLockedCustomUser");

        loginCustomUser = findProperty("loginCustomUser");
        customUserLogin = findProperty("customUserLogin");
        customUserUpcaseLogin = findProperty("customUserUpcaseLogin");

        sha256PasswordCustomUser = findProperty("sha256PasswordCustomUser");
        sha256PasswordCustomUser.setEchoSymbols(true);

        calculatedHash = findProperty("calculatedHash");

        lastActivityCustomUser = findProperty("lastActivityCustomUser");
        ignorePrintTypeCustomUser = findProperty("ignorePrintTypeCustomUser");

        useLDAP = findProperty("useLDAP");
        serverLDAP = findProperty("serverLDAP");
        portLDAP =  findProperty("portLDAP");
        baseDNLDAP =  findProperty("baseDNLDAP");
        userDNSuffixLDAP =  findProperty("userDNSuffixLDAP");

        generateLoginPassword = findAction("generateLoginPassword");

        needRestartCustomUser = findProperty("needRestartCustomUser");
        needShutdownCustomUser = findProperty("needShutdownCustomUser");


    }
}
