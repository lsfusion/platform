package lsfusion.server.form.instance;

public interface ReallyChanged {
    
    boolean containsChange(CalcPropertyObjectInstance instance);

    void addChange(CalcPropertyObjectInstance instance);
}
