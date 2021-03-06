package com.nncloudtv.lib.stream;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.nncloudtv.lib.NnNetUtil;

public class LiveStreamLib implements StreamLib {
    
    protected final static Logger log = Logger.getLogger(LiveStreamLib.class.getName());
    
    public static final String REGEX_LIVESTREAM_VIDEO_URL = "^https?:\\/\\/new\\.livestream\\.com\\/accounts\\/([0-9]+)\\/events\\/([0-9]+)\\/videos\\/([0-9]+)$";
    public static final String REGEX_LIVESTREAM_EVENT_URL = "^https?:\\/\\/new\\.livestream\\.com\\/accounts\\/([0-9]+)\\/events\\/([0-9]+)\\/?$";
    public static final String REGEX_LIVESTREAM_PAN_URL   = "^https?:\\/\\/new\\.livestream\\.com\\/(.+)(\\/(.+))+$";
    public static final String REGEX_LIVESTREAM_PAN_VIDEO = "^https?:\\/\\/new\\.livestream\\.com\\/(.+)(\\/videos\\/[0-9]+)$";
    
    public String getLiveStreamApiUrl(String urlStr) {
        
        if (urlStr == null) { return null; }
        
        String normalizedUrl = null;
        
        if (urlStr.matches(REGEX_LIVESTREAM_EVENT_URL) || urlStr.matches(REGEX_LIVESTREAM_VIDEO_URL)) {
            
            normalizedUrl = urlStr;
            
        } else if (urlStr.matches(REGEX_LIVESTREAM_PAN_URL)) {
            
            normalizedUrl = normalizeUrl(urlStr);
            
        }
        
        if (normalizedUrl != null) {
            
            return normalizedUrl.replaceFirst("\\/\\/new\\.", "//api.new.");
            
        } else {
            
            return null;
        }
    }
    
    public boolean isUrlMatched(String urlStr) {
        
        return (urlStr == null) ? null : urlStr.matches(REGEX_LIVESTREAM_PAN_URL);
    }
    
    public String normalizeUrl(String urlStr) {
        
        if (urlStr == null) { return null; }
        
        if (urlStr.matches(REGEX_LIVESTREAM_EVENT_URL) || urlStr.matches(REGEX_LIVESTREAM_VIDEO_URL)) {
            
            // already normalized
            return urlStr;
        }
        
        if (urlStr.matches(REGEX_LIVESTREAM_PAN_URL)) {
            
            log.info("livestream pan url format matched");
            
            try {
                
                String metaTag = "meta[name=apple-itunes-app]";
                Document doc = Jsoup.connect(urlStr).get();
                Element element = doc.select(metaTag).first();
                if (element == null) {
                    log.warning("meta tag is not found " + metaTag);
                    return null;
                }
                
                String contentStr = element.attr("content");
                if (contentStr == null) {
                    log.warning("contentStr is null");
                    return null;
                }
                log.info(contentStr);
                String[] split = contentStr.split(",");
                for (String str : split) {
                    
                    str = str.trim();
                    String[] attr = str.split("=");
                    if (attr.length >= 2 && attr[0].trim().equals("app-argument")) {
                        
                        String appArgument = attr[1].trim();
                        log.info("app-argument = " + appArgument);
                        
                        if (appArgument != null) {
                            
                            if (appArgument.matches(REGEX_LIVESTREAM_VIDEO_URL)) {
                                
                                return appArgument;
                                
                            } else if (appArgument.matches(REGEX_LIVESTREAM_EVENT_URL)) {
                                
                                Matcher matcher = Pattern.compile(REGEX_LIVESTREAM_PAN_VIDEO).matcher(urlStr);
                                
                                if (matcher.find()) {
                                    
                                    appArgument += matcher.group(2);
                                    log.info("pan video");
                                }
                                
                                return appArgument;
                            }
                        }
                    }
                }
                
                log.info("no proper app-argument found");
                
            } catch (IOException e) {
                
                log.warning(e.getClass().getName());
                log.warning(e.getMessage());
                
                return null;
            }
        }
        
        return null;
    }
    
    public String getDirectVideoUrl(String urlStr) {
        
        return getDirectVideoUrl(urlStr, false);
    }
    
    public String getHtml5DirectVideoUrl(String urlStr) {
        
        return getDirectVideoUrl(urlStr, true);
    }
    
    public String getDirectVideoUrl(String urlStr, boolean html5) {
        
        if (urlStr == null) { return null; }
        
        if (urlStr.matches(REGEX_LIVESTREAM_VIDEO_URL)) {
            
            log.info("livestream video url format");
            
            urlStr = getLiveStreamApiUrl(urlStr);
            log.info("api url = " + urlStr);
            String content = NnNetUtil.urlGet(urlStr);
            if (content == null) {
                
                log.warning("livestream api return empty");
                return null;
            }
            log.info("content size = " + content.length());
            try {
                
                JSONObject videoJson = new JSONObject(content);
                System.out.println(videoJson.toString());
                String progressiveUrl = videoJson.getString("progressive_url");
                log.info("progressive_url = " + progressiveUrl);
                
                return progressiveUrl;
                
            } catch (JSONException e) {
                
                log.warning(e.getClass().getName());
                log.warning(e.getMessage());
                return null;
            }
            
        } else if (urlStr.matches(REGEX_LIVESTREAM_EVENT_URL)) {
            
            log.info("livestream event url format");
            
            urlStr = getLiveStreamApiUrl(urlStr);
            log.info("api url = " + urlStr);
            String content = NnNetUtil.urlGet(urlStr);
            if (content == null) {
                
                log.warning("livestream api return empty");
                return null;
            }
            log.info("content size = " + content.length());
            try {
                
                JSONObject respJson = new JSONObject(content);
                System.out.println(respJson.toString());
                if (respJson.isNull("stream_info")) {
                    
                    JSONObject feedJson = respJson.getJSONObject("feed");
                    JSONArray dataJson = feedJson.getJSONArray("data");
                    
                    for (int i = 0; i < dataJson.length(); i++) {
                        
                        if (dataJson.getJSONObject(i).getString("type").equals("video")) {
                            
                            String  progressiveUrl = dataJson.getJSONObject(i).getJSONObject("data").getString("progressive_url");
                            log.info("progressive_url = " + progressiveUrl);
                            
                            return progressiveUrl;
                        }
                    }
                    
                } else {
                    
                    JSONObject streamInfoJson = respJson.getJSONObject("stream_info");
                    
                    String m3u8 = streamInfoJson.getString("m3u8_url");
                    log.info("m3u8_url = " + m3u8);
                    if (html5) {
                        
                        // check 30X status
                        URL url = new URL(m3u8);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setInstanceFollowRedirects(false);
                        int code = conn.getResponseCode();
                        log.info("m3u8 status code = " + code);
                        if (code == 301 || code == 302) {
                            
                            String location = conn.getHeaderField("Location");
                            log.info("fetch redirection location = " + location);
                            if (location != null) {
                                
                                return location;
                            }
                        }
                    }
                    
                    return m3u8;
                }
                
            } catch (MalformedURLException e) {
                
                log.warning(e.getClass().getName());
                log.warning(e.getMessage());
                
                return null;
                
            } catch (JSONException e) {
                
                log.warning(e.getClass().getName());
                log.warning(e.getMessage());
                
                return null;
                
            } catch (IOException e) {
                
                log.warning(e.getClass().getName());
                log.warning(e.getMessage());
                
                return null;
            }
            
        } else if (urlStr.matches(REGEX_LIVESTREAM_PAN_URL)) {
            
            return getDirectVideoUrl(normalizeUrl(urlStr), html5);
            
        }
        
        log.info("does not match any");
        
        return null;
    }
    
    public InputStream getDirectVideoStream(String urlStr) {
        
        // need implement
        
        return null;
    }
    
}
