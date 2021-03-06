package com.nncloudtv.service;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.nncloudtv.dao.MsoNotificationDao;
import com.nncloudtv.lib.NNF;
import com.nncloudtv.lib.NnDateUtil;
import com.nncloudtv.model.MsoNotification;

@Service
public class MsoNotificationManager {
    
    static MsoNotificationDao dao = NNF.getMsoNotiDao();
    protected static final Logger log = Logger.getLogger(MsoNotificationManager.class.getName());
    
    public MsoNotification save(MsoNotification notification) {
        Date now = NnDateUtil.now();
        notification.setUpdateDate(now);
        if (notification.getCreateDate() == null)
            notification.setCreateDate(now);
        return dao.save(notification);
    }
    
    public Collection<MsoNotification> saveAll(Collection<MsoNotification> notifications) {
        Date now = NnDateUtil.now();
        for (MsoNotification notification : notifications) {
            notification.setUpdateDate(now);
            if (notification.getCreateDate() == null) {
                notification.setCreateDate(now);
            }
        }
        return dao.saveAll(notifications);
    }
    
    public List<MsoNotification> list(int page, int limit, String sort, String filter) {
        return dao.list(page, limit, sort, filter);
    }
    
    public List<MsoNotification> listScheduled(int page, int limit, String filter) {
        return dao.listScheduled(page, limit, filter);
    }
    
    public List<MsoNotification> listScheduled(Date dueDate) {
        return dao.listScheduled(dueDate);
    }
    
    public MsoNotification findById(long id) {
        return dao.findById(id);
    }
    
    public void delete(MsoNotification notification) {
        if (notification != null) {
            dao.delete(notification);
        }
    }
    
    public MsoNotification findById(String notificationIdStr) {
        
        return dao.findById(notificationIdStr);
    }
    
}

