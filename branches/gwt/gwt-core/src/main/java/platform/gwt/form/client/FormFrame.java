package platform.gwt.form.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStripButton;
import net.customware.gwt.dispatch.client.DefaultExceptionHandler;
import platform.gwt.base.client.ui.ToolStripPanel;
import platform.gwt.form.client.ui.GGroupObjectController;
import platform.gwt.form.client.ui.LoadingWindow;
import platform.gwt.form.shared.actions.GetForm;
import platform.gwt.form.shared.actions.GetFormResult;
import platform.gwt.form.shared.actions.form.ChangeGroupObject;
import platform.gwt.form.shared.actions.form.FormChangesResult;
import platform.gwt.form.shared.actions.form.GetRemoteChanges;
import platform.gwt.view.GForm;
import platform.gwt.view.GGroupObject;
import platform.gwt.view.changes.GFormChanges;
import platform.gwt.view.changes.GGroupObjectValue;
import platform.gwt.view.changes.dto.GFormChangesDTO;

import java.util.HashMap;

public class FormFrame extends HLayout implements EntryPoint {
    private final FormDispatchAsync dispatcher = new FormDispatchAsync(new DefaultExceptionHandler());

    private GForm form;

    public void onModuleLoad() {
        dispatcher.execute(new GetForm(getFormSID()), new AsyncCallback<GetFormResult>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.warn("Ошибка при попытке открыть форму: " + caught.getMessage());
            }

            @Override
            public void onSuccess(GetFormResult result) {
                initialize(result.form);
                draw();
            }
        });
    }

    public String getFormSID() {
        try {
            Dictionary dict = Dictionary.getDictionary("parameters");
            return dict.get("formSID");
        } catch (Exception ignored) {
        }

        try {
            return Window.Location.getParameter("formSID");
        } catch (Exception ignored) {
        }

        return null;
    }

    private HashMap<GGroupObject, GGroupObjectController> controllers = new HashMap<GGroupObject, GGroupObjectController>();

    private void initialize(GForm form) {
        this.form = form;
        dispatcher.setForm(form);

        setWidth100();
        setHeight100();
        setAutoHeight();

        VLayout main = new VLayout();
        main.setWidth100();
        main.setHeight100();
        main.setAutoHeight();

        main.addMember(new ToolStripPanel("form.png", form.caption) {
            @Override
            protected void addButtonsAfterLocaleChooser() {
                addSeparator();

                ToolStripButton logoffBtn = new ToolStripButton();
                logoffBtn.setIcon("refresh.png");
                logoffBtn.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        refreshData();
                    }
                });

                addMember(logoffBtn);
            }
        });

        SectionStack groupsStack = new SectionStack();
        groupsStack.setAnimateSections(false);
        groupsStack.setMargin(20);
        groupsStack.setVisibilityMode(VisibilityMode.MULTIPLE);
        groupsStack.setCanResizeSections(true);
        groupsStack.setOverflow(Overflow.VISIBLE);

        for (GGroupObject group : form.groupObjects) {
            GGroupObjectController controller = new GGroupObjectController(this, form, group);
            groupsStack.addSection(controller.getSection());

            controllers.put(group, controller);
        }
        main.addMember(groupsStack);

        addMember(main);

        applyRemoteChanges(form.changes);
    }

    public void applyRemoteChanges(GFormChangesDTO changesDTO) {
        GFormChanges fc = GFormChanges.remap(form, changesDTO);
        for (GGroupObjectController controller : controllers.values()) {
            controller.processFormChanges(fc);
        }
    }

    private void refreshData() {
        dispatcher.execute(new GetRemoteChanges(), new FormChangesBlockingCallback());
    }

    public void changeGroupObject(GGroupObject group, GGroupObjectValue key) {
        dispatcher.execute(new ChangeGroupObject(group.ID, key.getValues(group)), new FormChangesBlockingCallback());
    }

    private class FormChangesBlockingCallback implements AsyncCallback<FormChangesResult> {
        private final LoadingWindow wl;

        public FormChangesBlockingCallback() {
            this.wl = LoadingWindow.showLoadingBlocker();
        }

        @Override
        public void onFailure(Throwable t) {
            SC.warn("Ошибка во время чтения данных с сервера: " + t.getMessage());
        }

        @Override
        public void onSuccess(FormChangesResult result) {
            applyRemoteChanges(result.changes);
            wl.destroy();
        }
    }
}
