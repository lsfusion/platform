package platform.server.logics.property.actions;

import platform.server.classes.ConcreteClass;
import platform.server.classes.ValueClass;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.DataObject;
import platform.server.logics.LogicsModule;
import platform.server.logics.NullValue;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;

import java.sql.SQLException;
import java.util.*;

import static platform.server.logics.PropertyUtils.getValueClasses;

public class ExecutePropertiesActionProperty extends ActionProperty {

    private final int writeType;

    private final LP[] dataProperties;
    private final HashSet<Property> changedProps;

    private final Map<PropertyInterface, ClassPropertyInterface>[] mapInterfaces;
    private final ClassPropertyInterface[] mapResults;

    /**
     * Если writeType == false, то сначала идут номера для интерфейсов, потом - для входа
     * @param imapInterfaces должны быть 0-based
     */
    public ExecutePropertiesActionProperty(String sID, String caption, int writeType, LP[] dataProperties, int[][] imapInterfaces) {
        super(sID, caption, getValueClasses(writeType == LogicsModule.EPA_INTERFACE, dataProperties, imapInterfaces));

        this.dataProperties = dataProperties;
        this.writeType = writeType;
        this.mapInterfaces = new Map[dataProperties.length];
        this.mapResults = writeType == LogicsModule.EPA_INTERFACE ? new ClassPropertyInterface[dataProperties.length] : null;

        List<ClassPropertyInterface> listInterfaces = (List<ClassPropertyInterface>)interfaces;

        for (int i = 0; i < dataProperties.length; ++i) {
            LP dataProperty = dataProperties[i];
            List<? extends PropertyInterface> propInterfaces = dataProperty.listInterfaces;
            int[] imapPropInterfaces = imapInterfaces[i];

            Map<PropertyInterface, ClassPropertyInterface> mapPropInterfaces = new HashMap<PropertyInterface, ClassPropertyInterface>();
            for (int j = 0; j < imapPropInterfaces.length; ++j) {
                ClassPropertyInterface mapInterface = listInterfaces.get(imapPropInterfaces[j]);

                if (writeType == LogicsModule.EPA_INTERFACE && j == imapPropInterfaces.length - 1) {
                    mapResults[i] = mapInterface;
                } else {
                    mapPropInterfaces.put(propInterfaces.get(j), mapInterface);
                }
            }

            mapInterfaces[i] = mapPropInterfaces;
        }

        changedProps = new HashSet<Property>();
        for (LP prop : dataProperties) {
            changedProps.add(prop.property);
        }
    }

    @Override
    public Set<Property> getChangeProps() {
        return changedProps;
    }

    @Override
    public void execute(ExecutionContext context) throws SQLException {
        for (int i = 0; i < dataProperties.length; ++i) {
            LP dataProperty = dataProperties[i];
            Map<?, ClassPropertyInterface> mapPropInterfaces = mapInterfaces[i];
            List<?> propInterfaces = dataProperty.listInterfaces;

            ObjectValue execValue;
            switch (writeType) {
                case LogicsModule.EPA_INTERFACE :
                    execValue = context.getKeyValue(mapResults[i]);
                    break;
                case LogicsModule.EPA_DEFAULT :
                    ValueClass valueClass = dataProperty.property.getCommonClasses().value;
                    execValue = ObjectValue.getValue(valueClass.getDefaultValue(), (ConcreteClass)valueClass);
                    break;
                case LogicsModule.EPA_NULL :
                    execValue = NullValue.instance;
                    break;
                default :
                    throw new RuntimeException("Unknown writeType");
            }

            boolean inForm = context.getRemoteForm() != null && dataProperty.property instanceof UserProperty;

            DataObject[] execInterfaces = new DataObject[propInterfaces.size()];
            Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> execMapObjects = new HashMap<ClassPropertyInterface, PropertyObjectInterfaceInstance>();
            for (int j = 0; j < propInterfaces.size(); j++) {
                ClassPropertyInterface execInterface = mapPropInterfaces.get(propInterfaces.get(j));
                execInterfaces[j] = context.getKeyValue(execInterface);
                if (inForm)
                    execMapObjects.put(execInterface, context.getObjectInstance(execInterface));
            }

            if (inForm) {
                ((UserProperty)dataProperty.property).execute(new ExecutionContext(dataProperty.getMapValues(execInterfaces), execValue, context.getSession(), context.getModifier(), context.getActions(), context.getRemoteForm(), execMapObjects, true));
            } else {
                context.addActions(dataProperty.execute(execValue.getValue(), context.getSession(), execInterfaces));
            }
        }
    }
}
