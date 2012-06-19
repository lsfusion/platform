package platform.interop.action;

import java.io.IOException;

public class UserReloginClientAction extends ExecuteClientAction {
    public String login;

    public UserReloginClientAction(String login){
        this.login = login;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}