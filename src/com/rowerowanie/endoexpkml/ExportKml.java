/*
 * Copyright (C) 2015 rowerowanie.com.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package com.rowerowanie.endoexpkml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author w4bx3
 */
public class ExportKml {

    private final String kmlHead = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<kml xmlns=\"http://earth.google.com/kml/2.1\">\n"
            + "<Folder>\n"
            + "	<name>Export from Endomondo</name>\n"
            + "	<Placemark>\n"
            + "		<name>track</name>\n"
            + "		<Style>\n"
            + "			<LineStyle>\n"
            + "				<color>cc0000ff</color>\n"
            + "				<width>4</width>\n"
            + "			</LineStyle>\n"
            + "		</Style>\n"
            + "		<LineString>\n"
            + "			<altitudeMode>clampToGround</altitudeMode>\n"
            + "			<coordinates>";

    private final String kmlFoot = "</coordinates>\n"
            + "		</LineString>\n"
            + "	</Placemark></Folder>\n"
            + "</kml>";
    
    private String url;

    /**
     * @return the proxyHost
     */
    public String getProxyHost() {
        return proxyHost;
    }

    /**
     * @param proxyHost the proxyHost to set
     */
    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    /**
     * @return the proxyPort
     */
    public Integer getProxyPort() {
        return proxyPort;
    }

    /**
     * @param proxyPort the proxyPort to set
     */
    public void setProxyPort(Integer proxyPort) {
        this.proxyPort = proxyPort;
    }
  
    private class Point {

        Double distance;
        Double alt;
        Double lng;
        Double lat;

        public Point(Double distance, Double alt, Double lng, Double lat) {
            this.distance = distance;
            this.alt = alt;
            this.lng = lng;
            this.lat = lat;
        }

        @Override
        public String toString() {
            return lng + "," + lat + "," + alt;
        }
    }
    
    private String proxyHost;
    private Integer proxyPort;

    private InputStream loadData() throws MalformedURLException, IOException {
        HttpURLConnection connection ;
        if (StringUtils.isNotBlank(this.proxyHost)) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(this.proxyHost, proxyPort));
            connection = (HttpURLConnection) new URL(url).openConnection(proxy);
        }
        else {
            connection = (HttpURLConnection) new URL(url).openConnection();
        }
                                
        return connection.getInputStream();
    }

    private int stage = 0;
    private int mode = 0;
    private Double distance =0.0;
    private Double alt =0.0;
    private Double lng;
    private Double lat;

    private Double parseValueLine(String s) {
        String[] d = s.split(":");
        return Double.parseDouble(StringUtils.remove(d[1], ","));
    }

    public void exportToKml(String url, String outFile) throws IOException {
        this.url = url;
        List<String> kml = new ArrayList<>();
        List<Point> points = new ArrayList<>();
        List<String> lines = IOUtils.readLines(loadData());

        for (String line : lines) {
            if (line.contains("endomondo.AppRegistry.get('draw-workout-controller').draw({")) {
                stage = 1;
            }
            if ((stage == 1) && (line.contains("\"data\": ["))) {
                stage = 3;
                mode = 0;
            }
            if ((stage == 3) && (line.contains("\"values\": {"))) {
                mode = 1;
            }

            if ((stage == 3) && (mode == 1) && (line.contains("\"distance\": "))) {
                distance = parseValueLine(line);
            }
            if ((stage == 3) && (mode == 1) && (line.contains("\"alt\": "))) {
                alt = parseValueLine(line);
            }
            if ((stage == 3) && (mode == 1) && (line.contains("\"lng\": "))) {
                lng = parseValueLine(line);
            }
            if ((stage == 3) && (mode == 1) && (line.contains("\"lat\": "))) {
                lat = parseValueLine(line);

                System.out.println("lng=" + lng + " lat=" + lat + " alt=" + alt + " dist=" + distance);
                points.add(new Point(distance, alt, lng, lat));
            }

            if ((stage == 3) && (mode == 1) && (line.contains("]"))) {
                mode = 0;
                stage = 0;
            }
        }

        if (!points.isEmpty()) {
            kml.add(kmlHead);
            for (Point p : points) {
                kml.add(p.toString());
            }
            kml.add(kmlFoot);
            FileUtils.writeLines(new File(outFile), kml);
        }
    }    
}
