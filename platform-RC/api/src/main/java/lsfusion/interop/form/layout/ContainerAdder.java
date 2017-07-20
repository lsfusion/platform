package lsfusion.interop.form.layout;

public abstract class ContainerAdder<C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>> {
    
    private final static ContainerAdder DEFAULT = new ContainerAdder() {
        public void add(AbstractContainer container, AbstractComponent component) {
            container.add(component);
        }};            
    public static <C extends AbstractContainer<C, T>, T extends AbstractComponent<C, T>> ContainerAdder<C, T> DEFAULT() {
        return DEFAULT; 
    }  
    
    public abstract void add(C container, T component);
}
