/*
 *   $Id$
 *
 *   Copyright 2007 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.services.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Contacts a given URL which should be an OME server which will return either
 * an empty String or a URL which points to a needed upgrade.
 * 
 * @author Josh Moore, josh at glencoesoftware.com
 * @since 3.0-Beta2.3
 */
public class UpgradeCheck {

    private final static Log log = LogFactory.getLog(UpgradeCheck.class);

    int poll;
    String url;
    String version;

    public void setPoll(int poll) {
        this.poll = poll;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * If the {@link #url} has been set to null or the empty string, then no
     * upgrade check will be performed (silently). If however the string is an
     * invalid URL, a warning will be printed.
     * 
     * This method should <em>never</em> throw an exception.
     */
    public void start() {
        if (url == null || url.length() == 0) {
            return; // EARLY EXIT!
        }

        String query = url + "?version=" + version + ";poll=" + poll;
        URL _url;
        try {
            _url = new URL(query);
        } catch (Exception e) {
            log.warn("Could not connect to " + query + ":" + e.getMessage());
            return;
        }

        try {
            URLConnection conn = _url.openConnection();
            conn.setUseCaches(false);
            conn.addRequestProperty("User-Agent", "OMERO.upgrades");
            conn.connect();

            InputStream in = conn.getInputStream();
            BufferedInputStream bufIn = new BufferedInputStream(in);

            StringBuilder sb = new StringBuilder();
            while (true) {
                int data = bufIn.read();
                if (data == -1) {
                    break;
                } else {
                    sb.append((char) data);
                }
            }
            String result = sb.toString();
            if (result.length() == 0) {
                log.info("no update needed");
            } else {
                log.warn("UPGRADE AVAILABLE:" + result);
            }
        } catch (UnknownHostException uhe) {
            log.error("Unknown host:" + url);
        } catch (IOException ioe) {
            log.error("I/O Error on UpgradeCheck", ioe);
        }
    }

}