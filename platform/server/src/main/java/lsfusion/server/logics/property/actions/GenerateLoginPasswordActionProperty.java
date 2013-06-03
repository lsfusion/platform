package lsfusion.server.logics.property.actions;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ServerResourceBundle;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Random;

public class GenerateLoginPasswordActionProperty extends AdminActionProperty {

    private LCP email;
    private LCP userLogin;
    private LCP userPassword;

    private final ClassPropertyInterface customUserInterface;

    public GenerateLoginPasswordActionProperty(LCP email, LCP userLogin, LCP userPassword, ValueClass customUser) {
        super("generateLoginPassword", ServerResourceBundle.getString("logics.property.actions.generate.login.and.password"), new ValueClass[]{customUser});

        this.email = email;
        this.userLogin = userLogin;
        this.userPassword = userPassword;

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
