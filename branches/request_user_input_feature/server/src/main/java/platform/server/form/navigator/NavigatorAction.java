package platform.server.form.navigator;

import platform.server.logics.BusinessLogics;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.Property;

import javax.swing.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;

public class NavigatorAction<T extends BusinessLogics<T>> extends NavigatorElement<T> {
    private static ImageIcon image = new ImageIcon(NavigatorAction.class.getResource("/images/action.png"));

    private ActionProperty property;

    public NavigatorAction(String sID, String caption) {
        this(null, sID, caption);
    }

    public NavigatorAction(NavigatorElement<T> parent, String sID, String caption) {
        super(parent, sID, caption);
    }

    @Override
    public byte getTypeID() {
        return 2;
    }

    @Override
    public void serialize(DataOutputStream outStream, Collection<NavigatorElement> elements) throws IOException {
        super.serialize(outStream, elements);
    }

    @Override
    public ImageIcon getImage() {
        return image;
    }

    public void setProperty(Property property) {
        this.property = (ActionProperty) property;
    }

    public ActionProperty getProperty() {
        return property;
    }
}