package com.nncloudtv.lib.stream;

import java.io.InputStream;
import java.util.logging.Logger;

public class YahooScreenLib implements StreamLib {
    
    protected static final Logger log = Logger.getLogger(YahooScreenLib.class.getName());
    
    public static final String REGEX_YAHOO_SCREEN_URL = "^https?:\\/\\/(tw\\.)?screen\\.yahoo\\.com\\/(.+)\\.html$";
    
    public boolean isUrlMatched(String urlStr) {
        
        return (urlStr == null) ? false : urlStr.matches(REGEX_YAHOO_SCREEN_URL);
    }
    
    public String normalizeUrl(String urlStr) {
        
        return urlStr;
    }
    
    public String getDirectVideoUrl(String urlStr) {
        
        return YouTubeLib.getYouTubeDLUrl(urlStr);
    }
    
    public String getHtml5DirectVideoUrl(String urlStr) {
        
        return YouTubeLib.getYouTubeDLUrl(urlStr);
    }
    
    public InputStream getDirectVideoStream(String urlStr) {
        
        return YouTubeLib.getYouTubeDLStream(urlStr);
    }
    
}
