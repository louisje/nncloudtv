package com.nncloudtv.web.api;

import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.nncloudtv.exception.NotPurchasedException;
import com.nncloudtv.lib.CookieHelper;
import com.nncloudtv.lib.FacebookLib;
import com.nncloudtv.lib.NNF;
import com.nncloudtv.lib.NnLogUtil;
import com.nncloudtv.lib.NnStringUtil;
import com.nncloudtv.model.LocaleTable;
import com.nncloudtv.model.Mso;
import com.nncloudtv.model.NnChannel;
import com.nncloudtv.model.NnEpisode;
import com.nncloudtv.service.NnChannelManager;
import com.nncloudtv.service.NnStatusMsg;
import com.nncloudtv.service.PlayerApiService;
import com.nncloudtv.web.json.facebook.FacebookMe;
import com.nncloudtv.web.json.player.ChannelLineup;

/**
// * This is API specification for 9x9 chaPlayer. Please note although the document is written in JavaDoc form, it is generic Web Service API via HTTP request-response, no Java necessary.
 * <p>
 * <blockquote>/
// * Example:
 * <p>
 * Player Request: <br/>
 * http://qa.9x9.tv/playerAPI/brandInfo?mso=9x9
 * <p>
 * Service response:  <br/>
 * 0    success<br/>
 * --<br/>
 * name        9x9<br/>
 * title    9x9.tv<br/>
 * </blockquote>
 * <p>
 * <b>In this document, method name is used as part of the URL</b>, examples:
 * <p>   
 * <blockquote>
 * http://hostname:port/playerAPI/channelBrowse?category=1<br/>
 * http://hostname:port/playerAPI/brandInfo?mso=9x9<br/>
 * </blockquote>
 * 
 * <p>
 * <b>API categories:</b
 * <p>
 * <blockquote>
 * Brand information: brandInfo
 * <p>
 * Account related: guestRegister, signup, login, userTokenVerify, signout, fbLogin, fbSignup, 
 *                  setUserProfile, getUserProfile, setUesrPref
 * <p>
 * <p>
 * Category listing: categoryInfo, tagInfo, setInfo
 * <p>
 * Curator: curator
 * <p>
 * Channel and program listing: channelLineup, programInfo
 * <p>
 * IPG action: moveChannel, channelSubmit, subscribe, unsubscribe, setSetInfo
 * <p>
 * YouTube connect: obtainAccount, bulkSubscribe 
 * <p>
 * YouTube info update: virtualChannelAdd, channelUpdate 
 * <p>
 * Data collection: pdr, programRemove
 * <p>
 * System message: staticContent 
 * </blockquote>
 * <p>
 * <b>9x9 Player API always returns a string:</b>
 * <p>
 * First line is status code and status message, separated by tab.<br/>
 * <p>
 * Different sets of data are separated by "--\n".
 * <p>
 * Data representation is \t separated of each field, \n separated of each record.
 * <p>
 * <blockquote>
 * Example 1: login 
 * <p>
 * 0    success  <br/>
 * -- <br/>
 * token    a466D491UaaU245P412a <br/>
 * name    a
 * <p>
 * Example 2: categoryBrowse
 * <p>
 * 0    success  <br/>
 * -- <br/>
 * 1201    Movie    5 <br/>
 * 1203    TV    2 <br/>
 * 1204 Sports 2 <br/>
 * </blockquote>
 * <p>     
 * Please note each api's document omits status code and status message.
 * <p>    
 * <b>Basic API flows:</b>
 * <blockquote>
 * The first step is to call brandInfo to retrieve brand information. It returns brand id, brand logo, and any necessary brand information.
 * <p>
 * The next step depends on the UI requirement. Use categoryBrowse to find category listing based on the brand. 
 * Or get an account first. Use userTokenVerify if there's an existing user token. 
 * If there's no token at hand, either sign up for user as a guest(guestRegister) or ask user to signup(signup).
 * <p>
 * Channel and program listing(channelLineup and programInfo) would be ready after an account is registered.
 * <p>
 * </blockquote>
 * <p>
 * <b>Guideline</b> 
 * <blockquote>
 * If there's any API change in terms of return value, new fields will be added to the end of the line, or present in the next block.
 * <p>
 * Please prepare your player being able to handle it. i.e. existing player should NOT have to modify your code to be able to work with this kind of API change.
 * </blockquote>
 */

@Controller
@RequestMapping("playerAPI")
public class PlayerApiController {
    
    protected static final Logger log = Logger.getLogger(PlayerApiController.class.getName());
    
    protected static final PlayerApiService playerApiService = NNF.getPlayerApiService(); // reusable, save memory
    protected static final String PLAYER_ERR_MSG = NnStatusMsg.getPlayerMsg(NnStatusCode.ERROR);
    
    /**
     * To be ignored  
     */
    @ExceptionHandler(Exception.class)
    public String exception(Exception e) {
        NnLogUtil.logException(e);
        return "error/blank";
    }
    
    /**
     * Register a guest account. A "guest" cookie will be set.
     * If ipg is provided, guest is automatically subscribed to all the channels in the ipg. 
     * 
     * @param ipg ipg identifier, it is optional
     * @return please reference login
     */    
    @RequestMapping(value="guestRegister")
    public @ResponseBody Object guestRegister(
            @RequestParam(value="ipg", required = false) String ipg, 
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req, 
            HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.guestRegister(ctx, resp);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    } 
    
    /**
     * <p> Get suggested apps
     *  
     * @param mso mso name 
     * @param sphere "en" or "zh"
     * @param os "android" or "ios", or keep it empty for server to decide.
     * @return <p>Returns data in two sections. First is the short list, second is the complete list. </p>
     *         <p>Each app has the following information: app name, app description, app thumbnail, app store url
     */
    @RequestMapping(value="relatedApps", produces = ApiGeneric.PLAIN_TEXT_UTF8)
    public @ResponseBody Object relatedApps(
            @RequestParam(value = "stack",  required = false) String stack,
            @RequestParam(value = "sphere", required = false) String sphere,
            HttpServletRequest req, HttpServletResponse resp) {
        
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.relatedApps(ctx, stack, sphere);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return output;
    } 
    
    /**
     *  User signup.
     *  
     *  <p>only POST operation is supported.</p>
     *  
     *  @param email email
     *  @param password password
     *  @param name display name
     *  @param captcha captcha image file name
     *  @param text captcha text
     *  @param sphere zh or en
     *  @param ui-lang zh or en
     *  @param year year or birth
     *  @param temp not specify means false 
     *  @return please reference login
     */    
    @RequestMapping(value="signup")
    public @ResponseBody Object signup(HttpServletRequest req, HttpServletResponse resp) {
        
        ApiContext ctx = new ApiContext(req);
        String email     = ctx.getParam("email");
        String password  = ctx.getParam("password");
        String mso       = ctx.getParam("mso");
        String name      = ctx.getParam("name");
        String gender    = ctx.getParam("gender");
        String userToken = ctx.getParam("user");
        String captcha   = ctx.getParam("captcha");
        String text      = ctx.getParam("text");
        String sphere    = ctx.getParam("sphere", LocaleTable.LANG_EN);
        String year      = ctx.getParam("year");
        String uiLang    = ctx.getParam("ui-lang", LocaleTable.LANG_EN);
        String rx        = ctx.getParam("rx");
        boolean isTemp   = Boolean.parseBoolean(ctx.getParam("temp"));
                
        log.info("signup: email=" + email + ";name=" + name + ";mso:" + mso + 
                 ";userToken=" + userToken + ";sphere=" + sphere + 
                 ";year=" + year + ";ui-lang=" + uiLang + 
                 ";rx=" + rx);
        Object output = PLAYER_ERR_MSG;
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.signup(ctx, email, password, name, userToken, captcha, text, sphere, uiLang, year, gender, isTemp, resp);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }    
    
    /**
     * Pass every param passing from Facebook in its original format
     * 
     * @param id facebook id
     * @param name name
     * @param username facebook username
     * @param birthday birthday
     * @param email user email
     * @param locale locale
     * @param token access token
     * @param expire expiration date
     * @return 
     */
    @RequestMapping(value="fbSignup")
    public @ResponseBody Object fbSignup(HttpServletRequest req, HttpServletResponse resp) {
        String msoString = req.getParameter("mso");
        if (msoString == null) {
            msoString = "9x9"; 
        }
        FacebookMe me = new FacebookMe();
        me.setId(req.getParameter("id"));
        me.setName(req.getParameter("name"));
        me.setUsername(req.getParameter("username"));
        me.setBirthday(req.getParameter("birthday"));
        me.setEmail(req.getParameter("email"));
        me.setLocale(req.getParameter("locale"));
        me.setAccessToken(req.getParameter("token"));
        String expire = req.getParameter("expire");
        
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.fbDeviceSignup(ctx, me, expire, msoString, resp);
        } catch (Exception e){
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
        
     }
    
    /**
     * Verify user token <br/>
     * Example: http://host:port/playerAPI/userTokenVerify?token=QQl0l208W2C4F008980F
     * 
     * @param token user key 
     * @return Will delete the user cookie if token is invalid.<br/>
     *            Return info please reference login.
     */    
    @RequestMapping(value="userTokenVerify")
    public @ResponseBody Object userTokenVerify(
            @RequestParam(value="token") String token,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req, 
            HttpServletResponse resp) {
        log.info("userTokenVerify() : userToken=" + token);
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.userTokenVerify(ctx, token, resp);
        } catch (Exception e){
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * "user" cookie will be removed
     * 
     * @param user user key identifier 
     */        
    @RequestMapping(value="signout")
    public @ResponseBody Object signout(
            @RequestParam(value="user", required=false) String userKey,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            CookieHelper.deleteCookie(resp, CookieHelper.USER);
            CookieHelper.deleteCookie(resp, CookieHelper.GUEST);
            output = NnStatusMsg.getPlayerMsg(NnStatusCode.SUCCESS);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }    
    
    /**
     * Get brand information. 
     *     
     * @param mso mso name, optional, server returns default mso 9x9 if omiited
     * @param os "android" or "ios" or "web" or leave it empty and determined by server.
     * @return <p>Data returns in key and value pair. Key and value is tab separated. Each pair is \n separated.<br/> 
     *            keys include "key", "name", logoUrl", "jingleUrl", "preferredLangCode" "debug", "facebook-clientid", "youtube", "chromecast-id", "ga", "flurry"<br/></p>
     *         <p>Example: <br/>
     *          0    success <br/>
     *          --<br/>
     *          key    1<br/>
     *          name    9x9<br/>
     *          title    9x9.tv<br/>
     *          logoUrl    /WEB-INF/../images/logo_9x9.png<br/>
     *          jingleUrl    /WEB-INF/../videos/opening.swf<br/>
     *          logoClickUrl    /<br/>
     *          preferredLangCode    en<br/>
     *          debug    1<br/>
     *         </p>
     *         <p>Ff <b>ad=1</b> parameter is carried, AdInfo will be appended as a section block.<br/>
     *         Format: [id] TAB [type] TAB [name] TAB [url]<br/>
     *         </p>
     *         <p>Example: <br/>
     *          --<br/>
     *          1   0   ad hello world!!   http://s3.aws.amazon.com/creative_video0.mp4<br/>
     *          2   1   vastAd hello world!!   http://rec.scupio.com/recweb/vast.aspx?creativeid=9x9test&d=15&video=VAD20140507123513649.mp4&adid=133872&rid=7241<br/>
     *         </p>
     */    
    @RequestMapping(value = "brandInfo")
    public @ResponseBody Object brandInfo(HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.brandInfo(ctx);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    //http://stackoverflow.com/questions/4403643/supporting-multiple-content-types-in-a-spring-mvc-controller/4404881#4404881
    //http://stackoverflow.com/questions/3620323/how-to-change-default-media-type-for-spring-mvc-from-xml-to-json
    //http://www.mkyong.com/spring-mvc/spring-3-mvc-contentnegotiatingviewresolver-example/
    //http://stackoverflow.com/questions/3616359/who-sets-response-content-type-in-spring-mvc-responsebody/5268157#5268157
    //0 produces = "*.*;charset"
    //1. try the setting http://forum.spring.io/forum/spring-projects/web/74209-responsebody-and-utf-8
    //2. try the chinese way
    //3. separate the xml and text/plain    
    //@RequestMapping(value="detect")    
    @RequestMapping(value="detect")
    public @ResponseBody Object detect(String format, HttpServletResponse resp, HttpServletRequest req) {
        System.out.println("content-type:" + req.getContentType());
        String contentType = req.getContentType();
        if (contentType == null || (contentType != null && !contentType.contains("json"))  ) {
            return "hello world\n";
            /*
            try {
                resp.setContentType("text/plain;charset=utf-8");                
                resp.getWriter().print("hello world\n");
                resp.flushBuffer();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "empty\n";
            */
        }
        NnChannelManager chMngr = NNF.getChannelMngr();
        NnChannel c = chMngr.findById(1);
        ChannelLineup json = (ChannelLineup) chMngr.composeEachChannelLineup(c, new ApiContext(req));
        return json;
        //return "player/api";
    }
    
    /**   
     * For directory query. Returns list of categories.
     * To get furthur info, use categoryInfo   
     * 
     * @param lang en or zh 
     * @return <p>Block one, the requested category info. Always one for now. Block two, list of categories.
     *            List of categories has category id, cateogory name, channel count, items after category. 
     *            Currently items after category will always be "ch" indicating channels.     
     *         <p>Example: <br/>
     *            id    0 <br/>
     *            --<br/>
     *            1    News    55    ch <br/>
     *            9    Sports    124    ch <br/>
     *            22    Music    165    ch
     */
    @RequestMapping(value="category")
    public @ResponseBody Object category(
            @RequestParam(value="category", required=false) String category,
            @RequestParam(value="flatten", required=false) String isFlatten,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req,
            HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            boolean flatten = Boolean.parseBoolean(isFlatten);
            output = playerApiService.category(ctx, category, flatten);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Collecting PDR
     * 
     * @param user user token
     * @param session indicates the session when user starts using the player
     * @param pdr pdr data
     *           <p> Expecting lines(separated by \n) of the following:<br/>  
     *           delta verb info <br/>
     *           Example: delta watched 1 1 2 3 <br/>
     *           Note: first 1 is channel, the rest are program ids. <br/>  
     *           Note: each field is separated by tab.
     *           </p> 
     */    
    @RequestMapping(value="pdr")
    public @ResponseBody Object pdr(
            @RequestParam(value="user", required=false) String userToken,
            @RequestParam(value="device", required=false) String deviceToken,
            @RequestParam(value="session", required=false) String session,
            @RequestParam(value="pdr", required=false) String pdr,
            @RequestParam(value="rx", required=false) String rx,
            HttpServletRequest req, HttpServletResponse resp) {
        
        ApiContext ctx = new ApiContext(req);
        String pdrServer = ctx.getRoot();
        String path = "/playerAPI/pdrServer";
        
        if (ctx.isProductionSite()) {
            pdrServer = "http://v32d.9x9.tv";
        } else {
            log.info("at pdr devel server");
        }
        try {
            String urlStr = pdrServer + path;
            log.info("forward to " + urlStr);
            String params = "user=" + userToken + 
             "&device=" + deviceToken + 
             "&session=" + session +
             "&pdr=" + NnStringUtil.urlencode("" + pdr) +
             "&rx=" + rx +
             "&mso=" + ctx.getMsoName();
            //log.info(urlStr + "?" + params);
            
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(params);
            writer.close();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                log.info("redirection failed");
            }
        } catch(ConnectException e) {
            log.warning("connect to pdr server timeout, but not big problem.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return NnStatusMsg.getPlayerMsg(NnStatusCode.SUCCESS);
    }
    
    /**
     * To be ignored 
     */
    @RequestMapping(value="pdrServer")
    public @ResponseBody Object pdrServer(
            @RequestParam(value="user", required=false) String userToken,
            @RequestParam(value="device", required=false) String deviceToken,
            @RequestParam(value="session", required=false) String session,
            @RequestParam(value="pdr", required=false) String pdr,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req,
            HttpServletResponse resp) {
        log.info("user=" + userToken + ";device=" + deviceToken + ";session=" + session);
        //log.info("pdr = " + pdr);
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.assemblePlayerMsgs(NnStatusCode.DATABASE_READONLY);
            output = playerApiService.pdr(ctx, userToken, deviceToken, session, pdr);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Retrieves set information. A "set" can be a dayparting, or a set.
     * 
     * @param set set id
     * @param landing the name used as part of the URL. query with either set or landing
     * @param programInfo set to  
     * @return first block: status <br/>
     *         second block: brand info, returns in key and value pair. <br/>                     
     *         third block: set info, returns in key and value pair. 
     *         Examples: id, name, imageUrl, bannerImageUrl, bannerImageUrl2, channeltag. "channeltag" format please reference "portal" <br/>
     *         4th block: channel details. reference "channelLineup". <br/>
     *         5th block: first episode of every channel from the 4th block. reference "programInfo". <br/>
     *         <p>
     *         Example: <br/>
     *         0    success<br/>
      *         --<br/>
     *         name    daai<br/>
     *         imageUrl    http://9x9ui.s3.amazonaws.com/9x9playerV52/images/logo_tzuchi.png<br/>
     *         intro    daai<br/>
     *         --<br/>
     *         name    Daai3x3<br/>
     *         imageUrl    null<br/>
     *         --<br/>
     *         1    396    channel1    channel1 http://podcast.daaitv.org/Daai_TV_Podcast/da_ai_dian_shi/da_ai_dian_shi_files/shapeimage_3.png    3    0    0    2...<br/>    
     *         2    399    channel2    channel2 http://podcast.daaitv.org/Daai_TV_Podcast/jing_si_yu/jing_si_yu_files/shapeimage_4.png    3    0    0    2    ...<br/>
     *         --<br/>
     *         11274    50265348    Melissa hello  1   50  http://i.ytimg.com/vi/ss0ELKuua2I/mqdefault.jpg     .....<br/>       
     */
    @RequestMapping(value="setInfo")
    public @ResponseBody Object setInfo(
            @RequestParam(value="set", required=false) String id,
            @RequestParam(value="landing", required=false) String name,
            @RequestParam(value="time", required = false) String time,
            @RequestParam(value="programInfo", required=false) String programInfo,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req,
            HttpServletResponse resp) {
        log.info("setInfo: id =" + id + ";landing=" + name);
        if (programInfo == null)
            programInfo = "true";
        boolean isProgramInfo = Boolean.parseBoolean(programInfo);
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.setInfo(ctx, id, name, time, isProgramInfo);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /** 
     * Get channel list based on tag. 
     * 
     * @param name tag name. currently only one name is supported
     * @param start the start of the index
     * @param count count of records returned
     * @return list of channel information <br/>
     *         First block is tag id, tag name, start index, counts of records. <br/> 
     *         Second block is list of channel information. Please reference channelLineup, version = 32 <br/>
     *         Example of the first block: <br/>
     *              id    1539<br/>
     *              name    news<br/>
     *              start   1<br/>
     *              count   1<br/>
     *              total   1<br/>
     */
    @RequestMapping(value="tagInfo")
    public @ResponseBody Object tagInfo(
            @RequestParam(value="name", required=false) String name,
            @RequestParam(value="mso", required=false) String mso,
            @RequestParam(value="start", required=false) String start,
            @RequestParam(value="count", required=false) String count,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req,
            HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.tagInfo(ctx, name, start, count);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /** 
     * Get list of channels under the category. Start and count is for the channel records in the last block.  
     *  
     * @param category category id
     * @param start the start of the index, start from 0
     * @param count count of records
     * 
     * @return First block has category info, id and name. <br/>
     *         Second block lists the most popular tags. Separated by \n <br/> 
     *         Third block lists channels under the category. Format please reference channelLineup.
     *         <p>  Example:
     *         0    SUCCESS<br/>
     *         --<br/>
     *         id    2<br/>
     *         name    Tech & Gaming<br/>
     *         --<br/>
     *         tech<br/>
     *         gaming<br/>
     *         --<br/>
     *         (channelLineup version > 32)
     */
    @RequestMapping(value="categoryInfo")
    public @ResponseBody Object categoryInfo(
            @RequestParam(value="category", required=false) String id,
            @RequestParam(value="programInfo", required=false) String programInfo,
            @RequestParam(value="mso", required=false) String mso,
            @RequestParam(value="start", required=false) String start,
            @RequestParam(value="count", required=false) String count,
            @RequestParam(value="sort", required=false) String sort,
            @RequestParam(value="tag", required=false) String tag,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req,
            HttpServletResponse resp) {
        log.info("categoryInfo: id =" + id);
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            boolean isProgramInfo = Boolean.parseBoolean(programInfo);
            output = playerApiService.categoryInfo(ctx, id, tag, start, count, sort, isProgramInfo);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
        
    /**
     * User subscribes a channel on a designated grid location.
     * 
     * <p>Example: http://host:port/playerAPI/subscribe?user=QQl0l208W2C4F008980F&channel=51&grid=2</p>
     * 
     * @param user user's unique identifier
     * @param channel channelId
     * @param set setId
     * @param grid grid location, from 1 to 81. use with channel
     * @param pos set location, from 1 to 9. use with set       
     * @return status code and status message for the first block; <br/>
     *         second block shows channel id, status code and status message
     */        
    @RequestMapping(value="subscribe")
    public @ResponseBody Object subscribe(
            @RequestParam(value="user", required=false) String userToken, 
            @RequestParam(value="channel", required=false) String channelId,
            @RequestParam(value="set", required=false) String setId,
            @RequestParam(value="grid", required=false) String gridId, 
            @RequestParam(value="pos", required=false) String pos,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req,
            HttpServletResponse resp) {
        log.info("subscribe: userToken=" + userToken+ "; channel=" + channelId + "; grid=" + gridId + "; set=" + setId + ";pos=" + pos);
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.subscribe(ctx, userToken, channelId, gridId);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * User unsubscribes a channel or a set. 
     * 
     * To unsubscribe a channel, use params channel and grid; to unsubscribe a set, use param set.
     * 
     * <p>Example: http://host:port/playerAPI/unsubscribe?user=QQl0l208W2C4F008980F&channel=51</p>
     * 
     * @param user user's unique identifier
     * @param channel channelId
     * @param grid grid location. use with channel.   
     * giving channel only is valid (for backward compatibility), 
     * but since one channel can exist on multiple  locations in a smart guide,
     * it could result in unsubscribing on an unexpected grid location. 
     * @param pos set position
     * @return status code and status message
     */            
    @RequestMapping(value="unsubscribe")
    public @ResponseBody Object unsubscribe(
            @RequestParam(value="user", required=false) String userToken, 
            @RequestParam(value="channel", required=false) String channelId,
            @RequestParam(value="grid", required=false) String grid,
            @RequestParam(value="pos", required=false) String pos,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req,
            HttpServletResponse resp) {            
        log.info("userToken=" + userToken + "; channel=" + channelId + "; pos=" + pos + "; seq=" + grid);
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.unsubscribe(ctx, userToken, channelId, grid, pos);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Get list of channel based on special stack
     *  
     * @param stack legal values include "recommend", "hot", "mayLike", "featured", "trending"
     * @param lang language, en or zh
     * @param userToken user token, used for recommend
     * @param channel channel id, used for recommend
     * @return Reference channelLineup
     */
    @RequestMapping(value="channelStack")
    public @ResponseBody Object channelStack(
            @RequestParam(value="stack", required=false) String stack,
            @RequestParam(value="user", required=false) String userToken,
            @RequestParam(value="sphere", required=false) String sphere,
            @RequestParam(value="channel", required=false) String channel,
            @RequestParam(value="reduced", required=false) String reduced,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req,
            HttpServletResponse resp) {
        
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            boolean isReduced= Boolean.parseBoolean(reduced);
            output = playerApiService.channelStack(ctx, stack, sphere, userToken, channel, isReduced);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Share-in Channel List. Return curator channels as a set. If channel is not a curator chanenl, return one set that contains only that channel.
     * 
     * @param user user token 
     * @param channel channel id
     * @return First block: mso info. 
     *         <p> Second block: set info. id, name, image url. "name" here is the curator name.
     *         <p> Third block: channel info. channels under the set. please reference channelLineup.
     *         <p> Fourth block: program info. programs under the set. please reference programInfo. 
     * 
     */
    @RequestMapping(value="shareInChannelList")
    public @ResponseBody Object shareInChannelList(
            @RequestParam(value="user", required=false) String userToken,
            @RequestParam(value="channel", required=false) String channelIdStr,
            HttpServletRequest req,
            HttpServletResponse resp) {
        log.info("userToken=" + userToken + ";channel=" + channelIdStr);
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            if (channelIdStr == null)
                ctx.assemblePlayerMsgs(NnStatusCode.INPUT_MISSING);
            Long channelId = Long.parseLong(channelIdStr);
            output = playerApiService.shareInChannelList(ctx, channelId);
        } catch (Exception e){
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Get channel information 
     * 
     * @param user user's unique identifier. 
     * @param curator curator's id. giving curator id returns channels the curator creates.
     * @param subscriptions curator id. giving subscriptions returns channels the curator subscribes.
     * @param stack trending, recommended, hot
     * @param userInfo true or false. Whether to return user information as login does. If asked, it will be returned after status code.
     * @param channel channel id, optional, can be one or multiple;  example, channel=1 or channel=1,2,3
     * @param setInfo true or false. Whether to return subscription set information.  
     * @param required true or false. Will return error in status block if the requested channel is not found.
     * @param stack featured/recommended/hot/trending. Each stack in return is separated by "--\n"
     * @param sort use with 'user', for user's subscription sorting. There are two options: "date" or "grid". Default is "grid" if not specified. "date" returns channels in update date descending order. 
     * @return A string of all of requested channel information
     *         <p>
     *         First block: status. Second block: set information. This block shows only if setInfo is set to true. 
     *         Third block: channel information. It would be the second block if setInfo is false
     *         <p>
     *         Set info has following fields: <br/>
     *         position, set id, set name, set image url, set type
     *         <p>  
     *         Channel info has following fields: <br/>
     *         1.  grid position, <br/> 
     *         2.  channel id, <br/>
     *         3.  channel name, <br/>
     *         4.  channel description, <br/> 
     *         5.  channel image url, first is channel thumbnail, followed by 3 latest episode thumbnail(could be empty), all separeted by "|". Version before 3.2 will have one image url without |<br/>
     *         6.  program count, <br/> 
     *         7.  channel type(integer, see note), <br/> 
     *         8.  channel status(integer, see note), <br/>
     *         9.  contentType(integer, see note), <br/> 
     *         10. youtube id (for player youtube query), <br/>
     *         11. channel/episodes last update time (see note) <br/>
     *         12. channel sorting (see note), <br/> 
     *         13. piwik id, <br/> 
     *         14. last watched episode <br/>
     *         15. youtube real channel name <br/>
     *         16. subscription count. it is the last field of the version before 3.2.<br/>
     *         17. view count <br/>
     *         18. tags, separated by comma. example "run,marathon" <br/>         
     *         19. curator id <br/>
     *         20. curator name <br/>
     *         21. curator description <br/>
     *         22. curator imageUrl <br/>
     *         23. subscriber ids, separated by "|" <br/>
     *         24. subscriber images, separated by "|" <br/>
     *         25. last episode title<br/>
     *         26. poi information, each poi is separated by "|". 
     *         Each poi unit has start time, end time, poi type, urlencode json poi context; they are separated by ";".
     *         Poi currently there's only one type hyper link. This info is also included in the poi context.
     *         A poi unit looks like this: 3;5;1;%7B%0Amessage%3A+%22%E6%9B%B4%E5%A4%9A%E5%A3%B9%E5%82%B3%E5%AA%92%E5%85%A7%E5%B9%95%2C%E7%9B%A1%E5%9C%A8%E5%AA%92%E9%AB%94%E5%81%9C%E7%9C%8B%E8%81%BD%22%2C%0Abutton%3A+%5B%0A%7Btext%3A+%22%E4%BA%86%E8%A7%A3%E6%9B%B4%E5%A4%9A%22%2C+actionUrl%3A+%22http%3A%2F%2Fwww.9x9.tv%2Fview%3Fch%3D1380%26ep%3D6789%5D%22%7D%0A++++++++%5D%0A%7D|<br/> 
     *         </blockquote>                  
     *         <p>
     *         set type: TYPE_USER = 1; TYPE_READONLY = 2;
     *         <p>
     *         channel type: TYPE_GENERAL = 1; TYPE_READONLY = 2;
     *         <p>
     *         status: STATUS_SUCCESS = 0; STATUS_ERROR = 1;
     *         <p> 
     *         contentType: SYSTEM_CHANNEL=1; PODCAST=2; 
     *                      YOUTUBE_CHANNEL=3; YOUTUBE_PLAYERLIST=4                        
     *                      FACEBOOK_CHANNEL=5; 
     *                      MIX_CHANNEL=6; SLIDE=7;
     *                      MAPLESTAGE_VARIETY=8; MAPLESTAGE_SOAP=9  
     *                      DAYPARTING = 14; TRENDING = 15;  
     *         <p>
     *         channel episodes last update time: it does not always accurate on Youtube channels. It will pass channel create date on FB channels.
     *         <p>
     *         sorting: NEWEST_TO_OLDEST=1; SORT_OLDEST_TO_NEWEST=2; SORT_MAPEL=3
     *         <p> 
     *         Example: <br/>
     *         0    success<br/>
     *         --<br/>
     *         1239   1   Daai3x3   null<br/>
     *         -- <br/>
     *         1    1207    Channel1    http://hostname/images/img.jpg    3    1    0    3    http://www.youtube.com/user/android <br/>
     *         </p>
     */        
    @RequestMapping(value="channelLineup")
    public @ResponseBody Object channelLineup(
            @RequestParam(value="v", required=false) String v,
            @RequestParam(value="user", required=false) String userToken,
            @RequestParam(value="programInfo", required=false) String programInfo,
            @RequestParam(value="subscriptions", required=false) String subscriptions,
            @RequestParam(value="curator", required=false) String curatorIdStr,
            @RequestParam(value="userInfo", required=false) String userInfo,
            @RequestParam(value="channel", required=false) String channelIds,
            @RequestParam(value="setInfo", required=false) String setInfo,
            @RequestParam(value="required", required=false) String required,
            @RequestParam(value="tag", required=false) String tag,
            @RequestParam(value="category", required=false) String category,
            @RequestParam(value="reduced", required=false) String reduced,
            @RequestParam(value="sort", required=false) String sort,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req,
            HttpServletResponse resp) {
        log.info("userToken=" + userToken + ";isUserInfo=" + userInfo + ";channel=" + channelIds + ";setInfo=" + setInfo);                
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            boolean isUserInfo = Boolean.parseBoolean(userInfo);
            boolean isSetInfo  = Boolean.parseBoolean(setInfo);
            boolean isRequired = Boolean.parseBoolean(required);
            boolean isReduced  = Boolean.parseBoolean(reduced);
            boolean isProgramInfo = Boolean.parseBoolean(programInfo);
            output = playerApiService.channelLineup(ctx, userToken, curatorIdStr, subscriptions, isUserInfo, channelIds, isSetInfo, isRequired, isReduced, isProgramInfo, sort);
        } catch (Exception e){
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * To be ignored 
     */
    @RequestMapping(value="subscriberLineup")
    public @ResponseBody Object subscriberLineup(
            @RequestParam(value="v", required=false) String v,
            @RequestParam(value="user", required=false) String userToken,
            @RequestParam(value="curator", required=false) String curatorIdStr,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req,
            HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.subscriberLineup(ctx, userToken, curatorIdStr);
        } catch (Exception e){
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Get program information based on query criteria.
     * 
     * <p>
     * Examples: <br/>
     *  http://host:port/playerAPI/programInfo?channel=*&user=QQl0l208W2C4F008980F <br/>
     *  http://host:port/playerAPI/programInfo?channel=*&ipg=13671109 <br/>
     *  http://host:port/playerAPI/programInfo?channel=153,158 <br/>
     *  http://host:port/playerAPI/programInfo?channel=153 <br/>
     * </p> 
     * @param  channel (1)Could be a channel Id, e.g. channel=1 <br/>
     *                 (2)Could be list of channels, e.g. channels = 34,35,36.<br/>
     *                 (3)[deprecated] Could be *, all the programs, e.g. channel=* (user is required for wildcard query). 
     * @param  user user's unique identifier, it is required for wildcard query
     * @param  userInfo true or false. Whether to return user information as login. If asked, it will be returned after status code. 
     * @param  ipg  ipg's unique identifier, it is required for wildcard query
     * @param  sidx [deprecated]the start index for pagination
     * @param  limit [deprecated] the count of records
     * @param  start the start index for pagination. If "start" param is presented, the pagination info will be shown in the return data in the 2nd block.
     * @param  count the count of records. Currently server always sets it 50.
     * @param  time   0-23, required for dayparting channels (channel type 14). 
     * @return <p>If "start" is presented, data returns in two blocks. First block is pagination information. Second block is program information.
     *         <p>First block (if pagination enabled): channelId, number of return records, total number of records. Example:<br/>
     *            25096    50    121 <br/> 
     *            25103    50    84
     *         <p>Second block (if pagination enabled, otherwise it's first block): Programs info. Each program is separate by \n.</p>
     *         <p>Program info has: <br/>
     *           1. channelId <br/>
     *           2. programId <br/>
     *           3. program name, version after 3.2 has "|" to separate between sub-episodes, it starts from the umbrella episode name and follows by each sub-episode's name. <br/>
     *           4. description(max length=256), version after 3.2 has "|" to separate between sub-episodes, it starts from the umbrella episode description and follows by each sub-episode's description.<br/>
     *           5. programType, version after 3.2 has "|" to separate between sub-episodes, expect the first field to be empty since it has no umbrella program type.<br/>
     *           6. duration, version after 3.2 has "|" to separate between sub-episodes, it starts from the umbrella episode duration and follows by each sub-episode's duration.<br/>
     *           7. programThumbnailUrl, version after 3.2 has "|" to separate between sub-episodes, it starts from the umbrella episode image and follows by each sub-episode's image.<br/>
     *           8. programLargeThumbnailUrl, version after 3.2 has "|" to separate between sub-episodes, rule same as programThumbnailUrl<br/>
     *           9. url1(mpeg4/slideshow), version after 3.2 has "|" to separate between videos. 
     *           Each unit starts with a video url, separated by ";" is the start time (unit is seconds), 
     *           after the next ";" is the end play time of the video. Expect the first field to be empty since there is no umbrella video url.
     *           Example: |http://www.youtube.com/watch?v=TDpqS6GS_OQ;50;345|http://www.youtube.com/watch?v=JcmKq9kmP5U;0;380<br/> 
     *           10. url2(webm), reserved<br/> 
     *           11. url3(flv more likely), reserved<br/>
     *           12. storageId, referenced episodeId<br/> 
     *           13. publish date timestamp, version before 3.2 stops here<br/>
     *           14. reserved<br/>
     *           15. title card <br/>
     *           16. poi information, each poi is separated by "|". 
     *           Each poi unit starts with a number, to indicate what sub-episode it is in; follows start time; end time; poi type; urlencode json context. 
     *           The information within the poi unit is separated by ";".
     *           Currently there's only one poi type which is hyper link. This info is also included in the context.
     *           <br/>
     */        
    @RequestMapping(value="programInfo")
    public @ResponseBody Object programInfo(
            @RequestParam(value="v",        required = false) String v,
            @RequestParam(value="channel",  required = false) String channelIds,
            @RequestParam(value="episode",  required = false) String episodeIdStr,
            @RequestParam(value="user",     required = false) String userToken,
            @RequestParam(value="userInfo", required = false) String userInfo,
            @RequestParam(value="ipg",      required = false) String ipgId,
            @RequestParam(value="sidx",     required = false) String sidx,
            @RequestParam(value="limit",    required = false) String limit,
            @RequestParam(value="start",    required = false) String start,
            @RequestParam(value="count",    required = false) String count,
            @RequestParam(value="time",     required = false) String time,
            @RequestParam(value="rx",       required = false) String rx,
            HttpServletRequest req, HttpServletResponse resp) {
        
        log.info("params: channel:" + channelIds + ";episode:" + episodeIdStr + ";user:" + userToken + ";ipg:" + ipgId);
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            boolean isUserInfo = Boolean.parseBoolean(userInfo);
            output = playerApiService.programInfo(ctx, channelIds, episodeIdStr, userToken, ipgId, isUserInfo, sidx, limit, start, count, time);
        } catch (NotPurchasedException e){
            output = ctx.assemblePlayerMsgs(NnStatusCode.IAP_NOT_PURCHASED);
        } catch (Exception e){
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Generate a channel based on a podcast RSS feed or a YouTube URL.
     * 
     * <p>Only POST operation is supported.</p>
     *  
     * @param url YouTube url
     * @param user user's unique identifier
     * @param grid grid location, 1 - 81
     * @param category category id, not mandatory
     * @param langCode language code, en or zh.
     * @param tag tag string, separated by comma
     * @param name separated by "|". they are names of channel|episode1|episode2|episode3 
     * @param image separated by "|". they are thumbnails of channel|episode1|episode2|episode3 
     *  
     * @return channel id, channel name, image url. <br/>
     */    
    @RequestMapping(value="channelSubmit")
    public @ResponseBody Object channelSubmit(HttpServletRequest req, HttpServletResponse resp) {
        String url = req.getParameter("url") ;
        String name = req.getParameter("name") ;
        String image = req.getParameter("image") ;
        String userToken= req.getParameter("user");
        String grid = req.getParameter("grid");
        String categoryIds = req.getParameter("category");
        String tags = req.getParameter("tag");
        String rx = req.getParameter("rx");
        
        ApiContext ctx = new ApiContext(req);
        log.info("player input - userToken=" + userToken+ "; url=" + url + 
                 ";grid=" + grid + ";categoryId=" + categoryIds +
                 ";rx=" + rx + ";tags" + tags + ";lang=" + ctx.getLang());
        Object output = PLAYER_ERR_MSG;
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.channelSubmit(ctx, categoryIds, userToken, url, grid, name, image, tags);
        } catch (Exception e){
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * User login. A "user" cookie will be set.
     * 
     * <p>Only POST operation is supported.</p>
     * 
     * @param email email
     * @param password password
     * 
     * @return If signup succeeds, the return message will be
     *         <p>preference1 key name (tab) preference1 value (\n)<br/>
     *            preference2 key name (tab) preference2 value (\n)<br/>
     *            preferences.....
     *         </p> 
     *         <p> Example: <br/>
     *         0    success <br/>
     *         --<br/>
     *         token    QQl0l208W2C4F008980F<br/>
     *         name    c<br/>
     *         lastLogin    1300822489194<br/>
     */
    @RequestMapping(value="login")
    public @ResponseBody Object login(HttpServletRequest req, HttpServletResponse resp) {
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        @SuppressWarnings("unused")
        String mso = req.getParameter("mso");
        String rx = req.getParameter("rx");
        log.info("login: email=" + email + ";rx=" + rx);
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.login(ctx, email, password, resp);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Set user preference. Preferences can be retrieved from login, or APIs with isUserInfo option.
     * Things are not provided in userProfile API should be stored in user preference.  
     *     
     * @param user user token
     * @param key preference name
     * @param value preference value
     * @return status block
     */          
    @RequestMapping(value="setUserPref")
    public @ResponseBody Object setUserPref(
            @RequestParam(value="user", required=false)String user,
            @RequestParam(value="key", required=false)String key,
            @RequestParam(value="value", required=false)String value,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req,
            HttpServletResponse resp) {
        log.info("userPref: key(" + key + ");value(" + value + ")");
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.setUserPref(ctx, user, key, value);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Change subscription's set(group) name.
     * 
     * @param user user token
     * @param name set name
     * @param pos set position, from 1 to 9 
     * @return status
    */
    @RequestMapping(value="setSetInfo")
    public @ResponseBody Object setSetInfo (
            @RequestParam(value="user", required=false) String userToken,
            @RequestParam(value="name", required=false) String name,
            @RequestParam(value="pos", required=false) String pos,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req,
            HttpServletResponse resp) {
        log.info("setInfo: user=" + userToken + ";pos =" + pos);
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.setSetInfo(ctx, userToken, name, pos);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Static content for help or general section
     * 
     * @param key key name to retrieve the content
     * @param lang en or zh
     * @return static content
     */
    @RequestMapping(value="staticContent")
    public @ResponseBody Object staticContent(
            @RequestParam(value="key", required=false) String key,
            HttpServletRequest req, HttpServletResponse resp) {
        
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.staticContent(ctx, key);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Register a device. Will set a "device" cookie if registration is successful.
     * 
     * @param user user token, optional. will bind to device if user token is provided.
     * @return device token
     */
    @RequestMapping(value="deviceRegister")
    public @ResponseBody Object deviceRegister(
            @RequestParam(value="user", required=false) String userToken,
            @RequestParam(value="type", required=false) String type,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req,
            HttpServletResponse resp) {
        log.info("user:" + userToken);
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.deviceRegister(ctx, userToken, type, resp);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Used for version before 3.2.
     * List recommendation sets.
     * 
     * @return <p>lines of set info.
     *         <p>Set info includes set id, set name, set description, set image, set channel count. Fields are separated by tab.          
     */        
    @RequestMapping(value = "listRecommended")
    public @ResponseBody Object listRecommended(
            HttpServletRequest req, HttpServletResponse resp) {
        
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.listrecommended(ctx);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Verify device token
     *  
     * @param device device token
     * @return user token, user name, user email if any. multiple entries will be separated by \n
     */
    @RequestMapping(value="deviceTokenVerify")
    public @ResponseBody Object deviceTokenVerify(
            @RequestParam(value="device", required=false) String token,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req,
            HttpServletResponse resp) {
        log.info("user:" + token);
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.deviceTokenVerify(ctx, token);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Bind a user to device
     * 
     * @param device device token
     * @param user user token
     * @return status
     */
    @RequestMapping(value="deviceAddUser")
    public @ResponseBody Object deviceAddUser(
            @RequestParam(value="device", required=false) String deviceToken,
            @RequestParam(value="user", required=false) String userToken,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req,
            HttpServletResponse resp) {
        log.info("user:" + userToken + ";device=" + deviceToken);
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.deviceAddUser(ctx, deviceToken, userToken);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Unbind a user from the device
     * 
     * @param device device token
     * @param user user token
     * @return status
     */
    @RequestMapping(value="deviceRemoveUser")
    public @ResponseBody Object deviceRemoveUser(
            @RequestParam(value="device", required=false) String deviceToken,
            @RequestParam(value="user", required=false) String userToken,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req,
            HttpServletResponse resp) {
        log.info("user:" + userToken + ";device=" + deviceToken);
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.deviceRemoveUser(ctx, deviceToken, userToken);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * For users to report anything. Either user or device needs to be provided.
     * User report content can either use "comment" (version before 3.2), or "key" and "value" pair.
     * 
     * @param user user token
     * @param device device token
     * @param session session id, same as pdr session id
     * @param type type of report, not required, examples like feedback, sales, problem
     * @param comment user's problem description. Can be omitted if use key/value.
     * @param key used with value, like key and value pair
     * @param value used with key
     * @return report id
     */
    @RequestMapping(value="userReport")
    public @ResponseBody Object userReport(
            @RequestParam(value="user", required=false) String user,
            @RequestParam(value="device", required=false) String device,
            @RequestParam(value="session", required=false) String session,
            @RequestParam(value="comment", required=false) String comment,
            @RequestParam(value="type", required=false) String type,
            @RequestParam(value="key", required=false) String item,
            @RequestParam(value="value", required=false) String value,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req,
            HttpServletResponse resp) {
        log.info("user:" + user + ";session=" + session);
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            String query = req.getQueryString();
            String[] params = query.split("&");
            for (String param : params) {
                String[] pairs = param.split("=");
                if (pairs.length > 1 && pairs[0].equals("comment"))
                    comment = pairs[1];
                if (pairs.length > 1 && pairs[0].equals("value"))
                    value = pairs[1];
            }
            if (value != null)
                comment = value;
            output = playerApiService.userReport(ctx, user, device, session, type, item, comment);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Set user profile information. Facebook users will be turned down for most of the options.
     * 
     * @param user user token
     * @param <p>key keys include "name", "email", "gender", "year", "sphere", "ui-lang", "password", "oldPassword", "description", "image", "phone" <br/> 
     *               Keys are separated by comma.
     * @param <p>value value that pairs with keys. values are separated by comma. The sequence of value has to be the same as 
     *        the sequence of keys. 
     *        <p>Key and value are used in pairs with corresponding sequence. 
     *           For example key=name,email,gender&value=john,john@example.com,1
     *        <p>password: if password is provided, oldPassword becomes a mandatory field.
     *        <p>gender: valid gender value is 1 and 0
     *        <p>ui-lang: ui language. Currently valid values are "zh" and "en".
     *        <p>sphere: content region. Currently valid values are "zh" and "en".
     *        <p>phone: only number is allowed
     */
    //localhost:8080/playerAPI/setUserProfile?user=8s12689Ns28RN2992sut&key=description,lang&value=hello%2C妳好,en
    @RequestMapping(value="setUserProfile")
    public @ResponseBody Object setUserProfile(
            @RequestParam(value="user", required=false)String user,
            @RequestParam(value="key", required=false)String key,
            @RequestParam(value="value", required=false)String value,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req,
            HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            String query = req.getQueryString();
            String[] params = query.split("&");
            for (String param : params) {
                String[] pairs = param.split("=");
                if (pairs.length > 1 && pairs[0].equals("value"))
                    value = pairs[1];
            }
            log.info("set user profile: key(" + key + ");value(" + value + ")");
            output = playerApiService.setUserProfile(ctx, user, key, value);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }    
    
    /**
     * Request to reset the password. System will send out an email to designated email address. 
     *  
     * @param email user email
     * @return status
     */
    @RequestMapping(value="forgotpwd")
    public @ResponseBody Object forgotpwd(
            @RequestParam(value="email", required=false)String email,
            HttpServletRequest req, HttpServletResponse resp) {
        log.info("forgot password email:" + email);
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.forgotpwd(ctx, email);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Reset password
     * 
     * @param token token in the email
     * @param email user's email
     * @param password user's new password
     * @return status
     */
    @RequestMapping(value="resetpwd")
    public @ResponseBody Object resetpwd(
            @RequestParam(value="email", required=false)String email,
            @RequestParam(value="token", required=false)String token,
            @RequestParam(value="password", required=false)String password,
            HttpServletRequest req,
            HttpServletResponse resp) {
        log.info("reset password email:" + token);
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.resetpwd(ctx, email, token, password);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }    
    
    /**
     * Get user profile information
     * 
     * @param user user token
     * @return <p>Data returns in key and value pair. Key and value is tab separated. Each pair is \n separated.<br/>
     *            keys include "name", "email", "gender", "year", "sphere" "ui-lang", "description", "image"</p>
     *         <p>Example<br/>: name John <br/>email john@example.com<br/>ui-lang en                 
     */    
    @RequestMapping(value="getUserProfile")
    public @ResponseBody Object getUserProfile(
            @RequestParam(value="user", required=false)String user,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.getUserProfile(ctx, user);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * For user's sharing via email function. Captcha and text is used for captcah verification. It is not required for ios device.
     * 
     * @param user user token
     * @param toEmail required. receiver email
     * @param toName receiver name 
     * @param subject email subject
     * @param content required. email content
     * @param captcha captcha
     * @param text captcha text
     * @return status
     */
    @RequestMapping(value="shareByEmail")
    public @ResponseBody Object shareByEmail(
            @RequestParam(value="user", required=false) String userToken,
            @RequestParam(value="toEmail", required=false) String toEmail,
            @RequestParam(value="toName", required=false) String toName,
            @RequestParam(value="subject", required=false) String subject,
            @RequestParam(value="content", required=false) String content,
            @RequestParam(value="captcha", required=false) String captcha,
            @RequestParam(value="text", required=false) String text,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req, HttpServletResponse resp) {
        log.info("user:" + userToken + ";to whom:" + toEmail + ";content:" + content);
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.shareByEmail(ctx, userToken, toEmail, toName, subject, content, captcha, text);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Request captcha for later verification
     * 
     * @param user user token 
     * @param action action 1 is used for signup. action 2 is used for shareByEmail
     * @return status
     */
    @RequestMapping(value="requestCaptcha")
    public @ResponseBody Object requestCaptcha(
            @RequestParam(value="user", required=false) String token,
            @RequestParam(value="action", required=false) String action,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req, HttpServletResponse resp) {
        log.info("user:" + token);
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.requestCaptcha(ctx, token, action);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Save user's channel sorting sequence
     * 
     * @param user user token
     * @param channel channel id
     * @param sorting sorting sequence. NEWEST_TO_OLDEST = 1, OLDEST_TO_NEWEST=2  
     * @return status
     */        
    @RequestMapping(value="saveSorting")
    public @ResponseBody Object saveSorting(
            @RequestParam(value="user", required=false) String userToken,
            @RequestParam(value="channel", required=false) String channelId,
            @RequestParam(value="sorting", required=false) String sorting,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req, HttpServletResponse resp) {
        log.info("user:" + userToken + ";channel:" + channelId + ";sorting:" + sorting);
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.saveSorting(ctx, userToken, channelId, sorting);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * @deprecated
     * Save User Sharing
     *
     * @param user user's unique identifier
     * @param channel channel id
     * @param program program id
     * @param set set id (place holder for now)
     * @return A unique sharing identifier
     */
    @RequestMapping(value="saveShare")
    public @ResponseBody Object saveShare(
            @RequestParam(value="user", required=false) String userToken, 
            @RequestParam(value="channel", required=false) String channelId,
            @RequestParam(value="set", required=false) String setId,
            @RequestParam(value="program", required=false) String programId,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req, HttpServletResponse resp) {
        log.info("saveShare(" + userToken + ")");
        return (new ApiContext(req)).assemblePlayerMsgs(NnStatusCode.API_DEPRECATED);
    }
    
    /**
     * @deprecated
     * Load User Sharing
     *
     * @param id unique identifier from saveShare
     * @return  Returns a program to play follows channel information.
     *             The program to play returns in the 2nd section, format please reference programInfo format.
     *          3rd section is channel information, format please reference channelLineup.
     */
    @RequestMapping(value="loadShare")
    public @ResponseBody Object loadShare(
            @RequestParam(value="id") Long id, 
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req) {
        log.info("ipgShare:" + id);
        return (new ApiContext(req)).assemblePlayerMsgs(NnStatusCode.API_DEPRECATED);
    }
    
    /**
     *  Return personal history, for streaming portal version
     * 
     * @param user user token
     * @param mso mso name, default is 9x9 if not given
     * @return list of channels. Reference channelLineup.
     */
    @RequestMapping(value="personalHistory")
    public @ResponseBody Object personalHistory(
            @RequestParam(value="user", required=false) String userToken,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.personalHistory(ctx, userToken);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * @deprecated
     * User's recently watched channel and its episode.
     * 
     * @param user user token
     * @param count number of recently watched entries
     * @param channelInfo true or false
     * @param episodeIndex true or false. if episodeIndex = true, count has to be less 5.
     * @return Fist block: Lines of channel id and program id.<br/>
     *         Second block: if channelInfo is set to true, detail channel information will be returned. Please reference channelLineup for format.
     */
    @RequestMapping(value="recentlyWatched")
    public @ResponseBody Object recentlyWatched(
            @RequestParam(value="user", required=false) String userToken,
            @RequestParam(value="count", required=false) String count,
            @RequestParam(value="channel", required=false) String channel,
            @RequestParam(value="channelInfo", required=false) String channelInfo,
            @RequestParam(value="episodeIndex", required=false) String episodeIndex,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            boolean isChannelInfo = Boolean.parseBoolean(channelInfo);
            boolean isEpisodeIndex = Boolean.parseBoolean(episodeIndex);
            output = playerApiService.userWatched(ctx, userToken, count, isChannelInfo, isEpisodeIndex, channel);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Copy a channel to grid location
     * 
     * @param user user's unique identifier
     * @param channel channel id
     * @param grid grid location 
     * 
     * @return status code and status message
    */
    @RequestMapping(value="copyChannel")
    public @ResponseBody Object copyChannel(
            @RequestParam(value="user", required=false) String userToken, 
            @RequestParam(value="channel", required=false) String channelId,
            @RequestParam(value="grid", required=false) String grid,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req, HttpServletResponse resp){
        log.info("userToken=" + userToken + ";grid=" + grid);
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.copyChannel(ctx, userToken, channelId, grid);
        } catch (Exception e){
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }    
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Move a channel from grid 1 to grid2
     * 
     * @param user user's unique identifier
     * @param grid1 "from" grid
     * @param grid2 "to" grid 
     * 
     * @return status code and status message
    */
    @RequestMapping(value="moveChannel")
    public @ResponseBody Object moveChannel(
            @RequestParam(value="user", required=false) String userToken, 
            @RequestParam(value="grid1", required=false) String grid1,
            @RequestParam(value="grid2", required=false) String grid2,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req,
            HttpServletResponse resp){
        log.info("userToken=" + userToken + ";grid1=" + grid1 + ";grid2=" + grid2);
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.moveChannel(ctx, userToken, grid1, grid2);
        } catch (Exception e){
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }    
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Search channel name and description, curator name and description
     * 
     * @param text search text
     * @param type if not specified return all types. "9x9" returns 9x9 channels, type 6. "youtube" returns youtube channels and playlists, type 3 or 4. 
     * @param start start index
     * @param count number of records returned. Returns 9 if not specified. Max is 20.
     * @return matched channels and curators
     *         <p> 
     *         For version before 3.2, search returns a list of channel info. Please reference channelLineup. 
     *         For version 3.2, search returns in 5 blocks. Describes as follows.
     *         <p>
     *         First block: general statistics. Format in the following paragraph <br/>
     *         Second block: list of curators. Please reference curator api <br/>
     *         Third block: curatos' channels. Please reference channelLineup api.<br/>
     *         Forth block: List of matched channels. Please reference channelLineup api<br/>
     *         Fifth block: List of suggested channels. It will only return things when there's no match of curator and channel.<br/>              
     *         <p>  
     *         General statistics: (item name : number of return records : total number of records)<br/>
     *         curator    4    4 <br/> 
     *         channel    2    2 <br/>
     *         suggestion    0  0 <br/> 
     */
    @RequestMapping(value="search")
    public @ResponseBody Object search(
            @RequestParam(value="text", required=false) String text,
            @RequestParam(value="type", required=false) String type,
            @RequestParam(value="start", required=false) String start,
            @RequestParam(value="count", required=false) String count,
            @RequestParam(value="stack", required=false) String stack,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.search(ctx, text, stack, type, start, count);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Mark a program bad when player sees it 
     * 
     * @param user user token
     * @param program programId
     */    
    @RequestMapping(value="programRemove")
    public @ResponseBody Object programRemove(
            @RequestParam(value="program", required=false) String programId,
            @RequestParam(value="youtube", required=false) String ytVideoId,
            @RequestParam(value="user", required=false) String userToken,
            @RequestParam(value="bird", required=false) String secret,
            @RequestParam(value="status", required=false) String status,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req, HttpServletResponse resp) {
        log.info("bad program:" + programId + ";reported by user:" + userToken);
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int systemStatus = playerApiService.prepService(ctx);
            if (systemStatus != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(systemStatus));
            output = playerApiService.programRemove(ctx, programId, ytVideoId, userToken, secret, status);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Create a 9x9 channel. To be ignored.
     * 
     * @param name name
     * @param description description
     * @param image image url
     * @param temp not specify means false 
     */    
    @RequestMapping(value="channelCreate")
    public @ResponseBody Object channelCreate(
            @RequestParam(value="user", required=false) String user,
            @RequestParam(value="name", required=false) String name,
            @RequestParam(value="description", required=false) String description,
            @RequestParam(value="image", required=false) String image,
            @RequestParam(value="rx", required = false) String rx,
            @RequestParam(value="temp", required=false) String temp,
            HttpServletRequest req, HttpServletResponse resp) {
        
        log.info("user:" + user + ";name:" + name + ";description:" + description + ";temp:" + temp);
        Object output = PLAYER_ERR_MSG;        
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            boolean isTemp= Boolean.parseBoolean(temp);
            output = playerApiService.channelCreate(ctx, user, name, description, image, isTemp);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Create a 9x9 program. To be ignored.
     * 
     * @param channel channel id
     * @param name name
     * @param image image url
     * @param description description
     * @param audio audio url
     * @param video video url
     * @param temp not specify means false 
     */    
    @RequestMapping(value="programCreate")
    public @ResponseBody Object programCreate(
            @RequestParam(value="channel", required=false) String channel,
            @RequestParam(value="name", required=false) String name,
            @RequestParam(value="image", required=false) String image,
            @RequestParam(value="description", required=false) String description,
            @RequestParam(value="audio", required=false) String audio,
            @RequestParam(value="video", required=false) String video,
            @RequestParam(value="temp", required=false) String temp,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req, HttpServletResponse resp) {
        
        log.info("name:" + name + ";description:" + description + ";audio:" + audio+ ";video:" + video);
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            boolean isTemp= Boolean.parseBoolean(temp);
            output = playerApiService.programCreate(ctx, channel, name, description, image, audio, video, isTemp);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Set program properties. To be ignored.
     * 
     * @param program program id
     * @param property program property
     * @param value program property value
     */    
    @RequestMapping(value="setProgramProperty")
    public @ResponseBody Object setProgramProperty(
            @RequestParam(value="program", required=false) String program,
            @RequestParam(value="property", required=false) String property,
            @RequestParam(value="value", required=false) String value,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req, HttpServletResponse resp) {
                        
        log.info("program:" + program + ";property:" + property + ";value:" + value);
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.setProgramProperty(ctx, program, property, value);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Set channel property. To be ignored.
     * 
     * @param channel channel id
     * @param property channel property
     * @param value channel property value
     */    
    @RequestMapping(value="setChannelProperty")
    public @ResponseBody Object setChannelProperty(
            @RequestParam(value="channel", required=false) String channel,
            @RequestParam(value="property", required=false) String property,
            @RequestParam(value="value", required=false) String value,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req, HttpServletResponse resp) {
                        
        log.info("channel:" + channel + ";property:" + property + ";value:" + value);
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.setChannelProperty(ctx, channel, property, value);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Mix of account authentication and directory listing  
     * 
     * If token is provided, will do userTokenVerify.
     * If token is not provided, will do login
     * If token and email/password is not provided, will do guestRegister.
     *  
     * @param token if not empty, will do userTokenVerify
     * @param email if token is not provided, will do login check with email and password
     * @param password password 
     * @return please reference api introduction
     */
    @RequestMapping(value="quickLogin")
    public @ResponseBody Object quickLogin(
            @RequestParam(value="token", required=false) String token,
            @RequestParam(value="email", required=false) String email,
            @RequestParam(value="password", required=false) String password,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.quickLogin(ctx, token, email, password, resp);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Mix of account authentication and directory listing  
     * 
     * If token is provided, will do userTokenVerify.
     * If token is not provided, will do login
     * If token and email/password is not provided, will do guestRegister.
     *  
     * @param token if not empty, will do userTokenVerify
     * @param email if token is not provided, will do login check with email and password
     * @param password password 
     * @return please reference api introduction
     */
    @RequestMapping(value="auxLogin")
    public @ResponseBody Object auxLogin(
            @RequestParam(value="token", required=false) String token,
            @RequestParam(value="email", required=false) String email,
            @RequestParam(value="password", required=false) String password,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.auxLogin(ctx, token, email, password, resp);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Mix of sphere related content listing 
     *   
     * @param token if not empty, will do userTokenVerify
     * @param email if token is not provided, will do login check with email and password
     * @param password password 
     * @param rx rx
     * @return please reference api introduction
     */    
    @RequestMapping(value="sphereData")
    public @ResponseBody Object sphereData(
            @RequestParam(value="token", required=false) String token,
            @RequestParam(value="email", required=false) String email,
            @RequestParam(value="password", required=false) String password,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status == NnStatusCode.API_FORCE_UPGRADE)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.sphereData(ctx, token, email, password, resp);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * iOS flipr demo feature. Users search. 
     * 
     * @param email email address
     * @param name user name
     */
    @RequestMapping(value="graphSearch")
    public @ResponseBody Object graphSearch(
            @RequestParam(value="email", required=false) String email,
            @RequestParam(value="name", required=false) String name,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.graphSearch(ctx, email, name);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }            
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * iOS flipr demo feature. Invite users
     * 
     * @param userToken user token
     * @param toEmail invitee email
     * @param toName invitee name
     * @param channel channel id
     * @return status
     */
    @RequestMapping(value="userInvite")
    public @ResponseBody Object userInvite(
            @RequestParam(value="user", required=false) String userToken,
            @RequestParam(value="toEmail", required=false) String toEmail,
            @RequestParam(value="toName", required=false) String toName,
            @RequestParam(value="channel", required=false) String channel,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.userInvite(ctx, userToken, toEmail, toName, channel);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * iOS flipr demo feature, search users. Check user invitation status.
     * 
     * @param token invitation token
     * @return status
     */
    @RequestMapping(value="inviteStatus")
    public @ResponseBody Object inviteStatus(
            @RequestParam(value="token", required=false) String token,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.inviteStatus(ctx, token);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * iOS flipr demo feature, search users. Check invite status
     * 
     * @param userToken user token
     * @param toEmail disconnect email
     * @param channel channel id
     * @return status
     */
    @RequestMapping(value="disconnect")
    public @ResponseBody Object disconnect(
            @RequestParam(value="user", required=false) String userToken,
            @RequestParam(value="toEmail", required=false) String toEmail,
            @RequestParam(value="channel", required=false) String channel,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.disconnect(ctx, userToken, toEmail, channel);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * iOS flipr demo feature, search users. Notify subscribers with channel status.
     * 
     * @param userToken user token
     * @param channel channel id
     * @return status
     */
    @RequestMapping(value="notifySubscriber")
    public @ResponseBody Object notifySubscriber(
            @RequestParam(value="user", required=false) String userToken,
            @RequestParam(value="channel", required=false) String channel,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.notifySubscriber(ctx, userToken, channel);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Curator info. Use "curator" to get specific curator information.
     * Or specify stack = featured to get list of featured curators. 
     * 
     * @param curator curator id
     * @param stack if specify "featued" will return list of featured curators
     * @param profile curator's 9x9 url
     * @return list of curator information. <br/>
     *         First block: <br/>
     *         curator id, <br/>
     *         curator name,<br/>
     *         curator description<br/> 
     *         curator image url,<br/>
     *         curator profile url,<br/>
     *         channel count, (channel count curator create) <br/>
     *         channel subscription count, <br/>
     *         follower count, <br/> 
     *         top channel id <br/>
     *         <p> Second block: <br/>
     *         Reference channelLineup
     */
    @RequestMapping(value="curator")
    public @ResponseBody Object curator(
            @RequestParam(value="curator", required=false) String profile,
            @RequestParam(value="user", required=false) String user,
            @RequestParam(value="stack", required=false) String stack,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.curator(ctx, profile, user, stack);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    /**
     * <p> Record user's favorite channel and episode.</p>
     * <p> For non-Youtube episode, supply channel and program id. 
     * Supply the rest (video, name, image, duration, channelid) for Youtube channels.
     * </p> 
     * <p> delete equalst to true will delete the exising favorite program.
     * For deletion, user and program is expected.
     * 
     * @param user user token
     * @param channel channel id
     * @param program program id
     * @param video youtube video url
     * @param name video name
     * @param image image url
     * @param delete true or false. default is false
     * @param req
     * @param resp
     * @return status
     */
    @RequestMapping(value="favorite")
    public @ResponseBody Object favorite(
            @RequestParam(value="user", required=false) String user,
            @RequestParam(value="channel", required=false) String channel,
            @RequestParam(value="program", required=false) String program,
            @RequestParam(value="video", required=false) String fileUrl,
            @RequestParam(value="name", required=false) String name,
            @RequestParam(value="image", required=false) String imageUrl,
            @RequestParam(value="duration", required=false) String duration,
            @RequestParam(value="delete", required=false) String delete,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            boolean del = Boolean.parseBoolean(delete);
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.favorite(ctx, user, channel, program, fileUrl, name, imageUrl, duration, del);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Link of facebook login
     */
    @RequestMapping(value="fbLogin")
    public String fbLogin(HttpServletRequest req) {
        
        ApiContext ctx = new ApiContext(req);
        String appDomain = (req.isSecure() ? "https://" : "http://") + ctx.getAppDomain();
        String referrer = req.getHeader(ApiContext.HEADER_REFERRER);
        log.info("referer = " + referrer);
        if (referrer == null || referrer.isEmpty()) {
            referrer = appDomain + "/tv";
            log.info("rewrite referer = " + referrer);
        }
        String fbLoginUri = appDomain + "/fb/login";
        String msoName = req.getParameter("mso");
        Mso mso = NNF.getMsoMngr().findOneByName(msoName);
        String url = FacebookLib.getDialogOAuthPath(referrer, fbLoginUri, mso);
        String userCookie = CookieHelper.getCookie(req, CookieHelper.USER);
        log.info("FACEBOOK: user:" + userCookie + " redirect to fbLogin:" + url);
        return "redirect:" + url;
    }
    
    /**
     * @deprecated 
     */
    @RequestMapping(value="episodeUpdate")
    public @ResponseBody Object episodeUpdate(
            @RequestParam(value="epId", required=false) long epId ) {
        
        NnEpisode e = NNF.getEpisodeMngr().findById(epId);
        if (e != null) {
            int duration = NNF.getEpisodeMngr().calculateEpisodeDuration(e);
            log.info("new duration:" + duration);
            e.setDuration(duration);
            NNF.getEpisodeMngr().save(e);
        }
        return "OK";
    }
    
    /**
     * Get list of episodes based on channel stack. Used by Android device.
     *  
     * @param stack leagle value includes "recommend", "hot", "mayLike", "featured", "trending"
     * @param lang
     * @param userToken
     * @param channel
     * @return Reference channelLineup and programInfo. Work in progress.
     */
    @RequestMapping(value="virtualChannel")
    public @ResponseBody Object virtualChannel(
            @RequestParam(value="stack", required=false) String stack,
            @RequestParam(value="user", required=false) String userToken,
            @RequestParam(value="channel", required=false) String channel,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.virtualChannel(ctx, stack, userToken, channel, true);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    @RequestMapping(value="latestEpisode")
    public @ResponseBody Object latestEpisode(
            @RequestParam(value="channel", required=false) String channel,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.latestEpisode(ctx, channel);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
   
    /**
     * Used by Android device. Things to list on the front page. Current main entrance for this API is portal?type=whatson.
     * 
     * @param time hour, 0-23
     * @param stack reserved
     * @param user user token
     * @return <p>Two sections, First is things to disply, see the following. 
     *            Second is the list of episodes, please reference VirtualChannel.
     *         <p>Things to display: name, type(*1), stack name, default open(1) or closed(0), icon   
     *         <p>*1: 0 stack, 1 subscription, 2 account, 3 channel, 4 directory, 5 search  
     */
    @RequestMapping(value="whatson")
    public @ResponseBody Object whatson(
            @RequestParam(value="time", required=false) String time,
            @RequestParam(value="minimal", required=false) String minimal,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            boolean isMinimal = Boolean.parseBoolean(minimal);
            output = playerApiService.whatson(ctx, time, isMinimal);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Used by Android device. Things to list on the front page
     * 
     * @param time hour, 0-23
     * @param stack reserved
     * @param user user token
     * @return <p>Two sections, First is things to disply, see the following. 
     *            Second is the list of episodes, please reference VirtualChannel.
     *         <p>Things to display: name, type(*1), stack name, default open(1) or closed(0), icon   
     *         <p>*1: 0 stack, 1 subscription, 2 account, 3 channel, 4 directory, 5 search  
     */
    @RequestMapping(value="frontpage")
    public @ResponseBody Object frontpage(
            @RequestParam(value="time", required=false) String time,
            @RequestParam(value="stack", required=false) String stack,
            @RequestParam(value="user", required=false) String user,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.frontpage(ctx, time, stack, user);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Streaming portal API. It returns list of sets; channel list of the first set; first episode of every channel in the first set.
     * 
     * @param time hour, 0-23, required
     * @param lang en or zh. default is en
     * @param type "frontpage" or "whatson". If not specified, "frontpage" data will be returned.
     * @param minimal set minimal to true returns only set information. ie: no channel nor program information
     * @return <p>Three sections, First is sets. It has set id, set name, set description, set image, set channel count, banner image 1, banner image 2, channel tag. 
     *            Channel tag format in the note.<br/> 
     *            Second is the list of channels of the first set from the first section. Format please reference chanenlLineup. <br/>
     *            Third is the first episode of every channel from the second section. Format please reference programInfo.
     *         <p>Note: channel format: tag name separated by ";", channel id separated by ",".
     *            Example: TAG_NAME1:channelId1,channelId;TAG_NAME2:channelId1,channelId2   
     */    
    @RequestMapping(value="portal")
    public @ResponseBody Object portal(
            @RequestParam(value="time", required=false) String time,
            @RequestParam(value="type", required=false) String type,
            @RequestParam(value="minimal", required=false) String minimal,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            boolean isMinimal = Boolean.parseBoolean(minimal);
            output = playerApiService.portal(ctx, time, isMinimal, type);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    @RequestMapping(value="bulkIdentifier")
    public @ResponseBody Object bulkIdentifier(
            @RequestParam(value="channelNames", required=false) String ytUsers,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            ytUsers = req.getParameter("channelNames");
            output = playerApiService.bulkIdentifier(ctx, ytUsers);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    @RequestMapping(value="bulkSubscribe")
    public @ResponseBody Object bulkSubscribe(
            @RequestParam(value="channelNames", required=false) String ytUsers,
            @RequestParam(value="user", required=false) String userToken,
            @RequestParam(value="mso", required=false) String mso,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            ytUsers = req.getParameter("channelNames");
            userToken = req.getParameter("user");
            output = playerApiService.bulkSubscribe(ctx, userToken, ytUsers);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    @RequestMapping(value="virtualChannelAdd")
    public @ResponseBody Object virtualChannelAdd(
            @RequestParam(value="user", required=false) String user,
            @RequestParam(value="channel", required=false) String channel,
            @RequestParam(value="payload", required=false) String payload,
            @RequestParam(value="queued", required = false) String queued,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            boolean isQueued = Boolean.parseBoolean(queued);
            if (queued == null) isQueued = true;
            log.info("in queue?" + isQueued);  
            
            user = req.getParameter("user");
            channel = req.getParameter("channel");
            payload = req.getParameter("payload");
            
            output = playerApiService.virtualChannelAdd(ctx, user, channel, payload, isQueued);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    //used by android only, no cookie is set
    @RequestMapping(value="obtainAccount")
    public @ResponseBody Object obtainAccount(HttpServletRequest req, HttpServletResponse resp) {
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String name = req.getParameter("name");
        @SuppressWarnings("unused")
        String mso = req.getParameter("mso");
                
        log.info("signup: email=" + email + ";name=" + name); 
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.obtainAccount(ctx, email, password, name, req, resp);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }    
    
    /**
     * Create a 9x9 channel. To be ignored.
     * 
     * @param name name
     * @param description description
     * @param image image url
     * @param temp not specify means false 
     */    
    @RequestMapping(value="channelUpdate")
    public @ResponseBody Object channelUpdate(
            @RequestParam(value="user", required=false) String user,
            @RequestParam(value="queued", required = false) String queued,
            @RequestParam(value="payload", required=false) String payload,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            boolean isQueued = Boolean.parseBoolean(queued);
            if (queued == null) isQueued = true;
            log.info("in queue?" + isQueued);
            user = req.getParameter("user");
            payload = req.getParameter("payload");
            output = playerApiService.channelUpdate(ctx, user, payload, isQueued);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    @RequestMapping(value="endpointRegister")
    public @ResponseBody Object endpointRegister(
            @RequestParam(value="action", required=false) String action,
            @RequestParam(value="user", required=false) String userToken,
            @RequestParam(value="mso", required=false) String mso,
            @RequestParam(value="device", required=false) String device,
            @RequestParam(value="vendor", required=false) String vendor,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.endpointRegister(ctx, userToken, device, vendor, action);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    //http://www.9x9.tv/poiaction?poiId={}&userId={}&select={}
    //{msg:"", duration:0} 
    @RequestMapping(value="poiAction")
    public @ResponseBody Object poiAction(
            @RequestParam(value="poi", required=false) String poiId,
            @RequestParam(value="user", required=false) String userToken,
            @RequestParam(value="device", required=false) String deviceToken,
            @RequestParam(value="vendor", required=false) String vendor,
            @RequestParam(value="select", required=false) String select,
            @RequestParam(value="rx", required = false) String rx,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.poiAction(ctx, userToken, deviceToken, vendor, poiId, select);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     *  Get (vimeo) video url. Server backup solution.
     *  
     *  @param originalUrl (vimeo) video url
     *  @param programId
     *  @return list of video files. current there are two entries: hd and all. <br/>
     *          example: <br/>
     *          url    http://av11.hls1.vimeocdn.com/i/,49543/202/5816355,.mp4.csmil/master.m3u8?primaryToken=1408660417_acd5f72c3440eb079b6f9b5de1839fa3
     *  
     */
    @RequestMapping(value={"getVimeoDirectUrl","getDirectUrl"})
    public @ResponseBody Object getDirectUrl (
            @RequestParam(value="originalUrl", required=false) String url,
            @RequestParam(value="programId", required=false) String programIdStr,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.getDirectUrl(ctx, url, programIdStr);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
   
    @RequestMapping(value={"getUserNames"})
    public @ResponseBody Object getUserNames (
            @RequestParam(value="id", required=true) String ids,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = NnStatusMsg.getPlayerMsg(NnStatusCode.ERROR);
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.getUserNames(ctx, ids);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }    

    /**
     * Get list of signed urls.
     * @param url. not final. for now it's object name not complete path. example: "_DSC0006-X3.jpg" or "layer1/_DSC0006-X3.jpg"
     * @return list of urls. not final.
     */
    @RequestMapping(value="generateSignedUrls")
    public @ResponseBody Object generateSignedUrls (
            @RequestParam(value="url", required=false) String url,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.generateSignedUrls(ctx, url);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * To list read/unread push notifications
     * 
     * Will list notifications which resently in one week
     * 
     * @param device device token
     * @param clean mark all notifications as read after API call
     * @return isRead, timestamp, message, content, logo, title. multiple entries will be separated by \n
     */
    @RequestMapping(value="notificationList")
    public @ResponseBody Object notificationList (
            @RequestParam(value="device", required=true) String token,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.notificationList(ctx, token);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    /**
     * Add a purchase
     */
    
    @RequestMapping(value="addPurchase")
    public @ResponseBody Object addPurchase(
            @RequestParam(value="productId", required=false) String productIdRef,
            @RequestParam(value="purchaseToken", required=false) String purchaseToken,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.addPurchase(ctx, productIdRef, purchaseToken);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Get purchased items
     */
    @RequestMapping(value="getPurchases")
    public @ResponseBody Object getPurchases(
            HttpServletRequest req, HttpServletResponse resp) {
        
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.getPurchases(ctx);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Get all available items
     */
    @RequestMapping(value="getItems")
    public @ResponseBody Object getItems(HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.getItems(ctx);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
    
    /**
     * Get all available items
     */
    @RequestMapping(value="chat")
    public @ResponseBody Object chat(
            @RequestParam(value="user", required=false) String userToken,
            HttpServletRequest req, HttpServletResponse resp) {
        Object output = PLAYER_ERR_MSG;
        ApiContext ctx = new ApiContext(req);
        try {
            int status = playerApiService.prepService(ctx);
            if (status != NnStatusCode.SUCCESS)
                return ctx.playerResponse(resp, ctx.assemblePlayerMsgs(status));
            output = playerApiService.chat(ctx, userToken);
        } catch (Exception e) {
            output = ctx.handlePlayerException(e);
        } catch (Throwable t) {
            NnLogUtil.logThrowable(t);
        }
        return ctx.playerResponse(resp, output);
    }
}

