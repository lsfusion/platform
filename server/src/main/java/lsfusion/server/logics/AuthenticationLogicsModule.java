package lsfusion.server.logics;

import lsfusion.base.BaseUtils;
import lsfusion.interop.remote.UserInfo;
import lsfusion.server.classes.AbstractCustomClass;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CurrentComputerFormulaProperty;
import lsfusion.server.logics.property.CurrentUserFormulaProperty;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import static lsfusion.base.BaseUtils.nullTrim;

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

    public LCP hostnameComputer;
    public LCP scannerComPortComputer;
    public LCP scannerSingleReadComputer;
    public LCP useDiscountCardReaderComputer;

    public LCP currentComputer;
    public LCP hostnameCurrentComputer;

    public LCP minHashLength;
    public LCP useLDAP;
    public LCP serverLDAP;
    public LCP portLDAP;
    public LCP baseDNLDAP;
    public LCP userDNSuffixLDAP;

    public LCP useBusyDialog;

    public LAP deliveredNotificationAction;

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
        currentUser = addProperty(null, new LCP<>(new CurrentUserFormulaProperty(user)));
        makePropertyPublic(currentUser, "currentUser", new ArrayList<ResolveClassSet>());
        currentComputer = addProperty(null, new LCP<>(new CurrentComputerFormulaProperty(computer)));
        makePropertyPublic(currentComputer, "currentComputer", new ArrayList<ResolveClassSet>());

        super.initProperties();

        currentUserName = findProperty("currentUserName[]");

        // Компьютер
        hostnameComputer = findProperty("hostname[Computer]");
        scannerComPortComputer = findProperty("scannerComPort[Computer]");
        scannerSingleReadComputer = findProperty("scannerSingleRead[Computer]");
        useDiscountCardReaderComputer = findProperty("useDiscountCardReader[Computer]");

        hostnameCurrentComputer = findProperty("hostnameCurrentComputer[]");

        isLockedCustomUser = findProperty("isLocked[CustomUser]");

        loginCustomUser = findProperty("login[CustomUser]");
        customUserLogin = findProperty("customUser[STRING[30]]");
        customUserUpcaseLogin = findProperty("customUserUpcase[?]");

        sha256PasswordCustomUser = findProperty("sha256Password[CustomUser]");
        sha256PasswordCustomUser.setEchoSymbols(true);

        calculatedHash = findProperty("calculatedHash[]");

        lastActivityCustomUser = findProperty("lastActivity[CustomUser]");
        ignorePrintTypeCustomUser = findProperty("ignorePrintTypeCustom[User]");

        minHashLength = findProperty("minHashLength[]");
        useLDAP = findProperty("useLDAP[]");
        serverLDAP = findProperty("serverLDAP[]");
        portLDAP =  findProperty("portLDAP[]");
        baseDNLDAP =  findProperty("baseDNLDAP[]");
        userDNSuffixLDAP =  findProperty("userDNSuffixLDAP[]");

        useBusyDialog = findProperty("useBusyDialog[]");

        deliveredNotificationAction = findAction("deliveredNotificationAction[CustomUser]");

        generateLoginPassword = findAction("generateLoginPassword[CustomUser]");


    }

    public boolean checkPassword(DataObject userObject, String password, ExecutionStack stack) throws SQLException, SQLHandledException {
        boolean authenticated = true;
        try(DataSession session = createSession()) {
            String hashPassword = (String) sha256PasswordCustomUser.read(session, userObject);
            String newHashInput = BaseUtils.calculateBase64Hash("SHA-256", nullTrim(password), UserInfo.salt);
            if (hashPassword == null || !hashPassword.trim().equals(newHashInput)) {
                //TODO: убрать, когда будем считать, что хэши у всех паролей уже перебиты
                Integer minHashLengthValue = (Integer) minHashLength.read(session);
                String oldHashInput = BaseUtils.calculateBase64HashOld("SHA-256", nullTrim(password), UserInfo.salt);
                if (minHashLengthValue == null)
                    minHashLengthValue = oldHashInput.length();
                //если совпали первые n символов, считаем пароль правильным и сохраняем новый хэш в базу
                if (hashPassword != null &&
                        hashPassword.trim().substring(0, Math.min(hashPassword.trim().length(), minHashLengthValue)).equals(oldHashInput.substring(0, Math.min(oldHashInput.length(), minHashLengthValue)))) {
                    sha256PasswordCustomUser.change(newHashInput, session, userObject);
                    session.apply(BL, stack);
                } else {
                    authenticated = false;
                }
            }
        }
        return authenticated;
    }
}