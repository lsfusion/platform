package lsfusion.gwt.client.form.classes.view;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.classes.GObjectClass;

import static com.google.gwt.safehtml.shared.SafeHtmlUtils.fromString;
import static lsfusion.gwt.client.base.GwtClientUtils.stopPropagation;

public abstract class ClassTreePanel extends Tree {
    private final GObjectClass defaultClass;

    private TreeItem defaultClassNode;

    public ClassTreePanel(GObjectClass baseClass, GObjectClass defaultClass) {
        super();

        sinkEvents(Event.ONDBLCLICK);

        this.defaultClass = defaultClass;

        addClassNode(null, baseClass);

        setSelectedItem(defaultClassNode);

        if (defaultClassNode != null) {
            setSelectedItem(defaultClassNode);
        }
    }

    private void addClassNode(TreeItem parentNode, GObjectClass objectClass) {
        final TreeItem classNode = parentNode == null
                                   ? addItem(fromString(objectClass.caption))
                                   : parentNode.addItem(fromString(objectClass.caption));
        classNode.setUserObject(objectClass);

        for (GObjectClass childClass : objectClass.children) {
            addClassNode(classNode, childClass);
        }

        classNode.setState(true);

        if (objectClass.ID == defaultClass.ID) {
            defaultClassNode = classNode;
        }
    }

    public GObjectClass getSelectedClass() {
        TreeItem selectedNode = getSelectedItem();
        if (selectedNode != null) {
            return (GObjectClass) selectedNode.getUserObject();
        }
        return null;
    }

    public abstract void classChosen();

    @Override
    public void onBrowserEvent(Event event) {
        if (event.getTypeInt() == Event.ONDBLCLICK) {
            stopPropagation(event);
            classChosen();
        }
        super.onBrowserEvent(event);
    }
}
