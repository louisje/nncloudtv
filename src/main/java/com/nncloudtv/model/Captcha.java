package com.nncloudtv.model;

import java.util.Date;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.nncloudtv.lib.NnDateUtil;
import com.nncloudtv.lib.NnStringUtil;

/**  
 * To realize captcha.
 * Captcha is created by a different service and store corresponding information here.
 */
@PersistenceCapable(table = "captcha", detachable = "true")
public class Captcha implements PersistentBaseModel {
    
    private static final long serialVersionUID = 8631029787591344239L;
    private static final boolean cachable = false;
    
    public boolean isCachable() {
        return cachable;
    }
    
    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private long id;
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    @Persistent
    private long batch; //to sync with captcha creation service
    
     //captcha image to show to users, example 1/1.jpg
    @Persistent
    @Column(jdbcType = NnStringUtil.VARCHAR, length = NnStringUtil.NORMAL_STRING_LENGTH)
    private String fileName;
    
    @Persistent
    @Column(jdbcType = NnStringUtil.VARCHAR, length = NnStringUtil.NORMAL_STRING_LENGTH)
    private String name; //captcha answer 
    
    @Persistent
    private double random;     //to replace MySql's select random row's feature
    
    @Persistent
    private Date createDate;
            
    @Persistent
    private boolean toBeExpired; //whether this batch of captcha has expired
    
    @Persistent
    private Date lockedDate; //it's used only when the whole batch is marked expired, but user's still using it in the valid window 
    
    public static short ACTION_SIGNUP = 1;
    public static short ACTION_EMAIL = 2;
    
    public Captcha(long batch, String name, String fileName) {
        this.batch = batch;
        this.name = name;
        this.fileName = fileName;
        this.random = Math.random();
        this.createDate = NnDateUtil.now();
        this.toBeExpired = false;
    }
    
    public long getBatch() {
        return batch;
    }
    
    public void setBatch(long batch) {
        this.batch = batch;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public Date getCreateDate() {
        return createDate;
    }
    
    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }
    
    public double getRandom() {
        return random;
    }
    
    public void setRandom(double random) {
        this.random = random;
    }
    
    public boolean isToBeExpired() {
        return toBeExpired;
    }
    
    public void setToBeExpired(boolean toBeExpired) {
        this.toBeExpired = toBeExpired;
    }
    
    public Date getLockedDate() {
        return lockedDate;
    }
    
    public void setLockedDate(Date lockedDate) {
        this.lockedDate = lockedDate;
    }
    
}
