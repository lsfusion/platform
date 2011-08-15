package platform.server.logics.property.actions;

import platform.base.BaseUtils;
import platform.server.classes.ValueClass;
import platform.server.logics.DataObject;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.Property;

import java.sql.SQLException;
import java.util.*;

public class GenerateLoginPasswordActionProperty extends ActionProperty {

    private LP email;
    private LP userLogin;
    private LP userPassword;

    private final ClassPropertyInterface customUserInterface;

    public GenerateLoginPasswordActionProperty(LP email, LP userLogin, LP userPassword, ValueClass customUser) {
        super("generateLoginPassword", ServerResourceBundle.getString("logics.property.actions.generate.login.and.password"), new ValueClass[]{customUser});

        this.email = email;
        this.userLogin = userLogin;
        this.userPassword = userPassword;

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        customUserInterface = i.next();
    }

    public void execute(ExecutionContext context) throws SQLException {
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
            userLogin.execute(login, context, userObject);
        userPassword.execute(password, context, userObject);
    }

    @Override
    public Set<Property> getChangeProps() {
        return BaseUtils.toSet(userLogin.property, userPassword.property);
    }
}
