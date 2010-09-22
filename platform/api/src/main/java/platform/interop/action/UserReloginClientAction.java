package platform.interop.action;

import java.io.IOException;

public class UserReloginClientAction extends ClientAction {
    public String login;

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }

    public UserReloginClientAction(String login){
        this.login = login;
    }
}