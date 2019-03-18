package lsfusion.client.authentication;

import org.apache.commons.codec.binary.Base64;

public class UserInfo {
    public String name;
    public boolean savePassword;
    public String password;

    public UserInfo () {
        this(null, false, "");
    }

    public UserInfo (String name, boolean savePassword, String password) {
        this.name = name;
        this.savePassword = savePassword;
        this.password = password;
    }

    @Override
    public String toString() {
        String string = this.name + "\t" + savePassword;
        if (savePassword) {
            string += "\t" + Base64.encodeBase64URLSafeString(password.getBytes()) ;
        }
        return string;
    }

    public UserInfo copy() {
        return new UserInfo(name, savePassword, password);
    }
}