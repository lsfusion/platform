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

    public Dimension getMaxPreferredSize() {
        return adjustMaxPreferredSize(AbstractClientContainerView.calculateMaxPreferredSize((Widget)getComponent(1)));
    }

    public Dimension adjustMaxPreferredSize(Dimension dimension) {
        return new Dimension(dimension.width + 5, dimension.height + /*legend.getOffsetHeight() + */5);
    }

    public void setCaption(String caption) {
//            titledBorder.setTitle(caption);
//            repaint()
        // we have to reset titled border, setTitle / repaint doesnt'work sonewhy
        titledBorder = caption != null ? new TitledBorder(caption) : null;
        setBorder(titledBorder);
    }
}
