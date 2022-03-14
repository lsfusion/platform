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
    public LP<?> loginCustomUser;
    public LP customUserNormalized;
    public LP logNameCustomUser;
    public LP sha256PasswordCustomUser;
    public LP calculatedHash;
    public LP currentUser;
    public LP currentUserName;

    public LP intersectingLoginsCount;

    public LP currentAuthToken;
    public LP secret;
    public LP resultAuthToken;

    public LP hostnameComputer;
    public LP computerHostname;
    public LP currentComputer;
    public LP hostnameCurrentComputer;

    public LP useLDAP;
    public LP serverLDAP;
    public LP portLDAP;
    public LP baseDNLDAP;
    public LP userDNSuffixLDAP;

    public LP webClientSecret;

    //OAuth2
    public LP oauth2id;
    public LP oauth2ClientId;
    public LP oauth2ClientSecret;
    public LP oauth2ClientAuthenticationMethod;
    public LP oauth2Scope;
    public LP oauth2AuthorizationUri;
    public LP oauth2TokenUri;
    public LP oauth2JwkSetUri;
    public LP oauth2UserInfoUri;
    public LP oauth2UserNameAttributeName;
    public LP oauth2ClientName;

    public LP language;
    public LP country;
    public LP timeZone;
    public LP twoDigitYearStart;
    public LP dateFormat;
    public LP timeFormat;

    public LP clientLanguage;
    public LP clientCountry;
    public LP clientDateFormat;
    public LP clientTimeFormat;

    public LP defaultLanguage;
    public LP defaultCountry;
    public LP defaultTimezone;
    public LP defaultTwoDigitYearStart;
    public LP defaultDateFormat;
    public LP defaultTimeFormat;

    public LP serverLanguage;
    public LP serverCountry;
    public LP serverTimezone;
    public LP serverTwoDigitYearStart;
    public LP serverDateFormat;
    public LP serverTimeFormat;

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

        currentUserName = findProperty("currentUserName[]");

        intersectingLoginsCount = findProperty("intersectingLoginsCount[]");

        // Компьютер
        hostnameComputer = findProperty("hostname[Computer]");
        computerHostname = findProperty("computer[ISTRING[100]]");
        hostnameCurrentComputer = findProperty("hostnameCurrentComputer[]");

        isLockedCustomUser = findProperty("isLocked[CustomUser]");

        loginCustomUser = findProperty("login[CustomUser]");
        customUserNormalized = findProperty("customUserNormalized[ISTRING[100]]");
        logNameCustomUser = findProperty("logName[CustomUser]");

        sha256PasswordCustomUser = findProperty("sha256Password[CustomUser]");
        sha256PasswordCustomUser.setEchoSymbols(true);

        calculatedHash = findProperty("calculatedHash[]");

        secret = findProperty("secret[]");
        resultAuthToken = findProperty("resultAuthToken[]");

        useLDAP = findProperty("useLDAP[]");
        serverLDAP = findProperty("serverLDAP[]");
        portLDAP = findProperty("portLDAP[]");
        baseDNLDAP = findProperty("baseDNLDAP[]");
        userDNSuffixLDAP = findProperty("userDNSuffixLDAP[]");

        webClientSecret = findProperty("webClientSecretKey[]");

        oauth2id = findProperty("id[OAuth2]");
        oauth2ClientId = findProperty("clientId[OAuth2]");
        oauth2ClientSecret = findProperty("clientSecret[OAuth2]");
        oauth2ClientAuthenticationMethod = findProperty("clientAuthenticationMethod[OAuth2]");
        oauth2Scope = findProperty("scope[OAuth2]");
        oauth2AuthorizationUri = findProperty("authorizationUri[OAuth2]");
        oauth2TokenUri = findProperty("tokenUri[OAuth2]");
        oauth2JwkSetUri = findProperty("jwkSetUri[OAuth2]");
        oauth2UserInfoUri = findProperty("userInfoUri[OAuth2]");
        oauth2UserNameAttributeName = findProperty("userNameAttributeName[OAuth2]");
        oauth2ClientName = findProperty("clientName[OAuth2]");

        language = findProperty("language[CustomUser]");
        country = findProperty("country[CustomUser]");
        timeZone = findProperty("timeZone[CustomUser]");
        twoDigitYearStart = findProperty("twoDigitYearStart[CustomUser]");
        dateFormat = findProperty("dateFormat[CustomUser]");
        timeFormat = findProperty("timeFormat[CustomUser]");

        clientCountry = findProperty("clientCountry[CustomUser]");
        clientLanguage = findProperty("clientLanguage[CustomUser]");
        clientDateFormat = findProperty("clientDateFormat[CustomUser]");
        clientTimeFormat = findProperty("clientTimeFormat[CustomUser]");

        defaultLanguage = findProperty("defaultUserLanguage[]");
        defaultCountry = findProperty("defaultUserCountry[]");
        defaultTimezone = findProperty("defaultUserTimezone[]");
        defaultTwoDigitYearStart = findProperty("defaultUserTwoDigitYearStart[]");
        defaultDateFormat = findProperty("defaultUserDateFormat[]");
        defaultTimeFormat = findProperty("defaultUserTimeFormat[]");

        serverLanguage = findProperty("serverLanguage[]");
        serverCountry = findProperty("serverCountry[]");
        serverTimezone = findProperty("serverTimezone[]");
        serverTwoDigitYearStart = findProperty("serverTwoDigitYearStart[]");
        serverDateFormat = findProperty("serverDateFormat[]");
        serverTimeFormat = findProperty("serverTimeFormat[]");

        userFontSize = findProperty("fontSize[CustomUser]");
        colorThemeStaticName = findProperty("colorThemeStaticName[CustomUser]");
        
        deliveredNotificationAction = findAction("deliveredNotificationAction[CustomUser]");
        
        syncUsers = findAction("syncUsers[ISTRING[100], JSONFILE]");
    }

    //todo: replace usages to checkPassword without stack param after upgrading erp to version 5
    public boolean checkPassword(DataSession session, DataObject userObject, String password, ExecutionStack stack) throws SQLException, SQLHandledException {
        return checkPassword(session, userObject, password);
    }

    public boolean checkPassword(DataSession session, DataObject userObject, String password) throws SQLException, SQLHandledException {
        String hashPassword = (String) sha256PasswordCustomUser.read(session, userObject);
        String newHashInput = BaseUtils.calculateBase64Hash("SHA-256", nullTrim(password), UserInfo.salt);
        return hashPassword != null && hashPassword.trim().equals(newHashInput);
    }
}