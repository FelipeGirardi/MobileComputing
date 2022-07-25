package mobile.tracker;

import android.location.Location;
import android.util.Xml;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class GPXWriter {
    private final XmlSerializer xmlSerializer;
    private final DateFormat dateFormat;

    public GPXWriter(File file) {
        FileOutputStream stream = null;
        xmlSerializer = Xml.newSerializer();

        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        dateFormat.setTimeZone(timeZone);

        try {
            stream = new FileOutputStream(file);
            //StringWriter writer = new StringWriter();
            //xmlSerializer.setOutput(writer);
            xmlSerializer.setOutput(stream, "UTF-8");
            xmlSerializer.startDocument("UTF-8", true);

            xmlSerializer.startTag(null, "gpx");
            xmlSerializer.attribute(null, "xmlns", "http://www.topografix.com/GPX/1/1");
            xmlSerializer.attribute(null, "version", "1.1");
            xmlSerializer.attribute(null, "creator", "Jonas Horstmann");
            xmlSerializer.attribute(null, "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            xmlSerializer.attribute(null, "xsi:schemaLocation", "http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd");

            xmlSerializer.startTag(null, "metadata");
            xmlSerializer.startTag(null, "name");
            xmlSerializer.text("GPS Trace Example");
            xmlSerializer.endTag(null, "name");
            xmlSerializer.endTag(null, "metadata");

            xmlSerializer.startTag(null, "trk");
            xmlSerializer.startTag(null, "name");
            xmlSerializer.text("my custom track");
            xmlSerializer.endTag(null, "name");
            xmlSerializer.startTag(null, "trkseg");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void writeLocation(Location loc) throws IOException {
        String lat = String.valueOf(loc.getLatitude());
        String lon = String.valueOf(loc.getLongitude());
        xmlSerializer.startTag(null, "trkpt");
        xmlSerializer.attribute(null, "lat", lat);
        xmlSerializer.attribute(null, "lon", lon);
        xmlSerializer.startTag(null, "time");
        xmlSerializer.text(dateFormat.format(new Date()));
        xmlSerializer.endTag(null, "time");
        xmlSerializer.endTag(null, "trkpt");
        xmlSerializer.flush();
    }

    public void close() throws IOException {
        xmlSerializer.endTag(null, "trkseg");
        xmlSerializer.endTag(null, "trk");
        xmlSerializer.endTag(null, "gpx");
        xmlSerializer.endDocument();
        xmlSerializer.flush();
    }


}
