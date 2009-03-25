package platform.server.view.form;

import platform.interop.form.RemoteFormInterface;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.query.ChangeQuery;
import platform.server.data.query.Join;
import platform.server.data.query.JoinQuery;
import platform.server.logics.classes.IntegralClass;
import platform.server.logics.data.TableFactory;
import platform.server.logics.session.DataSession;

import java.util.*;

public class GroupObjectImplement extends ArrayList<ObjectImplement> {

    // глобальный идентификатор чтобы писать во ViewTable
    public final int ID;
    public GroupObjectImplement(int iID) {

        if (iID >= RemoteFormInterface.GID_SHIFT)
            throw new RuntimeException("sID must be less than " + RemoteFormInterface.GID_SHIFT);

        ID = iID;
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
    List<GroupObjectValue> keys = null;
    // какие ключи активны
    Map<GroupObjectValue,Map<PropertyObjectImplement,Object>> keyOrders = null;

    // 0 !!! - изменился объект, 1 - класс объекта, 2 !!! - отбор, 3 !!! - хоть один класс, 4 !!! - классовый вид

    public final static int UPDATED_OBJECT = (1);
    public final static int UPDATED_KEYS = (1 << 2);
    public final static int UPDATED_GRIDCLASS = (1 << 3);
    public final static int UPDATED_CLASSVIEW = (1 << 4);

    public int updated = UPDATED_GRIDCLASS | UPDATED_CLASSVIEW;

    public int
            pageSize = 50;

    GroupObjectValue getObjectValue() {
        GroupObjectValue Result = new GroupObjectValue();
        for(ObjectImplement Object : this)
            Result.put(Object,Object.idObject);

        return Result;
    }

    // получает Set группы
    public Set<GroupObjectImplement> getClassGroup() {

        Set<GroupObjectImplement> Result = new HashSet<GroupObjectImplement>();
        Result.add(this);
        return Result;
    }

    void fillSourceSelect(JoinQuery<ObjectImplement, ?> query, Set<GroupObjectImplement> classGroup, TableFactory tableFactory, DataSession session) {

        // фильтры первыми потому как ограничивают ключи
        for(Filter filt : filters) filt.fillSelect(query, classGroup, session);

        // докинем Join ко всем классам, те которых не было FULL JOIN'ом остальные Join'ом
        for(ObjectImplement object : this) {

            if (object.baseClass instanceof IntegralClass) continue;

            // не было в фильтре
            // если есть remove'классы или новые объекты их надо докинуть
            JoinQuery<KeyField, PropertyField> objectQuery = tableFactory.objectTable.getClassJoin(object.gridClass);
            if(session !=null && session.changes.addClasses.contains(object.gridClass)) {
                // придется UnionQuery делать, ObjectTable'а Key и AddClass Object'а
                ChangeQuery<KeyField,PropertyField> resultQuery = new ChangeQuery<KeyField, PropertyField>(objectQuery.keys);

                resultQuery.add(objectQuery);

                // придется создавать запрос чтобы ключи перекодировать
                JoinQuery<KeyField, PropertyField> addQuery = new JoinQuery<KeyField, PropertyField>(objectQuery.keys);
                Join<KeyField,PropertyField> addJoin = new Join<KeyField, PropertyField>(tableFactory.addClassTable.getClassJoin(session, object.gridClass));
                addJoin.joins.put(tableFactory.addClassTable.object,addQuery.mapKeys.get(tableFactory.objectTable.key));
                addQuery.and(addJoin.inJoin);
                resultQuery.add(addQuery);

                objectQuery = resultQuery;
            }

            Join<KeyField, PropertyField> ObjectJoin = new Join<KeyField, PropertyField>(objectQuery);
            ObjectJoin.joins.put(tableFactory.objectTable.key, query.mapKeys.get(object));
            query.and(ObjectJoin.inJoin);

            if(session !=null && session.changes.removeClasses.contains(object.gridClass))
                tableFactory.removeClassTable.excludeJoin(query, session, object.gridClass, query.mapKeys.get(object));
        }
    }
}
