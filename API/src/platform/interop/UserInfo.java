package platform.interop;

import java.io.Serializable;

public class UserInfo implements Serializable {

    public String firstName = "";
    public String lastName = "";

    public UserInfo() {

    }

    public UserInfo(String ifirstName, String ilastName) {

        firstName = ifirstName;
        lastName = ilastName;
    }

}
