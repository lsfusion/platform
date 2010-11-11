package platform.interop.action;

import java.io.IOException;

public class UserReloginClientAction extends AbstractClientAction {
    public String login;

    @Override
    public void dispatch(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }

    public UserReloginClientAction(String login){
        this.login = login;
    }
}