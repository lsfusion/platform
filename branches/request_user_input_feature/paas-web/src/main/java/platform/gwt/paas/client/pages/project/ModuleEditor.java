package platform.gwt.paas.client.pages.project;

import com.gwtplatform.dispatch.shared.DispatchAsync;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.ViewLoader;
import com.smartgwt.client.widgets.WidgetCanvas;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import platform.gwt.codemirror.client.CodeMirror;
import platform.gwt.paas.client.Paas;
import platform.gwt.paas.client.common.ErrorHandlingCallback;
import platform.gwt.paas.client.widgets.Toolbar;
import platform.gwt.paas.shared.actions.GetModuleTextAction;
import platform.gwt.paas.shared.actions.GetModuleTextResult;
import platform.gwt.paas.shared.actions.UpdateModulesAction;
import platform.gwt.paas.shared.actions.VoidResult;

public class ModuleEditor extends HLayout {
    private final DispatchAsync dispatcher = Paas.ginjector.getDispatchAsync();

    private Toolbar toolbar;

    private ViewLoader loader;
    private VLayout mainPane;

    private CodeMirror codeMirror;

    private final int moduleId;

    public ModuleEditor(final int moduleId) {
        this.moduleId = moduleId;

        setOverflow(Overflow.HIDDEN);

        loader = new ViewLoader();
        loader.setIcon("loading.gif");
        loader.setLoadingMessage("Загрузка...");

        toolbar = new Toolbar();
        toolbar.addToolStripButton("save.png", "Save", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                saveModule();
            }
        });

        toolbar.addToolStripButton("refresh.png", "Refresh", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                updateModuleText();
            }
        });

        codeMirror = new CodeMirror();

        WidgetCanvas codeMirrorCanvas = new WidgetCanvas(codeMirror);
        codeMirrorCanvas.setWidth100();
        codeMirrorCanvas.setHeight100();

        mainPane = new VLayout();
        mainPane.addMember(toolbar);
        mainPane.addMember(codeMirrorCanvas);

        addMember(mainPane);

        updateModuleText();
    }

    private void saveModule() {
        dispatcher.execute(new UpdateModulesAction(moduleId, getCurrentText()), new ErrorHandlingCallback<VoidResult>() {
            @Override
            public void success(VoidResult result) {
                SC.say("Saved successfully!");
            }
        });
    }

    public void showLoader() {
        removeMember(mainPane);
        addMember(loader);
    }

    public void hideLoader() {
        removeMember(loader);
        addMember(mainPane);
    }

    public void setModuleText(String text) {
        codeMirror.setValue(text);
    }

    public String getCurrentText() {
        return codeMirror.getValue();
    }

    public void updateModuleText() {
        showLoader();
        dispatcher.execute(new GetModuleTextAction(moduleId), new ErrorHandlingCallback<GetModuleTextResult>() {
            @Override
            public void preProcess() {
                hideLoader();
            }

            @Override
            public void success(GetModuleTextResult result) {
                setModuleText(result.text);
            }
        });
    }
}
