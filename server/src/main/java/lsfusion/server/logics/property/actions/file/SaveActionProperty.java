package lsfusion.server.logics.property.actions.file;

import lsfusion.base.BaseUtils;
import lsfusion.interop.action.SaveFileClientAction;
import lsfusion.server.classes.DynamicFormatFileClass;
import lsfusion.server.classes.FileClass;
import lsfusion.server.classes.StaticFormatFileClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;

import java.sql.SQLException;

import static lsfusion.base.BaseUtils.trim;

public class SaveActionProperty extends FileActionProperty {

    public SaveActionProperty(LocalizedString caption, ValueClass... valueClasses) {
        super(caption, valueClasses);

        drawOptions.setImage("save.png");
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ObjectValue filesObject = context.getKeys().getValue(0);
        if(filesObject instanceof DataObject) {
            Object files = filesObject.getValue();
            Type dataClass = ((DataObject) filesObject).getType();

            String fileName = context.getKeyCount() >= 2 ? trim((String) context.getKeys().getValue(1).getValue()) : "new file";

            if (dataClass instanceof FileClass) {
                for (byte[] file : ((FileClass) dataClass).getFiles(files)) {
                    String extension;
                    byte[] saveFile = file;
                    if (dataClass instanceof DynamicFormatFileClass) {
                        extension = BaseUtils.getExtension(file);
                        saveFile = BaseUtils.getFile(file);
                    } else {
                        extension = BaseUtils.firstWord(((StaticFormatFileClass) dataClass).getOpenExtension(file), ",");
                    }
                    context.delayUserInterfaction(new SaveFileClientAction(saveFile, fileName + "." + extension));
                }
            }
        }
    }
}
