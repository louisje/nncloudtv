package com.nncloudtv.model;

import java.util.Date;
import java.util.logging.Logger;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.NotPersistent;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.nncloudtv.lib.NnDateUtil;
import com.nncloudtv.lib.NnStringUtil;
import com.nncloudtv.lib.stream.YouTubeLib;

/**
 * a Channel
 */
@PersistenceCapable(table = "nnchannel", detachable = "true")
public class NnChannel implements PersistentModel {
    
    private static final long serialVersionUID = -110451972264224377L;
    private static final boolean cachable = true;
    
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
    @Column(jdbcType = NnStringUtil.VARCHAR, length = NnStringUtil.EXTENDED_STRING_LENGTH)
    private String name; //the name we define, versus oriName
    
    @Persistent
    @Column(jdbcType = NnStringUtil.VARCHAR, length = NnStringUtil.EXTENDED_STRING_LENGTH)
    private String oriName; //original podcast/youtube name 
    
    @Persistent
    @Column(jdbcType = NnStringUtil.VARCHAR, length = NnStringUtil.VERY_LONG_STRING_LENGTH)
    private String intro;
    
    @NotPersistent
    private String moreImageUrl;
    
    @NotPersistent
    private String socialFeeds;
    
    @NotPersistent
    private String bannerImageUrl;
    
    //be warned: for youtube channels, imageUrl actually include 3 imageUrls, separated by "|"
    //related functions are: getMoreImageUrl(three episode), 
    //                       getPlayerPrefImageUrl(reflect the status), 
    //                       getOneImageUrl (to get one image in "|" situation)
    @Persistent
    @Column(jdbcType = NnStringUtil.VARCHAR, length = NnStringUtil.NORMAL_STRING_LENGTH)
    private String imageUrl; 
    public static String IMAGE_PROCESSING_URL = "http://s3.amazonaws.com/9x9ui/war/v0/images/processing.png";
    public static String IMAGE_RADIO_URL      = "http://s3.amazonaws.com/9x9ui/war/v0/images/9x9-watermark.jpg";
    public static String IMAGE_ERROR_URL      = "http://s3.amazonaws.com/9x9ui/war/v0/images/error.png";
    public static String IMAGE_FB_URL         = "http://s3.amazonaws.com/9x9ui/war/v0/images/facebook-icon.gif";
    public static String IMAGE_WATERMARK_URL  = "http://s3.amazonaws.com/9x9ui/war/v0/images/9x9-watermark.jpg";
    public static String IMAGE_DEFAULT_URL    = "http://s3.amazonaws.com/9x9ui/war/v0/images/9x9-watermark.jpg";
    public static String IMAGE_EPISODE_URL    = "http://s3.amazonaws.com/9x9ui/war/v0/images/episode-default.png";
    
    public static final int DEFAULT_WIDTH = 640;
    public static final int DEFAULT_HEIGHT = 480;
    
    @Persistent
    private boolean isPublic; // set to false when paidChannel = true
    
    @Persistent
    private boolean paidChannel;
    
    @Persistent
    private boolean readonly;
    
    //used for testing without changing the program logic
    //isTemp set to true means it can be wiped out
    @Persistent
    private boolean isTemp;
    
    @Persistent
    @Column(jdbcType = NnStringUtil.VARCHAR, length = NnStringUtil.SHORT_STRING_LENGTH)
    private String lang; //used with LangTable
    
    @Persistent
    @Column(jdbcType = NnStringUtil.VARCHAR, length = NnStringUtil.SHORT_STRING_LENGTH)
    private String sphere; //used with LangTable
    
    @Persistent
    @Column(jdbcType = NnStringUtil.VARCHAR, length = NnStringUtil.EXTENDED_STRING_LENGTH)
    private String sourceUrl; //unique if not null
    
    @Persistent
    @Column(jdbcType = NnStringUtil.VARCHAR, length = NnStringUtil.EXTENDED_STRING_LENGTH)
    private String tag;
    
    @NotPersistent
    private short type; //Use with MsoIpg and Subscription, to define attributes such as MsoIpg.TYPE_READONLY
    
    @Persistent
    public short contentType;
    public static final short CONTENTTYPE_SYSTEM           = 1;
    public static final short CONTENTTYPE_PODCAST          = 2;
    public static final short CONTENTTYPE_YOUTUBE_CHANNEL  = 3;
    public static final short CONTENTTYPE_YOUTUBE_PLAYLIST = 4;
    public static final short CONTENTTYPE_FACEBOOK         = 5;
    public static final short CONTENTTYPE_MIXED            = 6; //9x9 channel
    public static final short CONTENTTYPE_SLIDE            = 7;
    public static final short CONTENTTYPE_MAPLE_VARIETY    = 8;
    public static final short CONTENTTYPE_MAPLE_SOAP       = 9;
    public static final short CONTENTTYPE_YOUTUBE_SPECIAL_SORTING = 10;
    public static final short CONTENTTYPE_FAVORITE         = 11;
    public static final short CONTENTTYPE_FAKE_FAVORITE    = 12;
    public static final short CONTENTTYPE_YOUTUBE_LIVE     = 13;
    public static final short CONTENTTYPE_DAYPARTING_MASK  = 14;
    public static final short CONTENTTYPE_TRENDING         = 15;
    public static final short CONTENTTYPE_VIRTUAL_CHANNEL1 = 16; // experiment
    public static final short CONTENTTYPE_VIRTUAL_CHANNEL2 = 17; // experiment
    
    @Persistent
    private short status;
    //general
    public static final short STATUS_SUCCESS           = 0;
    public static final short STATUS_ERROR             = 1;
    public static final short STATUS_PROCESSING        = 2;
    public static final short STATUS_WAIT_FOR_APPROVAL = 3;
    public static final short STATUS_REMOVED           = 4;
    //invalid
    public static final short STATUS_INVALID_FORMAT    = 51;
    public static final short STATUS_URL_NOT_FOUND     = 53;
    //quality
    public static final short STATUS_NO_VALID_EPISODE  = 100;
    public static final short STATUS_BAD_QUALITY       = 101;
    //internal
    public static final short STATUS_TRANSCODING_DB_ERROR = 1000;
    public static final short STATUS_NNVMSO_JSON_ERROR = 1001;
    
    //status note or pool status note
    //can be number to indicate any kind of status or text
    @Persistent
    @Column(jdbcType = NnStringUtil.VARCHAR, length = NnStringUtil.LONG_STRING_LENGTH)
    private String note;
    
    @Persistent
    private short seq; //use with subscription, to specify sequence in IPG. 
    
    @Persistent
    private short sorting;
    public static final short SORT_NEWEST_TO_OLDEST = 1; //default
    public static final short SORT_OLDEST_TO_NEWEST = 2;
    public static final short SORT_DESIGNATED       = 3;
    public static final short SORT_POSITION_FORWARD = 4;
    public static final short SORT_POSITION_REVERSE = 5;
    public static final short SORT_TIMED_LINEAR     = 6;
    
    //define channel type. anything > 10 is fdm pool. anything > 20 is browse pool
    @Persistent
    private short poolType;
    public static final short POOL_BASE      = 0;
    public static final short POOL_FDM       = 10;
    public static final short POOL_BROWSE    = 20;
    public static final short POOL_BILLBOARD = 30; 
    
    @NotPersistent
    private String recentlyWatchedProgram;
    
    @NotPersistent
    private short cntItem;     // IAP item count
    
    @Persistent
    private int cntEpisode;   // episode count
    
    @NotPersistent
    private int cntFollower;  // follower count
    
    @Persistent
    private int cntSubscribe; // subscription count
    
    @NotPersistent
    private long cntView;     // viewing count, in shard table
    
    @NotPersistent
    private long cntVisit;    // visit count
    
    @Persistent 
    private Date createDate;
        
    @Persistent
    private Date updateDate;
    
    @Persistent
    @Column(jdbcType = NnStringUtil.VARCHAR, length = NnStringUtil.NORMAL_STRING_LENGTH)
    private String transcodingUpdateDate; //timestamps from transcoding server
    
    @Persistent
    @Column(jdbcType = NnStringUtil.VARCHAR, length = NnStringUtil.SHORT_STRING_LENGTH)
    private String userIdStr; //format: shard-userId, example: 1-1
    
    //can be removed if player making a separate query
    //format: shard-userId, example: 1-1, separated by ";"
    //up to 3 subscribers
    @Persistent
    @Column(jdbcType = NnStringUtil.VARCHAR, length = NnStringUtil.NORMAL_STRING_LENGTH)
    private String subscribersIdStr; 
    
    @NotPersistent
    private long categoryId;
    
    @NotPersistent
    private short timeStart;
    
    @NotPersistent
    private short timeEnd;
    
    // used in set, mark as true means the results sorting that this channel will put in the first
    @NotPersistent
    private boolean alwaysOnTop;
    
    @NotPersistent
    private boolean featured;
    
    // used in YouTube Sync Channel, true means back-end will auto sync with YouTube in a fixed time 
    @NotPersistent
    private String autoSync;
    
    @NotPersistent
    private String playbackUrl;
    
    protected static final Logger log = Logger.getLogger(NnChannel.class.getName());
    
    public NnChannel(String name, String intro, String imageUrl) {
        this.name = name;
        this.intro = intro;
        this.imageUrl = imageUrl;
        Date now = NnDateUtil.now();
        this.createDate = now;
        this.updateDate = now;
    }
    
    public NnChannel(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
    
    public String getFakeId(String userProfileUrl) {
        return "f-" + userProfileUrl;
    }
    
    //for fake channel implementation
    public String getIdStr() {
        if (contentType == NnChannel.CONTENTTYPE_FAKE_FAVORITE)
            return this.getNote(); //hack using note to store fake id
        else
            return String.valueOf(id);
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
    
    public String getPlayerName() {
        String name = this.getName(); 
        if (name != null) {         
           name = name.replaceAll("\\s", " ");
           name = NnStringUtil.revertHtml(name);
        }
        return name;
    }
    
    public String getPlayerIntro() {
        String pintro = this.getIntro(); 
        if (pintro != null) {
            pintro = pintro.replaceAll("\\s", " ");
            pintro = pintro.replaceAll("\\|", " ");
        }
        return pintro;
    }
    
    public void setIntro(String intro) {
        if (intro != null) {
            intro = intro.replaceAll("\n", "{BR}");
            intro = intro.replaceAll("\t", " ");
        }
        this.intro = intro;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public String getOneImageUrl() {
        String oneImageUrl = imageUrl;
        if (imageUrl != null && imageUrl.contains("|")) {
            oneImageUrl = oneImageUrl.substring(0, oneImageUrl.indexOf("|"));
        }
        return oneImageUrl;
    }
    
    public String getPlayerPrefImageUrl() {
        String imageUrl = getImageUrl();
        if ((getStatus() == NnChannel.STATUS_ERROR) || 
            (getStatus() != NnChannel.STATUS_WAIT_FOR_APPROVAL &&
             getStatus() != NnChannel.STATUS_SUCCESS && 
             getStatus() != NnChannel.STATUS_PROCESSING)) {    
            imageUrl = IMAGE_ERROR_URL;
        } else if (getImageUrl() == null) {
            imageUrl = ""; 
        }
        
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public boolean isPublic() {
        return isPublic;
    }
    
    public boolean getIsPublic() {
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
    
    public Date getSyncDate() {
        
        if (transcodingUpdateDate == null) {
            return null;
        }
        
        Long syncDate = null;
        try {
            syncDate = Long.valueOf(transcodingUpdateDate);
        } catch (NumberFormatException e) {
            log.info("String value \"" + transcodingUpdateDate + "\" can't evaluate to type Long.");
            return null;
        }
        
        return new Date (syncDate*1000);
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
    
    public long getCntView() {
        return cntView;
    }
    
    public void setCntView(long cntView) {
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
        
        if (userIdStr == null) {
            return 0;
        }
        String[] splits = userIdStr.split("-");
        if (splits.length > 1) {
            
            return Long.parseLong(splits[1]);
            
        } else {
            
            return Long.parseLong(userIdStr);
        }
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
    
    public NnChannel setNote(String note) {
        
        this.note = note;
        
        return this;
    }
    
    public int getCntFollower() {
        return cntFollower;
    }
    
    public void setCntFollower(int cntFollower) {
        this.cntFollower = cntFollower;
    }
    
    public String getSubscribersIdStr() {
        return subscribersIdStr;
    }
    
    public void setSubscribersIdStr(String subscribersIdStr) {
        this.subscribersIdStr = subscribersIdStr;
    }
    
    public long getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }
    
    public int getCntEpisode() {
       return cntEpisode;
    }
    
    public void setCntEpisode(int cntEpisode) {
        this.cntEpisode = cntEpisode;
    }
    
    public String getMoreImageUrl() {
        return moreImageUrl;
    }
    
    public void setMoreImageUrl(String moreImageUrl) {
    
        this.moreImageUrl = moreImageUrl;
    }
    
    public short getCntItem() {
        return cntItem;
    }
    
    public void setCntItem(short cntItem) {
        this.cntItem = cntItem;
    }
    
    public long getCntVisit() {
        return cntVisit;
    }
    
    public void setCntVisit(long cntVisit) {
        this.cntVisit = cntVisit;
    }
    
    public short getTimeStart() {
        return timeStart;
    }
    
    public void setTimeStart(short timeStart) {
        this.timeStart = timeStart;
    }
    
    public short getTimeEnd() {
        return timeEnd;
    }
    
    public void setTimeEnd(short timeEnd) {
        this.timeEnd = timeEnd;
    }
    
    public boolean isAlwaysOnTop() {
        return alwaysOnTop;
    }
    
    public void setAlwaysOnTop(boolean alwaysOnTop) {
        this.alwaysOnTop = alwaysOnTop;
    }
    
    public boolean isReadonly() {
        return readonly;
    }
    
    public NnChannel setReadonly(boolean readonly) {
        
        this.readonly = readonly;
        
        return this;
    }
    
    public String getAutoSync() {
        return autoSync;
    }
    
    public void setAutoSync(String autoSync) {
        this.autoSync = autoSync;
    }
    
    public String getPlaybackUrl() {
        
        return playbackUrl;
    }
    
    public void setPlaybackUrl(String playbackUrl) {
    
        this.playbackUrl = playbackUrl;
    }
    
    public boolean isFeatured() {
        return featured;
    }
    
    public void setFeatured(boolean featured) {
        this.featured = featured;
    }
    
    public boolean isPaidChannel() {
        return paidChannel;
    }
    
    public void setPaidChannel(boolean paidChannel) {
        this.paidChannel = paidChannel;
    }
    
    public String getBannerImageUrl() {
        return bannerImageUrl;
    }
    
    public void setBannerImageUrl(String bannerImageUrl) {
        this.bannerImageUrl = bannerImageUrl;
    }
    
    public String getSocialFeeds() {
        return socialFeeds;
    }
    
    public void setSocialFeeds(String socialFeeds) {
        this.socialFeeds = socialFeeds;
    }
}
