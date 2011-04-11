package platform.server.logics.property.actions;

import platform.interop.action.ClientAction;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.CompareWhere;
import platform.server.data.where.Where;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.GroupObjectInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.DataSession;
import platform.server.session.PropertyChange;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.*;

public class GroupChangeActionProperty<M extends PropertyInterface, G extends PropertyInterface> extends ActionProperty {

    private final GroupObjectEntity filterGroupObject;
    private final Property<G> getterProperty;
    private final Property<M> mainProperty;

    private HashSet<M> grouping;

    private Map<G, M> mapGetToMain;
    private Map<G, ClassPropertyInterface> mapGetToThis;

    private Map<M, ClassPropertyInterface> mapMainToThis;

    private static <M extends PropertyInterface, G extends PropertyInterface> ValueClass[] getValueClassList(Property<M> mainProperty, Property<G> getterProperty, G[] getterExtra) {
        List<ValueClass> result = new ArrayList<ValueClass>();

        for (M mFace : mainProperty.interfaces) {
            result.add(mainProperty.getMapClasses().get(mFace));
        }

        for (G gFace : getterExtra) {
            result.add(getterProperty.getMapClasses().get(gFace));
        }

        return result.toArray(new ValueClass[result.size()]);
    }

    /**
     * @param mainProperty - свойство, куда будем писать
     * @param getterProperty - свойство, из которого будем читать значение
     */
    public GroupChangeActionProperty(String sID, String caption, GroupObjectEntity filterGroupObject,
                                     Property<M> mainProperty, M[] imainGrouping,
                                     Property<G> getterProperty, Map<G, M> mapGetToMain, G[] getterExtra) {
        super(sID, caption, getValueClassList(mainProperty, getterProperty, getterExtra));

        this.filterGroupObject = filterGroupObject;

        this.mainProperty = mainProperty;
        this.getterProperty = getterProperty;

        this.mapGetToMain = new HashMap<G, M>(mapGetToMain);

        this.grouping = new HashSet<M>(Arrays.asList(imainGrouping));

        this.mapGetToThis = new HashMap<G, ClassPropertyInterface>();
        this.mapMainToThis = new HashMap<M, ClassPropertyInterface>();

        int n = mainProperty.interfaces.size();
        Iterator<M> mainIFaces = mainProperty.interfaces.iterator();
        int i = 0;
        for (ClassPropertyInterface iFace : interfaces) {
            if (i < n) {
                assert mainIFaces.hasNext();

                M mainIFace = mainIFaces.next();
                mapMainToThis.put(mainIFace, iFace);
                for (G getIFace : filterValues(mapGetToMain, mainIFace)) {
                    mapGetToThis.put(getIFace, iFace);
                }
            } else {
                mapGetToThis.put(getterExtra[i - n], iFace);
            }
            ++i;
        }
    }

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        FormInstance<?> form = (FormInstance<?>) executeForm.form;
        DataSession session = form.session;

        Map<M, KeyExpr> mainKeys = mainProperty.getMapKeys();

        Where changeWhere = CompareWhere.compareValues(
                filterNotKeys(mainKeys, grouping),
                join(mapMainToThis, keys)
        );

        if (filterGroupObject != null) {
            GroupObjectInstance groupInstance = form.instanceFactory.getInstance(filterGroupObject);
            changeWhere = changeWhere.and(
                    groupInstance.getWhere(
                            filterKeys(crossJoin(join(mapMainToThis, mapObjects), mainKeys), groupInstance.objects),
                            session.modifier));
        }

        Map<G, Expr> getterKeys = new HashMap<G, Expr>();
        for (G getIFace : getterProperty.interfaces) {
            if (mapGetToMain.containsKey(getIFace)) {
                getterKeys.put(getIFace, mainKeys.get(mapGetToMain.get(getIFace)));
            } else {
                getterKeys.put(getIFace, keys.get(mapGetToThis.get(getIFace)).getExpr());
            }
        }

        Expr setExpr = getterProperty.getExpr(getterKeys, session.modifier);

        PropertyChange mainPropertyChange = new PropertyChange(mainKeys, setExpr, changeWhere);

        actions.addAll(
                session.execute(mainProperty, mainPropertyChange, session.modifier, executeForm, join(mapMainToThis, mapObjects))
        );
    }
}
