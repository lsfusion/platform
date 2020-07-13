package lsfusion.interop.connection.authentication;

import java.io.Serializable;

public enum OAuth2Clients implements Serializable {
    GOOGLE("Google"),
    GITHUB("Github");

    public String client;

    OAuth2Clients(String client){
        this.client = client;
    }
}
