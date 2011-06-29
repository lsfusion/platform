package platform.gwt.view.classes;

import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.events.FormItemClickHandler;
import com.smartgwt.client.widgets.form.fields.events.FormItemIconClickEvent;
import platform.gwt.view.GPropertyDraw;
import platform.gwt.view.logics.FormLogicsProvider;
import platform.gwt.view.logics.SelectObjectCallback;

public class GObjectType extends GType {
    public static final GObjectType instance = new GObjectType();

    public FormItem createPanelFormItem(FormLogicsProvider formLogics, GPropertyDraw property) {
        return new DialogFormItem(formLogics, property);
    }

    private static class DialogFormItem extends FormItem {
        public DialogFormItem(final FormLogicsProvider formLogics, final GPropertyDraw property) {
            setAttribute("readOnly", true);
            setSelectOnFocus(false);

            FormItemIcon formItemIcon = new FormItemIcon();
            formItemIcon.setSrc("edit_object.png");
            setIcons(formItemIcon);

            formItemIcon.addFormItemClickHandler(new FormItemClickHandler() {
                @Override
                public void onFormItemClick(final FormItemIconClickEvent event) {
                    formLogics.selectObject(property, new SelectObjectCallback() {
                        @Override
                        public void objectSelected(Object selectedValue, Object displayValue) {
                            event.getItem().setValue(displayValue);
                        }
                    });
                }
            });
        }
    }

//    private static class DialogFormItem extends CanvasItem {
//
//        private TextItem tiValue;
//        private Button btnSelect;
//        private HStack mainPane;
//
//        public DialogFormItem(final FormLogicsProvider formLogics, final GPropertyDraw property) {
//            setShouldSaveValue(true);
//            setCanFocus(true);
//
//            tiValue = new TextItem();
//            tiValue.setShowTitle(false);
//            tiValue.setAttribute("readOnly", "true");
//            FormItemIcon formItemIcon = new FormItemIcon();
//            formItemIcon.setSrc("edit_object.png");
//            tiValue.setIcons(formItemIcon);
//
//            DynamicForm valueForm = new DynamicForm();
//            valueForm.setNumCols(1);
//            valueForm.setHeight(1);
//            valueForm.setWidth("*");
//            valueForm.setOverflow(Overflow.VISIBLE);
//            valueForm.setFields(tiValue);
//
//            btnSelect = new Button();
//            btnSelect.setIcon("edit_object.png");
//            btnSelect.setWidth(20);
//            btnSelect.setHeight(20);
//            btnSelect.setLayoutAlign(VerticalAlignment.CENTER);
//            btnSelect.setShowRollOverIcon(false);
//            btnSelect.setShowDownIcon(false);
//            btnSelect.setShowDown(false);
//            btnSelect.addClickHandler(new ClickHandler() {
//                @Override
//                public void onClick(ClickEvent event) {
//                    formLogics.selectObject(property, new SelectObjectCallback() {
//                        @Override
//                        public void objectSelected(Object selectedValue, Object displayValue) {
//                            storeValue(displayValue);
//                        }
//                    });
//                }
//            });
//
//            formItemIcon.addFormItemClickHandler(new FormItemClickHandler() {
//                @Override
//                public void onFormItemClick(FormItemIconClickEvent event) {
//                    formLogics.selectObject(property, new SelectObjectCallback() {
//                        @Override
//                        public void objectSelected(Object selectedValue, Object displayValue) {
//                            storeValue(displayValue);
//                        }
//                    });
//                }
//            });
//
//
//            mainPane = new HStack();
//            mainPane.setWidth(1);
//            mainPane.setHeight(1);
//            mainPane.setOverflow(Overflow.VISIBLE);
//            mainPane.addMember(valueForm);
////            mainPane.addMember(btnSelect);
//
//            setCanvas(mainPane);
//
//            addShowValueHandler(new ShowValueHandler() {
//                @Override
//                public void onShowValue(ShowValueEvent event) {
//                    DialogFormItem item = (DialogFormItem) event.getSource();
//                    item.tiValue.setValue(item.getValue());
//                }
//            });
//
//            addFocusHandler(new FocusHandler() {
//                @Override
//                public void onFocus(FocusEvent event) {
//                    DialogFormItem item = (DialogFormItem) event.getSource();
//                    item.tiValue.focusInItem();
//                }
//            });
//        }
//    }
}
