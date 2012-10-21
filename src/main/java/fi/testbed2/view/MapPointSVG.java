package fi.testbed2.view;

import fi.testbed2.app.Logging;
import fi.testbed2.util.ColorUtil;

/**
 * Map point image as SVG.
 */
public class MapPointSVG {

    private String xmlContent;

    public MapPointSVG(String colorHex) {

        double opacity = ColorUtil.getOpacityFromARGB(colorHex);
        String colorWithoutAlpha = ColorUtil.getColorWithoutAlpha(colorHex);
        int strokeWidth = 20;

        Logging.debug("MapPoint opacity: "+opacity);
        Logging.debug("MapPoint color: "+colorWithoutAlpha);

        xmlContent = "<?xml version=\"1.0\"?>\n" +
                "<svg width=\"200\" height=\"200\" xmlns=\"http://www.w3.org/2000/svg\">\n" +
                " <g>\n" +
                "  <title>Layer 1</title>\n" +
                "  <g id=\"Layer_1\">\n" +
                "    <circle id=\"point_circle\" cx=\"100\" cy=\"100\" r=\"80\" stroke=\"#000000\" stroke-width=\""+strokeWidth+"\" fill=\""+colorWithoutAlpha+"\" fill-opacity=\""+opacity+"\" />\n" +
                "  </g>\n" +
                " </g>\n" +
                "</svg>";
    }

    public String getXmlContent() {
        return xmlContent;
    }

}
