package lsfusion.server.physics.dev.integration.internal.to;

import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.file.FileData;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.ExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.open.stat.ImportAction;
import lsfusion.server.logics.form.stat.struct.imports.hierarchy.json.JSONReader;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.group.Group;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.debug.ActionDelegationType;

import java.io.IOException;
import java.sql.SQLException;

// !!! ONLY ACTIONS CREATED WITH INTERNAL OPERATOR !!!!
public abstract class InternalAction extends ExplicitAction {
    protected ScriptingLogicsModule LM;
    
    protected LP<?> is(ValueClass valueClass) {
        return LM.is(valueClass);
    }

    protected LP<?> object(ValueClass valueClass) {
        return LM.object(valueClass);
    }

    public InternalAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(classes);
        this.LM = LM;
    }

    protected LP<?> findProperty(String name) throws ScriptingErrorLog.SemanticErrorException {
        return LM.findProperty(name);
    }

    protected LP<?>[] findProperties(String... names) throws ScriptingErrorLog.SemanticErrorException {
        LP<?>[] result = new LP[names.length];
        for (int i = 0; i < names.length; i++) {
            result[i] = findProperty(names[i]);
        }
        return result;
    }

    //этот метод нужен для дебаггера, чтобы была общая точка для дебаггинга всех executeInternal
    public void commonExecuteInternalDelegate(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        executeInternal(context);
    }

    protected LA<?> findAction(String name) throws ScriptingErrorLog.SemanticErrorException {
        return LM.findAction(name);
    }

    protected ValueClass findClass(String name) throws ScriptingErrorLog.SemanticErrorException {
        return LM.findClass(name);
    }

    protected Group findGroup(String name) throws ScriptingErrorLog.SemanticErrorException {
        return LM.findGroup(name);
    }

    protected FormEntity findForm(String name) throws ScriptingErrorLog.SemanticErrorException {
        return LM.findForm(name);
    }

    @Override
    public ActionDelegationType getDelegationType(boolean modifyContext) {
        return ActionDelegationType.IN_DELEGATE; // jump to java code
    }

    protected ClassPropertyInterface getParamInterface(int i) {
        return getOrderInterfaces().get(i);
    }

    protected Object getParam(int i, ExecutionContext<ClassPropertyInterface> context) {
        return context.getKeyObject(getParamInterface(i));
    }

    protected ObjectValue getParamValue(int i, ExecutionContext<ClassPropertyInterface> context) {
        return context.getKeyValue(getParamInterface(i));
    }

    public ObjectValue readResult(ExecutionContext<ClassPropertyInterface> context, LA<?> la) throws SQLException, SQLHandledException {
        return getBaseLM().getExportValueProperty().readFirstNotNull(context.getSession(), new Result<>(), la.action);
    }

    public Object readJSONResult(ExecutionContext<ClassPropertyInterface> context, LA<?> la) throws SQLException, SQLHandledException, IOException {
        ObjectValue result = readResult(context, la);
        return readJSON(result, null);
    }

    public static Object readJSON(ObjectValue result, Type valueType) {
        String charset = ExternalUtils.jsonCharset.toString();
        FileData file = readFile(result, valueType, charset);
        if(file != null)
            return JSONReader.readObject(file.getRawFile(), charset);
        return null;
    }

    @Override
    protected boolean isSync() {
        return true;
    }

    @Override
    protected boolean allowNulls() { // does not allow by default
        return false;
    }

    protected boolean hasNoChange() {
        return false;
    }
    @Override
    public boolean hasFlow(ChangeFlowType type, ImSet<Action<?>> recursiveAbstracts) {
        if(type.isSession() && !hasNoChange())
            return true;
        if(type == ChangeFlowType.ANYEFFECT)
            return true;
        return super.hasFlow(type, recursiveAbstracts);
    }
}
