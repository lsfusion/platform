package lsfusion.interop.remote;

import java.io.Serializable;
import java.util.List;

public class Authentication implements Serializable {
    public String userName;
    public List<String> roles;

    public Authentication(String userName, List<String> roles) {
        this.userName = userName;
        this.roles = roles;
    }
}
