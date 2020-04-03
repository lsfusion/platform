package lsfusion.server.physics.admin.authentication;

import lsfusion.base.BaseUtils;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.user.AbstractCustomClass;
import lsfusion.server.logics.classes.user.ConcreteCustomClass;
import lsfusion.server.physics.admin.authentication.property.CurrentAuthTokenProperty;
import lsfusion.server.physics.admin.authentication.property.CurrentComputerProperty;
import lsfusion.server.physics.admin.authentication.property.CurrentUserProperty;
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

    public LP firstNameContact;
    public LP lastNameContact;
    public LP emailContact;
    public LP contactEmail;

    public LP isLockedCustomUser;
    public LP isLockedLogin;
    public LP<?> loginCustomUser;
    public LP customUserLogin;
    public LP customUserUpcaseLogin;
    public LP sha256PasswordCustomUser;
    public LP calculatedHash;
    public LP lastActivityCustomUser;
    public LP lastComputerCustomUser;
    public LP currentUser;
    public LP currentUserName;
    public LP nameContact;
    public LP currentUserAllowExcessAllocatedBytes;
    public LP allowExcessAllocatedBytes;

    public LP currentAuthToken;
    public LP secret;

    public LP hostnameComputer;
    public LP computerHostname;
    public LP currentComputer;
    public LP hostnameCurrentComputer;

    public LP minHashLength;
    public LP useLDAP;
    public LP serverLDAP;
    public LP portLDAP;
    public LP baseDNLDAP;
    public LP userDNSuffixLDAP;

    public LP useBusyDialog;
    public LP useRequestTimeout;
    public LP devMode;

    public LP language;
    public LP country;
    public LP timeZone;
    public LP twoDigitYearStart;
    
    public LP clientLanguage;
    public LP clientCountry;

    public LP defaultLanguage;
    public LP defaultCountry;
    public LP defaultTimezone;
    public LP defaultTwoDigitYearStart;

    public LP serverLanguage;
    public LP serverCountry;
    public LP serverTimezone;
    public LP serverTwoDigitYearStart;

    public LP userFontSize;
    
    public LP colorThemeStaticName;
    
    public LA deliveredNotificationAction;
    
    public LA<?> syncUsers;

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
        currentUser = addProperty(null, new LP<>(new CurrentUserProperty(user)));
        makePropertyPublic(currentUser, "currentUser", new ArrayList<>());
        currentComputer = addProperty(null, new LP<>(new CurrentComputerProperty(computer)));
        makePropertyPublic(currentComputer, "currentComputer", new ArrayList<>());
        currentAuthToken = addProperty(null, new LP<>(new CurrentAuthTokenProperty()));
        makePropertyPublic(currentAuthToken, "currentAuthToken", new ArrayList<>());

        super.initMainLogic();

        firstNameContact = findProperty("firstName[Contact]");
        lastNameContact = findProperty("lastName[Contact]");
        emailContact = findProperty("email[Contact]");
        contactEmail = findProperty("contact[STRING[400]]");

        nameContact = findProperty("name[Contact]");
        currentUserName = findProperty("currentUserName[]");
        allowExcessAllocatedBytes = findProperty("allowExcessAllocatedBytes[CustomUser]");
        currentUserAllowExcessAllocatedBytes = findProperty("currentUserAllowExcessAllocatedBytes[]");

        // Компьютер
        hostnameComputer = findProperty("hostname[Computer]");
        computerHostname = findProperty("computer[ISTRING[100]]");
        hostnameCurrentComputer = findProperty("hostnameCurrentComputer[]");

        isLockedCustomUser = findProperty("isLocked[CustomUser]");
        isLockedLogin = findProperty("isLockedLogin[BPSTRING[100]]");

        loginCustomUser = findProperty("login[CustomUser]");
        customUserLogin = findProperty("customUser[BPSTRING[100]]");
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
        devMode = findProperty("devMode[]");

        language = findProperty("language[CustomUser]");
        country = findProperty("country[CustomUser]");
        timeZone = findProperty("timeZone[CustomUser]");
        twoDigitYearStart = findProperty("twoDigitYearStart[CustomUser]");
        
        clientCountry = findProperty("clientCountry[CustomUser]");
        clientLanguage = findProperty("clientLanguage[CustomUser]");

        defaultLanguage = findProperty("defaultUserLanguage[]");
        defaultCountry = findProperty("defaultUserCountry[]");
        defaultTimezone = findProperty("defaultUserTimezone[]");
        defaultTwoDigitYearStart = findProperty("defaultUserTwoDigitYearStart[]");

        serverLanguage = findProperty("serverLanguage[]");
        serverCountry = findProperty("serverCountry[]");
        serverTimezone = findProperty("serverTimezone[]");
        serverTwoDigitYearStart = findProperty("serverTwoDigitYearStart[]");
        
        userFontSize = findProperty("fontSize[CustomUser]");
        colorThemeStaticName = findProperty("colorThemeStaticName[CustomUser]");
        
        deliveredNotificationAction = findAction("deliveredNotificationAction[CustomUser]");
        
        syncUsers = findAction("syncUsers[ISTRING[100], JSONFILE]");
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
        return true;
    }
}