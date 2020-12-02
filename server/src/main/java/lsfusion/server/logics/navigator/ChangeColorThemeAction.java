package lsfusion.server.logics.navigator;

import lsfusion.base.BaseUtils;
import lsfusion.interop.action.ChangeColorThemeClientAction;
import lsfusion.interop.base.view.ColorTheme;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.authentication.AuthenticationLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;

public class ChangeColorThemeAction extends InternalAction {
    private final ClassPropertyInterface colorThemeInterface;

    public ChangeColorThemeAction(AuthenticationLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = getOrderInterfaces().iterator();
        colorThemeInterface = i.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) {
        Long colorTheme = (Long) context.getKeyValue(colorThemeInterface).getValue();
        String colorThemeString = null;
        if (colorTheme != null) {
            try {
                String colorThemeStaticName = (String) findProperty("staticName[StaticObject]").read(context, context.getSession().getDataObject(findClass("ColorTheme"), colorTheme));
                colorThemeString = colorThemeStaticName != null ? colorThemeStaticName.substring(colorThemeStaticName.indexOf(".") + 1) : null;
            } catch (SQLException | SQLHandledException | ScriptingErrorLog.SemanticErrorException e) {
                return;
            }
        }
        context.delayUserInteraction(new ChangeColorThemeClientAction(BaseUtils.nvl(ColorTheme.get(colorThemeString), ColorTheme.DEFAULT)));
    }
}