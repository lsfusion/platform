package lsfusion.server.logics;

import lsfusion.base.BaseUtils;
import lsfusion.server.logics.authentication.UserInfo;
import lsfusion.server.classes.AbstractCustomClass;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CurrentAuthTokenFormulaProperty;
import lsfusion.server.logics.property.CurrentComputerFormulaProperty;
import lsfusion.server.logics.property.CurrentUserFormulaProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

import static lsfusion.base.BaseUtils.nullTrim;

public class AuthenticationLogicsModule extends ScriptingLogicsModule{
    public ConcreteCustomClass computer;
    public AbstractCustomClass user;
    public ConcreteCustomClass systemUser;
    public ConcreteCustomClass customUser;

    public LCP firstNameContact;
    public LCP lastNameContact;
    public LCP emailContact;
    public LCP contactEmail;

    public LCP isLockedCustomUser;
    public LCP isLockedLogin;
    public LCP<?> loginCustomUser;
    public LCP customUserLogin;
    public LCP customUserUpcaseLogin;
    public LCP sha256PasswordCustomUser;
    public LCP calculatedHash;
    public LCP lastActivityCustomUser;
    public LCP lastComputerCustomUser;
    public LCP currentUser;
    public LCP currentUserName;
    public LCP nameContact;
    public LCP currentUserAllowExcessAllocatedBytes;
    public LCP allowExcessAllocatedBytes;

    public LCP currentAuthToken;
    public LCP secret;

    public LCP hostnameComputer;
    public LCP computerHostname;
    public LCP currentComputer;
    public LCP hostnameCurrentComputer;

    public LCP minHashLength;
    public LCP useLDAP;
    public LCP serverLDAP;
    public LCP portLDAP;
    public LCP baseDNLDAP;
    public LCP userDNSuffixLDAP;

    public LCP useBusyDialog;
    public LCP useRequestTimeout;

    public LCP language;
    public LCP country;
    public LCP timeZone;
    public LCP twoDigitYearStart;
    
    public LCP clientLanguage;
    public LCP clientCountry;

    public LCP defaultLanguage;
    public LCP defaultCountry;
    public LCP defaultTimeZone;
    public LCP defaultTwoDigitYearStart;
    
    public LCP userFontSize;
    
    public LAP deliveredNotificationAction;
    
    public LAP<?> syncUsers;

    public AuthenticationLogicsModule(BusinessLogics BL, BaseLogicsModule baseLM) throws IOException {
        super(AuthenticationLogicsModule.class.getResourceAsStream("/system/Authentication.lsf"), "/system/Authentication.lsf", baseLM, BL);
    }

    @Override
    public void initMetaAndClasses() throws RecognitionException {
        super.initMetaAndClasses();

        computer = (ConcreteCustomClass) findClass("Computer");
        user = (AbstractCustomClass) findClass("User");
        systemUser = (ConcreteCustomClass) findClass("SystemUser");
        customUser = (ConcreteCustomClass) findClass("CustomUser");
    }

    @Override
    public void initMainLogic() throws RecognitionException {
        // Текущий пользователь
        currentUser = addProperty(null, new LCP<>(new CurrentUserFormulaProperty(user)));
        makePropertyPublic(currentUser, "currentUser", new ArrayList<ResolveClassSet>());
        currentComputer = addProperty(null, new LCP<>(new CurrentComputerFormulaProperty(computer)));
        makePropertyPublic(currentComputer, "currentComputer", new ArrayList<ResolveClassSet>());
        currentAuthToken = addProperty(null, new LCP<>(new CurrentAuthTokenFormulaProperty()));
        makePropertyPublic(currentAuthToken, "currentAuthToken", new ArrayList<ResolveClassSet>());

        super.initMainLogic();

        firstNameContact = findProperty("firstName[Contact]");
        lastNameContact = findProperty("lastName[Contact]");
        emailContact = findProperty("email[Contact]");
        contactEmail = findProperty("contact[VARSTRING[400]]");

        nameContact = findProperty("name[Contact]");
        currentUserName = findProperty("currentUserName[]");
        allowExcessAllocatedBytes = findProperty("allowExcessAllocatedBytes[CustomUser]");
        currentUserAllowExcessAllocatedBytes = findProperty("currentUserAllowExcessAllocatedBytes[]");

        // Компьютер
        hostnameComputer = findProperty("hostname[Computer]");
        computerHostname = findProperty("computer[VARISTRING[100]]");
        hostnameCurrentComputer = findProperty("hostnameCurrentComputer[]");

        isLockedCustomUser = findProperty("isLocked[CustomUser]");
        isLockedLogin = findProperty("isLockedLogin[STRING[100]]");

        loginCustomUser = findProperty("login[CustomUser]");
        customUserLogin = findProperty("customUser[STRING[100]]");
        customUserUpcaseLogin = findProperty("customUserUpcase[?]");

        sha256PasswordCustomUser = findProperty("sha256Password[CustomUser]");
        sha256PasswordCustomUser.setEchoSymbols(true);

        calculatedHash = findProperty("calculatedHash[]");

        lastActivityCustomUser = findProperty("lastActivity[CustomUser]");
        lastComputerCustomUser = findProperty("lastComputer[CustomUser]");

        secret = findProperty("secret[]");

        minHashLength = findProperty("minHashLength[]");
        useLDAP = findProperty("useLDAP[]");
        serverLDAP = findProperty("serverLDAP[]");
        portLDAP = findProperty("portLDAP[]");
        baseDNLDAP = findProperty("baseDNLDAP[]");
        userDNSuffixLDAP = findProperty("userDNSuffixLDAP[]");

        useBusyDialog = findProperty("useBusyDialog[]");
        useRequestTimeout = findProperty("useRequestTimeout[]");

        language = findProperty("language[CustomUser]");
        country = findProperty("country[CustomUser]");
        timeZone = findProperty("timeZone[CustomUser]");
        twoDigitYearStart = findProperty("twoDigitYearStart[CustomUser]");
        
        clientCountry = findProperty("clientCountry[CustomUser]");
        clientLanguage = findProperty("clientLanguage[CustomUser]");

        defaultLanguage = findProperty("defaultUserLanguage[]");
        defaultCountry = findProperty("defaultUserCountry[]");
        defaultTimeZone = findProperty("defaultUserTimeZone[]");
        defaultTwoDigitYearStart = findProperty("defaultUserTwoDigitYearStart[]");
        
        userFontSize = findProperty("fontSize[CustomUser]");
        
        deliveredNotificationAction = findAction("deliveredNotificationAction[CustomUser]");
        
        syncUsers = findAction("syncUsers[VARISTRING[100], JSONFILE]");
    }
    
    public boolean checkPassword(DataSession session, DataObject userObject, String password, ExecutionStack stack) throws SQLException, SQLHandledException {
        boolean authenticated = true;
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
                session.applyException(BL, stack);
            } else {
                authenticated = false;
            }
        }
        return authenticated;
    }
}