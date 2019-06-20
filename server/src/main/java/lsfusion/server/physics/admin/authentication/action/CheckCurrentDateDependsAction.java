package lsfusion.server.physics.admin.authentication.action;

import lsfusion.interop.action.ConfirmClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.admin.authentication.security.SecurityLogicsModule;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import javax.swing.*;
import java.sql.SQLException;
import java.util.Iterator;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class CheckCurrentDateDependsAction extends InternalAction {

    private final ClassPropertyInterface propertyInterface;

    public CheckCurrentDateDependsAction(SecurityLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        propertyInterface = i.next();
    }

    @Override
    public void executeInternal(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        BusinessLogics BL = context.getBL();

        DataObject propertyObject = context.getDataKeyValue(propertyInterface);

        boolean allow = true;
        Property<?> property = BL.findProperty((String) BL.reflectionLM.canonicalNameProperty.read(context, propertyObject)).property;
        if (Property.depends(property, BL.timeLM.currentDate.property)) {
            allow = JOptionPane.YES_OPTION == (Integer) context.requestUserInteraction(
                    new ConfirmClientAction(localize("{security.property.depends.on.current.date}"),
                            localize(LocalizedString.createFormatted("{security.property.depends.on.current.date.are.you.sure}", property.toString()))));
        }
        if (!allow) {
            try(ExecutionContext.NewSession<ClassPropertyInterface> newContext = context.newSession()) {
                BL.reflectionLM.userLoggableProperty.change((Boolean) null, newContext, propertyObject);
                newContext.apply();
            }
        }
    }
}