package platform.server.view.form;

import platform.interop.form.RemoteFormInterface;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.MapKeysInterface;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.properties.DataProperty;
import platform.server.logics.properties.DefaultData;
import platform.server.logics.properties.Property;
import platform.server.session.TableChanges;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.*;

public class GroupObjectImplement extends ArrayList<ObjectImplement> implements MapKeysInterface<ObjectImplement> {

    // глобальный идентификатор чтобы писать во ViewTable
    public final int ID;
    public GroupObjectImplement(int iID,int iOrder,int iPageSize,boolean iGridClassView,boolean iSingleViewType) {

        assert (iID < RemoteFormInterface.GID_SHIFT);

        ID = iID;
        order = iOrder;
        pageSize = iPageSize;
        gridClassView = iGridClassView;
        singleViewType = iSingleViewType;
    }

    public Map<ObjectImplement, KeyExpr> getMapKeys() {
        return ObjectImplement.getMapKeys(this);
    }

    public void addObject(ObjectImplement object) {
        add(object);
        object.groupTo = this;
    }

    public Integer order = 0;

    // классовый вид включен или нет
    public boolean gridClassView = true;
    public boolean singleViewType = false;

    // закэшированные

    // вообще все фильтры
    Set<Filter> mapFilters = new HashSet<Filter>();
    List<PropertyView> mapOrders = new ArrayList<PropertyView>();

    // с активным интерфейсом
    public Set<Filter> filters = new HashSet<Filter>();
    LinkedHashMap<PropertyObjectImplement,Boolean> orders = new LinkedHashMap<PropertyObjectImplement, Boolean>();

    boolean upKeys, downKeys;
    List<Map<ObjectImplement,DataObject>> keys = null;
    // какие ключи активны
    Map<Map<ObjectImplement,DataObject>,Map<PropertyObjectImplement,ObjectValue>> keyOrders = null;

    // 0 !!! - изменился объект, 1 - класс объекта, 2 !!! - отбор, 3 !!! - хоть один класс, 4 !!! - классовый вид

    public final static int UPDATED_OBJECT = (1);
    public final static int UPDATED_KEYS = (1 << 2);
    public final static int UPDATED_GRIDCLASS = (1 << 3);
    public final static int UPDATED_CLASSVIEW = (1 << 4);

    public int updated = UPDATED_GRIDCLASS | UPDATED_CLASSVIEW;

    public int pageSize = 50;

    Map<ObjectImplement,DataObject> getGroupObjectValue() {
        Map<ObjectImplement,DataObject> result = new HashMap<ObjectImplement, DataObject>();
        for(ObjectImplement object : this) {
            DataObject objectValue = object.getValue();
            if(objectValue!=null)
                result.put(object,object.getValue());
        }

        return result;
    }

    public Map<ObjectImplement,DataObject> findGroupObjectValue(Map<ObjectImplement,Object> map) {
        for(Map<ObjectImplement,DataObject> keyRow : keys) {
            boolean equal = true;
            for(Map.Entry<ObjectImplement,DataObject> keyEntry : keyRow.entrySet())
                if(!keyEntry.getValue().object.equals(map.get(keyEntry.getKey()))) {
                    equal = false;
                    break;
                }
            if(equal)
                return keyRow;
        }

        throw new RuntimeException("key not found");
    }

    // получает Set группы
    public Set<GroupObjectImplement> getClassGroup() {

        Set<GroupObjectImplement> result = new HashSet<GroupObjectImplement>();
        result.add(this);
        return result;
    }

    void fillSourceSelect(JoinQuery<ObjectImplement, ?> query, Set<GroupObjectImplement> classGroup, TableChanges session, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps) throws SQLException {

        for(Filter filt : filters)
            filt.fillSelect(query, classGroup, session, defaultProps, noUpdateProps);

        // докинем Join ко всем классам, те которых не было FULL JOIN'ом остальные Join'ом
        for(ObjectImplement object : this)
            query.and(DataSession.getIsClassWhere(session,query.mapKeys.get(object),object.getGridClass(),null));
    }
}
