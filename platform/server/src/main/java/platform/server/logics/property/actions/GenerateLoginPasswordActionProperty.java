package platform.server.logics.property.actions;

import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.sql.SQLException;
import java.util.*;

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
        DataObject userObject = context.getKeyValue(customUserInterface);

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
    public PropsNewSession aspectChangeExtProps() {
        return getChangeProps((CalcProperty)userLogin.property, (CalcProperty)userPassword.property);
    }
}
