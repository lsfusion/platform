package lsfusion.server.logics;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.interop.remote.UserInfo;
import lsfusion.server.classes.AbstractCustomClass;
import lsfusion.server.classes.ConcreteCustomClass;
import lsfusion.server.classes.StringClass;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.KeyExpr;
import lsfusion.server.data.query.QueryBuilder;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CurrentComputerFormulaProperty;
import lsfusion.server.logics.property.CurrentUserFormulaProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.DataSession;
import lsfusion.server.session.NoPropertyTableUsage;
import org.antlr.runtime.RecognitionException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

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
    public LCP loginCustomUser;
    public LCP customUserLogin;
    public LCP customUserUpcaseLogin;
    public LCP sha256PasswordCustomUser;
    public LCP calculatedHash;
    public LCP lastActivityCustomUser;
    public LCP lastComputerCustomUser;
    public LCP currentUser;
    public LCP currentUserName;
    public LCP currentUserAllowExcessAllocatedBytes;

    public LCP hostnameComputer;
    public LCP scannerComPortComputer;
    public LCP scannerSingleReadComputer;
    public LCP useDiscountCardReaderComputer;
    public LCP scalesComPortComputer;

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

    public LCP userLanguage;
    public LCP userCountry;
    public LCP userTimeZone;
    public LCP userTwoDigitYearStart;
    
    public LCP clientLanguage;
    public LCP clientCountry;

    public LCP defaultLanguage;
    public LCP defaultCountry;
    public LCP defaultTimeZone;
    public LCP defaultTwoDigitYearStart;
    
    public LCP userFontSize;
    
    public LAP deliveredNotificationAction;

    public LAP generateLoginPassword;

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

        super.initMainLogic();

        firstNameContact = findProperty("firstName[Contact]");
        lastNameContact = findProperty("lastName[Contact]");
        emailContact = findProperty("email[Contact]");
        contactEmail = findProperty("contact[VARSTRING[400]]");

        currentUserName = findProperty("currentUserName[]");
        currentUserAllowExcessAllocatedBytes = findProperty("currentUserAllowExcessAllocatedBytes[]");

        // Компьютер
        hostnameComputer = findProperty("hostname[Computer]");
        scannerComPortComputer = findProperty("scannerComPort[Computer]");
        scannerSingleReadComputer = findProperty("scannerSingleRead[Computer]");
        useDiscountCardReaderComputer = findProperty("useDiscountCardReader[Computer]");
        scalesComPortComputer = findProperty("scalesComPort[Computer]");

        hostnameCurrentComputer = findProperty("hostnameCurrentComputer[]");

        isLockedCustomUser = findProperty("isLocked[CustomUser]");
        isLockedLogin = findProperty("isLockedLogin[STRING[30]]");

        loginCustomUser = findProperty("login[CustomUser]");
        customUserLogin = findProperty("customUser[STRING[30]]");
        customUserUpcaseLogin = findProperty("customUserUpcase[?]");

        sha256PasswordCustomUser = findProperty("sha256Password[CustomUser]");
        sha256PasswordCustomUser.setEchoSymbols(true);

        calculatedHash = findProperty("calculatedHash[]");

        lastActivityCustomUser = findProperty("lastActivity[CustomUser]");
        lastComputerCustomUser = findProperty("lastComputer[CustomUser]");

        minHashLength = findProperty("minHashLength[]");
        useLDAP = findProperty("useLDAP[]");
        serverLDAP = findProperty("serverLDAP[]");
        portLDAP = findProperty("portLDAP[]");
        baseDNLDAP = findProperty("baseDNLDAP[]");
        userDNSuffixLDAP = findProperty("userDNSuffixLDAP[]");

        useBusyDialog = findProperty("useBusyDialog[]");
        useRequestTimeout = findProperty("useRequestTimeout[]");

        userLanguage = findProperty("language[CustomUser]");
        userCountry = findProperty("country[CustomUser]");
        userTimeZone = findProperty("timeZone[CustomUser]");
        userTwoDigitYearStart = findProperty("twoDigitYearStart[CustomUser]");
        
        clientCountry = findProperty("clientCountry[CustomUser]");
        clientLanguage = findProperty("clientLanguage[CustomUser]");

        defaultLanguage = findProperty("defaultUserLanguage[]");
        defaultCountry = findProperty("defaultUserCountry[]");
        defaultTimeZone = findProperty("defaultUserTimeZone[]");
        defaultTwoDigitYearStart = findProperty("defaultUserTwoDigitYearStart[]");
        
        userFontSize = findProperty("fontSize[CustomUser]");
        
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
                    session.applyException(BL, stack);
                } else {
                    authenticated = false;
                }
            }
        }
        return authenticated;
    }
    
    public Set<String> syncUsers(final Set<String> userNames) throws SQLException, SQLHandledException {
        final DataSession session = createSession();

        final String KEY_NAME = "login";
        KeyExpr loginExpr = new KeyExpr(KEY_NAME);
        ImRevMap<String, KeyExpr> imRevMap = MapFact.singletonRev(KEY_NAME, loginExpr);

        QueryBuilder<String, String> query = new QueryBuilder<>(imRevMap);
        query.and(customUserLogin.getExpr(loginExpr).getWhere());
        query.and(isLockedLogin.getExpr(loginExpr).getWhere().not());

        NoPropertyTableUsage<String> table = null;
        Set<String> result = new HashSet<>();
        try {
            if (userNames != null) {
                final StringClass KEY_CLASS = StringClass.get(30);

                table = new NoPropertyTableUsage<>("syncUsers", SetFact.singletonOrder(KEY_NAME), new Type.Getter<String>() {
                    @Override
                    public Type getType(String key) {
                        return KEY_CLASS;
                    }
                });

                ImSet<ImMap<String, DataObject>> keys = SetFact.fromJavaSet(userNames).mapItSetValues(new GetValue<ImMap<String, DataObject>, String>() {
                    @Override
                    public ImMap<String, DataObject> getMapValue(String value) {
                        return MapFact.singleton(KEY_NAME, new DataObject(value, KEY_CLASS));
                    }
                });

                table.writeKeys(session.sql, keys, session.getOwner());
                query.and(table.join(imRevMap).getWhere());
            }

            ImOrderMap<ImMap<String, Object>, ImMap<String, Object>> queryResult = query.execute(session);
            for (ImMap<String, Object> keys : queryResult.keyIt()) {
                result.add(((String) keys.get(KEY_NAME)));
            }
        } finally {
            if (table != null) {
                table.drop(session.sql, session.getOwner());
            }
        }
        return result;
    }
}