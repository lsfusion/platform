package lsfusion.server.physics.admin.authentication.action;

import lsfusion.interop.action.ConfirmClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.physics.admin.authentication.security.SecurityLogicsModule;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import javax.swing.*;
import java.sql.SQLException;
import java.util.Iterator;

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
                    new ConfirmClientAction("Свойство зависит от текущей даты",
                            String.format("Свойство %s зависит от текущей даты. Вы уверены, что хотите его залогировать?", property.toString())));
        }
        if (!allow) {
            try(ExecutionContext.NewSession<ClassPropertyInterface> newContext = context.newSession()) {
                BL.reflectionLM.userLoggableProperty.change((Boolean) null, newContext, propertyObject);
                newContext.apply();
            }
        }
    }
}