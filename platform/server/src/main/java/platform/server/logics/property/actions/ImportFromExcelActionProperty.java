package platform.server.logics.property.actions;

import platform.interop.action.ClientAction;
import platform.server.classes.*;
import platform.server.data.type.ParseException;
import platform.server.data.type.Type;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.view.form.*;
import platform.server.view.form.client.RemoteFormView;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

public class ImportFromExcelActionProperty extends ActionProperty {
    private static Logger LOGGER = Logger.getLogger(ImportFromExcelActionProperty.class.getName());

    public static final String NAME = "importFromExcel";
    private CustomClass valueClass;

    public ImportFromExcelActionProperty(String sID, CustomClass valueClass) {
        super(NAME, sID, "Импортировать (" + valueClass + ")", new ValueClass[]{});

        this.valueClass = valueClass;
    }

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions,
                        RemoteFormView executeForm, Map<ClassPropertyInterface, PropertyObjectInterface> mapObjects) throws SQLException {

        Sheet sh;
        try {
            ByteArrayInputStream inFile = new ByteArrayInputStream((byte[]) value.getValue());
            sh = Workbook.getWorkbook(inFile).getSheet(0);
        } catch (Exception e) {
            LOGGER.severe("Не могу прочитать .xsl файл.");
            return;
        }

        // читаем sid свойств из первого ряда таблицы
        Set<String> definedPropertiesSIDs = new HashSet<String>();
        for (int i = 0; i < sh.getColumns(); ++i) {
            definedPropertiesSIDs.add(sh.getCell(i, 0).getContents());
        }

        // находим используемые свойства
        Map<String, PropertyView> definedProperties = new HashMap<String, PropertyView>();
        for (Object prop : executeForm.form.properties) {
            PropertyView property = (PropertyView) prop;
            if (definedPropertiesSIDs.contains(property.view.property.sID)) {
                definedProperties.put(property.view.property.sID, property);
            }
        }

        for (int i = 1; i < sh.getRows(); ++i) {
            RemoteForm<?> form = (RemoteForm<?>) executeForm.form;
            DataObject instance = form.addObject((ConcreteCustomClass) valueClass);
            
            for (int j = 0; j < sh.getColumns(); ++j) {
                Cell cell = sh.getCell(j, i);
                String cellValue = cell.getContents();

                String propertySID = sh.getCell(j, 0).getContents();
                PropertyView property = definedProperties.get(propertySID);
                if (property != null) {
                    try {
                        Type type = property.view.getChangeProperty().property.getType();
                        form.changeProperty(property, type.parseString(cellValue));
                    } catch (ParseException e) {
                        LOGGER.log(Level.WARNING, "не конвертировано значение совйства", e);
                    }
                }
            }
        }
    }

    @Override
    protected DataClass getValueClass() {
        return FileActionClass.getInstance("Файлы таблиц", "xls");
    }
}
