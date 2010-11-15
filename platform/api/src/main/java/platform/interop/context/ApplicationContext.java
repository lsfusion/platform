package platform.interop.context;

public class ApplicationContext {

    private IncrementDependency incrementDependency = new IncrementDependency();

    public void addDependency(String field, IncrementView view) {
        incrementDependency.add(field, view);
    }

    public void addDependency(Object object, String field, IncrementView view) {
        incrementDependency.add(object, field, view);
    }

    public void removeDependency(String field, IncrementView view) {
        incrementDependency.remove(field, view);
    }

    public void removeDependency(Object object, String field, IncrementView view) {
        incrementDependency.remove(object, field, view);
    }

    public void updateDependency(Object object, String field) {
        incrementDependency.update(object, field);
    }
}

