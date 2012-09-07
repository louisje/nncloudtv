package com.nncloudtv.model;

import java.io.Serializable;
import java.util.Date;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.nncloudtv.lib.YouTubeLib;

/**
 * a Channel
 */
@PersistenceCapable(table="nnchannel", detachable="true")
public class NnChannel implements Serializable {
    private static final long serialVersionUID = 6138621615980949044L;

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
    private long id;
            
    @Persistent
    @Column(jdbcType="VARCHAR", length=500)
    private String name; //the name we define, versus oriName

    @Persistent
    @Column(jdbcType="VARCHAR", length=500)
    private String oriName; //original podcast/youtube name 
    
    @Persistent
    @Column(jdbcType="VARCHAR", length=500)
    private String intro;

    @Persistent
    @Column(jdbcType="VARCHAR", length=255)
    private String imageUrl; 
    public static String IMAGE_PROCESSING_URL = "http://s3.amazonaws.com/9x9ui/war/v0/images/processing.png";
    public static String IMAGE_RADIO_URL = "http://s3.amazonaws.com/9x9ui/war/v0/images/9x9-watermark.jpg";
    public static String IMAGE_ERROR_URL = "http://s3.amazonaws.com/9x9ui/war/v0/images/error.png";
    public static String IMAGE_FB_URL = "http://s3.amazonaws.com/9x9ui/war/v0/images/facebook-icon.gif";
    public static String IMAGE_WATERMARK_URL= "http://s3.amazonaws.com/9x9ui/war/v0/images/9x9-watermark.jpg";
    
    @Persistent
    private boolean isPublic;

    //used for testing without changing the program logic
    //isTemp set to true means it can be wiped out
    @Persistent
    private boolean isTemp;    
    
    @Persistent
    @Column(jdbcType="VARCHAR", length=5)
    private String lang; //used with LangTable

    @Persistent
    @Column(jdbcType="VARCHAR", length=5)
    private String sphere; //used with LangTable
    
    @Persistent
    private int programCnt;
    
    @Persistent
    @Column(jdbcType="VARCHAR", length=500)
    private String sourceUrl; //unique if not null

    @NotPersistent
    private short type; //Use with MsoIpg and Subscription, to define attributes such as MsoIpg.TYPE_READONLY

    @Persistent
    @Column(jdbcType="VARCHAR", length=500)
    private String tag;
    
    @Persistent
    public short contentType;
    public static final short CONTENTTYPE_SYSTEM = 1;
    public static final short CONTENTTYPE_PODCAST = 2;
    public static final short CONTENTTYPE_YOUTUBE_CHANNEL = 3;
    public static final short CONTENTTYPE_YOUTUBE_PLAYLIST = 4;
    public static final short CONTENTTYPE_FACEBOOK = 5;
    public static final short CONTENTTYPE_MIXED = 6;
    public static final short CONTENTTYPE_SLIDE = 7;
    public static final short CONTENTTYPE_MAPLE_VARIETY = 8;
    public static final short CONTENTTYPE_MAPLE_SOAP = 9;
    public static final short CONTENTTYPE_YOUTUBE_SPECIAL_SORTING = 10;
    public static final short CONTENTTYPE_FAVORITE = 11;
    
    @Persistent
    @Column(jdbcType="VARCHAR", length=255)
    private String piwik;
    
    @Persistent
    private short status;
    //general
    public static final short STATUS_SUCCESS = 0;
    public static final short STATUS_ERROR = 1;
    public static final short STATUS_PROCESSING = 2;
    public static final short STATUS_WAIT_FOR_APPROVAL = 3;
    //invalid
    public static final short STATUS_INVALID_FORMAT = 51;
    public static final short STATUS_URL_NOT_FOUND = 53;
    //quality
    public static final short STATUS_NO_VALID_EPISODE = 100;
    public static final short STATUS_BAD_QUALITY = 101;
    //internal
    public static final short STATUS_TRANSCODING_DB_ERROR = 1000;
    public static final short STATUS_NNVMSO_JSON_ERROR = 1001;        
                            
    //status note or pool status note
    //can be number to indicate any kind of status or text
    @Persistent
    @Column(jdbcType="VARCHAR", length=10)
    private String note;
    
    @NotPersistent
    private short seq; //use with subscription, to specify sequence in IPG. 
    
    //not used
    public static final short SORT_NEWEST_TO_OLDEST = 1; //default
    public static final short SORT_OLDEST_TO_NEWEST = 2;
    public static final short SORT_DESIGNATED = 3;
    @Persistent
    private short sorting;

    @Persistent
    private short poolType; //default 0, anything > 0 is "special"
    public static final short POOL_NORMAL = 0;
    public static final short POOL_FEATUERD = 1;
    public static final short POOL_HOTTEST = 2;
    public static final short POOL_TRENDING = 3; 
    
    @NotPersistent
    private String recentlyWatchedProgram;  

    @NotPersistent
    private int cntFollower; // follower count
    
    @Persistent    
    private int cntSubscribe; //subscription count

    @NotPersistent    
    private int cntView; //viewing count, in shard table
    
    @Persistent 
    private Date createDate;
        
    @Persistent
    private Date updateDate;

    @Persistent
    @Column(jdbcType="VARCHAR", length=25)    
    private String userIdStr; //format: shard-userId, example: 1-1
    
    @Persistent
    @Column(jdbcType="VARCHAR", length=255)
    private String transcodingUpdateDate; //timestamps from transcoding server            
        
    public NnChannel(String name, String intro, String imageUrl) {
        this.name = name;
        this.intro = intro;
        this.imageUrl = imageUrl;
        Date now = new Date();
        this.createDate = now;
        this.updateDate = now;
    }
    
    public NnChannel(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
        
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
        
    public String getPlayerPrefSource() {
        if (getSourceUrl() != null && getSourceUrl().contains("http://www.youtube.com"))
            return YouTubeLib.getYouTubeChannelName(getSourceUrl());        
        if (getContentType() == NnChannel.CONTENTTYPE_FACEBOOK)
            return getSourceUrl();
        return "";
    }
    
    public String getIntro() {
        return intro;
    }

    public void setIntro(String intro) {
        if (intro != null) {
            intro = intro.replaceAll("\n", " ");
            intro = intro.replaceAll("\t", " ");
            int introLenth = (intro.length() > 256 ? 256 : intro.length()); 
            intro = intro.substring(0, introLenth);
        }
        this.intro = intro;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getPlayerPrefImageUrl() {
        String imageUrl = getImageUrl();
        if ((getStatus() == NnChannel.STATUS_ERROR) || 
            (getStatus() != NnChannel.STATUS_WAIT_FOR_APPROVAL &&
             getStatus() != NnChannel.STATUS_SUCCESS && 
             getStatus() != NnChannel.STATUS_PROCESSING)) {    
            imageUrl = IMAGE_ERROR_URL;
        } 
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }
    
    public Date getCreateDate() {
        return createDate;
    }

    public short getType() {
        return type;
    }

    public void setType(short type) {
        this.type = type;
    }

    public int getProgramCnt() {
        return programCnt;
    }

    public void setProgramCnt(int cnt) {
        this.programCnt = cnt;
    }
    
    public int getStatus() {
        return status;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public short getSeq() {
        return seq;
    }

    public void setSeq(short seq) {
        this.seq = seq;
    }

    public short getContentType() {
        return contentType;
    }

    public void setContentType(short contentType) {
        this.contentType = contentType;
    }

    public void setTranscodingUpdateDate(String transcodingUpdateDate) {
        this.transcodingUpdateDate = transcodingUpdateDate;
    }

    public String getTranscodingUpdateDate() {
        return transcodingUpdateDate;
    }

    public String getOriName() {
        return oriName;
    }

    public void setOriName(String oriName) {
        this.oriName = oriName;
    }

    public short getSorting() {
        return sorting;
    }

    public void setSorting(short sorting) {
        this.sorting = sorting;
    }

    public String getRecentlyWatchedProgram() {
        return recentlyWatchedProgram;
    }

    public void setRecentlyWatchedProgram(String recentlyWatchedProgram) {
        this.recentlyWatchedProgram = recentlyWatchedProgram;
    }

    public String getPiwik() {
        return piwik;
    }

    public void setPiwik(String piwik) {
        this.piwik = piwik;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public boolean isTemp() {
        return isTemp;
    }

    public void setTemp(boolean isTemp) {
        this.isTemp = isTemp;
    }

    public short getPoolType() {
        return poolType;
    }

    public void setPoolType(short poolType) {
        this.poolType = poolType;
    }

    public int getCntSubscribe() {
        return cntSubscribe;
    }

    public void setCntSubscribe(int cntSubscribe) {
        this.cntSubscribe = cntSubscribe;
    }

    public int getCntView() {
        return cntView;
    }

    public void setCntView(int cntView) {
        this.cntView = cntView;
    }

    public short getShard(String userId) {
        if (userId == null)
            return 0;
        String[] splits = userId.split("-");
        if (splits.length > 1)
            return Short.parseShort(splits[0]);
        else
            return 1;
    }
    
    public long getUserId() {
        return getUserId(userIdStr);
    }
    
    public long getUserId(String userId) {
        if (userId == null)
            return 0;
        String[] splits = userId.split("-");
        if (splits.length > 1)
            return Short.parseShort(splits[1]);
        else
            return Short.parseShort(userId);        
    }
    
    public String getUserIdStr() {
        return userIdStr;
    }

    public void setUserIdStr(String userIdStr) {
        this.userIdStr = userIdStr;
    }

    public void setUserIdStr(short shard, long userId) {
        if (shard == 0) {
            userIdStr = String.valueOf(userId);
        } else {
            userIdStr = shard + "-" + userId;
        }
    }

    public String getSphere() {
        return sphere;
    }

    public void setSphere(String sphere) {
        this.sphere = sphere;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public int getCntFollower() {
        return cntFollower;
    }

    public void setCntFollower(int cntFollower) {
        this.cntFollower = cntFollower;
    }
    
    /*
    public String getUserImageUrl() {
        if (userInfo != null) {            
            String[] info = userInfo.split("\\|");
            if (userInfo.length() > 1) {
                String userImageUrl = info[1];
                return userImageUrl;
            }
        }
        return NnUser.IMAGE_URL_DEFAULT; 
    }
    
    public String getUserName() {        
        if (userInfo != null) {            
            String[] info = userInfo.split("\\|");
            if (userInfo.length() > 0) {
                String userName = info[0];
                return userName;
            }
        }
        return null;
    }
    */
}
