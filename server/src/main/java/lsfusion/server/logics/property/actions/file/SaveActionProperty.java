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
    private LCP<?> fileNameProp;

    public SaveActionProperty(String caption, LCP fileProperty, LCP fileNameProp) {
        super(caption, fileProperty.getInterfaceClasses(ClassType.filePolicy));

        this.fileProperty = fileProperty;
        this.fileNameProp = fileNameProp;
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        ObjectValue[] objects = new ObjectValue[context.getKeyCount()];
        int i = 0;
        for (ClassPropertyInterface classInterface : interfaces) {
            objects[i++] = context.getKeyValue(classInterface);
        }
        FileClass fileClass = (FileClass) fileProperty.property.getType();
        Object fileNameObject = fileNameProp != null ? fileNameProp.read(context, objects) : null;
        String fileName = fileNameObject != null ? fileNameObject.toString().trim() : "new file";
        for (byte[] file : fileClass.getFiles(fileProperty.read(context, objects))) {
            String extension;
            byte[] saveFile = file; 
            if (fileClass instanceof DynamicFormatFileClass) {
                extension = BaseUtils.getExtension(file);
                saveFile = BaseUtils.getFile(file);
            } else {
                extension = BaseUtils.firstWord(((StaticFormatFileClass) fileClass).getOpenExtension(file), ",");
            }
            context.delayUserInterfaction(new SaveFileClientAction(saveFile, fileName + "." + extension));
        }
    }

    @Override
    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        super.proceedDefaultDesign(propertyView, view);
        propertyView.design.setImagePath("save.png");
    }
}
