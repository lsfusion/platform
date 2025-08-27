/**
 * BrowserPane.java
 * (c) Peter Bielik and Radek Burget, 2011-2012
 * <p>
 * SwingBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * SwingBox is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with SwingBox. If not, see <http://www.gnu.org/licenses/>.
 */

//modified from org.fit.cssbox.swingbox

package lsfusion.client.form.property.cell.classes.view.link;

import com.google.common.base.Throwables;
import lsfusion.base.ReflectionUtils;
import org.fit.cssbox.swingbox.SwingBoxDocument;
import org.fit.cssbox.swingbox.SwingBoxEditorKit;
import org.fit.cssbox.swingbox.SwingBoxViewFactory;
import org.fit.cssbox.swingbox.util.*;
import org.fit.cssbox.swingbox.util.GeneralEvent.EventType;
import org.fit.net.DataURLHandler;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.Provider;
import java.util.*;
import java.util.List;

public class BrowserPane extends JEditorPane {
    private static final long serialVersionUID = 7303652028812084960L;
    private InputStream loadingStream;
    private Hashtable<String, Object> pageProperties;
    private Document document;
    private static EditorKit swingBoxEditorKit = null;

    /**
     * Instantiates a new browser pane.
     */
    public BrowserPane() {
        super();
        init();
    }

    /**
     * Initial settings
     */
    protected void init() {
        // "support for SSL"
        String handlerPkgs = System.getProperty("java.protocol.handler.pkgs");
        if ((handlerPkgs != null) && !(handlerPkgs.isEmpty())) {
            handlerPkgs = handlerPkgs + "|com.sun.net.ssl.internal.www.protocol";
        } else {
            handlerPkgs = "com.sun.net.ssl.internal.www.protocol";
        }
        System.setProperty("java.protocol.handler.pkgs", handlerPkgs);

        Class providerClass = ReflectionUtils.classForName("com.sun.net.ssl.internal.ssl.Provider");
        if (providerClass != null) {
            try {
                java.security.Security.addProvider((Provider) providerClass.getDeclaredConstructor().newInstance());
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                     NoSuchMethodException ignored) {
            }
        }

        // Create custom EditorKit if needed
        if (swingBoxEditorKit == null) {
            swingBoxEditorKit = new SwingBoxEditorKit();
        }

        setEditable(false);
        setContentType("text/html");

        activateTooltip(true);

        Caret caret = getCaret();
        if (caret instanceof DefaultCaret)
            ((DefaultCaret) caret).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
    }

    @Override
    public Document getDocument() {
        return document;
    }

    @Override
    public void setDocument(Document document) {
        this.document = document;
        super.setDocument(document);
    }

    /**
     * Activates tooltips.
     *
     * @param show if true, shows tooltips.
     */
    public void activateTooltip(boolean show) {
        if (show) {
            ToolTipManager.sharedInstance().registerComponent(this);
        } else {
            ToolTipManager.sharedInstance().unregisterComponent(this);
        }
        ToolTipManager.sharedInstance().setEnabled(show);
    }

    /**
     * Fires general event. All registred listeners will be notified.
     *
     * @param e the event
     */
    public void fireGeneralEvent(GeneralEvent e) {
        Object[] listeners = listenerList.getListenerList();

        // notify those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == GeneralEventListener.class) {
                ((GeneralEventListener) listeners[i + 1]).generalEventUpdate(e);
            }
        }

    }

    /**
     * Renders current content to graphic context, which is returned. May return
     * null;
     *
     * @return the Graphics2D context
     * @see Graphics2D
     */
    public Graphics2D renderContent() {
        View view = null;
        ViewFactory factory = getEditorKit().getViewFactory();
        if (factory instanceof SwingBoxViewFactory) {
            view = ((SwingBoxViewFactory) factory).getViewport();
        }

        if (view != null) {
            int w = (int) view.getPreferredSpan(View.X_AXIS);
            int h = (int) view.getPreferredSpan(View.Y_AXIS);

            Rectangle rec = new Rectangle(w, h);

            BufferedImage img = new BufferedImage(w, h,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setClip(rec);
            view.paint(g, rec);

            return g;
        }

        return null;
    }

    /**
     * Renders current content to given graphic context, which is updated and
     * returned. Context must have set the clip, otherwise NullPointerException
     * is throwen.
     *
     * @param g the context to be rendered to.
     * @return the Graphics2D context
     * @see Graphics2D
     */
    public Graphics2D renderContent(Graphics2D g) {

        if (g.getClip() == null)
            throw new NullPointerException(
                    "Clip is not set on graphics context");
        ViewFactory factory = getEditorKit().getViewFactory();
        if (factory instanceof SwingBoxViewFactory) {
            View view = ((SwingBoxViewFactory) factory).getViewport();
            if (view != null) view.paint(g, g.getClip());
        }

        return g;
    }

    @Override
    public void setText(String t) {
        try {
            URL url = DataURLHandler.createURL(null, "data:text/html," + t);
            setPage(url);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void scrollToReference(String reference) {
        tryScrollToReference(reference);
    }

    /**
     * This method has the same purpose as {@link BrowserPane#scrollToReference(String)}.
     * However, it allows checking whether the reference exists in the document.
     *
     * @param reference the named location to scroll to
     */
    public void tryScrollToReference(String reference) {
        Element dst = findElementToScroll(reference, getDocument().getDefaultRootElement());
        if (dst != null) {
            try {
                Rectangle bottom = new Rectangle(0, getHeight() - 1, 1, 1);
                Rectangle rec = modelToView(dst.getStartOffset());
                if (rec != null) {
                    scrollRectToVisible(bottom); //move to the bottom and back in order to put the reference to the window top
                    scrollRectToVisible(rec);
                }
            } catch (BadLocationException e) {
                UIManager.getLookAndFeel().provideErrorFeedback(this);
            }
        }
    }

    private Element findElementToScroll(String ref, Element root) {
        String name = (String) root.getAttributes().getAttribute(SwingBoxDocument.ElementNameAttribute);
        if (!Constants.BACKGROUND.equals(name)) //do not consider backgrounds
        {
            //try the id attribute
            String eid = (String) root.getAttributes().getAttribute(Constants.ATTRIBUTE_ELEMENT_ID);
            if (ref.equalsIgnoreCase(eid)) {
                return root;
            }
            //or try the name attribute of <a>
            else {
                Anchor anchor = (Anchor) root.getAttributes().getAttribute(Constants.ATTRIBUTE_ANCHOR_REFERENCE);
                if (anchor != null && anchor.isActive()) {
                    if (anchor.getProperties().get(Constants.ELEMENT_A_ATTRIBUTE_NAME).equals(ref))
                        return root;
                }
            }
        }

        int n = root.getElementCount();
        Element child;
        for (int i = 0; i < n; i++) {
            if ((child = findElementToScroll(ref, root.getElement(i))) != null)
                return child;
        }
        return null;
    }

    @Override
    public EditorKit getEditorKitForContentType(String type) {
        if (type.equalsIgnoreCase("text/html") || type.equalsIgnoreCase("application/xhtml+xml")
                || type.equalsIgnoreCase("text/xhtml")) {
            return swingBoxEditorKit;
        } else {
            return super.getEditorKitForContentType(type);
        }
    }

    @Override
    protected InputStream getStream(URL page) throws IOException {
        final URLConnection conn = setConnectionProperties(page.openConnection());
        // http://stackoverflow.com/questions/875467/java-client-certificates-over-https-ssl

        if (conn instanceof HttpsURLConnection) {
            // XXX toto moc nefunguje
            System.out.println("$ Connection is HTTPS !!");
        } else if (conn instanceof HttpURLConnection) {
            HttpURLConnection hconn = (HttpURLConnection) conn;
            hconn.setInstanceFollowRedirects(false);
            Object postData = getPostData();
            if (postData != null) {
                handlePostData(hconn, postData);
            }
            int response = hconn.getResponseCode();
            boolean redirect = (response >= 300 && response <= 399);

            /*
             * In the case of a redirect, we want to actually change the URL
             * that was input to the new, redirected URL
             */
            if (redirect) {
                String loc = conn.getHeaderField("Location");
                if (loc.startsWith("http")) {
                    page = new URL(loc);
                } else {
                    page = new URL(page, loc);
                }
                return getStream(page);
            }
        }

        // Connection properties handler should be forced to run on EDT,
        // as it instantiates the EditorKit.
        if (SwingUtilities.isEventDispatchThread()) {
            handleConnectionProperties(conn);
        } else {
            try {
                SwingUtilities.invokeAndWait(() -> handleConnectionProperties(conn));
            } catch (InterruptedException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
        return conn.getInputStream();
    }

    @Override
    public void setPage(final URL newPage) throws IOException {
        fireGeneralEvent(new GeneralEvent(this, EventType.page_loading_begin,
                newPage, null));

        if (newPage == null) {
            // TODO fire general event here
            throw new IOException("invalid url");
        }
        final URL oldPage = getPage();
        Object postData = getPostData();

        if ((oldPage == null) || !oldPage.sameFile(newPage) || (postData != null)) {
            // different url or POST method, load the new content

            final InputStream in = getStream(newPage);
            // editor kit is set according to content type
            EditorKit kit = getEditorKit();

            if (kit == null) {
                UIManager.getLookAndFeel().provideErrorFeedback(this);
            } else {
                document = createDocument(kit, newPage);

                int p = getAsynchronousLoadPriority(document);

                if (p < 0) {
                    // load synchro
                    loadPage(newPage, oldPage, in, document);
                } else {
                    // load asynchro
                    Thread t = new Thread(() -> loadPage(newPage, oldPage, in, document));
                    t.setDaemon(true);
                    t.start();
                }
            }
        } else if (oldPage.sameFile(newPage)) {
            if (newPage.getRef() != null) {
                final String reference = newPage.getRef();
                SwingUtilities.invokeLater(() -> scrollToReference(reference));
            }
        }


    }

    private void loadPage(final URL newPage, final URL oldPage, final InputStream in, final Document doc) {
        boolean done = false;
        try {

            synchronized (this) {
                if (loadingStream != null) {
                    // we are loading asynchronously, so we need to cancel
                    // the old stream.
                    loadingStream.close();
                }

                loadingStream = in;
            }

            // read the content
            read(loadingStream, doc);
            // set the document to the component
            setDocument(doc);

            final String reference = newPage.getRef();
            // Have to scroll after painted.
            SwingUtilities.invokeLater(() -> {
                scrollRectToVisible(new Rectangle(0, 0, 1, 1)); // top of the pane
                if (reference != null)
                    scrollToReference(reference);
            });

            done = true;

        } catch (IOException ioe) {
            UIManager.getLookAndFeel().provideErrorFeedback(this);
        } finally {
            synchronized (this) {
                if (loadingStream != null) {
                    try {
                        loadingStream.close();
                    } catch (IOException ignored) {
                    }
                }
                loadingStream = null;
            }

            if (done) {
                SwingUtilities.invokeLater(() -> firePropertyChange("page", oldPage, newPage));
            }
        }

    }

    private Document createDocument(EditorKit kit, URL page) {
        // we have pageProperties, because we can be in situation that
        // old page is beeing removed & new page is not yet created...
        // we need somewhere store important data.
        Document doc = kit.createDefaultDocument();
        if (pageProperties != null) {
            // transfer properties discovered in stream to the
            // document property collection.
            for (Enumeration<String> e = pageProperties.keys(); e
                    .hasMoreElements(); ) {
                Object key = e.nextElement();
                doc.putProperty(key, pageProperties.get(key));
            }
        }
        if (doc.getProperty(Document.StreamDescriptionProperty) == null) {
            doc.putProperty(Document.StreamDescriptionProperty, page);
        }
        return doc;
    }

    private int getAsynchronousLoadPriority(Document doc) {
        return (doc instanceof AbstractDocument ? ((AbstractDocument) doc)
                .getAsynchronousLoadPriority() : -1);
    }

    private Object getPostData() {
        return getDocument().getProperty(Constants.PostDataProperty);
    }

    /**
     * Handle URL connection properties (most notably, content type).
     */
    private void handleConnectionProperties(URLConnection conn) {
        if (pageProperties == null) {
            pageProperties = new Hashtable<>(22);
        }

        String type = conn.getContentType();
        if (type != null) {
            // XXX mozno prepisat podla seba, setContentType, len pre text/****
            setContentType(type); // >> XXX putClientProperty("charset",
            // charset); !!!
            // charset\s*=[\s'"]*([\-_a-zA-Z0-9]+)[\s'",;]*
            // pageProperties.put("content-type", type);
        }

        pageProperties.put(Document.StreamDescriptionProperty, conn.getURL());

        Map<String, List<String>> header = conn.getHeaderFields();

        Set<String> keys = header.keySet();
        Object obj;
        for (String key : keys) {
            obj = header.get(key);
            if (key != null && obj != null) {
                pageProperties.put(key, obj);
            }
        }

        System.out.println("# pageProperties #");
        for (String k : pageProperties.keySet()) {
            System.out.println(k + " : " + pageProperties.get(k));
        }

    }

    private URLConnection setConnectionProperties(URLConnection conn) {
        // http://www.useragentstring.com/index.php
        // http://tools.ietf.org/html/rfc1945
        // Opera 11.50 : Opera/9.80 (X11; Linux i686; U; sk) Presto/2.9.168
        // Version/11.50
        // CSSBox : Mozilla/5.0 (compatible; BoxBrowserTest/2.x; Linux)
        // CSSBox/2.x (like Gecko)
        // FireFox : Mozilla/5.0 (Windows; U; Windows NT 6.0; en-US; rv:1.9b5)
        // Gecko/2008032620 Firefox/3.0b5
        // IE8 : Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0;
        // .NET CLR 2.0.50727; .NET CLR 1.1.4322; .NET CLR 3.0.04506.30; .NET
        // CLR 3.0.04506.648)
        // SwingBox : Mozilla/5.0 (compatible; SwingBox/1.x; Linux; U)
        // CSSBox/2.x (like Gecko)
        /*
         * An unofficial format, based on the above, used by Web browsers is as
         * follows: Mozilla/[version] ([system and browser information])
         * [platform] ([platform details]) [extensions]. For example, Safari on
         * the iPad has used the following: Mozilla/5.0 (iPad; U; CPU OS 3_2_1
         * like Mac OS X; en-us) AppleWebKit/531.21.10 (KHTML, like Gecko)
         * Mobile/7B405.
         */

        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (compatible; SwingBox/1.x; Linux; U) CSSBox/4.x (like Gecko)");
        conn.setRequestProperty("Accept-Charset", "utf-8");

        return conn;
    }

    private void handlePostData(HttpURLConnection conn, Object postData)
            throws IOException {
        conn.setDoOutput(true);
        DataOutputStream os = null;
        try {
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");
            os = new DataOutputStream(conn.getOutputStream());
            os.writeBytes((String) postData);
        } finally {
            if (os != null) {
                os.close();
            }
        }
    }

    @Override
    public void read(InputStream in, Object desc) throws IOException {
        super.read(in, desc); // !!! na toto sa tiez pozriet
    }

    void read(InputStream in, Document doc) throws IOException {
        EditorKit kit = getEditorKit();

        try {
            kit.read(in, doc, 0);

        } catch (BadLocationException ble) {
            throw new IOException(ble);
        }

    }

}
