package lsfusion.client.form.design.view;

public class CaptionPanel extends FlexPanel {
    public CaptionPanel(String caption, boolean vertical) {
        super(vertical);

        titledBorder = createBorder(caption);
//                updateCaption();
        setBorder(titledBorder);
        addMouseListener(titledBorder);
        addMouseMotionListener(titledBorder);
    }

    protected TitledBorder titledBorder;
    
    protected TitledBorder createBorder(String caption) {
        return new TitledBorder(caption);
    }

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
