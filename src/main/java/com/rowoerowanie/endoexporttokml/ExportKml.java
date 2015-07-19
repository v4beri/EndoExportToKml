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
package com.rowoerowanie.endoexporttokml;

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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

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

    private InputStream loadData(String url) throws MalformedURLException, IOException {
        System.out.println("łączenie z " + url);
        
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
    
    private String convertUrl(String u) throws Exception {
        
        // https://www.endomondo.com/workouts/562753612/14312735 
        // https://www.endomondo.com/rest/v1/users/14312735/workouts/562753612
        
        
        String[] arr = u.trim().split("/");
        if (arr.length >= 4) {
            String workoutId = arr[arr.length - 1];
            String userId = arr[arr.length - 2];
            return String.format("https://www.endomondo.com/rest/v1/users/%s/workouts/%s", new Object[] { workoutId, userId });
        }
        else {
            throw new Exception("Nieprawidłowy adres URL");                    
        }
    }

    public void exportToKml(String url, String outFile)  {
        try {
            this.url = url;
            List<String> kml = new ArrayList<>();
            List<Point> points = new ArrayList<>();
            String lines = IOUtils.toString(loadData(convertUrl(url))).trim();
            
            JSONObject obj = new JSONObject(lines);
            
            
            System.out.println("Trasa: " + obj.get("id"));
            
            JSONArray arr = obj.getJSONObject("points").getJSONArray("points");
            
            for (int i = 0; i < arr.length(); i++) {
                JSONObject p = arr.getJSONObject(i);
                /*p.getString("time");
                p.getString("latitude");
                p.getString("longitude");
                p.getString("altitude");
                p.getString("duration");*/
                                
                
                points.add(new Point(
                        p.getDouble("distance"),
                        p.optDouble("altitude", 0),
                        p.getDouble("longitude"),
                        p.getDouble("latitude")));
                
            }
            
            
            if (!points.isEmpty()) {
                System.out.println("Wygenerowano kml punktów "+ points.size());
                
                kml.add(kmlHead);
                for (Point p : points) {
                    kml.add(p.toString());
                }
                kml.add(kmlFoot);
                FileUtils.writeLines(new File(outFile), kml);
            }
        } catch (IOException ex) {
            Logger.getLogger(ExportKml.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(ExportKml.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    
}
