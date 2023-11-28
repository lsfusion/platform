package lsfusion.server.logics.classes.data.utils.string;

import com.google.common.base.Throwables;
import lsfusion.interop.action.CopyToClipboardClientAction;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.UtilsLogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.integration.internal.to.InternalAction;

import java.sql.SQLException;
import java.util.Iterator;

public class CopyToClipboardAction extends InternalAction {

    private final ClassPropertyInterface textInterface;

    public CopyToClipboardAction(UtilsLogicsModule LM, ValueClass... classes) {
        super(LM, classes);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        textInterface = i.next();
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            String text = (String) context.getDataKeyValue(textInterface).getValue();
            if (text != null && !text.isEmpty()) {
                context.requestUserInteraction(new CopyToClipboardClientAction(text));
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}