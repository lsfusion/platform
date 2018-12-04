package lsfusion.gwt.form.server.logics;

import lsfusion.client.logics.ClientForm;
import lsfusion.client.logics.ClientFormChanges;
import lsfusion.client.serialization.ClientSerializationPool;
import lsfusion.gwt.form.server.convert.ClientComponentToGwtConverter;
import lsfusion.gwt.form.server.convert.ClientFormChangesToGwtConverter;
import lsfusion.gwt.form.server.form.spring.FormProvider;
import lsfusion.gwt.form.server.form.spring.FormSessionObject;
import lsfusion.gwt.form.server.logics.spring.LogicsProvider;
import lsfusion.gwt.form.server.navigator.spring.NavigatorProvider;
import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.shared.actions.form.FormAction;
import lsfusion.gwt.form.shared.actions.logics.LogicsAction;
import lsfusion.gwt.form.shared.view.GForm;
import lsfusion.gwt.form.shared.view.GFormUserPreferences;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.action.FormClientAction;
import lsfusion.interop.form.FormUserPreferences;
import lsfusion.interop.form.RemoteFormInterface;
import net.customware.gwt.dispatch.shared.Result;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public abstract class LogicsActionHandler<A extends LogicsAction<R>, R extends Result> extends lsfusion.gwt.base.server.dispatch.SimpleActionHandlerEx<A, R, RemoteLogicsInterface> {

    public LogicsActionHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    public LogicsProvider getLogicsProvider() {
        return ((LSFusionDispatchServlet)servlet).getBLProvider();
    }

}
