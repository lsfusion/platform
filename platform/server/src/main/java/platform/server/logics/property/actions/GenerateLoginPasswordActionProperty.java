package platform.server.logics.property.actions;

import platform.base.BaseUtils;
import platform.interop.action.ClientAction;
import platform.server.classes.ValueClass;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.Property;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.*;

public class GenerateLoginPasswordActionProperty extends ActionProperty {

    private LP email;
    private LP userLogin;
    private LP userPassword;

    private final ClassPropertyInterface customUserInterface;

    public GenerateLoginPasswordActionProperty(LP email, LP userLogin, LP userPassword, ValueClass customUser) {
        super("generateLoginPassword", "Сгенерировать логин и пароль", new ValueClass[]{customUser});

        this.email = email;
        this.userLogin = userLogin;
        this.userPassword = userPassword;

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        customUserInterface = i.next();
    }

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        throw new RuntimeException("no need");
    }

    @Override
    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) throws SQLException {
        DataObject userObject = keys.get(customUserInterface);

        String currentEmail = (String) email.read(session, userObject);

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

        if (userLogin.read(session, userObject) == null)
            userLogin.execute(login, session, userObject);
        userPassword.execute(password, session, userObject);
    }

    @Override
    public Set<Property> getChangeProps() {
        return BaseUtils.toSet(userLogin.property, userPassword.property);
    }
}
