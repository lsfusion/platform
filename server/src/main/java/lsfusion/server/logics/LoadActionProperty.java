package lsfusion.server.logics;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.DataClass;
import lsfusion.server.classes.FileClass;
import lsfusion.server.data.type.Type;
import lsfusion.server.form.view.DefaultFormView;
import lsfusion.server.form.view.PropertyDrawView;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.actions.SystemExplicitActionProperty;

import java.sql.SQLException;

public class LoadActionProperty extends SystemExplicitActionProperty {

    LCP<?> fileProperty;

    LoadActionProperty(String sID, String caption, LCP fileProperty) {
        super(sID, caption, fileProperty.getInterfaceClasses());

        this.fileProperty = fileProperty;
    }

    protected DataClass getReadType() {
        return (FileClass) fileProperty.property.getType();
    }

    @Override
    public Type getSimpleRequestInputType(boolean optimistic) {
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
            objects[i++] = context.getDataKeyValue(classInterface);
        fileProperty.change(objectValue, context, objects);
    }

    @Override
    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        super.proceedDefaultDesign(propertyView, view);
        propertyView.design.setIconPath("load.png");
    }
}
