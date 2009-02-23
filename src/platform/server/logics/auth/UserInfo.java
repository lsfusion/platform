package platform.server.logics.auth;

import java.io.Serializable;

public class UserInfo implements Serializable {

    public String firstName = "";
    public String lastName = "";

    UserInfo() {

    }

    public UserInfo(String ifirstName, String ilastName) {

        firstName = ifirstName;
        lastName = ilastName;
    }

}
