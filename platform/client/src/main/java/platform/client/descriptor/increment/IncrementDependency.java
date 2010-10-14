package platform.client.descriptor.increment;

import platform.base.WeakIdentityHashSet;
import platform.base.WeakIdentityHashMap;

import java.util.*;

public class IncrementDependency {

    private final static Map<String, WeakHashMap<IncrementView, Collection<Object>>> mapViewObjects = new HashMap<String, WeakHashMap<IncrementView, Collection<Object>>>();
    public static void add(Object object, String field, IncrementView view) {
        WeakHashMap<IncrementView, Collection<Object>> viewObjects = mapViewObjects.get(field);
        if(viewObjects==null) {
            viewObjects = new WeakHashMap<IncrementView, Collection<Object>>();
            mapViewObjects.put(field, viewObjects);
        }

        Collection<Object> objects = viewObjects.get(view);
        if(objects==null) {
            objects = new ArrayList<Object>();
            viewObjects.put(view, objects);
        }
        objects.add(object);

        view.update(object, field);
    }

    private final static Map<String, WeakIdentityHashSet<IncrementView>> mapViews = new HashMap<String, WeakIdentityHashSet<IncrementView>>();
    public static void add(String field, IncrementView view) {
        WeakIdentityHashSet<IncrementView> views = mapViews.get(field);
        if(views==null) {
            views = new WeakIdentityHashSet<IncrementView>();
            mapViews.put(field, views);
        }
        views.add(view);

        view.update(null, field);
    }

    // метод который должен вызываться из setter'ов, add'
    public static void update(Object object, String field) {
        Set<IncrementView> views = new HashSet<IncrementView>();
        
        // обновляем прямые подписки
        WeakHashMap<IncrementView, Collection<Object>> fieldViewObjects = mapViewObjects.get(field);
        if(fieldViewObjects!=null)
            for(Map.Entry<IncrementView,Collection<Object>> fieldViewObject : fieldViewObjects.entrySet())
                if(fieldViewObject.getValue().contains(object))
                    views.add(fieldViewObject.getKey());

        // обновляем общие подписи
        WeakIdentityHashSet<IncrementView> fieldViews = mapViews.get(field);
        if(fieldViews!=null)
            for(IncrementView fieldView : fieldViews)
                views.add(fieldView);

        for(IncrementView view : views)
            view.update(object, field);
    }
}
