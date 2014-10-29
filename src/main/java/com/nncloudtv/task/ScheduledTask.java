package com.nncloudtv.task;

import java.util.Date;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.nncloudtv.lib.CacheFactory;
import com.nncloudtv.lib.NnDateUtil;

@Service
@EnableScheduling
public class ScheduledTask {
    
    protected static Logger log = Logger.getLogger(ScheduledTask.class.getName());
    
    public final int MC_INTERVAL = 584141; // memcache check interval (milliseconds)
    public final int GC_INTERVAL = 604171; // garbage collection interval (milliseconds)
    
    @Scheduled(fixedRate = GC_INTERVAL)
    public void triggerGC() {
        
        System.gc(); // trigger garbage collection
        
        long max   = Runtime.getRuntime().maxMemory();
        long total = Runtime.getRuntime().totalMemory();
        long free  = Runtime.getRuntime().freeMemory();
        
        log.info("[memory] max = " + FileUtils.byteCountToDisplaySize(max)
                   +  ", total = " + FileUtils.byteCountToDisplaySize(total)
                   +   ", free = " + FileUtils.byteCountToDisplaySize(free));
        if (max == total && free < total / 100) {
            log.warning("available memory is less than 1%");
        }
    }
    
    @Scheduled(fixedDelay = MC_INTERVAL)
    public void checkingMemcacheServer() {
        
        if (CacheFactory.isEnabled) {
            
            log.info("[memcache] checking memcache server(s) ...");
            long before = NnDateUtil.now().getTime();
            CacheFactory.reconfigClient();
            log.info("[memcache] reconfig costs " + (NnDateUtil.now().getTime() - before) + " milliseconds");
        }
        
    }
    
}
