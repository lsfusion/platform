package lsfusion.interop.form.layout;

public abstract class ContainerAdder<C extends AbstractContainer<T, Str>, T extends AbstractComponent, Str> {
    
    private final static ContainerAdder DEFAULT = new ContainerAdder() {
        public void add(AbstractContainer container, AbstractComponent component) {
            container.add(component);
        }};            
    public static <C extends AbstractContainer<T, Str>, T extends AbstractComponent, Str> ContainerAdder<C, T, Str> DEFAULT() {
        return DEFAULT; 
    }  
    
    public abstract void add(C container, T component);
}
