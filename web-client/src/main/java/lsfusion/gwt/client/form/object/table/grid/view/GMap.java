package lsfusion.gwt.client.form.object.table.grid.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.Element;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.base.jsni.NativeHashMap;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.object.table.grid.controller.GGridController;

import java.util.HashMap;
import java.util.Map;

public class GMap extends GSimpleStateTableView {

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

            updateMarker(marker, object);
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
            marker.bindPopup(popupElement);
        var thisObject = this;
        marker.on('click', function (e) {
            thisObject.@GMap::changeSimpleGroupObject(*)(key);
        });
        return marker;
    }-*/;

    protected native void removeMarker(JavaScriptObject marker)/*-{
        marker.remove();
    }-*/;

    protected native void updateMarker(JavaScriptObject marker, JavaScriptObject element)/*-{
        var L = $wnd.L;

        marker.setLatLng([element.latitude != null ? element.latitude : 0, element.longitude != null ? element.longitude : 0]);
        marker.setIcon(element.icon != null ? new L.Icon(Object.assign({}, L.Icon.Default.prototype.options, {
                iconUrl : element.icon
                })) : new L.Icon.Default());
    }-*/;

    protected native void fitBounds(JavaScriptObject map, JsArray<JavaScriptObject> markers)/*-{
        var L = $wnd.L;

        if(markers.length > 0)
            map.fitBounds(new L.featureGroup(markers).getBounds());
    }-*/;
}
