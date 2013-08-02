package com.nncloudtv.lib;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.spy.memcached.MemcachedClient;
import net.spy.memcached.OperationTimeoutException;
import net.spy.memcached.internal.CheckedOperationTimeoutException;

public class CacheFactory {
    
    protected static final Logger log = Logger.getLogger(CacheFactory.class.getName());
    
    public static final int EXP_DEFAULT = 2592000;
    public static final int PORT_DEFAULT = 11211;
    public static final int ASYNC_CACHE_TIMEOUT = 2;
    public static final int DELAY_CHECK_THRESHOLD = 100;
    public static final String ERROR = "ERROR";
    
    public static boolean isRunning = true;
    private static int delay_check = 0;
    
    public static MemcachedClient getClient() {
        System.setProperty("net.spy.log.LoggerImpl", "net.spy.memcached.compat.log.SunLogger"); 
        Logger.getLogger("net.spy.memcached").setLevel(Level.SEVERE);
        MemcachedClient cache = null;
     
        try {
            Properties properties = new Properties();
            properties.load(CacheFactory.class.getClassLoader().getResourceAsStream("memcache.properties"));
            String server = properties.getProperty("server");
            //log.info("memcache server:" + server);
            cache = new MemcachedClient(new InetSocketAddress(server, CacheFactory.PORT_DEFAULT));
        } catch (NullPointerException e) {
            log.severe("memcache is missing");
        } catch (IOException e) {
            log.severe("memcache io exception");
        } catch (Exception e) {
            log.severe("memcache exception");
            e.printStackTrace();
        }
        
        return cache;
    }    

    public static Object get(String key) {
        
        if (delay_check++ > CacheFactory.DELAY_CHECK_THRESHOLD) {
            delay_check = 0;
            CacheFactory.isRunning = false;
        } else if (!CacheFactory.isRunning) {
            log.warning("cache is not running");
            return null;
        }
        MemcachedClient cache = CacheFactory.getClient();
        if (cache == null) return null;
        
        Object obj = null;
        Future<Object> future = null;
        try {
            future = cache.asyncGet(key);
            obj = future.get(ASYNC_CACHE_TIMEOUT, TimeUnit.SECONDS); // Asynchronously 
            CacheFactory.isRunning = true;
        } catch (CheckedOperationTimeoutException e){
            log.warning("get CheckedOperationTimeoutException");
        } catch (OperationTimeoutException e) {
            log.severe("get OperationTimeoutException");
        } catch (NullPointerException e) {
            log.warning("future not found");
        } catch (Exception e) {
            log.severe("get Exception");
            e.printStackTrace();
        } finally {
            cache.shutdown(); 
            if (future != null)
                future.cancel(false);
        }
        if (obj == null)
            log.info("cache [" + key + "] --> missed");
        return obj;
    }

    public static Object set(String key, Object obj) {
        
        if (!CacheFactory.isRunning) return null;
        MemcachedClient cache = CacheFactory.getClient();
        if (cache == null) return null;
        
        Future<Object> future = null;
        Object retObj = null;
        try {
            cache.set(key, CacheFactory.EXP_DEFAULT, obj);
            future = cache.asyncGet(key);;
            retObj = future.get(ASYNC_CACHE_TIMEOUT, TimeUnit.SECONDS);
        } catch (CheckedOperationTimeoutException e){
            log.warning("get CheckedOperationTimeoutException");
        } catch (OperationTimeoutException e) {
            log.severe("memcache OperationTimeoutException");
        } catch (NullPointerException e) {
            log.warning("future not found");
        } catch (Exception e) {
            log.severe("get Exception");
            e.printStackTrace();
        } finally {
            cache.shutdown();
            if (future != null)
                future.cancel(false);
        }
        if (retObj == null)
            log.info("cache [" + key + "] --> not saved");
        else
            log.info("cache [" + key + "] --> saved");
        return retObj;
    }    

    public static void delete(String key) {
        
        if (!CacheFactory.isRunning) return;
        MemcachedClient cache = CacheFactory.getClient();
        if (cache == null) return;
        
        try {
            cache.delete(key).get();
        } catch (OperationTimeoutException e) {
            log.severe("get OperationTimeoutException");
        } catch (Exception e) {
            log.severe("get Exception");
            e.printStackTrace();
        } finally {
            cache.shutdown();            
        }
        log.info("cache [" + key + "] --> deleted");
    }    
    
}
