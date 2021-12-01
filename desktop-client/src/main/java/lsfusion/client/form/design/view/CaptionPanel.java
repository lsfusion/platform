package lsfusion.client.form.design.view;

import lsfusion.client.form.design.view.widget.Widget;

import java.awt.*;

public class CaptionPanel extends FlexPanel {
    public CaptionPanel(String caption, boolean vertical) {
        super(vertical);

        titledBorder = new TitledBorder(caption);
//                updateCaption();
        setBorder(titledBorder);
    }

    private TitledBorder titledBorder;

//        @Override
//        public boolean isValidateRoot() {
//            return isTopContainerView();
//        }

//        @Override
//        public void validate() {
//            if (isTopContainerView()) {
//                formLayout.preValidateMainContainer();
//            }
//            super.validate();
//        }
//
//        @Override
//        protected void validateTree() {
//            if (isTopContainerView()) {
//                formLayout.preValidateMainContainer();
//            }
//            super.validateTree();
//        }

    public void setCaption(String caption) {
//            titledBorder.setTitle(caption);
//            repaint()
        // we have to reset titled border, setTitle / repaint doesnt'work sonewhy
        titledBorder = caption != null ? new TitledBorder(caption) : null;
        setBorder(titledBorder);
    }
}
