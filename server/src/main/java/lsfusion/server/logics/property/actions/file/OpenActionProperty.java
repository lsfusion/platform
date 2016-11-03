package lsfusion.server.logics.property.actions.file;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.interop.action.OpenFileClientAction;
import lsfusion.interop.action.OpenUriClientAction;
import lsfusion.server.classes.DataClass;
import lsfusion.server.classes.DynamicFormatFileClass;
import lsfusion.server.classes.FileClass;
import lsfusion.server.classes.StaticFormatFileClass;
import lsfusion.server.classes.link.LinkClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.SystemExplicitActionProperty;

import java.net.URI;
import java.sql.SQLException;

/**
* Created by IntelliJ IDEA.
* User: Hp
* Date: 11.09.12
* Time: 9:37
* To change this template use File | Settings | File Templates.
*/
public class OpenActionProperty extends SystemExplicitActionProperty {

    LCP<?> fileProperty;

    public OpenActionProperty(LocalizedString caption, LCP fileProperty) {
        super(caption, fileProperty.getInterfaceClasses(ClassType.filePolicy));

        this.fileProperty = fileProperty;

        drawOptions.setImage("open.png");
    }

    @Override
    public ImMap<CalcProperty, Boolean> aspectUsedExtProps() {
        return MapFact.<CalcProperty, Boolean>singleton(fileProperty.property, false);
    }

    private DataClass getDataClass() {
        return (DataClass) fileProperty.property.getType();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ObjectValue[] objects = new ObjectValue[context.getKeyCount()];
        int i = 0; // здесь опять учитываем, что порядок тот же
        for (ClassPropertyInterface classInterface : interfaces)
            objects[i++] = context.getKeyValue(classInterface);
        DataClass dataClass = getDataClass();
        if(dataClass instanceof FileClass) {
            for (byte[] file : ((FileClass)dataClass).getFiles(fileProperty.read(context, objects))) {
                if (dataClass instanceof DynamicFormatFileClass)
                    context.delayUserInterfaction(new OpenFileClientAction(BaseUtils.getFile(file), BaseUtils.getExtension(file)));
                else
                    context.delayUserInterfaction(new OpenFileClientAction(file, BaseUtils.firstWord(((StaticFormatFileClass) dataClass).getOpenExtension(file), ",")));
            }
        } else if (dataClass instanceof LinkClass) {
            for (URI file : ((LinkClass) dataClass).getFiles(fileProperty.read(context, objects))) {
                context.delayUserInterfaction(new OpenUriClientAction(file));
            }
        }
    }
}
