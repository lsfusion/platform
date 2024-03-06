package lsfusion.client.navigator;

import lsfusion.base.file.AppFileDataImage;
import lsfusion.base.file.AppImage;
import lsfusion.client.form.property.cell.classes.view.ImagePropertyRenderer;

public class ClientImageElementNavigator extends ClientElementNavigator {

    public ClientImageElementNavigator(String canonicalName) {
        super(canonicalName);
    }

    public void update(ClientNavigatorElement rootElement, Object value) {
        ClientNavigatorElement navigatorElement = rootElement.findElementByCanonicalName(canonicalName);
        if(value instanceof AppFileDataImage)
            navigatorElement.fileImage = ImagePropertyRenderer.convertValue((AppFileDataImage) value);
        else
            navigatorElement.appImage = (AppImage) value;
    }
}