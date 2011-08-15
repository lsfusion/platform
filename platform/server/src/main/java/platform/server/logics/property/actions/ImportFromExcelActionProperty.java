package platform.server.logics.property.actions;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import org.apache.log4j.Logger;
import platform.interop.ClassViewType;
import platform.interop.KeyStrokes;
import platform.server.classes.*;
import platform.server.data.type.ParseException;
import platform.server.data.type.Type;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.PropertyDrawInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.form.view.DefaultFormView;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;

import java.io.ByteArrayInputStream;
import java.sql.SQLException;
import java.util.*;

public class ImportFromExcelActionProperty extends ActionProperty {
    private static Logger logger = Logger.getLogger(ImportFromExcelActionProperty.class);

    private CustomClass valueClass;

    public ImportFromExcelActionProperty(String sID, CustomClass valueClass) {
        super(sID, ServerResourceBundle.getString("logics.property.actions.import") + "(" + valueClass + ")", new ValueClass[]{});

        this.valueClass = valueClass;
    }

    public void execute(ExecutionContext context) throws SQLException {

        Sheet sh;
        try {
            ByteArrayInputStream inFile = new ByteArrayInputStream((byte[]) context.getValueObject());
            sh = Workbook.getWorkbook(inFile).getSheet(0);
        } catch (Exception e) {
            logger.fatal(ServerResourceBundle.getString("logics.property.actions.failed.to.read.xls.file"));
            return;
        }

        // читаем sid свойств из первого ряда таблицы
        Set<String> definedPropertiesSIDs = new HashSet<String>();
        for (int i = 0; i < sh.getColumns(); ++i) {
            definedPropertiesSIDs.add(sh.getCell(i, 0).getContents());
        }

        // находим используемые свойства
        Map<String, PropertyDrawInstance> definedProperties = new HashMap<String, PropertyDrawInstance>();
        for (PropertyDrawInstance<?> property : context.getFormInstance().properties)
            if (definedPropertiesSIDs.contains(property.propertyObject.property.getSID())) {
                definedProperties.put(property.propertyObject.property.getSID(), property);
            }

        for (int i = 1; i < sh.getRows(); ++i) {
            FormInstance<?> form = context.getFormInstance();
            form.addObject((ConcreteCustomClass) valueClass);

            for (int j = 0; j < sh.getColumns(); ++j) {
                Cell cell = sh.getCell(j, i);
                String cellValue = cell.getContents();

                String propertySID = sh.getCell(j, 0).getContents();
                PropertyDrawInstance property = definedProperties.get(propertySID);
                if (property != null) {
                    try {
                        Type type = property.propertyObject.getType();
                        form.changeProperty(property, type.parseString(cellValue), false);
                    } catch (ParseException e) {
                        logger.warn(ServerResourceBundle.getString("logics.property.actions.property.value.not.converted"), e);
                    }
                }
            }
        }
    }

    @Override
    public DataClass getValueClass() {
        return FileActionClass.getDefinedInstance(false, ServerResourceBundle.getString("logics.property.actions.table.files"), "xls");
    }

    @Override
    public void proceedDefaultDraw(PropertyDrawEntity<ClassPropertyInterface> entity, FormEntity form) {
        super.proceedDefaultDraw(entity, form);
        entity.shouldBeLast = true;
        entity.forceViewType = ClassViewType.PANEL;
    }

    @Override
    public void proceedDefaultDesign(DefaultFormView view, PropertyDrawEntity<ClassPropertyInterface> entity) {
        super.proceedDefaultDesign(view, entity);
        view.get(entity).editKey = KeyStrokes.getImportActionPropertyKeyStroke();
        view.get(entity).design.setIconPath("import.png");
        view.get(entity).showEditKey = false;
        view.get(entity).drawToToolbar = true;
    }

}
