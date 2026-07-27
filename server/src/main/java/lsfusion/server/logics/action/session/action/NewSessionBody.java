package lsfusion.server.logics.action.session.action;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.lambda.EConsumer;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.controller.context.ExecutionEnvironment;
import lsfusion.server.logics.action.controller.stack.ExecutionStack;
import lsfusion.server.logics.action.session.RecursiveBody;
import lsfusion.server.logics.action.session.classes.change.UpdateCurrentClassesSession;
import lsfusion.server.logics.form.interactive.instance.FormEnvironment;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;

// the deferred java NEW SESSION body (see ExecutionContext.newSession) - the recursion needs only the RecursiveBody contract, so there is no Action behind it
public class NewSessionBody implements RecursiveBody {

    private final ImMap<PropertyInterface, ObjectValue> keys;
    private final ImMap<PropertyInterface, PropertyObjectInterfaceInstance> mapObjects;
    private final FormInstance formInstance;
    private final EConsumer<ExecutionContext<PropertyInterface>, Exception> body;

    public <P extends PropertyInterface> NewSessionBody(ImMap<P, ? extends ObjectValue> keys, ImMap<P, PropertyObjectInterfaceInstance> mapObjects, FormInstance formInstance, EConsumer<ExecutionContext<P>, Exception> body) {
        this.keys = (ImMap<PropertyInterface, ObjectValue>) keys;
        this.mapObjects = (ImMap<PropertyInterface, PropertyObjectInterfaceInstance>) mapObjects;
        this.formInstance = formInstance;
        this.body = (EConsumer<ExecutionContext<PropertyInterface>, Exception>) (EConsumer) body;
    }

    // a java body, unlike the lsf one, may throw anything - the exceptions the callers do not handle themselves are propagated as runtime (so that they need no catches of their own)
    public static <P extends PropertyInterface> void run(ExecutionContext<P> context, EConsumer<ExecutionContext<P>, Exception> body) throws SQLException, SQLHandledException {
        try {
            body.accept(context);
        } catch (Exception e) {
            throw ExceptionUtils.propagate(e, SQLException.class, SQLHandledException.class);
        }
    }

    public Action<?> getSingleApplyAction() {
        return null;
    }

    public void execute(ExecutionEnvironment env, ExecutionStack stack) throws SQLException, SQLHandledException {
        run(new ExecutionContext<>(keys, null, env, null, null, FormEnvironment.create(mapObjects, formInstance), stack, true), body); // the context is built as in ExecutionEnvironment.execute
    }

    public RecursiveBody updateCurrentClasses(UpdateCurrentClassesSession session) throws SQLException, SQLHandledException {
        return new NewSessionBody(session.updateCurrentClasses(keys), FormEnvironment.updateCurrentClasses(session, mapObjects), formInstance, body);
    }

    @Override
    public String toString() {
        return "newSession";
    }
}
