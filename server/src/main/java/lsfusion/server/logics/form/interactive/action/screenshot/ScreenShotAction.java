package lsfusion.server.logics.form.interactive.action.screenshot;

import lsfusion.base.file.RawFileData;
import lsfusion.interop.action.OpenFileClientAction;
import lsfusion.interop.action.ScreenShotClientAction;
import lsfusion.interop.action.ScreenShotClientResult;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public class ScreenShotAction extends SystemExplicitAction {
    private final boolean html;
    private final boolean formRender;
    private final String containerSID;
    private final LP<?> targetProp;

    public ScreenShotAction(LocalizedString caption, boolean html, boolean formRender, String containerSID, LP<?> targetProp) {
        super(caption);
        this.html = html;
        this.formRender = formRender;
        this.containerSID = containerSID;
        this.targetProp = targetProp;
    }

    @Override
    protected void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        String targetSID = null;
        if (formRender || containerSID != null) {
            FormInstance formInstance = context.getFormInstance(false, false);
            if (formInstance == null)
                throw new RuntimeException("SCREENSHOT " + (formRender ? "FORM" : containerSID) + ": requires a current form context");

            ContainerView container;
            if (containerSID != null) {
                container = formInstance.entity.getRichDesign().getContainerBySID(containerSID);
                if (container == null)
                    throw new RuntimeException("SCREENSHOT: container '" + containerSID + "' not found in form '" + formInstance.entity.getCanonicalName() + "'");
            } else {
                container = formInstance.entity.getRichDesign().getMainContainer();
                if (container == null)
                    throw new RuntimeException("SCREENSHOT FORM: form '" + formInstance.entity.getCanonicalName() + "' has no main container");
            }
            targetSID = container.getSID();
        }

        ScreenShotClientResult result = (ScreenShotClientResult) context.requestUserInteraction(new ScreenShotClientAction(html, targetSID));
        if (result != null && result.data != null) {
            String extension = html ? "html" : "png";
            if (targetProp != null) {
                writeResult(targetProp, new RawFileData(result.data), extension, context, null);
            } else {
                context.requestUserInteraction(new OpenFileClientAction(new RawFileData(result.data), "screenshot", extension));
            }
        }
    }
}
