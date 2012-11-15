package platform.gwt.form.client.form.ui.classes;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import platform.gwt.form.client.MainFrameMessages;
import platform.gwt.form.client.form.ui.dialog.GModalWindow;
import platform.gwt.form.client.form.ui.dialog.WindowHiddenHandler;
import platform.gwt.form.shared.view.classes.GObjectClass;

public class GClassDialog extends GModalWindow {
    private static final MainFrameMessages messages = MainFrameMessages.Instance.get();

    private final boolean concreate;
    private final ClassChosenHandler classChosenHandler;

    private ClassTreePanel classPanel;

    private Button btnOk;
    private Button btnCancel;

    private GObjectClass chosenClass;

    public GClassDialog(GObjectClass baseClass, GObjectClass defaultClass, boolean concreate, final ClassChosenHandler classChosenHandler) {
        super(messages.choseClass());

        this.concreate = concreate;
        this.classChosenHandler = classChosenHandler;

        configureLayout(baseClass, defaultClass);

        bindUIHandlers();
    }

    private void configureLayout(GObjectClass baseClass, GObjectClass defaultClass) {
        classPanel = new ClassTreePanel(baseClass, defaultClass);
        classPanel.setPixelSize(500, 500);

        btnOk = new Button(messages.ok());
        btnCancel = new Button(messages.cancel());

        FlowPanel bottomPane = new FlowPanel();
        bottomPane.add(btnOk);
        bottomPane.add(btnCancel);

        VerticalPanel mainPane = new VerticalPanel();
        mainPane.add(classPanel);
        mainPane.add(bottomPane);
        mainPane.setCellHorizontalAlignment(bottomPane, HasAlignment.ALIGN_RIGHT);

        setWidget(mainPane);
    }

    private void bindUIHandlers() {
        btnOk.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                GObjectClass selectedClass = classPanel.getSelectedClass();
                if (selectedClass != null) {
                    chooseClass(selectedClass);
                }
            }
        });

        btnCancel.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                chooseClass(null);
            }
        });

        setWindowHiddenHandler(new WindowHiddenHandler() {
            @Override
            public void onHidden() {
                classChosenHandler.onClassChosen(chosenClass);
            }
        });
    }

    private void chooseClass(GObjectClass chosenClass) {
        if (chosenClass == null || !concreate || chosenClass.concreate) {
            this.chosenClass = chosenClass;
            hide();
        }
    }

    public static GClassDialog showDialog(GObjectClass baseClass, GObjectClass defaultClass, boolean concreate, ClassChosenHandler classChosenHandler) {
        GClassDialog classDlg = new GClassDialog(baseClass, defaultClass, concreate, classChosenHandler);
        classDlg.center();
        return classDlg;
    }
}
