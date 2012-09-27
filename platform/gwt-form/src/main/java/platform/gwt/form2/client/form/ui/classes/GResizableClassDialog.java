package platform.gwt.form2.client.form.ui.classes;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import platform.gwt.form2.client.MainFrameMessages;
import platform.gwt.form2.client.form.ui.dialog.GResizableModalWindow;
import platform.gwt.form2.client.form.ui.dialog.WindowHiddenHandler;
import platform.gwt.form2.shared.view.classes.GObjectClass;

public class GResizableClassDialog extends GResizableModalWindow {
    private static final MainFrameMessages messages = MainFrameMessages.Instance.get();

    private final boolean concreate;
    private final ClassChosenHandler classChosenHandler;

    private ClassTreePanel classPanel;

    private Button btnOk;
    private Button btnCancel;

    private GObjectClass chosenClass;

    public GResizableClassDialog(GObjectClass baseClass, GObjectClass defaultClass, boolean concreate, final ClassChosenHandler classChosenHandler) {
        super(messages.choseClass());

        this.concreate = concreate;
        this.classChosenHandler = classChosenHandler;

        configureLayout(baseClass, defaultClass);

        bindUIHandlers();
    }

    private void configureLayout(GObjectClass baseClass, GObjectClass defaultClass) {
        classPanel = new ClassTreePanel(baseClass, defaultClass);

        btnOk = new Button(messages.ok());
        btnCancel = new Button(messages.cancel());

        FlowPanel bottomPanel = new FlowPanel();
        bottomPanel.add(btnOk);
        bottomPanel.add(btnCancel);

        VerticalPanel bottomAlignedPanel = new VerticalPanel();
        bottomAlignedPanel.setWidth("100%");
        bottomAlignedPanel.add(bottomPanel);
        bottomAlignedPanel.setCellHorizontalAlignment(bottomPanel, HasAlignment.ALIGN_RIGHT);

        DockLayoutPanel centerPanel = new DockLayoutPanel(Style.Unit.PX);
        centerPanel.addSouth(bottomAlignedPanel, 36);
        centerPanel.add(classPanel);

        ResizeLayoutPanel mainPanel = new ResizeLayoutPanel();
        mainPanel.setPixelSize(500, 500);
        mainPanel.add(centerPanel);

        setContentWidget(mainPanel);
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

    public static GResizableClassDialog showDialog(GObjectClass baseClass, GObjectClass defaultClass, boolean concreate, ClassChosenHandler classChosenHandler) {
        GResizableClassDialog classDlg = new GResizableClassDialog(baseClass, defaultClass, concreate, classChosenHandler);
        classDlg.center();
        return classDlg;
    }
}
