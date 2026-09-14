package lsfusion.gwt.client.base.view;

// the panel a forms window puts its forms in - the standard tab strip's, the single-form view's. The flex layout
// treats it as one thing: it manages its own children, showing one of them and hiding the rest, so the line pass
// leaves it alone; and it is a container in its own right, so it gets the border and the padding of one.
// A form's OWN tabs are a FlexTabbedPanel too and are none of this, which is why the marker is not that class
public interface FormsContainerPanel {
}
