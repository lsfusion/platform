package lsfusion.server.logics.property.actions;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.ValueClass;
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
    private LCP userLogin;
    private LCP userPassword;

    private final ClassPropertyInterface customUserInterface;

    public GenerateLoginPasswordActionProperty(AuthenticationLogicsModule lm) {
        super(lm, new ValueClass[]{lm.getClassByName("CustomUser")});

        try {
            this.email = lm.findLCPByCompoundName("Contact.emailContact");
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw new RuntimeException(e);
        }
        this.userLogin = lm.getLCPByName("loginCustomUser");
        this.userPassword = lm.getLCPByName("passwordCustomUser");

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        customUserInterface = i.next();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
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

        if (userLogin.read(context, userObject) == null)
            userLogin.change(login, context, userObject);
        userPassword.change(password, context, userObject);
    }

    @Override
    public ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        return getChangeProps((CalcProperty)userLogin.property, (CalcProperty)userPassword.property);
    }
}
