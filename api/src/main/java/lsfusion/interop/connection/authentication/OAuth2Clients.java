package lsfusion.interop.connection.authentication;

import java.io.Serializable;

public enum OAuth2Clients implements Serializable {
    GOOGLE("google"),
    GITHUB("github");

    public String client;

    OAuth2Clients(String client){
        this.client = client;
    }
}
