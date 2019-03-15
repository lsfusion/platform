package lsfusion.server.physics.admin.service.action;

import com.google.common.base.Throwables;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.base.controller.thread.ThreadLocalContext;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.ScriptingAction;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Iterator;

import static org.apache.commons.lang3.StringUtils.trimToNull;

public class UpdateSettingActionProperty extends ScriptingAction {
    private final ClassPropertyInterface settingInterface;
    private final ClassPropertyInterface userRoleInterface;
    private final ClassPropertyInterface forceCloneInterface; // optimization

    public UpdateSettingActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        settingInterface = i.next();
        userRoleInterface = i.next();
        forceCloneInterface = i.next();
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        try {

            ObjectValue settingObject = context.getKeyValue(settingInterface);
            ObjectValue userRoleObject = context.getKeyValue(userRoleInterface);
            ObjectValue forceCloneObject = context.getKeyValue(forceCloneInterface);

            Settings settings = ThreadLocalContext.getRoleSettings((Long) userRoleObject.getValue(), !forceCloneObject.isNull());
            if(settings != null) {
                String nameSetting = trimToNull((String) findProperty("name[Setting]").read(context, settingObject));
                String valueSetting = trimToNull((String) findProperty("value[Setting, UserRole]").read(context, settingObject, userRoleObject));

                ThreadLocalContext.setPropertyValue(settings, nameSetting, valueSetting);
            }

        } catch (ScriptingErrorLog.SemanticErrorException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | CloneNotSupportedException e) {
            throw Throwables.propagate(e);
        }

    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}
