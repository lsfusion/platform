package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.RequiresResize;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;
import org.vectomatic.dom.svg.OMSVGDocument;
import org.vectomatic.dom.svg.OMSVGFEColorMatrixElement;
import org.vectomatic.dom.svg.OMSVGFilterElement;
import org.vectomatic.dom.svg.OMSVGSVGElement;
import org.vectomatic.dom.svg.utils.OMSVGParser;

import java.util.HashMap;
import java.util.Map;

public class GMap extends GSimpleStateTableView implements RequiresResize {

    public GMap(GFormController form, GGridController grid) {
        super(form, grid);
    }

    private JavaScriptObject map;
    private Map<GGroupObjectValue, JavaScriptObject> markers = new HashMap<>();
    @Override
    protected void render(Element renderElement, com.google.gwt.dom.client.Element recordElement, JsArray<JavaScriptObject> listObjects) {
        if(map == null) {
            map = createMap(renderElement);
            renderElement.getStyle().setProperty("zIndex", "0"); // need this because leaflet uses z-indexes and therefore dialogs for example are shown below layers
        }

        Map<GGroupObjectValue, JavaScriptObject> oldMarkers = new HashMap<>(markers);
        for(int i=0,size=listObjects.length();i<size;i++) {
            JavaScriptObject object = listObjects.get(i);
            GGroupObjectValue key = getKey(object);

            JavaScriptObject marker = oldMarkers.remove(key);
            if(marker == null) {
                marker = createMarker(map, recordElement, fromObject(key));
                markers.put(key, marker);
            }

            String filterID = createFilter(getMarkerColor(object));
            updateMarker(map, marker, object, filterID);
        }
        for(Map.Entry<GGroupObjectValue, JavaScriptObject> oldMarker : oldMarkers.entrySet()) {
            removeMarker(oldMarker.getValue());
            markers.remove(oldMarker.getKey());
        }
        
        fitBounds(map, GwtSharedUtils.toArray(markers.values()));
    }

    protected native JavaScriptObject createMap(com.google.gwt.dom.client.Element element)/*-{
        var L = $wnd.L;
        var map = L.map(element);

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        }).addTo(map);

        return map;            
    }-*/;

    protected native JavaScriptObject createMarker(JavaScriptObject map, com.google.gwt.dom.client.Element popupElement, JavaScriptObject key)/*-{
        var L = $wnd.L;

        var marker = L.marker([0, 0]).addTo(map);
        if (popupElement !== null)
            marker.bindPopup(popupElement, {maxWidth: Number.MAX_SAFE_INTEGER});
        var thisObject = this;
        marker.on('click', function (e) {
            thisObject.@GMap::changeSimpleGroupObject(*)(key);
        });
        return marker;
    }-*/;

    protected native void removeMarker(JavaScriptObject marker)/*-{
        marker.remove();
    }-*/;

    protected native void appendSVG(JavaScriptObject map, com.google.gwt.dom.client.Element svg)/*-{
        map._container.appendChild(svg)
    }-*/;

    protected native String getMarkerColor(JavaScriptObject element)/*-{
        return element.color;
    }-*/;

    protected String createFilter(String colorStr) {
        String svgID = null;
        if (colorStr != null) {
            int red = Integer.valueOf(colorStr.substring(1, 3), 16);
            int green = Integer.valueOf(colorStr.substring(3, 5), 16);
            int blue = Integer.valueOf(colorStr.substring(5, 7), 16);
            svgID = "svg_" + red + "_" + green + "_" + blue;

            com.google.gwt.dom.client.Element svgEl = Document.get().getElementById(svgID);
            if (svgEl == null) {
                OMSVGDocument doc = OMSVGParser.currentDocument();

                OMSVGSVGElement svgElement = doc.createSVGSVGElement();
                OMSVGFilterElement svgFilterElement = doc.createSVGFilterElement();
                svgFilterElement.setId(svgID);
                svgFilterElement.setAttribute("color-interpolation-filters", "sRGB");

                OMSVGFEColorMatrixElement svgfeColorMatrixElement = doc.createSVGFEColorMatrixElement();
                svgfeColorMatrixElement.setAttribute("type", "matrix");
                svgfeColorMatrixElement.setAttribute("values", (float) red / 256 + " 0 0 0  0 \n" +
                        (float) green / 256 + " 0 0 0  0  \n" +
                        (float) blue / 256 + " 0 0 0  0 \n" +
                        "0 0 0 1  0");
                svgFilterElement.appendChild(svgfeColorMatrixElement);
                svgElement.appendChild(svgFilterElement);

                appendSVG(map, svgElement.getElement());
            }
        }
        return svgID;
    }
    
    protected native void updateMarker(JavaScriptObject map, JavaScriptObject marker, JavaScriptObject element, String filterID)/*-{
        var L = $wnd.L;

        marker.setLatLng([element.latitude != null ? element.latitude : 0, element.longitude != null ? element.longitude : 0]);
        marker.setIcon(element.icon != null ? new L.Icon(Object.assign({}, L.Icon.Default.prototype.options, {
                iconUrl : element.icon
                })) : new L.Icon.Default());
        if (filterID) {
            var filterStyleValue = "url(#" + filterID + ")";
            if (map._loaded) {
                marker._icon.style.filter = filterStyleValue;
            } else { // marker._icon is undefined when the map is not loaded
                map.on('load', function (e) {
                    marker._icon.style.filter = filterStyleValue;
                });
            }
        }
    }-*/;

    protected native void fitBounds(JavaScriptObject map, JsArray<JavaScriptObject> markers)/*-{
        var L = $wnd.L;

        if(markers.length > 0)
            map.fitBounds(new L.featureGroup(markers).getBounds());
    }-*/;

    @Override
    public void onResize() {
        resized(map);
    }

    protected native void resized(JavaScriptObject map)/*-{
        if (map) {
            map._onResize();
        }
    }-*/;
}
