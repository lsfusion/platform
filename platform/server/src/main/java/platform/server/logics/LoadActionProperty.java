package platform.server.logics;

import platform.base.col.interfaces.immutable.ImMap;
import platform.server.classes.DataClass;
import platform.server.classes.FileClass;
import platform.server.data.type.Type;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.actions.SystemActionProperty;

import java.sql.SQLException;

public class LoadActionProperty extends SystemActionProperty {

    LCP<?> fileProperty;

    LoadActionProperty(String sID, String caption, LCP fileProperty) {
        super(sID, caption, fileProperty.getInterfaceClasses());

        this.fileProperty = fileProperty;
    }

    protected DataClass getReadType() {
        return (FileClass) fileProperty.property.getType();
    }

    @Override
    public Type getSimpleRequestInputType() {
        return getReadType();
    }

    @Override
    public ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        return getChangeProps(fileProperty.property);
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        DataClass readType = getReadType();
        ObjectValue objectValue = context.requestUserData(readType, null);
        if (objectValue == null)
            return;

        DataObject[] objects = new DataObject[context.getKeyCount()];
        int i = 0; // здесь опять учитываем, что порядок тот же
        for (ClassPropertyInterface classInterface : interfaces)
            objects[i++] = context.getKeyValue(classInterface);
        fileProperty.change(objectValue.getValue(), context, objects);
    }

    @Override
    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        super.proceedDefaultDesign(propertyView, view);
        propertyView.design.setIconPath("load.png");
    }
}
