package lsfusion.base.context;

import lsfusion.base.identity.DefaultIDGenerator;
import lsfusion.base.identity.IDGenerator;

public class ApplicationContext {

    private IncrementDependency incrementDependency = new IncrementDependency();
    private Lookup lookup = new Lookup();
    private IDGenerator idGenerator = new DefaultIDGenerator();

    public void addDependency(String field, IncrementView view) {
        incrementDependency.add(field, view);
    }

    public void addDependency(Object object, String field, IncrementView view) {
        incrementDependency.add(object, field, view);
    }

    public void removeDependency(String field, IncrementView view) {
        incrementDependency.remove(field, view);
    }

    public void removeDependency(IncrementView view) {
        incrementDependency.remove(view);
    }

    public void removeDependency(Object object, String field, IncrementView view) {
        incrementDependency.remove(object, field, view);
    }

    public void updateDependency(Object object, String field) {
        incrementDependency.update(object, field);
    }

    public void setProperty(String name, Object object) {
        lookup.setProperty(name, object);
    }

    public Object getProperty(String name) {
        return lookup.getProperty(name);
    }

    public void addLookupResultChangeListener(String name, Lookup.LookupResultChangeListener listener) {
        lookup.addLookupResultChangeListener(name, listener);
    }

    public void removeLookupResultChangeListener(String name, Lookup.LookupResultChangeListener listener) {
        lookup.removeLookupResultChangeListener(name, listener);
    }

    public void idRegister(int ID) {
        idGenerator.idRegister(ID);
    }

    public int idShift(int ID) {
        return idGenerator.idShift(ID);
    }

    public int idShift() {
        return idGenerator.idShift();
    }
}

