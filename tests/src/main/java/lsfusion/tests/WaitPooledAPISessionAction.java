package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.BusinessLogicsBootstrap;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.session.controller.remote.RemoteSession;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.controller.remote.RemoteLogics;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.authentication.controller.remote.RemoteConnection;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.TimeUnit;

// waits until the pool of the Action API holds a session of the token given : the server returns the session of a call
// to the pool after the call has answered (RemoteLogics.runInNewSession; the pool and a session's token are private,
// reached by reflection). A test calls with the token, changes the user and calls again once the session is back : the
// only one of the token there, it is the session the pool offers the second call
public class WaitPooledAPISessionAction extends InternalAction {

    public WaitPooledAPISessionAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        String token = (String) getParam(0, context);
        RemoteLogics remoteLogics = (RemoteLogics) BusinessLogicsBootstrap.getSpringContextBean("remoteLogics");
        try {
            Field poolField = RemoteLogics.class.getDeclaredField("sessionsPool");
            poolField.setAccessible(true);
            Set<RemoteSession> pool = (Set<RemoteSession>) poolField.get(remoteLogics);
            Field tokenField = RemoteConnection.class.getDeclaredField("token");
            tokenField.setAccessible(true);

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (true) {
                for (RemoteSession session : pool) {
                    if (token.equals(((AuthenticationToken) tokenField.get(session)).string))
                        return;
                }
                if (System.nanoTime() - deadline >= 0)
                    throw new Error("the session of a call with the token did not come back to the pool");
                Thread.sleep(20);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw Throwables.propagate(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw Throwables.propagate(e);
        }
    }
}
