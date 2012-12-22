package platform.server.logics;

import platform.base.BaseUtils;
import platform.base.col.MapFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.interop.action.OpenFileClientAction;
import platform.server.classes.DynamicFormatFileClass;
import platform.server.classes.FileClass;
import platform.server.classes.StaticFormatFileClass;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.SystemActionProperty;

import java.sql.SQLException;

/**
* Created by IntelliJ IDEA.
* User: Hp
* Date: 11.09.12
* Time: 9:37
* To change this template use File | Settings | File Templates.
*/
public class OpenActionProperty extends SystemActionProperty {

    LCP<?> fileProperty;

    OpenActionProperty(String sID, String caption, LCP fileProperty) {
        super(sID, caption, fileProperty.getInterfaceClasses());

        this.fileProperty = fileProperty;
    }

    @Override
    public ImMap<CalcProperty, Boolean> aspectUsedExtProps() {
        return MapFact.<CalcProperty, Boolean>singleton(fileProperty.property, false);
    }

    private FileClass getFileClass() {
        return (FileClass) fileProperty.property.getType();
    }

    public void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        DataObject[] objects = new DataObject[context.getKeyCount()];
        int i = 0; // здесь опять учитываем, что порядок тот же
        for (ClassPropertyInterface classInterface : interfaces)
            objects[i++] = context.getKeyValue(classInterface);
        FileClass fileClass = getFileClass();
        for(byte[] file : fileClass.getFiles(fileProperty.read(context, objects))) {
            if (fileClass instanceof DynamicFormatFileClass)
                context.delayUserInterfaction(new OpenFileClientAction(BaseUtils.getFile(file), BaseUtils.getExtension(file)));
            else
                context.delayUserInterfaction(new OpenFileClientAction(file, BaseUtils.firstWord(((StaticFormatFileClass) fileClass).getOpenExtension(), ",")));
        }
    }

    @Override
    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        super.proceedDefaultDesign(propertyView, view);
        propertyView.design.setIconPath("open.png");
    }
}
