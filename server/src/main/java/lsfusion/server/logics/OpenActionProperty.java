package lsfusion.server.logics;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.interop.action.OpenFileClientAction;
import lsfusion.server.classes.DynamicFormatFileClass;
import lsfusion.server.classes.FileClass;
import lsfusion.server.classes.StaticFormatFileClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.view.DefaultFormView;
import lsfusion.server.form.view.PropertyDrawView;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ClassType;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.actions.SystemExplicitActionProperty;

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

    OpenActionProperty(String caption, LCP fileProperty) {
        super(caption, fileProperty.getInterfaceClasses(ClassType.filePolicy));

        this.fileProperty = fileProperty;
    }

    @Override
    public ImMap<CalcProperty, Boolean> aspectUsedExtProps() {
        return MapFact.<CalcProperty, Boolean>singleton(fileProperty.property, false);
    }

    private FileClass getFileClass() {
        return (FileClass) fileProperty.property.getType();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ObjectValue[] objects = new ObjectValue[context.getKeyCount()];
        int i = 0; // здесь опять учитываем, что порядок тот же
        for (ClassPropertyInterface classInterface : interfaces)
            objects[i++] = context.getKeyValue(classInterface);
        FileClass fileClass = getFileClass();
        for(byte[] file : fileClass.getFiles(fileProperty.read(context, objects))) {
            if (fileClass instanceof DynamicFormatFileClass)
                context.delayUserInterfaction(new OpenFileClientAction(BaseUtils.getFile(file), BaseUtils.getExtension(file)));
            else
                context.delayUserInterfaction(new OpenFileClientAction(file, BaseUtils.firstWord(((StaticFormatFileClass) fileClass).getOpenExtension(file), ",")));
        }
    }

    @Override
    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        super.proceedDefaultDesign(propertyView, view);
        propertyView.design.setImagePath("open.png");
    }
}
