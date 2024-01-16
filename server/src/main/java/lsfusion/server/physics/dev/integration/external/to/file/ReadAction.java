package lsfusion.server.physics.dev.integration.external.to.file;

import com.google.common.base.Throwables;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.file.FileData;
import lsfusion.base.file.RawFileData;
import lsfusion.base.file.ReadClientAction;
import lsfusion.base.file.ReadUtils;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.SystemAction;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.flow.FlowResult;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.classes.data.file.DynamicFormatFileClass;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.sql.SQLException;

public class ReadAction extends SystemAction {
    private final ExtraReadProcessor extraReadProcessor;
    private final LP<?> targetProp;
    private final boolean clientAction;
    private final boolean dialog;

    public ReadAction(LP<?> targetProp, boolean clientAction, boolean dialog) {
        super(LocalizedString.create("Read"), SetFact.singletonOrder(new PropertyInterface()));
        this.extraReadProcessor = new ExtraReadProcessor();
        this.targetProp = targetProp;
        this.clientAction = clientAction;
        this.dialog = dialog;
    }

    @Override
    protected ImMap<Property, Boolean> aspectChangeExtProps() {
        return getChangeProps(targetProp.property);
    }

    @Override
    protected FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        ObjectValue sourceProp = context.getSingleKeyValue();
        if(sourceProp instanceof DataObject) {
            assert sourceProp.getType() instanceof StringClass;
            String sourcePath = (String) ((DataObject) sourceProp).object;

            try {

                boolean isDynamicFormatFileClass = targetProp.property.getType() instanceof DynamicFormatFileClass;
                boolean isBlockingFileRead = Settings.get().isBlockingFileRead();
                ReadUtils.ReadResult readResult;
                if (clientAction) {
                    readResult = (ReadUtils.ReadResult) context.requestUserInteraction(new ReadClientAction(sourcePath, isDynamicFormatFileClass, isBlockingFileRead, dialog));
                } else {
                    readResult = ReadUtils.readFile(sourcePath, isDynamicFormatFileClass, isBlockingFileRead, false, extraReadProcessor);
                }
                if (readResult != null) {
                    if (isDynamicFormatFileClass)
                        targetProp.change((FileData) readResult.fileBytes, context);
                    else
                        targetProp.change((RawFileData) readResult.fileBytes, context);
                }
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
        return FlowResult.FINISH;
    }
}
