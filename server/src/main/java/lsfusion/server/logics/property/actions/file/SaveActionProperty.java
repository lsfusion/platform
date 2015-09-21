package lsfusion.server.logics.property.actions.file;

import lsfusion.base.BaseUtils;
import lsfusion.interop.action.SaveFileClientAction;
import lsfusion.server.classes.DynamicFormatFileClass;
import lsfusion.server.classes.FileClass;
import lsfusion.server.classes.StaticFormatFileClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.form.view.DefaultFormView;
import lsfusion.server.form.view.PropertyDrawView;
import lsfusion.server.logics.ObjectValue;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ClassType;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.actions.SystemExplicitActionProperty;

import java.sql.SQLException;

public class SaveActionProperty extends SystemExplicitActionProperty {
    private LCP<?> fileProperty;

    public SaveActionProperty(String caption, LCP fileProperty) {
        super(caption, fileProperty.getInterfaceClasses(ClassType.filePolicy));

        this.fileProperty = fileProperty;
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ObjectValue[] objects = new ObjectValue[context.getKeyCount()];
        int i = 0;
        for (ClassPropertyInterface classInterface : interfaces) {
            objects[i++] = context.getKeyValue(classInterface);
        }
        FileClass fileClass = (FileClass) fileProperty.property.getType();
        for (byte[] file : fileClass.getFiles(fileProperty.read(context, objects))) {
            if (fileClass instanceof DynamicFormatFileClass) {
                context.delayUserInterfaction(new SaveFileClientAction(BaseUtils.getFile(file), BaseUtils.getExtension(file)));
            } else {
                context.delayUserInterfaction(new SaveFileClientAction(file, BaseUtils.firstWord(((StaticFormatFileClass) fileClass).getOpenExtension(file), ",")));
            }
        }
    }

    @Override
    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        super.proceedDefaultDesign(propertyView, view);
        propertyView.design.setImagePath("save.png");
    }
}
