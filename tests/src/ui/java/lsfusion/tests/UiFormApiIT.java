package lsfusion.tests;

import lsfusion.base.BaseUtils;
import lsfusion.base.remote.RMIUtils;
import lsfusion.interop.action.FormClientAction;
import lsfusion.interop.base.exception.RemoteInternalException;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.connection.ComputerInfo;
import lsfusion.interop.connection.ConnectionInfo;
import lsfusion.interop.connection.UserInfo;
import lsfusion.interop.form.remote.RemoteFormInterface;
import lsfusion.interop.logics.remote.RemoteLogicsLoaderInterface;
import lsfusion.interop.navigator.NavigatorInfo;
import lsfusion.interop.navigator.remote.RemoteNavigatorInterface;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.SessionInfo;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

// the Form API - /form of the web server - is deprecated and closed : it opens no form, by its name or by its code, and
// the application server neither opens nor changes one for a client that asks it over rmi
public class UiFormApiIT extends UiTest {

    // a request from the page, in the session of its client
    private String formApi(String request) {
        return (String) page.evaluate("request => fetch('form', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: request })"
                + " .then(response => response.text().then(text => response.status + ' ' + text))", request);
    }

    @Test
    public void theWebServerOpensNoForm() {
        String refused = "403 The Form API is disabled";
        assertEquals(refused, formApi("{\"action\":\"create\",\"form\":\"uiFormApiByName\",\"data\":{\"name\":\"UiFormApi.uiFormApiForm\"}}"));
        assertEquals(refused, formApi("{\"action\":\"create\",\"form\":\"uiFormApiByCode\",\"data\":{\"script\":\"OBJECTS i = INTEGER PANEL\"}}"));
    }

    // as the web server did, for a client of its own
    @Test
    public void theApplicationServerOpensAndChangesNoForm() throws Exception {
        NavigatorInfo info = new NavigatorInfo(new SessionInfo(new ConnectionInfo(new ComputerInfo("localhost", "127.0.0.1"), UserInfo.NULL),
                new ExternalRequest(new ExternalRequest.Param[0])), null, null, null, null, null, null, null, null, null,
                BaseUtils.getPlatformVersion(), BaseUtils.getApiVersion());
        RemoteNavigatorInterface navigator = RMIUtils.<RemoteLogicsLoaderInterface>rmiLookup("localhost", rmiPort(), "default", "RemoteLogicsLoader")
                .getLogics().createNavigator(AuthenticationToken.ANONYMOUS, info);
        try {
            try {
                navigator.createFormExternal("{\"name\":\"UiFormApi.uiFormApiForm\"}");
                fail("the form was opened");
            } catch (RemoteInternalException e) {
                assertEquals(e.getMessage(), Integer.valueOf(403), e.status);
            }

            // the form the navigator opens for its client, at the first request of the navigator and of the form
            RemoteFormInterface form = Arrays.stream(navigator.executeNavigatorAction(0, -1, "UiFormApi.uiFormApiForm", 1, null).actions)
                    .filter(action -> action instanceof FormClientAction).map(action -> ((FormClientAction) action).remoteForm).findFirst()
                    .orElseThrow(() -> new AssertionError("the navigator opened no form"));
            try {
                form.changeExternal(0, -1, "{}");
                fail("the form was changed");
            } catch (RemoteInternalException e) {
                assertEquals(e.getMessage(), Integer.valueOf(403), e.status);
            }
        } finally {
            navigator.close();
        }
    }
}
