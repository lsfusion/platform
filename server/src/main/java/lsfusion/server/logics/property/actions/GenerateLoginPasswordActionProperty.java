package lsfusion.server.logics.property.actions;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.interop.remote.UserInfo;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.AuthenticationLogicsModule;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Random;

public class GenerateLoginPasswordActionProperty extends ScriptingActionProperty {

    private LCP email;
    private LCP loginCustomUser;
    private LCP sha256PasswordCustomUser;

    private final ClassPropertyInterface customUserInterface;

    public GenerateLoginPasswordActionProperty(AuthenticationLogicsModule lm) throws ScriptingErrorLog.SemanticErrorException {
        super(lm, new ValueClass[]{lm.getClass("CustomUser")});

        try {
            this.email = getLCP("Contact.emailContact");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        }
        this.loginCustomUser = getLCP("loginCustomUser");
        this.sha256PasswordCustomUser = getLCP("sha256PasswordCustomUser");

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        customUserInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        DataObject userObject = context.getDataKeyValue(customUserInterface);

        String currentEmail = (String) email.read(context, userObject);

        String login;
        int indexMail;
        if(currentEmail != null && (indexMail = currentEmail.indexOf("@"))>=0)
            login = currentEmail.substring(0, indexMail);
        else
            login = "login" + userObject.object;

        Random rand = new Random();
        String chars = "0123456789abcdefghijklmnopqrstuvwxyz";
        String password = "";
        for(int i=0;i<8;i++)
            password += chars.charAt(rand.nextInt(chars.length()));

        if (loginCustomUser.read(context, userObject) == null)
            loginCustomUser.change(login, context, userObject);
        String sha256Password = BaseUtils.calculateBase64Hash("SHA-256", password, UserInfo.salt);
        sha256PasswordCustomUser.change(sha256Password, context, userObject);
    }

    @Override
    public ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        return getChangeProps((CalcProperty) loginCustomUser.property, (CalcProperty) sha256PasswordCustomUser.property);
    }
}
