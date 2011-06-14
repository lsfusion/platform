package roman;

import platform.interop.action.ClientAction;
import platform.server.classes.DataClass;
import platform.server.classes.FileActionClass;
import platform.server.classes.ValueClass;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;

import java.util.List;
import java.util.Map;

public class TNVEDImportActionProperty extends ActionProperty {
    public static final int CLASSIFIER_IMPORT = 1;
    public static final int MIN_PRICES_IMPORT = 2;
    public static final int DUTIES_IMPORT = 3;

    private RomanLogicsModule LM;
    private String classifierType;
    private int importType;

    public TNVEDImportActionProperty(String sID, String caption, RomanLogicsModule LM, int importType, String classifierType) {
        super(sID, caption, new ValueClass[]{});
        this.LM = LM;
        this.classifierType = classifierType;
        this.importType = importType;
    }

    public TNVEDImportActionProperty(String sID, String caption, RomanLogicsModule LM, int importType) {
        this(sID, caption, LM, importType, "");
    }

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, DataSession session, Modifier<? extends Changes> modifier, List<ClientAction> actions,
                        RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects, boolean groupLast) {
        try {
            TNVEDImporter importer = null;
            switch (importType) {
                case CLASSIFIER_IMPORT:
                    importer = new TNVEDClassifierImporter(executeForm, value, LM, classifierType);
                    break;
                case MIN_PRICES_IMPORT:
                    importer = new TNVEDMinPricesImporter(executeForm, value, LM);
                    break;
                case DUTIES_IMPORT:
                    importer = new TNVEDDutiesImporter(executeForm, value, LM);
                    break;
            }
            importer.doImport();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DataClass getValueClass() {
        return FileActionClass.getDefinedInstance(false, "Файл базы данных \"DBF\"", "dbf");
    }
}
