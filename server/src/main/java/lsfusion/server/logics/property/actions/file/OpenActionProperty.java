package lsfusion.server.logics.property.actions.file;

import lsfusion.base.BaseUtils;
import lsfusion.interop.action.OpenFileClientAction;
import lsfusion.interop.action.OpenUriClientAction;
import lsfusion.server.classes.DynamicFormatFileClass;
import lsfusion.server.classes.FileClass;
import lsfusion.server.classes.StaticFormatFileClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.classes.link.LinkClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;

import java.net.URI;
import java.sql.SQLException;

import static lsfusion.base.BaseUtils.trim;

/**
* Created by IntelliJ IDEA.
* User: Hp
* Date: 11.09.12
* Time: 9:37
* To change this template use File | Settings | File Templates.
*/
public class OpenActionProperty extends FileActionProperty {
    boolean syncType;
    public OpenActionProperty(LocalizedString caption, boolean syncType, ValueClass... valueClasses) {
        super(caption, valueClasses);
        this.syncType = syncType;

        drawOptions.setImage("open.png");
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        ObjectValue filesObject = context.getKeys().getValue(0);
        if(filesObject instanceof DataObject) {
            Object files = filesObject.getValue();
            Type dataClass = ((DataObject) filesObject).getType();

            String fileName = context.getKeyCount() == 2 ? trim((String) context.getKeys().getValue(1).getValue()) : null;

            if (dataClass instanceof FileClass) {
                for (byte[] file : ((FileClass) dataClass).getFiles(files)) {
                    OpenFileClientAction action;
                    if (dataClass instanceof DynamicFormatFileClass)
                        action = new OpenFileClientAction(BaseUtils.getFile(file), fileName, BaseUtils.getExtension(file));
                    else
                        action = new OpenFileClientAction(file, fileName, BaseUtils.firstWord(((StaticFormatFileClass) dataClass).getOpenExtension(file), ","));

                    if(syncType)
                        context.requestUserInteraction(action);
                    else
                        context.delayUserInteraction(action);

                }
            } else if (dataClass instanceof LinkClass) {
                for (URI file : ((LinkClass) dataClass).getFiles(files)) {
                    if(syncType)
                        context.requestUserInteraction(new OpenUriClientAction(file));
                    else
                        context.delayUserInteraction(new OpenUriClientAction(file));
                }
            }
        }
    }
}
