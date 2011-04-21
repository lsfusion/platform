package platform.server.logics.property.actions;

import platform.base.Result;
import platform.interop.action.ClientAction;
import platform.server.classes.ValueClass;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExecutePropertiesActionProperty extends ActionProperty {

    private static ValueClass[] getValueClasses(boolean writeDefaults, LP[] dataProperties, int[][] mapInterfaces) {
        Map<Integer, ValueClass> mapClasses = new HashMap<Integer, ValueClass>();
        for (int i = 0; i < dataProperties.length; ++i) {
            LP dataProperty = dataProperties[i];
            int[] mapPropInterfaces = mapInterfaces[i];

            Result<ValueClass> result = new Result<ValueClass>();
            ValueClass[] propClasses = dataProperty.getCommonClasses(result);
            for (int j = 0; j < mapPropInterfaces.length; ++j) {
                ValueClass valueClass = (!writeDefaults && j == mapPropInterfaces.length - 1)
                                        ? result.result
                                        : propClasses[j];


                int thisIndex = mapPropInterfaces[j];

                ValueClass definedValueClass = mapClasses.get(thisIndex);
                if (definedValueClass != null) {
                    if (definedValueClass.isCompatibleParent(valueClass)) {
                        valueClass = definedValueClass;
                    } else {
                        assert valueClass.isCompatibleParent(definedValueClass);
                    }
                }

                mapClasses.put(thisIndex, valueClass);
            }
        }


        ValueClass classes[] = new ValueClass[mapClasses.size()];
        for (int i = 0; i < mapClasses.size(); ++i) {
            assert mapClasses.containsKey(i);
            classes[i] = mapClasses.get(i);
            assert classes[i] != null;
        }

        return classes;
    }

    private final boolean writeDefaults;

    private final LP[] dataProperties;

    private final Map<PropertyInterface, ClassPropertyInterface>[] mapInterfaces;
    private final ClassPropertyInterface[] mapResults;

    /**
     * Если writeDefaults == false, то сначала идут номера для интерфейсов, потом - для входа
     * @param imapInterfaces должны быть 0-based
     */
    public ExecutePropertiesActionProperty(String sID, String caption, boolean writeDefaults, LP[] dataProperties, int[][] imapInterfaces) {
        super(sID, caption, getValueClasses(writeDefaults, dataProperties, imapInterfaces));

        this.dataProperties = dataProperties;
        this.writeDefaults = writeDefaults;
        this.mapInterfaces = new Map[dataProperties.length];
        this.mapResults = writeDefaults ? null : new ClassPropertyInterface[dataProperties.length];

        List<ClassPropertyInterface> listInterfaces = (List<ClassPropertyInterface>)interfaces;

        for (int i = 0; i < dataProperties.length; ++i) {
            LP dataProperty = dataProperties[i];
            List<? extends PropertyInterface> propInterfaces = dataProperty.listInterfaces;
            int[] imapPropInterfaces = imapInterfaces[i];

            Map<PropertyInterface, ClassPropertyInterface> mapPropInterfaces = new HashMap<PropertyInterface, ClassPropertyInterface>();
            for (int j = 0; j < imapPropInterfaces.length; ++j) {
                ClassPropertyInterface mapInterface = listInterfaces.get(imapPropInterfaces[j]);

                if (!writeDefaults && j == imapPropInterfaces.length - 1) {
                    mapResults[i] = mapInterface;
                } else {
                    mapPropInterfaces.put(propInterfaces.get(j), mapInterface);
                }
            }

            mapInterfaces[i] = mapPropInterfaces;
        }
    }

    @Override
    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        FormInstance<?> form = (FormInstance<?>) executeForm.form;
        DataSession session = form.session;

        for (int i = 0; i < dataProperties.length; ++i) {
            LP dataProperty = dataProperties[i];
            Map<?, ClassPropertyInterface> mapPropInterfaces = mapInterfaces[i];
            List<?> propInterfaces = dataProperty.listInterfaces;

            Object execValue = writeDefaults
                               ? dataProperty.property.getCommonClasses().value.getDefaultValue()
                               : keys.get(mapResults[i]).getValue();

            DataObject[] execInterfaces = new DataObject[propInterfaces.size()];
            for (int j = 0; j < propInterfaces.size(); j++) {
                execInterfaces[j] = keys.get(
                        mapPropInterfaces.get(propInterfaces.get(j))
                );
            }

            actions.addAll(dataProperty.execute(execValue, session, execInterfaces));
        }
    }
}
