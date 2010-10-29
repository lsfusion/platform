package platform.client.descriptor.increment;

import platform.base.WeakIdentityHashSet;

import java.util.*;

public class IncrementDependency {

    private static int maxIndex = 0;
    private static WeakHashMap<IncrementView, Integer> viewOrders = new WeakHashMap<IncrementView, Integer>();
    private static void registerViewOrder(IncrementView viewOrder) {
        viewOrders.put(viewOrder, maxIndex++);
    }

    private final static Map<String, WeakHashMap<IncrementView, WeakIdentityHashSet<Object>>> mapViewObjects = new HashMap<String, WeakHashMap<IncrementView, WeakIdentityHashSet<Object>>>();
    public static void add(Object object, String field, IncrementView view) {
        WeakHashMap<IncrementView, WeakIdentityHashSet<Object>> viewObjects = mapViewObjects.get(field);
        if(viewObjects==null) {
            viewObjects = new WeakHashMap<IncrementView, WeakIdentityHashSet<Object>>();
            mapViewObjects.put(field, viewObjects);
        }

        WeakIdentityHashSet<Object> objects = viewObjects.get(view);
        if(objects==null) {
            objects = new WeakIdentityHashSet<Object>();
            viewObjects.put(view, objects);
        }

        registerViewOrder(view);

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

        registerViewOrder(view);

        views.add(view);

        view.update(null, field);
    }

    // метод который должен вызываться из setter'ов, add'
    public static void update(Object object, String field) {
        SortedMap<Integer, IncrementView> views = new TreeMap<Integer, IncrementView>();
        
        // обновляем прямые подписки
        WeakHashMap<IncrementView, WeakIdentityHashSet<Object>> fieldViewObjects = mapViewObjects.get(field);
        if(fieldViewObjects!=null)
            for(Map.Entry<IncrementView,WeakIdentityHashSet<Object>> fieldViewObject : fieldViewObjects.entrySet())
                if(fieldViewObject.getValue().contains(object)) {
                    Integer viewOrder = viewOrders.get(fieldViewObject.getKey());
                    if(viewOrder!=null)
                        views.put(viewOrder, fieldViewObject.getKey());
                }

        // обновляем общие подписи
        WeakIdentityHashSet<IncrementView> fieldViews = mapViews.get(field);
        if(fieldViews!=null)
            for(IncrementView fieldView : fieldViews) {
                Integer viewOrder = viewOrders.get(fieldView);
                if(viewOrder!=null)
                    views.put(viewOrder, fieldView);
            }

        for(IncrementView view : views.values())
            view.update(object, field);
    }
}
