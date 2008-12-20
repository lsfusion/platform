package platformlocal;

import java.util.*;
import java.io.Serializable;

public class AuthPolicy {

    Map<String, User> users = new HashMap();

    User addUser(String login, String password) {
        return addUser(login, password, new UserInfo());
    }

    User addUser(String login, String password, UserInfo userInfo) {

        User user = new User(login, password, userInfo);
        users.put(login, user);
        return user;
    }

    User getUser(String login, String password) {

        User user = users.get(login);
        if (user == null) return null;

        if (!password.equals(user.password)) return null;

        return user;
    }

    SecurityPolicy defaultSecurityPolicy = new SecurityPolicy();

    public SecurityPolicy getSecurityPolicy(User user) {

        SecurityPolicy securityPolicy = new SecurityPolicy();
        securityPolicy.override(defaultSecurityPolicy);
        securityPolicy.override(user.getSecurityPolicy());

        return securityPolicy;
    }
}

class UserPolicy {

    List<SecurityPolicy> securityPolicies = new ArrayList();

    public void addSecurityPolicy(SecurityPolicy policy) {
        securityPolicies.add(policy);
    }

    public SecurityPolicy getSecurityPolicy() {
        
        SecurityPolicy resultPolicy = new SecurityPolicy();
        for (SecurityPolicy policy : securityPolicies) {
            resultPolicy.override(policy);
        }

        return resultPolicy;
    }
}

class User extends UserPolicy {

    String login;
    String password;

    UserInfo userInfo;

    public User(String ilogin, String ipassword, UserInfo iuserInfo) {

        login = ilogin;
        password = ipassword;
        userInfo = iuserInfo;
    }

    List<UserGroup> userGroups = new ArrayList();

    public SecurityPolicy getSecurityPolicy() {

        SecurityPolicy resultPolicy = new SecurityPolicy();

        for (UserGroup userGroup : userGroups)
            resultPolicy.override(userGroup.getSecurityPolicy());

        resultPolicy.override(super.getSecurityPolicy());
        return resultPolicy;
    }
}

class UserInfo implements Serializable {

    String firstName = "";
    String lastName = "";

    UserInfo() {

    }

    UserInfo(String ifirstName, String ilastName) {

        firstName = ifirstName;
        lastName = ilastName;
    }

}

class UserGroup extends UserPolicy {

}

class SecurityPolicy {

    ClassSecurityPolicy cls = new ClassSecurityPolicy();
    PropertySecurityPolicy property = new PropertySecurityPolicy();
    NavigatorSecurityPolicy navigator = new NavigatorSecurityPolicy();

    public void override(SecurityPolicy policy) {
        cls.override(policy.cls);
        property.override(policy.property);
        navigator.override(policy.navigator);
    }
}

class ClassSecurityPolicy {

    ViewClassSecurityPolicy view = new ViewClassSecurityPolicy();
    EditClassSecurityPolicy edit = new EditClassSecurityPolicy();

    public void override(ClassSecurityPolicy cls) {
        view.override(cls.view);
        edit.override(cls.edit);
    }
}

class ViewClassSecurityPolicy {

    public void override(ViewClassSecurityPolicy view) {
    }
}

class EditClassSecurityPolicy {

    AddClassSecurityPolicy add = new AddClassSecurityPolicy();
    RemoveClassSecurityPolicy remove = new RemoveClassSecurityPolicy();
    ChangeClassSecurityPolicy change = new ChangeClassSecurityPolicy();

    public void override(EditClassSecurityPolicy edit) {
        add.override(edit.add);
        remove.override(edit.remove);
        change.override(edit.change);
    }
}

class AddClassSecurityPolicy extends AbstractSecurityPolicy<Class> {

}

class RemoveClassSecurityPolicy extends AbstractSecurityPolicy<Class> {

}

class ChangeClassSecurityPolicy extends AbstractSecurityPolicy<Class> {

}

class PropertySecurityPolicy {

    ViewPropertySecurityPolicy view = new ViewPropertySecurityPolicy();
    ChangePropertySecurityPolicy change = new ChangePropertySecurityPolicy();

    public void override(PropertySecurityPolicy policy) {
        view.override(policy.view);
        change.override(policy.change);
    }
}

class ViewPropertySecurityPolicy extends AbstractSecurityPolicy<Property> {

}

class ChangePropertySecurityPolicy extends AbstractSecurityPolicy<Property> {

}

class NavigatorSecurityPolicy extends AbstractSecurityPolicy<NavigatorElement> {
    
}

class AbstractSecurityPolicy<T> {

    private Set<T> permitted = new HashSet();
    private Set<T> denied = new HashSet();

    boolean defaultPermission = true;

    public void permit(T obj) { permitted.add(obj); }
    public void deny(T obj) { denied.add(obj); }

    public void permit(Collection<? extends T> colObj) { permitted.addAll(colObj); }
    public void deny(Collection<? extends T> colObj) { denied.addAll(colObj); }

    protected void override(AbstractSecurityPolicy<T> policy) {

        for (T obj : policy.denied) {
            permitted.remove(obj);
            denied.add(obj);
        }

        for (T obj : policy.permitted) {
            denied.remove(obj);
            permitted.add(obj);
        }
    }

    public boolean checkPermission(T obj) {

        if (permitted.contains(obj)) return true;
        if (denied.contains(obj)) return false;
        return defaultPermission;
    }
}

