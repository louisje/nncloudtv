package com.nncloudtv.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.RandomStringUtils;
import org.springframework.stereotype.Service;

import com.nncloudtv.dao.NnUserDao;
import com.nncloudtv.lib.AuthLib;
import com.nncloudtv.lib.NNF;
import com.nncloudtv.lib.NnDateUtil;
import com.nncloudtv.lib.NnLogUtil;
import com.nncloudtv.lib.NnNetUtil;
import com.nncloudtv.lib.NnStringUtil;
import com.nncloudtv.model.Mso;
import com.nncloudtv.model.NnChannel;
import com.nncloudtv.model.NnGuest;
import com.nncloudtv.model.NnUser;
import com.nncloudtv.model.NnUserPref;
import com.nncloudtv.model.NnUserProfile;
import com.nncloudtv.web.api.ApiContext;
import com.nncloudtv.web.api.NnStatusCode;
import com.nncloudtv.web.json.cms.User;
import com.nncloudtv.web.json.facebook.FacebookMe;
import com.nncloudtv.web.json.player.UserInfo;

@Service
public class NnUserManager {
    
    protected static final Logger log = Logger.getLogger(NnUserManager.class.getName());
    
    private NnUserDao dao = NNF.getUserDao();
    
    public static final short MSO_DEFAULT = 1;
    
    //@@@IMPORTANT email duplication is your responsibility
    public int create(NnUser user, HttpServletRequest req, short shard) {
        if (this.findByEmail(user.getEmail(), user.getMsoId(), req) != null) //!!!!! shard or req flexible
            return NnStatusCode.USER_EMAIL_TAKEN;
        NnUserProfile profile = user.getProfile();
        if (profile.getName() != null)
            profile.setName(profile.getName().replaceAll("\\s", " "));
        user.setEmail(user.getEmail().toLowerCase());
        if (shard == 0)
            shard= getShardByLocale(req);
        if (user.getToken() == null)
            user.setToken(NnUserManager.generateToken(shard));
        user.setShard(shard);
        Date now = NnDateUtil.now();
        user.setCreateDate(now);
        user.setUpdateDate(now);
        if (profile.getProfileUrl() == null) {
            profile.setProfileUrl(this.generateProfile(user.getProfile().getName()));
        }
        dao.save(user);
        profile.setUserId(user.getId());
        NNF.getProfileMngr().save(user, profile);
        resetChannelCache(user);
        return NnStatusCode.SUCCESS;
    }
    
    public NnUser createFakeYoutube(Map<String, String> info, HttpServletRequest req) {
        String name = info.get("author");
        if (name == null)
            return null;
        String imageUrl = info.get("thumbnail");
        
        /*
        if (info.get("type").equals("playlist")) {
            log.info("author:" + name);
            Map<String, String> authorData = YouTubeLib.getYouTubeEntry(name, true);
            imageUrl = authorData.get("thumbnail");
        }
        */
        name = name.toLowerCase();
        name = name.replaceAll("\\s", "");
        String email = name + "@9x9.tv";
        log.info("fake youtube email:" + email);
        NnUser user = this.findByEmail(email, 1, req);
        if (user != null)
            return user;
        user = new NnUser(email, "9x9x9x", NnUser.TYPE_FAKE_YOUTUBE);
        user.setShard((short)1);
        user.setMsoId(1);
        NnUserProfile profile = user.getProfile();
        user = this.save(user, true);
        profile.setProfileUrl(name);
        profile.setImageUrl(imageUrl);
        profile.setUserId(user.getId());
        NNF.getProfileMngr().save(user, user.getProfile());
        log.info("fake youtube user created:" + email);
        return user;
    }
    
    public NnUser setFbProfile(NnUser user, FacebookMe me) {
        if (user == null || me == null)
            return null;
        NnUserProfile profile = user.getProfile();
        if (me.getId() != null) {
            String imageUrl = "http://graph.facebook.com/" + me.getId() + "/picture?width=180&height=180";
            profile.setImageUrl(imageUrl);
        }
        user.setEmail(me.getId());
        user.setFbId(me.getEmail());
        profile.setName(me.getName());
        profile.setGender(me.getGender());
        profile.setSphere(me.getLocale());
        profile.setDob(me.getBirthday());
        user.setToken(me.getAccessToken());
        return user;
    }
    
    //TODO replace name with none-digit/characters
    public String generateProfile(String name) {
        String profile = RandomStringUtils.randomNumeric(10);
        return profile;
        /*
        String profile = "";
        if (name != null) {
            String random = RandomStringUtils.randomNumeric(5);
            name.replace(" ", "");
            profile = random + "-" + name;
        } else {
            profile = RandomStringUtils.randomAlphabetic(10);
        }
        return profile;
        */
    }
    
    public String forgotPwdToken(NnUser user) {
        byte[] pwd = user.getCryptedPassword();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] thedigest = md.digest(pwd);
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < thedigest.length; i++) {
              sb.append(Integer.toString((thedigest[i] & 0xff) + 0x100, 16).substring(1));
            }
            String token = sb.toString();
            log.info("Digest(in hex format):: " + token);
            return token;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    //Default is 1; Asia (tw, cn, hk) is 2
    public short getShardByLocale(HttpServletRequest req) {
        String locale = findLocaleByHttpRequest(req);
        short shard = NnUser.SHARD_DEFAULT;
        if (locale.equals("tw") || locale.equals("cn") || locale.equals("hk")) {
            shard = NnUser.SHARD_CHINESE;
        }
        return shard;
    }
    
    public String findLocaleByHttpRequest(HttpServletRequest req) {
        String ip = req.getRemoteAddr();
        log.info("findLocaleByHttpRequest() ip is " + ip);
        ip = NnNetUtil.getIp(req);
        log.info("try to find ip behind proxy " + ip);
        String country = "";
        try {
            //URL url = new URL("http://brussels.teltel.com/geoip/?ip=" + ip);
            URL url = new URL("http://geoip.9x9.tv/geoip/?ip=" + ip);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setDoOutput(true);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                log.info("findLocaleByHttpRequest() IP service returns error:" + connection.getResponseCode());
            }
            BufferedReader rd  = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line = rd.readLine();
            if (line != null) {
                log.info("country from locale service:" + line);
                country = line.toLowerCase();
            } //assuming one line
            rd.close();
        } catch (java.net.SocketTimeoutException e) {
           log.info("socket timeout");
        } catch (java.net.SocketException e) {
           log.info("socket connect exception");
        } catch (Exception e) {
            log.info("exception");
            NnLogUtil.logException(e);
        } finally {
        }
        log.info("country from query:" + country + ";with ip:" + ip);
        String locale = "en";
        if (country.equals("tw")) {
            locale = "zh";
        }
        return locale;
    }
    
    public static short shardIterate(short shard) {
        if (shard == NnUser.SHARD_DEFAULT)
            return NnUser.SHARD_CHINESE;
        return NnUser.SHARD_DEFAULT;
    }
    
    public NnUser createGuest(Mso mso, HttpServletRequest req) {
        String password = String.valueOf(("token" + Math.random() + NnDateUtil.timestamp()).hashCode());
        NnUser guest = new NnUser(NnUser.GUEST_EMAIL, password, NnUser.TYPE_USER);
        this.create(guest, req, (short)0);
        return guest;
    }
    
    public NnUser save(NnUser user, boolean resetCache) {
        if (user == null) {
            return null;
        }
        if (user.getPassword() != null) {
            user.setSalt(AuthLib.generateSalt());
            user.setCryptedPassword(AuthLib.encryptPassword(user.getPassword(), user.getSalt()));
        }
        user.setEmail(user.getEmail().toLowerCase());
        user.setUpdateDate(NnDateUtil.now());
        NnUserProfile profile = user.getProfile();
        profile = NNF.getProfileMngr().save(user, profile);
        if (resetCache)
            resetChannelCache(user);
        long msoId = user.getMsoId();
        user = dao.save(user);
        user.setMsoId(msoId);
        user.setProfile(profile);
        return user;
    }
    
    public void resetChannelCache(NnUser user) {
        NnChannelManager chMngr = NNF.getChannelMngr();
        List<NnChannel> channels = chMngr.findByUser(user, 0, false);
        chMngr.resetCache(channels);
    }
    
    /**
     * GAE can only write 5 records a sec, maybe safe enough to do so w/out DB retrieving.
     * taking the chance to speed up signin (meaning not to consult DB before creating the account).
     */
    public static String generateToken(short shard) {
        if (shard == 0)
            return null;
        String time = String.valueOf(NnDateUtil.timestamp());
        String random = RandomStringUtils.randomAlphabetic(10);
        String result = time + random;
        result = RandomStringUtils.random(20, 0, 20, true, true, result.toCharArray());
        result = shard + "-" + result;
        return result;
    }
    
    private NnUser populateUserProfile(NnUser user) {
        if (user == null) return null;
        NnUserProfile profile = NNF.getProfileMngr().findByUser(user);
        if (profile == null)
            profile = new NnUserProfile(user.getId(), user.getMsoId());
        user.setProfile(profile);
        return user;
    }
    
    //TODO able to assign shard
    //find by email means find by unique id
    public NnUser findByEmail(String email, long msoId, HttpServletRequest req) {
        short shard = getShardByLocale(req);
        log.info("find by email:" + email.toLowerCase());
        NnUser user = dao.findByEmail(email.toLowerCase(), shard);
        if (user != null && msoId > 0) {
            user.setMsoId(msoId);
            user = populateUserProfile(user);
        }
        return user;
    }
    
    // TODO rewrite
    public NnUser findAuthenticatedUser(String email, String password, long msoId, HttpServletRequest req) {
        short shard = getShardByLocale(req);
        NnUser user = dao.findAuthenticatedUser(email.toLowerCase(), password, shard);
        if (user != null && msoId > 0) {
            user.setMsoId(msoId);
            user = populateUserProfile(user);
        }
        return user;
    }
    
    public NnUser resetPassword(NnUser user) {
        user.setPassword(user.getPassword());
        user.setSalt(AuthLib.generateSalt());
        user.setCryptedPassword(AuthLib.encryptPassword(user.getPassword(), user.getSalt()));
        return save(user, true);
    }
     
    public void subscibeDefaultChannels(NnUser user) {
        
        List<NnChannel> channels = NNF.getChannelMngr().findMsoDefaultChannels(user.getMsoId(), false);
        NnUserSubscribeManager subManager = new NnUserSubscribeManager();
        for (NnChannel c : channels) {
            subManager.subscribeChannel(user, c);
        }
        log.info("user " +  user.getId() + "(" + user.getToken() + ") subscribe " + channels.size() + " channels (mso:" + user.getMsoId() + ")");
    }
    
    public NnUser findByToken(String token, long msoId) {
        if (token == null) { return null; }
        NnUser user = dao.findByToken(token);
        if (user != null && msoId > 0) {
            user.setMsoId(msoId);
            populateUserProfile(user);
        }
        return user;
    }
    
    //expect format shard-userId. example 1-1
    //if "-" is not present, assuming it's shard 1
    public NnUser findByIdStr(String idStr, long msoId) {
        if (idStr == null)
            return null;
        String[] splits = idStr.split("-");
        short shard = 1;
        long uid = 0;
        if (splits.length == 2) {
            uid = Long.parseLong(splits[1]);
            if (splits[0].equals("2"))
                shard = 2;
        } else {
            uid = Long.parseLong(idStr);
        }
        return this.findById(uid, msoId, shard);
    }
    
    //TODO add shard search
    public NnUser findByFbId(String fbId, long msoId) {
        return dao.findByFbId(fbId);
    }
    
    // find user by ID without providing shard number
    public NnUser findById(long id, long msoId) {
        NnUser user = dao.findById(id);
        if (user != null && msoId > 0) {
            user.setMsoId(msoId);
            user = populateUserProfile(user);
        }
        return user;
    }
    
    public NnUser findById(long id, long msoId, short shard) {
        NnUser user = dao.findById(id, shard);
        if (user != null && msoId > 0) {
            user.setMsoId(msoId);
            user = populateUserProfile(user);
        }
        return user;
    }
    
    //specify email or name is used in flipr, otherwise use generic to match email/name/intro
    public List<NnUser> search(String email, String name, String generic, long msoId) {
        List<NnUser> users = dao.search(email, name, generic, msoId);
        for (NnUser user : users ) {
            user.setMsoId(msoId);
            user = this.populateUserProfile(user);
        }
        return users;
        
    }
    
    public List<NnUser> findFeatured(long msoId) {
        List<NnUser> users = dao.findFeatured(msoId);
        for (NnUser user : users ) {
            user.setMsoId(msoId);
            user = populateUserProfile(user);
        }
        return users;
    }
    
    public NnUser findByProfileUrl(String profileUrl, long msoId) {
        NnUser user = dao.findByProfileUrl(profileUrl);
        if (user != null && msoId > 0) {
            user.setMsoId(msoId);
            user = populateUserProfile(user);
        }
        return user;
    }
    
    public String composeCuratorInfo(ApiContext ctx, List<NnUser> users, boolean chCntLimit, boolean isAllChannel) {
        log.info("looking for all channels of a curator?" + isAllChannel);
        String result = "";
        NnChannelManager chMngr = NNF.getChannelMngr();
        List<NnChannel> curatorChannels = new ArrayList<NnChannel>();
        for (NnUser u : users) {
            List<NnChannel> channels = new ArrayList<NnChannel>();
            if (chCntLimit) {
                channels = chMngr.findByUserAndHisFavorite(u, 1, isAllChannel);
            } else {
                channels = chMngr.findByUserAndHisFavorite(u, 0, isAllChannel);
            }
            curatorChannels.addAll(channels);
            String ch = "";
            if (channels.size() > 0) {
                ch = channels.get(0).getIdStr();
            }
            result += this.composeCuratorInfoStr(u, ch, ctx) + "\n";
        }
        result += "--\n";
        System.out.println("curator channel:" + curatorChannels.size());
        result += chMngr.composeChannelLineup(curatorChannels, ctx);
        return result;
    }
    
    public String composeSubscriberInfoStr(List<NnChannel> channels) {
        for (NnChannel c : channels) {
            if (c.getContentType() != NnChannel.CONTENTTYPE_FAKE_FAVORITE) {
            }
        }
        return "";
    }
    
    public String composeCuratorInfoStr(NnUser user, String channelId, ApiContext ctx) {
        //#!curator=xxxxxxxx
        String profileUrl = "";
        NnUserProfile profile = user.getProfile();
        if (profile.getProfileUrl() != null) {
            profileUrl = ctx.getRoot() + "/#!curator=" + profile.getProfileUrl();
        }
        
        String[] info = {
                profile.getBrandUrl(),
                profile.getName(),
                profile.getIntro(),
                profile.getImageUrl(),
                profileUrl,
                String.valueOf(profile.getCntChannel()),
                String.valueOf(profile.getCntSubscribe()),
                String.valueOf(profile.getCntFollower()),
                channelId,
               };
        String output = NnStringUtil.getDelimitedStr(info);
        return output;
    }
    
    public static boolean isGuestByToken(String token) {
        if (token == null || token.length() == 0)
            return true;
        if (token != null && token.contains(NnGuest.TOKEN_PREFIX)) {
            return true;
        }
        return false;
    }
    
    public NnUser purify(NnUser user) {
    
        if (user == null) {
            return null;
        }
        user.setSalt(null);
        user.setCryptedPassword(null);
        
        NnUserProfile profile = user.getProfile();
        if (profile != null) {
            profile.setName(NnStringUtil.revertHtml(profile.getName()));
            profile.setIntro(NnStringUtil.revertHtml(profile.getIntro()));
        }
        
        return user;
    }
    
    public List<NnUser> findAllByIds(Collection<Long> userIdSet, short shard) {
        
        return dao.findAllByIds(userIdSet, NnUserDao.getPersistenceManagerFactory(shard, null));
    }
    
    //UserInfo or String
    public Object getPlayerUserInfo(NnUser user, NnGuest guest, HttpServletRequest req, boolean login, short format) {
        if (user != null) {
            //prepare all the values
            String token = user.getToken();
            String userIdStr = user.getIdStr();
            NnUserProfile profile = user.getProfile();
            String name = profile.getName();
            if (name == null)
                name = user.getEmail();
            String lastLogin = String.valueOf(profile.getUpdateDate().getTime());
            String sphere = profile.getSphere();
            if (profile.getSphere() == null)
                sphere = findLocaleByHttpRequest(req);
            String lang = profile.getLang();
            if (profile.getLang() == null)
                lang = sphere;
            String curator = String.valueOf(profile.getProfileUrl());
            String created = "0";
            if (login)
                created = "1";
            String fbUser = "0";
            if (user.isFbUser())
                fbUser = "1";
            //format
            if (format == ApiContext.FORMAT_JSON) {
                UserInfo json = new UserInfo();
                json.setToken(token);
                json.setUserIdStr(userIdStr);
                json.setName(name);
                json.setLastLogin(lastLogin);
                json.setSphere(sphere);
                json.setUiLang(lang);
                json.setCurator(curator);
                json.setCreated(Boolean.parseBoolean(created));
                json.setFbUser(Boolean.parseBoolean(fbUser));
                List<NnUserPref> list = NNF.getPrefMngr().findByUser(user);
                for (NnUserPref pref : list) {
                    json.getPrefs().add(pref.getItem() + pref.getValue());
                }
                return json;
            } else {
                String output = PlayerApiService.assembleKeyValue("token", token);
                output += PlayerApiService.assembleKeyValue("userid", userIdStr);
                output += PlayerApiService.assembleKeyValue("name", name);
                output += PlayerApiService.assembleKeyValue("lastLogin", lastLogin);
                output += PlayerApiService.assembleKeyValue("sphere", sphere);
                output += PlayerApiService.assembleKeyValue("ui-lang", lang);
                output += PlayerApiService.assembleKeyValue("curator", curator);
                output += PlayerApiService.assembleKeyValue("created", created);
                output += PlayerApiService.assembleKeyValue("fbUser", fbUser);
                List<NnUserPref> list = NNF.getPrefMngr().findByUser(user);
                for (NnUserPref pref : list) {
                    output += PlayerApiService.assembleKeyValue(pref.getItem(), pref.getValue());
                }
                return output;
            }
        } else {
            return new NnGuestManager().getPlayerGuestRegister(guest, format, req);
        }
    }
    
    public static User composeUser(NnUser nnuser) {
        
        User user = new User();
        
        user.setId(nnuser.getId());
        user.setType(nnuser.getType());
        user.setShard(nnuser.getShard());
        user.setIdStr(nnuser.getIdStr());
        user.setCreateDate(nnuser.getCreateDate());
        user.setUpdateDate(nnuser.getUpdateDate());
        user.setUserEmail(nnuser.getUserEmail());
        user.setFbUser(nnuser.isFbUser());
        user.setPriv(NnUserProfile.PRIV_CMS);
        
        NnUserProfile profile = nnuser.getProfile();
        if (profile != null) {
            user.setName(NnStringUtil.revertHtml(profile.getName()));
            user.setIntro(NnStringUtil.revertHtml(profile.getIntro()));
            user.setImageUrl(profile.getImageUrl());
            user.setLang(profile.getLang());
            user.setProfileUrl(profile.getProfileUrl());
            user.setSphere(profile.getSphere());
            user.setCntSubscribe(profile.getCntSubscribe());
            user.setCntChannel(profile.getCntChannel());
            user.setCntFollower(profile.getCntFollower());
            user.setMsoId(profile.getMsoId());
            if (profile.getPriv() != null)
                user.setPriv(profile.getPriv());
            Mso mso = NNF.getMsoMngr().findById(profile.getMsoId());
            if (mso != null)
                user.setMsoName(mso.getName());
        }
        
        return user;
    }
}
