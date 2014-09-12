package com.nncloudtv.web.api;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.nncloudtv.lib.NNF;
import com.nncloudtv.lib.NnNetUtil;
import com.nncloudtv.lib.NnStringUtil;
import com.nncloudtv.lib.SearchLib;
import com.nncloudtv.model.LangTable;
import com.nncloudtv.model.Mso;
import com.nncloudtv.model.MsoConfig;
import com.nncloudtv.model.NnChannel;
import com.nncloudtv.model.NnChannelPref;
import com.nncloudtv.model.NnEpisode;
import com.nncloudtv.model.NnProgram;
import com.nncloudtv.model.NnUser;
import com.nncloudtv.model.NnUserPref;
import com.nncloudtv.model.NnUserProfile;
import com.nncloudtv.model.SysTag;
import com.nncloudtv.model.SysTagDisplay;
import com.nncloudtv.model.TitleCard;
import com.nncloudtv.model.YtProgram;
import com.nncloudtv.service.ApiContentService;
import com.nncloudtv.service.CategoryService;
import com.nncloudtv.service.MsoConfigManager;
import com.nncloudtv.service.NnChannelManager;
import com.nncloudtv.service.NnChannelPrefManager;
import com.nncloudtv.service.NnEpisodeManager;
import com.nncloudtv.service.NnProgramManager;
import com.nncloudtv.service.TitleCardManager;
import com.nncloudtv.web.json.cms.Category;

@Controller
@RequestMapping("api")
public class ApiContent extends ApiGeneric {
    
    protected static Logger log = Logger.getLogger(ApiContent.class.getName());
    
    private ApiContentService apiContentService;
    
    public ApiContent() {
        
        this.apiContentService = new ApiContentService(NNF.getChannelMngr(), NNF.getChPrefMngr(), NNF.getEpisodeMngr(), NNF.getProgramMngr());
    }
    
    @Autowired
    public ApiContent(ApiContentService apiContentService) {
        
        this.apiContentService = apiContentService;
    }
    
    @RequestMapping(value = "channels/{channelId}/autosharing/facebook", method = RequestMethod.DELETE)
    public @ResponseBody
    String facebookAutosharingDelete(HttpServletRequest req,
            HttpServletResponse resp,
            @PathVariable("channelId") String channelIdStr) {
        
        Long channelId = null;
        try {
            channelId = Long.valueOf(channelIdStr);
        } catch (NumberFormatException e) {
        }
        if (channelId == null) {
            notFound(resp, INVALID_PATH_PARAMETER);
            return null;
        }
        
        NnChannel channel = NNF.getChannelMngr().findById(channelId);
        if (channel == null) {
            notFound(resp, "Channel Not Found");
            return null;
        }
        
        Long verifiedUserId = userIdentify(req);
        if (verifiedUserId == null) {
            unauthorized(resp);
            return null;
        } else if (verifiedUserId != channel.getUserId()) {
            forbidden(resp);
            return null;
        }
        
        NnChannelPrefManager prefMngr = NNF.getChPrefMngr();
        
        prefMngr.delete(prefMngr.findByChannelIdAndItem(channelId, NnChannelPref.FB_AUTOSHARE));
        
        return ok(resp);
    }
    
    @RequestMapping(value = "channels/{channelId}/autosharing/facebook", method = RequestMethod.POST)
    public @ResponseBody
    String facebookAutosharingCreate(HttpServletRequest req,
            HttpServletResponse resp,
            @RequestParam(required = false) String mso,
            @PathVariable("channelId") String channelIdStr) {
        
        Long channelId = null;
        try {
            channelId = Long.valueOf(channelIdStr);
        } catch (NumberFormatException e) {
        }
        if (channelId == null) {
            notFound(resp, INVALID_PATH_PARAMETER);
            return null;
        }
        
        NnChannel channel = NNF.getChannelMngr().findById(channelId);
        if (channel == null) {
            notFound(resp, "Channel Not Found");
            return null;
        }
        
        Long verifiedUserId = userIdentify(req);
        if (verifiedUserId == null) {
            unauthorized(resp);
            return null;
        } else if (verifiedUserId != channel.getUserId()) {
            forbidden(resp);
            return null;
        }
        
        String fbUserId = req.getParameter("userId");
        String accessToken = req.getParameter("accessToken");
        if (fbUserId == null || accessToken == null) {
            
            badRequest(resp, MISSING_PARAMETER);
            return null;
        }
        
        String[] fbUserIdList = fbUserId.split(",");
        String[] accessTokenList = accessToken.split(",");
        
        if (fbUserIdList.length != accessTokenList.length) {
            
            badRequest(resp, INVALID_PARAMETER);
            return null;
        }
        
        Mso brand = NNF.getMsoMngr().findOneByName(mso);
        NnUser user = NNF.getUserMngr().findById(verifiedUserId, brand.getId());
        if (user == null) {
            notFound(resp, "User Not Found");
            return null;
        }
        
        NnUserPref fbUserToken = NNF.getPrefMngr().findByUserAndItem(user, NnUserPref.FB_TOKEN);
        if (fbUserToken == null || fbUserToken.getValue() == null) {
            forbidden(resp);
            return null;
        }
        
        List<NnChannelPref> prefList = new ArrayList<NnChannelPref>();
        NnChannelPrefManager chPrefMngr = NNF.getChPrefMngr();
        
        for (int i = 0; i < fbUserIdList.length; i++) {
            if (accessTokenList[i].equals(fbUserToken.getValue())) { // post to facebook time line use app token
                prefList.add(new NnChannelPref(channel.getId(), NnChannelPref.FB_AUTOSHARE, chPrefMngr.composeFacebookAutoshare(fbUserIdList[i], NNF.getConfigMngr().getFacebookInfo(MsoConfig.FACEBOOK_APPTOKEN, brand))));
            } else {
                prefList.add(new NnChannelPref(channel.getId(), NnChannelPref.FB_AUTOSHARE, chPrefMngr.composeFacebookAutoshare(fbUserIdList[i], accessTokenList[i])));
            }
        }
        
        chPrefMngr.delete(chPrefMngr.findByChannelIdAndItem(channelId, NnChannelPref.FB_AUTOSHARE));
        chPrefMngr.save(prefList);
        
        return ok(resp);
    }
    
    @RequestMapping(value = "channels/{channelId}/autosharing/facebook", method = RequestMethod.GET)
    public @ResponseBody
    List<Map<String, Object>> facebookAutosharing(HttpServletRequest req,
            HttpServletResponse resp,
            @PathVariable("channelId") String channelIdStr) {
        
        Long channelId = null;
        try {
            channelId = Long.valueOf(channelIdStr);
        } catch (NumberFormatException e) {
        }
        if (channelId == null) {
            notFound(resp, INVALID_PATH_PARAMETER);
            return null;
        }
        
        NnChannel channel = NNF.getChannelMngr().findById(channelId);
        if (channel == null) {
            notFound(resp, "Channel Not Found");
            return null;
        }
        
        Long verifiedUserId = userIdentify(req);
        if (verifiedUserId == null) {
            unauthorized(resp);
            return null;
        } else if (verifiedUserId != channel.getUserId()) {
            forbidden(resp);
            return null;
        }
        
        NnChannelPrefManager prefMngr = NNF.getChPrefMngr();
        List<NnChannelPref> prefList = prefMngr.findByChannelIdAndItem(channelId, NnChannelPref.FB_AUTOSHARE);
        
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        Map<String, Object> result;
        String[] parsedObj;
        for (NnChannelPref pref : prefList) {
            parsedObj = prefMngr.parseFacebookAutoshare(pref.getValue());
            if (parsedObj == null) {
                continue;
            }
            result = new TreeMap<String, Object>();
            result.put("userId", parsedObj[0]);
            result.put("accessToken", parsedObj[1]);
            results.add(result);
        }
        
        return results;
    }
    
    @RequestMapping(value = "episodes/{episodeId}/scheduledAutosharing/facebook", method = RequestMethod.GET)
    public @ResponseBody
    String facebookAutosharingScheduled(HttpServletRequest req,
            HttpServletResponse resp,
            @PathVariable("episodeId") String episodeIdStr) {
        
        Long episodeId = null;
        try {
            episodeId = Long.valueOf(episodeIdStr);
        } catch (NumberFormatException e) {
        }
        if (episodeId == null) {
            notFound(resp, INVALID_PATH_PARAMETER);
            return null;
        }
        
        NnEpisode episode = NNF.getEpisodeMngr().findById(episodeId);
        if (episode == null) {
            notFound(resp, "Episode Not Found");
            return null;
        }
        
        // mark as hook position
        NNF.getEpisodeMngr().autoShareToFacebook(episode);
        
        return ok(resp);
    }
    
    @RequestMapping(value = "channels/{channelId}/autosharing/brand", method = RequestMethod.GET)
    public @ResponseBody
    Map<String, Object> brandAutosharingGet(HttpServletRequest req,
            HttpServletResponse resp,
            @PathVariable("channelId") String channelIdStr) {
        
        Date now = new Date();
        log.info(printEnterState(now, req));
        
        Long channelId = null;
        try {
            channelId = Long.valueOf(channelIdStr);
        } catch (NumberFormatException e) {
            notFound(resp, INVALID_PATH_PARAMETER);
            log.info(printExitState(now, req, "404"));
            return null;
        }
        
        NnChannel channel = NNF.getChannelMngr().findById(channelId);
        if (channel == null) {
            notFound(resp, "Channel Not Found");
            log.info(printExitState(now, req, "404"));
            return null;
        }
        
        NnChannelPref pref = NNF.getChPrefMngr().getBrand(channel.getId());
        Mso mso = NNF.getMsoMngr().findByName(pref.getValue());
        String brand = pref.getValue();
        if (NNF.getMsoMngr().isValidBrand(channel, mso) == false) {
            brand = Mso.NAME_9X9;
        }
        
        Map<String, Object> result = new TreeMap<String, Object>();
        result.put("brand", brand);
        log.info(printExitState(now, req, "ok"));
        return result;
    }
    
    @RequestMapping(value = "channels/{channelId}/autosharing/brand", method = RequestMethod.PUT)
    public @ResponseBody String brandAutosharingSet(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("channelId") String channelIdStr) {
        
        Date now = new Date();
        log.info(printEnterState(now, req));
        
        Long channelId = null;
        try {
            channelId = Long.valueOf(channelIdStr);
        } catch (NumberFormatException e) {
            notFound(resp, INVALID_PATH_PARAMETER);
            log.info(printExitState(now, req, "404"));
            return null;
        }
        
        NnChannel channel = NNF.getChannelMngr().findById(channelId);
        if (channel == null) {
            notFound(resp, "Channel Not Found");
            log.info(printExitState(now, req, "404"));
            return null;
        }
        
        Long verifiedUserId = userIdentify(req);
        if (verifiedUserId == null) {
            unauthorized(resp);
            log.info(printExitState(now, req, "401"));
            return null;
        } else if (verifiedUserId != channel.getUserId()) {
            forbidden(resp);
            log.info(printExitState(now, req, "403"));
            return null;
        }
        
        // brand
        String brand = req.getParameter("brand");
        if (brand == null) {
            badRequest(resp, MISSING_PARAMETER);
            log.info(printExitState(now, req, "400"));
            return null;
        }
        Mso mso = NNF.getMsoMngr().findByName(brand);
        if (mso == null) {
            badRequest(resp, INVALID_PARAMETER);
            log.info(printExitState(now, req, "400"));
            return null;
        }
        if (NNF.getMsoMngr().isValidBrand(channel, mso) == false) {
            badRequest(resp, INVALID_PARAMETER);
            log.info(printExitState(now, req, "400"));
            return null;
        }
        
        NNF.getChPrefMngr().setBrand(channel.getId(), mso);
        
        log.info(printExitState(now, req, "ok"));
        return ok(resp);
    }
    
    @RequestMapping(value = "channels/{channelId}/autosharing/validBrands", method = RequestMethod.GET)
    public @ResponseBody
    List<Map<String, Object>> validBrandsAutosharingGet(HttpServletRequest req,
            HttpServletResponse resp,
            @PathVariable("channelId") String channelIdStr) {
        
        Date now = new Date();
        log.info(printEnterState(now, req));
        
        Long channelId = null;
        try {
            channelId = Long.valueOf(channelIdStr);
        } catch (NumberFormatException e) {
            notFound(resp, INVALID_PATH_PARAMETER);
            log.info(printExitState(now, req, "404"));
            return null;
        }
        
        NnChannel channel = NNF.getChannelMngr().findById(channelId);
        if (channel == null) {
            notFound(resp, "Channel Not Found");
            log.info(printExitState(now, req, "404"));
            return null;
        }
        
        List<Mso> msos = NNF.getMsoMngr().findValidMso(channel);
        
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        for (Mso mso : msos) {
            if (mso.getName().equals("5f") || mso.getName().equals("tzuchi")) { // hard coded for policy required
                // skip
            } else {
                Map<String, Object> result = new TreeMap<String, Object>();
                result.put("brand", mso.getName());
                results.add(result);
            }
        }
        
        log.info(printExitState(now, req, "ok"));
        return results;
    }
    
    @RequestMapping(value = "ytprograms/{ytProgramId}", method = RequestMethod.GET)
    public @ResponseBody
    YtProgram ytprogram(@PathVariable("ytProgramId") String ytProgramIdStr,
            HttpServletRequest req, HttpServletResponse resp) {
        
        Long ytProgramId = null;
        try {
            ytProgramId = Long.valueOf(ytProgramIdStr);
        } catch (NumberFormatException e) { }
        
        if (ytProgramId == null) {
            notFound(resp, INVALID_PATH_PARAMETER);
            return null;
        }
        YtProgram ytProgram = NNF.getProgramMngr().findYtProgramById(ytProgramId);
        if (ytProgram == null) {
            notFound(resp, "Pogram Not Found");
            return null;
        }
        return ytProgram;
    }
    
    @RequestMapping(value = "programs/{programId}", method = RequestMethod.GET)
    public @ResponseBody
    NnProgram program(@PathVariable("programId") String programIdStr,
            HttpServletRequest req, HttpServletResponse resp) {
        
        Long programId = null;
        try {
            programId = Long.valueOf(programIdStr);
        } catch (NumberFormatException e) {
        }
        
        if (programId == null) {
            notFound(resp, INVALID_PATH_PARAMETER);
            return null;
        }
        
        NnProgram program = NNF.getProgramMngr().findById(programId);
        if (program == null) {
            notFound(resp, "Pogram Not Found");
            return null;
        }
        
        program.setName(NnStringUtil.revertHtml(program.getName()));
        program.setIntro(NnStringUtil.revertHtml(program.getIntro()));
        
        return program;
    }
    
    @RequestMapping(value = "programs/{programId}", method = RequestMethod.PUT)
    public @ResponseBody
    NnProgram programUpdate(@PathVariable("programId") String programIdStr,
            HttpServletRequest req, HttpServletResponse resp) {
        
        Long programId = null;
        try {
            programId = Long.valueOf(programIdStr);
        } catch (NumberFormatException e) {
        }
        if (programId == null) {
            notFound(resp, INVALID_PATH_PARAMETER);
            return null;
        }
        
        NnProgram program = NNF.getProgramMngr().findById(programId);
        if (program == null) {
            notFound(resp, "Program Not Found");
            return null;
        }
        
        Long verifiedUserId = userIdentify(req);
        if (verifiedUserId == null) {
            unauthorized(resp);
            return null;
        }
        NnChannel channel = NNF.getChannelMngr().findById(program.getChannelId());
        if ((channel == null) || (verifiedUserId != channel.getUserId())) {
            forbidden(resp);
            return null;
        }
        
        // name
        String name = req.getParameter("name");
        if (name != null) {
            program.setName(NnStringUtil.htmlSafeAndTruncated(name));
        }
        
        // intro
        String intro = req.getParameter("intro");
        if (intro != null) {
            program.setIntro(NnStringUtil.htmlSafeAndTruncated(intro));
        }
        
        // imageUrl
        String imageUrl = req.getParameter("imageUrl");
        if (imageUrl != null) {
            program.setImageUrl(imageUrl);
        }
        
        // subSeq
        String subSeqStr = req.getParameter("subSeq");
        if (subSeqStr != null && subSeqStr.length() > 0) {
            Short subSeq = null;
            try {
                subSeq = Short.valueOf(subSeqStr);
            } catch (NumberFormatException e) {
            }
            if (subSeq == null) {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            } else {
                program.setSubSeq(subSeq);
            }
        }
        
        // startTime
        String startTimeStr = req.getParameter("startTime");
        if (startTimeStr != null) {
            Integer startTime = evaluateInt(startTimeStr);
            if (startTime != null && startTime >= 0) {
                program.setStartTime(startTime);
            }
        }
        
        // endTime
        String endTimeStr = req.getParameter("endTime");
        if (endTimeStr != null) {
            Integer endTime = evaluateInt(endTimeStr);
            if (endTime != null && endTime >= program.getStartTimeInt()) {
                program.setEndTime(endTime);
            }
        }
        
        // TODO poiPoint collision
        /*
        if (programMngr.isPoiCollision(program, program.getStartTimeInt(), program.getEndTimeInt())) {
            badRequest(resp, INVALID_PARAMETER);
            return null;
        }
        */
        
        // update duration = endTime - startTime
        if (program.getEndTimeInt() - program.getStartTimeInt() >= 0) {
            program.setDuration((short)(program.getEndTimeInt() - program.getStartTimeInt()));
        } else {
            // ex : new start = 10, old end = 5
            badRequest(resp, INVALID_PARAMETER);
            return null;
        }
        
        program = NNF.getProgramMngr().save(program);
        
        program.setName(NnStringUtil.revertHtml(program.getName()));
        program.setIntro(NnStringUtil.revertHtml(program.getIntro()));
        
        return program;
    }
    
    @RequestMapping(value = "programs/{programId}", method = RequestMethod.DELETE)
    public @ResponseBody
    String programDelete(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("programId") String programIdStr) {
        
        Long programId = null;
        try {
            programId = Long.valueOf(programIdStr);
        } catch (NumberFormatException e) {
        }
        if (programId == null) {
            notFound(resp, INVALID_PATH_PARAMETER);
            return null;
        }
        
        NnProgram program = NNF.getProgramMngr().findById(programId);
        if (program == null) {
            return "Program Not Found";
        }
        
        Long verifiedUserId = userIdentify(req);
        if (verifiedUserId == null) {
            unauthorized(resp);
            return null;
        }
        NnChannel channel = NNF.getChannelMngr().findById(program.getChannelId());
        if ((channel == null) || (verifiedUserId != channel.getUserId())) {
            forbidden(resp);
            return null;
        }
        
        NNF.getProgramMngr().delete(program);
        
        return ok(resp);
    }
    
    // delete programs in one episode
    @RequestMapping(value = "episodes/{episodeId}/programs", method = RequestMethod.DELETE)
    public @ResponseBody
    String programsDelete(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("episodeId") String episodeIdStr) {
    
        Long episodeId = null;
        try {
            episodeId = Long.valueOf(episodeIdStr);
        } catch (NumberFormatException e) {
        }
        if (episodeId == null) {
            notFound(resp, INVALID_PATH_PARAMETER);
            return null;
        }
        
        NnProgramManager programMngr = NNF.getProgramMngr();
        
        NnEpisode episode = NNF.getEpisodeMngr().findById(episodeId);
        if (episode == null) {
            notFound(resp, "Episode Not Found");
            return null;
        }
        
        Long verifiedUserId = userIdentify(req);
        if (verifiedUserId == null) {
            unauthorized(resp);
            return null;
        }
        NnChannel channel = NNF.getChannelMngr().findById(episode.getChannelId());
        if ((channel == null) || (verifiedUserId != channel.getUserId())) {
            forbidden(resp);
            return null;
        }
        
        List<NnProgram> episodePrograms = programMngr.findByEpisodeId(episode.getId());
        List<Long> episodeProgramIdList = new ArrayList<Long>();
        for (NnProgram episodeProgram : episodePrograms) {
            episodeProgramIdList.add(episodeProgram.getId());
        }
        
        String programIdsStr = req.getParameter("programs");
        if (programIdsStr == null) {
            badRequest(resp, MISSING_PARAMETER);
            return null;
        }
        log.info(programIdsStr);
        
        String[] programIdStrList = programIdsStr.split(",");
        List<NnProgram> programDeleteList = new ArrayList<NnProgram>();
        //List<TitleCard> titlecardDeleteList = new ArrayList<TitleCard>();
        
        for (String programIdStr : programIdStrList) {
            
            Long programId = null;
            try {
                
                programId = Long.valueOf(programIdStr);
                
            } catch(Exception e) {
            }
            if (programId != null) {
                
                NnProgram program = programMngr.findById(programId);
                if (program != null && episodeProgramIdList.indexOf(program.getId()) > -1) {
                    
                    programDeleteList.add(program);
                    /*
                    List<TitleCard> titlecards = titlecardMngr.findByProgramId(programId);
                    if (titlecards.size() > 0) {
                        titlecardDeleteList.addAll(titlecards);
                    }
                    */
                }
            }
        }
        log.info("program delete count = " + programDeleteList.size());
        
        programMngr.delete(programDeleteList);
        
        return ok(resp);
    }
    
    @RequestMapping(value = "episodes/{episodeId}/programs", method = RequestMethod.POST)
    public @ResponseBody
    NnProgram programCreate(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("episodeId") String episodeIdStr) {
        
        Long episodeId = null;
        try {
            episodeId = Long.valueOf(episodeIdStr);
        } catch (NumberFormatException e) {
        }
        if (episodeId == null) {
            notFound(resp, INVALID_PATH_PARAMETER);
            return null;
        }
        NnEpisode episode = NNF.getEpisodeMngr().findById(episodeId);
        if (episode == null) {
            notFound(resp, "Episode Not Found");
            return null;
        }
        
        Long verifiedUserId = userIdentify(req);
        if (verifiedUserId == null) {
            unauthorized(resp);
            return null;
        }
        NnChannel channel = NNF.getChannelMngr().findById(episode.getChannelId());
        if ((channel == null) || (verifiedUserId != channel.getUserId())) {
            forbidden(resp);
            return null;
        }
        
        // name
        String name = req.getParameter("name");
        if (name == null) {
            badRequest(resp, MISSING_PARAMETER);
            return null;
        }
        name = NnStringUtil.htmlSafeAndTruncated(name);
        
        // intro
        String intro = req.getParameter("intro");
        if (intro != null) {
            intro = NnStringUtil.htmlSafeAndTruncated(intro);
        }
        
        // imageUrl
        String imageUrl = req.getParameter("imageUrl");
        if (imageUrl == null) {
            imageUrl = NnChannel.IMAGE_WATERMARK_URL;
        }
        
        NnProgram program = new NnProgram(episode.getChannelId(), episodeId, name, intro, imageUrl);
        program.setPublic(true);
        
        // fileUrl
        String fileUrl = req.getParameter("fileUrl");
        if (fileUrl == null) {
            badRequest(resp, MISSING_PARAMETER);
            return null;
        }
        program.setFileUrl(fileUrl);
        
        // contentType
        program.setContentType(NnProgram.CONTENTTYPE_YOUTUBE);
        String contentTypeStr = req.getParameter("contentType");
        if (contentTypeStr != null) {
            
            Short contentType = Short.valueOf(contentTypeStr);
            if (contentType == null) {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
            
            program.setContentType(contentType);
        }
        
        // duration
        String durationStr = req.getParameter("duration");
        if (durationStr == null) {
            
            program.setDuration((short) 0);
            
        } else {
            Short duration = evaluateShort(durationStr);
            if ((duration == null) || (duration < 0)) {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
            program.setDuration(duration);
        }
        
        // startTime
        String startTimeStr = req.getParameter("startTime");
        if (startTimeStr == null) {
            
            program.setStartTime(0);
            
        } else {
            
            Short startTime = evaluateShort(startTimeStr);
            if ((startTime == null) || (startTime < 0)) {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
            program.setStartTime(startTime);
        }
        
        // endTime
        String endTimeStr = req.getParameter("endTime");
        if (endTimeStr == null) {
            
            program.setEndTime(program.getStartTimeInt() + program.getDurationInt());
        } else {
            
            Short endTime = evaluateShort(endTimeStr);
            if ((endTime == null) || (endTime < program.getStartTimeInt()) ) {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
            program.setEndTime(endTime);
        }
        
        // duration = endTime - startTime
        program.setDuration((short)(program.getEndTimeInt() - program.getStartTimeInt()));
        
        // subSeq
        String subSeqStr = req.getParameter("subSeq");
        if (subSeqStr == null) {
            
            program.setSubSeq(0);
        } else {
            Short subSeq = null;
            try {
                subSeq = Short.valueOf(subSeqStr);
            } catch (NumberFormatException e) {
            }
            if (subSeq == null) {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
            program.setSubSeq(subSeq);
        }
        
        // publish
        program.setPublishDate(new Date());
        program.setPublic(true);
        
        program = NNF.getProgramMngr().create(episode, program);
        
        program.setName(NnStringUtil.revertHtml(program.getName()));
        program.setIntro(NnStringUtil.revertHtml(program.getIntro()));
        
        return program;
    }
    
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "channels", method = RequestMethod.GET)
    public @ResponseBody
    List<NnChannel> channelsSearch(HttpServletRequest req,
            HttpServletResponse resp,
            @RequestParam(required = false, value = "mso") String msoName,
            @RequestParam(required = false, value = "sphere") String sphereStr,
            @RequestParam(required = false, value = "channels") String channelIdListStr,
            @RequestParam(required = false, value = "keyword") String keyword,
            @RequestParam(required = false, value = "userId") String userIdStr,
            @RequestParam(required = false, value = "ytPlaylistId") String ytPlaylistIdStr,
            @RequestParam(required = false, value = "ytUserId") String ytUserIdStr) {
    
        List<NnChannel> results = new ArrayList<NnChannel>();
        NnChannelManager channelMngr = NNF.getChannelMngr();
        Mso mso = NNF.getMsoMngr().findOneByName(msoName);
        boolean storeOnly = false;
        
        if (userIdStr != null) {
            
            Long userId = null;
            try {
                userId = Long.valueOf(userIdStr);
            } catch (NumberFormatException e) {
            }
            if (userId == null) {
                notFound(resp, INVALID_PARAMETER);
                return null;
            }
            
            NnUser user = NNF.getUserMngr().findById(userId, mso.getId());
            if (user == null) {
                notFound(resp, "User Not Found");
                return null;
            }
            
            results = channelMngr.findByUser(user, 0, false);
            
            Collections.sort(results, NnChannelManager.getComparator("seq"));
            
        } else if (channelIdListStr != null) {
            
            Set<Long> channelIds = new HashSet<Long>();
            for (String channelIdStr : channelIdListStr.split(",")) {
                
                Long channelId = null;
                try {
                    channelId = Long.valueOf(channelIdStr);
                } catch (NumberFormatException e) {
                }
                if (channelId != null) {
                    channelIds.add(channelId);
                }
            }
            
            results = channelMngr.findByIds(new ArrayList<Long>(channelIds));
            Set<Long> fetchedChannelIds = new HashSet<Long>();
            for (NnChannel channel : results) {
                fetchedChannelIds.add(channel.getId());
            }
            for (Long channelId : channelIds) {
                if (fetchedChannelIds.contains(channelId) == false) {
                    log.info("channel not found: " + channelId);
                }
            }
            
            log.info("total channels = " + results.size());
            if (msoName != null) {
                // filter out channels that not in MSO's store
                Set<Long> verifiedChannelIds = new HashSet<Long>(NNF.getMsoMngr().getPlayableChannels(results, mso.getId()));
                List<NnChannel> verifiedChannels = new ArrayList<NnChannel>();
                for (NnChannel channel : results) {
                    if (verifiedChannelIds.contains(channel.getId()) == true) {
                        verifiedChannels.add(channel);
                    }
                }
                results = verifiedChannels;
                log.info("total channels (filtered) = " + results.size());
            }
            
            Collections.sort(results, NnChannelManager.getComparator("updateDate"));
            
        } else if (keyword != null && keyword.length() > 0) {
            
            log.info("keyword: " + keyword);
            List<String> sphereList = new ArrayList<String>();
            String sphereFilter = null;
            if (sphereStr == null && msoName != null) {
                storeOnly = true;
                log.info("mso = " + msoName);
                MsoConfig supportedRegion = NNF.getConfigMngr().findByMsoAndItem(mso, MsoConfig.SUPPORTED_REGION);
                if (supportedRegion != null) {
                    List<String> spheres = MsoConfigManager.parseSupportedRegion(supportedRegion.getValue());
                    sphereStr = StringUtils.join(spheres, ',');
                    log.info("mso supported region = " + sphereStr);
                }
            }
            if (sphereStr != null && !sphereStr.isEmpty()) {
                storeOnly = true;
                String[] sphereArr = new String[0];
                sphereArr = sphereStr.split(",");
                for (String sphere : sphereArr) {
                    sphereList.add(NnStringUtil.escapedQuote(sphere));
                }
                sphereList.add(NnStringUtil.escapedQuote(LangTable.OTHER));
                sphereFilter = "sphere in (" + StringUtils.join(sphereList, ',') + ")";
                log.info("sphere filter = " + sphereFilter);
            }
            String type = req.getParameter("type");
            List<NnChannel> channels = new ArrayList<NnChannel>();
            if (type != null && type.equalsIgnoreCase("solr")) {
                log.info("search from Solr");
                Stack<?> stack = NnChannelManager.searchSolr(SearchLib.CORE_NNCLOUDTV, keyword, (storeOnly ? SearchLib.STORE_ONLY : null), sphereFilter, false, 0, 150);
                channels.addAll((List<NnChannel>) stack.pop());
                long solrNum = (Long) stack.pop();
                log.info("counts from solr = " + solrNum);
            } else {
                channels = NnChannelManager.search(keyword, (storeOnly ? SearchLib.STORE_ONLY : null), sphereFilter, false, 0, 150);
            }
            log.info("found channels = " + channels.size());
            
            if (sphereFilter == null) {
                
                Set<NnUserProfile> profiles = NNF.getProfileMngr().search(keyword, 0, 30);
                Set<Long> userIdSet = new HashSet<Long>();
                log.info("found profiles = " + profiles.size());
                for (NnUserProfile profile : profiles) {
                    userIdSet.add(profile.getUserId());
                }
                List<NnUser> users = NNF.getUserMngr().findAllByIds(userIdSet);
                log.info("found users = " + users.size());
                
                for (NnUser user : users) {
                    List<NnChannel> userChannels = channelMngr.findByUser(user, 30, false);
                    for (NnChannel channel : userChannels) {
                        if (channel.getStatus() == NnChannel.STATUS_SUCCESS && channel.isPublic()) {
                            if ((!sphereList.isEmpty() && sphereList.contains(channel.getSphere())) || sphereList.isEmpty()) {
                                log.info("from curator = " + channel.getName());
                                channels.add(channel);
                            }
                        }
                    }
                }
            }
            
            log.info("total channels = " + channels.size());
            if (msoName != null) {
                List<Long> channelIdList = NNF.getMsoMngr().getPlayableChannels(channels, mso.getId());
                results = channelMngr.findByIds(channelIdList);
                log.info("total channels (filtered) = " + channelIdList.size());
            } else {
                results = channels;
            }
            
            Collections.sort(results, NnChannelManager.getComparator("updateDate"));
        } else if (ytPlaylistIdStr != null || ytUserIdStr != null) {
            results = apiContentService.channelsSearch(mso.getId(), ytPlaylistIdStr, ytUserIdStr);
        }
        
        results = channelMngr.normalize(results);
        return results;
    }
    
    @RequestMapping(value = "channels/{channelId}", method = RequestMethod.GET)
    public @ResponseBody
    NnChannel channel(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("channelId") String channelIdStr) {
        
        channelIdStr = NnChannelManager.convertChannelId(channelIdStr);
        if (channelIdStr == null) {
            notFound(resp, INVALID_PATH_PARAMETER);
            return null;
        }
        
        NnChannelManager channelMngr = NNF.getChannelMngr();
        NnChannel channel = channelMngr.findById(channelIdStr);
        if (channel == null) {
            notFound(resp, "Channel Not Found");
            return null;
        }
        
        channelMngr.populateCategoryId(channel);
        if (channel.isReadonly() == false) {
            channelMngr.populateMoreImageUrl(channel);
        }
        channelMngr.populateAutoSync(channel);
        channelMngr.normalize(channel);
        
        return channel;
    }
    
    @RequestMapping(value = "channels/{channelId}", method = RequestMethod.PUT)
    public @ResponseBody
    NnChannel channelUpdate(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("channelId") String channelIdStr) {
        
        Date now = new Date();
        log.info(printEnterState(now, req));
        NnChannelManager channelMngr = NNF.getChannelMngr();
        
        Long channelId = evaluateLong(channelIdStr);
        if (channelId == null) {
            notFound(resp, INVALID_PATH_PARAMETER);
            log.info(printExitState(now, req, "404"));
            return null;
        }
        
        NnChannel channel = channelMngr.findById(channelId);
        if (channel == null) {
            notFound(resp, "Channel Not Found");
            log.info(printExitState(now, req, "404"));
            return null;
        }
        
        Long verifiedUserId = userIdentify(req);
        if (verifiedUserId == null) {
            unauthorized(resp);
            log.info(printExitState(now, req, "401"));
            return null;
        } else if (verifiedUserId != channel.getUserId()) {
            forbidden(resp);
            log.info(printExitState(now, req, "403"));
            return null;
        }
        
        // name
        String name = req.getParameter("name");
        if (name != null) {
            name = NnStringUtil.htmlSafeAndTruncated(name);
        }
        
        // intro
        String intro = req.getParameter("intro");
        if (intro != null) {
            intro = NnStringUtil.htmlSafeAndTruncated(intro);
        }
        
        // lang
        String lang = req.getParameter("lang");
        if (lang != null) {
            lang = NnStringUtil.validateLangCode(lang);
        }
        
        // sphere
        String sphere = req.getParameter("sphere");
        if (sphere != null) {
            sphere = NnStringUtil.validateLangCode(sphere);
        }
        
        // isPublic
        Boolean isPublic = null;
        String isPublicStr = req.getParameter("isPublic");
        if (isPublicStr != null) {
            isPublic = evaluateBoolean(isPublicStr);
        }
        
        // tag
        String tag = req.getParameter("tag");
        
        // imageUrl
        String imageUrl = req.getParameter("imageUrl");
        
        // categoryId
        Long categoryId = null;
        String categoryIdStr = req.getParameter("categoryId");
        if (categoryIdStr != null) {
            
            categoryId = evaluateLong(categoryIdStr);
            if (CategoryService.isSystemCategory(categoryId) == false) {
                categoryId = null;
            }
        }
        
        // updateDate
        Date updateDate = null;
        String updateDateStr = req.getParameter("updateDate");
        if (updateDateStr != null) {
            updateDate = new Date();
        }
        
        // sorting
        Short sorting = null;
        String sortingStr = req.getParameter("sorting");
        if (sortingStr != null) {
            sorting = evaluateShort(sortingStr);
        }
        
        // status
        Short status = null;
        String statusStr = req.getParameter("status");
        if (statusStr != null) {
            NnUserProfile superProfile = NNF.getProfileMngr().pickSuperProfile(verifiedUserId);
            if (hasRightAccessPCS(verifiedUserId, Long.valueOf(superProfile.getMsoId()), "0000001")) {
                status = evaluateShort(statusStr);
            }
        }
        
        NnChannel savedChannel = apiContentService.channelUpdate(channel.getId(), name, intro, lang, sphere, isPublic, tag,
                                    imageUrl, categoryId, updateDate, req.getParameter("autoSync"), sorting, status);
        if (savedChannel == null) {
            internalError(resp);
            log.warning(printExitState(now, req, "500"));
            return null;
        }
        
        channelMngr.populateCategoryId(savedChannel);
        channelMngr.populateAutoSync(savedChannel);
        channelMngr.normalize(savedChannel);
        
        log.info(printExitState(now, req, "ok"));
        return savedChannel;
    }
    
    // TODO: fix me
    @RequestMapping(value = "channels/{channelId}/youtubeSyncData", method = RequestMethod.PUT)
    public @ResponseBody
    String channelYoutubeDataSync(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("channelId") String channelIdStr) {
        
        Date now = new Date();
        log.info(printEnterState(now, req));
        
        Long channelId = evaluateLong(channelIdStr);
        if (channelId == null) {
            notFound(resp, INVALID_PATH_PARAMETER);
            log.info(printExitState(now, req, "404"));
            return null;
        }
        
        NnChannel channel = NNF.getChannelMngr().findById(channelId);
        if (channel == null) {
            notFound(resp, "Channel Not Found");
            log.info(printExitState(now, req, "404"));
            return null;
        }
        
        Long verifiedUserId = userIdentify(req);
        if (verifiedUserId == null) {
            unauthorized(resp);
            log.info(printExitState(now, req, "401"));
            return null;
        } else if (verifiedUserId != channel.getUserId()) {
            forbidden(resp);
            log.info(printExitState(now, req, "403"));
            return null;
        }
        
        Map<String, String> response = apiContentService.channelYoutubeDataSync(channel.getId());
        if (response == null || String.valueOf(HttpURLConnection.HTTP_OK).equals(response.get(NnNetUtil.STATUS)) == false ||
                "Ack\n".equals(response.get(NnNetUtil.TEXT)) == false) {
            msgResponse(resp, "NOT OK");
            log.info(printExitState(now, req, "not ok"));
            return null;
        }
        
        log.info(printExitState(now, req, "ok"));
        return ok(resp);
    }
    
    @RequestMapping(value = "tags", method = RequestMethod.GET)
    public @ResponseBody String[] tags(HttpServletRequest req, HttpServletResponse resp) {
        
        String categoryIdStr = req.getParameter("categoryId");
        if (categoryIdStr == null) {
            badRequest(resp, MISSING_PARAMETER);
            return null;
        }
        String lang = req.getParameter("lang");
        if (lang == null) {
            lang = NNF.getUserMngr().findLocaleByHttpRequest(req);
        }
        
        Long categoryId = null;
        try {
            categoryId = Long.valueOf(categoryIdStr);
        } catch (NumberFormatException e) {
        }
        if (categoryId == null) {
            badRequest(resp, INVALID_PARAMETER);
            return null;
        }
        
        SysTag sysTag = NNF.getSysTagMngr().findById(categoryId);
        if (sysTag == null) {
            
            badRequest(resp, "Category Not Found");
            return null;
        }
        
        SysTagDisplay tagDisplay = NNF.getDisplayMngr().findBySysTagIdAndLang(categoryId, lang);
        
        if (tagDisplay == null) {
            return new String[0];
        }
        String tagStr = tagDisplay.getPopularTag();
        if (tagStr == null || tagStr.length() == 0) {
            return new String[0];
        }
        return tagStr.split(",");
    }
    
    @RequestMapping(value = "categories", method = RequestMethod.GET)
    public @ResponseBody
    List<Category> categories(HttpServletRequest req, HttpServletResponse resp) {
        
        String lang = req.getParameter("lang");
        if (lang == null) {
            lang = NNF.getUserMngr().findLocaleByHttpRequest(req);
        }
        
        List<Category> categories = NNF.getCategoryService().getSystemCategories(lang);
        if (categories == null) {
            return new ArrayList<Category>();
        }
        
        return categories;
    }
    
    @RequestMapping(value = "store", method = RequestMethod.GET)
    public @ResponseBody
    List<Long> storeChannels(HttpServletRequest req, HttpServletResponse resp) {
        
        Date now = new Date();
        log.info(printEnterState(now, req));
        
        // categoryId
        long categoryId = 0;
        String categoryIdStr = req.getParameter("categoryId");
        if (categoryIdStr != null) {
            try {
                categoryId = Long.valueOf(categoryIdStr);
            } catch (NumberFormatException e) {
                badRequest(resp, INVALID_PARAMETER);
                log.info(printExitState(now, req, "400"));
                return null;
            }
            if (CategoryService.isSystemCategory(categoryId) == false) {
                badRequest(resp, INVALID_PARAMETER);
                log.info(printExitState(now, req, "400"));
                return null;
            }
        }
        
        // sphere
        String sphere = req.getParameter("sphere");
        List<String> spheres;
        if (sphere == null || sphere.isEmpty()) {
            spheres = null;
        } else {
            spheres = new ArrayList<String>();
            String[] values = sphere.split(",");
            for (String value : values) {
                if (value.equals(LangTable.LANG_ZH) || value.equals(LangTable.LANG_EN) || value.equals(LangTable.OTHER)) {
                    spheres.add(value);
                } else {
                    badRequest(resp, INVALID_PARAMETER);
                    log.info(printExitState(now, req, "400"));
                    return null;
                }
            }
        }
        
        List<Long> channelIds = new ArrayList<Long>();
        List<NnChannel> channels = NNF.getCategoryService().getSystemCategoryChannels(categoryId, spheres);
        for (NnChannel channel : channels) {
            channelIds.add(channel.getId());
        }
        log.info(printExitState(now, req, "ok"));
        return channelIds;
    }
    
    @RequestMapping(value = "channels/{channelId}/episodes", method = RequestMethod.GET)
    public @ResponseBody
    List<NnEpisode> channelEpisodes(HttpServletResponse resp,
            HttpServletRequest req,
            @PathVariable("channelId") String channelIdStr) {
        
        Date now = new Date();
        log.info(printEnterState(now, req));
        
        Long channelId = evaluateLong(channelIdStr);
        if (channelId == null) {
            notFound(resp, INVALID_PATH_PARAMETER);
            log.info(printExitState(now, req, "404"));
            return null;
        }
        
        NnChannel channel = NNF.getChannelMngr().findById(channelId);
        if (channel == null) {
            notFound(resp, "Channel Not Found");
            log.info(printExitState(now, req, "404"));
            return null;
        }
        
        // page
        String pageStr = req.getParameter("page");
        Long page = evaluateLong(pageStr);
        
        // rows
        String rowsStr = req.getParameter("rows");
        Long rows = evaluateLong(rowsStr);
        
        List<NnEpisode> results = apiContentService.channelEpisodes(channel.getId(), page, rows);
        if (results == null) {
            internalError(resp);
            log.warning(printExitState(now, req, "500"));
            return null;
        }
        
        log.info(printExitState(now, req, "ok"));
        return results;
    }
    
    // TODO: need to be optimized
    @RequestMapping(value = "channels/{channelId}/episodes/sorting", method = RequestMethod.PUT)
    public @ResponseBody
    String channelEpisodesSorting(HttpServletRequest req,
            HttpServletResponse resp, @PathVariable("channelId") String channelIdStr) {
        
        Long channelId = null;
        try {
            channelId = Long.valueOf(channelIdStr);
        } catch (NumberFormatException e) {
        }
        if (channelId == null) {
            notFound(resp, INVALID_PATH_PARAMETER);
            return null;
        }
        
        NnChannelManager channelMngr = NNF.getChannelMngr();
        NnEpisodeManager episodeMngr = NNF.getEpisodeMngr();
        
        NnChannel channel = channelMngr.findById(channelId);
        if (channel == null) {
            notFound(resp, "Channel Not Found");
            return null;
        }
        
        Long verifiedUserId = userIdentify(req);
        if (verifiedUserId == null) {
            unauthorized(resp);
            return null;
        } else if (verifiedUserId != channel.getUserId()) {
            forbidden(resp);
            return null;
        }
        
        String episodeIdsStr = req.getParameter("episodes");
        if (episodeIdsStr == null) {
            episodeMngr.reorderChannelEpisodes(channelId);
            return ok(resp);
        }
        String[] episodeIdStrList = episodeIdsStr.split(",");
        
        List<NnEpisode> episodes = episodeMngr.findByChannelId(channelId); // it must same as channelEpisodes result
        List<NnEpisode> orderedEpisodes = new ArrayList<NnEpisode>();
        List<Long> episodeIdList = new ArrayList<Long>();
        List<Long> checkedEpisodeIdList = new ArrayList<Long>();
        for (NnEpisode episode : episodes) {
            episodeIdList.add(episode.getId());
            checkedEpisodeIdList.add(episode.getId());
        }
        
        int index;
        for (String episodeIdStr : episodeIdStrList) {
            
            Long episodeId = null;
            try {
                
                episodeId = Long.valueOf(episodeIdStr);
                
            } catch(Exception e) {
            }
            if (episodeId != null) {
                index = episodeIdList.indexOf(episodeId);
                if (index > -1) {
                    orderedEpisodes.add(episodes.get(index));
                    checkedEpisodeIdList.remove(episodeId);
                }
            }
        }
        // parameter should contain all episodeId
        if (checkedEpisodeIdList.size() != 0) {
            badRequest(resp, INVALID_PARAMETER);
            return null;
        }
        
        int counter = 1;
        for (NnEpisode episode : orderedEpisodes) {
            episode.setSeq(counter);
            counter++;
        }
        
        episodeMngr.save(orderedEpisodes);
        channelMngr.renewChannelUpdateDate(channel.getId());
        
        return ok(resp);
    }
    
    @RequestMapping(value = "episodes", method = RequestMethod.GET)
    public @ResponseBody
    List<NnEpisode> episodesSearch(HttpServletResponse resp,
            HttpServletRequest req,
            @RequestParam(required = false, value = "channelId") String channelIdStr) {
        
        if (channelIdStr == null) {
            badRequest(resp, MISSING_PARAMETER);
            return null;
        }
        
        Long channelId = null;
        try {
            channelId = Long.valueOf(channelIdStr);
        } catch (NumberFormatException e) {
        }
        if (channelId == null) {
            badRequest(resp, INVALID_PARAMETER);
            return null;
        }
        
        NnChannel channel = NNF.getChannelMngr().findById(channelId);
        if (channel == null) {
            notFound(resp, "Channel Not Found");
            return null;
        }
        
        List<NnEpisode> results = null;
        
        // paging
        long page = 0, rows = 0;
        try {
            String pageStr = req.getParameter("page");
            String rowsStr = req.getParameter("rows");
            if (pageStr != null && rowsStr != null) {
                page = Long.valueOf(pageStr);
                rows = Long.valueOf(rowsStr);
            }
        } catch (NumberFormatException e) {
        }
        
        if (page > 0 && rows > 0) {
            
            results = NNF.getEpisodeMngr().list(page, rows, "seq", "asc", "channelId == " + channelId);
            
        } else {
            
            results = NNF.getEpisodeMngr().findByChannelId(channelId);
            
        }
        if (results == null) {
            return new ArrayList<NnEpisode>();
        }
        
        Collections.sort(results, NnEpisodeManager.getComparator("seq"));
        
        for (NnEpisode episode : results) {
            
            episode.setName(NnStringUtil.revertHtml(episode.getName()));
            episode.setIntro(NnStringUtil.revertHtml(episode.getIntro()));
            episode.setPlaybackUrl(NnStringUtil.getSharingUrl(false, null, episode.getChannelId(), episode.getId()));
        }
        
        return results;
    }
    
    @RequestMapping(value = "episodes/{episodeId}", method = RequestMethod.DELETE)
    public @ResponseBody
    String episodeDelete(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("episodeId") String episodeIdStr) {
    
        Long episodeId = null;
        try {
            episodeId = Long.valueOf(episodeIdStr);
        } catch (NumberFormatException e) {
        }
        if (episodeId == null) {
            notFound(resp, INVALID_PATH_PARAMETER);
            return null;
        }
        
        NnEpisode episode = NNF.getEpisodeMngr().findById(episodeId);
        if (episode == null) {
            
            return "Episode Not Found";
        }
        
        Long verifiedUserId = userIdentify(req);
        if (verifiedUserId == null) {
            unauthorized(resp);
            return null;
        }
        NnChannelManager channelMngr = NNF.getChannelMngr();
        NnChannel channel = channelMngr.findById(episode.getChannelId());
        if ((channel == null) || (verifiedUserId != channel.getUserId())) {
            forbidden(resp);
            return null;
        }
        
        // delete episode
        NNF.getEpisodeMngr().delete(episode);
        
        // re-calcuate episode count
        if (channel != null) {
            channel.setCntEpisode(channelMngr.calcuateEpisodeCount(channel));
            channelMngr.save(channel);
        }
        
        return ok(resp);
    }
    
    @RequestMapping(value = "episodes/{episodeId}", method = RequestMethod.GET)
    public @ResponseBody NnEpisode episode(HttpServletRequest req, HttpServletResponse resp, @PathVariable("episodeId") String episodeIdStr) {
        
        Long episodeId = null;
        try {
            episodeId = Long.valueOf(episodeIdStr);
        } catch (NumberFormatException e) {
        }
        if (episodeId == null) {
            notFound(resp, INVALID_PATH_PARAMETER);
            return null;
        }
        
        NnEpisode episode = NNF.getEpisodeMngr().findById(episodeId);
        if (episode == null) {
            notFound(resp, "Episode Not Found");
            return null;
        }
        
        episode.setName(NnStringUtil.revertHtml(episode.getName()));
        episode.setIntro(NnStringUtil.revertHtml(episode.getIntro()));
        
        return episode;
    }
    
    @RequestMapping(value = "episodes/{episodeId}", method = RequestMethod.PUT)
    public @ResponseBody
    NnEpisode episodeUpdate(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("episodeId") String episodeIdStr) {
        
        Long episodeId = null;
        try {
            episodeId = Long.valueOf(episodeIdStr);
        } catch (NumberFormatException e) {
        }
        if (episodeId == null) {
            notFound(resp, INVALID_PATH_PARAMETER);
            return null;
        }
        
        NnEpisodeManager episodeMngr = NNF.getEpisodeMngr();
        
        NnEpisode episode = episodeMngr.findById(episodeId);
        if (episode == null) {
            notFound(resp, "Episode Not Found");
            return null;
        }
        
        Long verifiedUserId = userIdentify(req);
        if (verifiedUserId == null) {
            unauthorized(resp);
            return null;
        }
        NnChannelManager channelMngr = NNF.getChannelMngr();
        NnChannel channel = channelMngr.findById(episode.getChannelId());
        if ((channel == null) || (verifiedUserId != channel.getUserId())) {
            forbidden(resp);
            return null;
        }
        
        // name
        String name = req.getParameter("name");
        if (name != null) {
            episode.setName(NnStringUtil.htmlSafeAndTruncated(name));
        }
        
        // intro
        String intro = req.getParameter("intro");
        if (intro != null) {
            episode.setIntro(NnStringUtil.htmlSafeAndTruncated(intro));
        }
        
        // imageUrl
        String imageUrl = req.getParameter("imageUrl");
        if (imageUrl != null) {
            episode.setImageUrl(imageUrl);
        }
        
        // scheduleDate
        String scheduleDateStr = req.getParameter("scheduleDate");
        if (scheduleDateStr != null) {
            
            if (scheduleDateStr.isEmpty()) {
                
                episode.setScheduleDate(null);
                
            } else {
                
                Long scheduleDateLong = null;
                try {
                    scheduleDateLong = Long.valueOf(scheduleDateStr);
                } catch (NumberFormatException e) {
                }
                if (scheduleDateLong == null) {
                    badRequest(resp, INVALID_PARAMETER);
                    return null;
                }
                
                episode.setScheduleDate(new Date(scheduleDateLong));
            }
        }
        
        // publishDate
        String publishDateStr = req.getParameter("publishDate");
        if (publishDateStr != null) {
            
            log.info("publishDate = " + publishDateStr);
            
            if (publishDateStr.isEmpty()) {
                
                log.info("set publishDate to null");
                episode.setPublishDate(null);
                
            } else if (publishDateStr.equalsIgnoreCase("NOW")) {
                
                episode.setPublishDate(new Date());
                
            } else {
                
                Long publishDateLong = null;
                try {
                    publishDateLong = Long.valueOf(publishDateStr);
                } catch (NumberFormatException e) {
                }
                if (publishDateLong == null) {
                    badRequest(resp, INVALID_PARAMETER);
                    return null;
                }
                
                episode.setPublishDate(new Date(publishDateLong));
            }
        }
        
        Long storageId = evaluateLong(req.getParameter("storageId"));
        if (storageId != null) {
            episode.setStorageId(storageId);
        }
        
        boolean autoShare = false;
        // isPublic
        String isPublicStr = req.getParameter("isPublic");
        if (isPublicStr != null) {
            Boolean isPublic = Boolean.valueOf(isPublicStr);
            if (isPublic != null) {
                if (episode.isPublic() == false && isPublic == true) {
                    autoShare = true;
                }
                episode.setPublic(isPublic);
            }
        }
        
        // rerun
        String rerunStr = req.getParameter("rerun");
        boolean rerun = false;
        if (rerunStr != null && Boolean.valueOf(rerunStr)) {
            rerun = true;
        }
        
        // duration
        String durationStr = req.getParameter("duration");
        if (durationStr != null) {
            Integer duration = evaluateInt(durationStr);
            if (duration != null && duration >= 0) {
                episode.setDuration(duration);
            } else {
                episode.setDuration(episodeMngr.calculateEpisodeDuration(episode));
            }
        } else {
            episode.setDuration(episodeMngr.calculateEpisodeDuration(episode));
        }
        
        // seq
        String seqStr = req.getParameter("seq");
        if (seqStr != null) {
            Integer seq = null;
            try {
                seq = Integer.valueOf(seqStr);
            } catch (NumberFormatException e) {
            }
            if (seq == null || seq < 1) {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
            episode.setSeq(seq);
        }
        
        episode = episodeMngr.save(episode, rerun);
        
        episode.setName(NnStringUtil.revertHtml(episode.getName()));
        episode.setIntro(NnStringUtil.revertHtml(episode.getIntro()));
        
        // mark as hook position
        if (autoShare == true) {
            episodeMngr.autoShareToFacebook(episode);
            channelMngr.renewChannelUpdateDate(episode.getChannelId());
        }
        
        return episode;
    }
    
    @RequestMapping(value = "channels/{channelId}/episodes", method = RequestMethod.POST)
    public @ResponseBody NnEpisode episodeCreate(HttpServletRequest req, HttpServletResponse resp, @PathVariable("channelId") String channelIdStr) {
        
        Long channelId = null;
        try {
            channelId = Long.valueOf(channelIdStr);
        } catch (NumberFormatException e) {
        }
        if (channelId == null) {
            notFound(resp, INVALID_PATH_PARAMETER);
            return null;
        }
        
        NnChannelManager channelMngr = NNF.getChannelMngr();
        
        NnChannel channel = channelMngr.findById(channelId);
        if (channel == null) {
            notFound(resp, "Channel Not Found");
            return null;
        }
        
        Long verifiedUserId = userIdentify(req);
        if (verifiedUserId == null) {
            unauthorized(resp);
            return null;
        } else if (verifiedUserId != channel.getUserId()) {
            forbidden(resp);
            return null;
        }
        
        // name
        String name = req.getParameter("name");
        if (name == null || name.isEmpty()) {
            
            badRequest(resp, MISSING_PARAMETER);
            return null;
        }
        name = NnStringUtil.htmlSafeAndTruncated(name);
        
        // intro
        String intro = req.getParameter("intro");
        if (intro != null && intro.length() > 0) {
            intro = NnStringUtil.htmlSafeAndTruncated(intro);
        }
        
        // imageUrl
        String imageUrl = req.getParameter("imageUrl");
        if (imageUrl == null) {
            imageUrl = NnChannel.IMAGE_WATERMARK_URL;
        }
        
        NnEpisode episode = new NnEpisode(channelId);
        episode.setName(name);
        episode.setIntro(intro);
        episode.setImageUrl(imageUrl);
        episode.setChannelId(channel.getId());
        
        // scheduleDate
        String scheduleDateStr = req.getParameter("scheduleDate");
        if (scheduleDateStr != null) {
            
            if (scheduleDateStr.isEmpty()) {
                
                episode.setScheduleDate(null);
                
            } else {
                
                Long scheduleDateLong = null;
                try {
                    scheduleDateLong = Long.valueOf(scheduleDateStr);
                } catch (NumberFormatException e) {
                }
                if (scheduleDateLong == null) {
                    badRequest(resp, INVALID_PARAMETER);
                    return null;
                }
                
                episode.setScheduleDate(new Date(scheduleDateLong));
            }
        }
        
        // publishDate
        String publishDateStr = req.getParameter("publishDate");
        if (publishDateStr != null) {
            
            log.info("publishDate = " + publishDateStr);
            
            if (publishDateStr.isEmpty()) {
                
                log.info("set publishDate to null");
                episode.setPublishDate(null);
                
            } else if (publishDateStr.equalsIgnoreCase("NOW")) {
                
                episode.setPublishDate(new Date());
                
            } else {
                
                Long publishDateLong = null;
                try {
                    publishDateLong = Long.valueOf(publishDateStr);
                } catch (NumberFormatException e) {
                }
                if (publishDateLong == null) {
                    badRequest(resp, INVALID_PARAMETER);
                    return null;
                }
                
                episode.setPublishDate(new Date(publishDateLong));
            }
        }
        
        boolean autoShare = false;
        // isPublic
        episode.setPublic(false); // default is draft
        String isPublicStr = req.getParameter("isPublic");
        if (isPublicStr != null) {
            Boolean isPublic = Boolean.valueOf(isPublicStr);
            if (isPublic != null) {
                if (isPublic == true) {
                    autoShare = true;
                }
                episode.setPublic(isPublic);
            }
        }
        
        Long storageId = evaluateLong(req.getParameter("storageId"));
        if (storageId != null) {
            episode.setStorageId(storageId);
        }
        
        // seq, default : at first position, trigger reorder 
        String seqStr = req.getParameter("seq");
        if (seqStr != null) {
            Integer seq = null;
            try {
                seq = Integer.valueOf(seqStr);
            } catch (NumberFormatException e) {
            }
            if (seq == null || seq < 1) {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
            episode.setSeq(seq);
        } else {
            episode.setSeq(0);
        }
        
        NnEpisodeManager episodeMngr = NNF.getEpisodeMngr();
        
        episode = episodeMngr.save(episode);
        if (episode.getSeq() == 0) { // use special value to trigger reorder
            episodeMngr.reorderChannelEpisodes(channelId);
        }
        
        episode.setName(NnStringUtil.revertHtml(episode.getName()));
        episode.setIntro(NnStringUtil.revertHtml(episode.getIntro()));
        
        channel.setCntEpisode(channelMngr.calcuateEpisodeCount(channel));
        channelMngr.save(channel);
        
        // mark as hook position 
        if (autoShare == true) {
            episodeMngr.autoShareToFacebook(episode);
            channelMngr.renewChannelUpdateDate(channelId);
        }
        
        return episode;
    }
    
    @RequestMapping(value = "episodes/{episodeId}/programs", method = RequestMethod.GET)
    public @ResponseBody
    List<NnProgram> episodePrograms(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable(value = "episodeId") String episodeIdStr) {
    
        Long episodeId = null;
        try {
            episodeId = Long.valueOf(episodeIdStr);
        } catch (NumberFormatException e) {
        }
        if (episodeId == null) {
            notFound(resp, INVALID_PATH_PARAMETER);
            return null;
        }
        
        NnEpisode episode = NNF.getEpisodeMngr().findById(episodeId);
        if (episode == null) {
            
            notFound(resp, "Episode Not Found");
            return null;
        }
        
        List<NnProgram> results = NNF.getProgramMngr().findByEpisodeId(episodeId);
        for (NnProgram result : results) {
            result.setName(NnStringUtil.revertHtml(result.getName()));
            result.setIntro(NnStringUtil.revertHtml(result.getIntro()));
        }
        
        return results;
    }
    
    @RequestMapping(value = "programs/{programId}/title_cards", method = RequestMethod.GET)
    public @ResponseBody
    List<TitleCard> programTitleCards(HttpServletRequest req,
            HttpServletResponse resp,
            @PathVariable("programId") String programIdStr) {
        
        Long programId = null;
        try {
            programId = Long.valueOf(programIdStr);
        } catch (NumberFormatException e) {
        }
        if (programId == null) {
            notFound(resp, INVALID_PATH_PARAMETER);
            return null;
        }
        
        TitleCardManager titleCardMngr = new TitleCardManager();
        List<TitleCard> results = titleCardMngr.findByProgramId(programId);
        
        for (TitleCard result : results) {
            result.setMessage(NnStringUtil.revertHtml(result.getMessage()));
        }
        
        return results;
    }
    
    @RequestMapping(value = "episodes/{episodeId}/title_cards", method = RequestMethod.GET)
    public @ResponseBody
    List<TitleCard> episodeTitleCards(HttpServletRequest req,
            HttpServletResponse resp,
            @PathVariable("episodeId") String episodeIdStr) {
    
        Long episodeId = null;
        try {
            episodeId = Long.valueOf(episodeIdStr);
        } catch (NumberFormatException e) {
        }
        if (episodeId == null) {
            notFound(resp, INVALID_PATH_PARAMETER);
            return null;
        }
        
        NnEpisode episode = NNF.getEpisodeMngr().findById(episodeId);
        if (episode == null) {
            
            notFound(resp, "Episode Not Found");
            return null;
        }
        
        TitleCardManager titleCardMngr = new TitleCardManager();
        List<TitleCard> results = titleCardMngr.findByEpisodeId(episodeId);
        
        for (TitleCard result : results) {
            result.setMessage(NnStringUtil.revertHtml(result.getMessage()));
        }
        
        return results;
    }

    @RequestMapping(value = "programs/{programId}/title_cards", method = RequestMethod.POST)
    public @ResponseBody
    TitleCard titleCardCreate(HttpServletResponse resp, HttpServletRequest req,
            @PathVariable("programId") String programIdStr) {
        
        Long programId = null;
        try {
            programId = Long.valueOf(programIdStr);
        } catch (NumberFormatException e) {
        }
        if (programId == null) {
            notFound(resp, INVALID_PATH_PARAMETER);
            return null;
        }
        NnProgram program = NNF.getProgramMngr().findById(programId);
        if (program == null) {
            notFound(resp, "Program Not Found");
            return null;
        }
        
        Long verifiedUserId = userIdentify(req);
        if (verifiedUserId == null) {
            unauthorized(resp);
            return null;
        }
        NnChannel channel = NNF.getChannelMngr().findById(program.getChannelId());
        if ((channel == null) || (verifiedUserId != channel.getUserId())) {
            forbidden(resp);
            return null;
        }
        
        // type
        String typeStr = req.getParameter("type");
        if (typeStr == null) {
            badRequest(resp, MISSING_PARAMETER);
            return null;
        }
        Short type = null;
        try {
            type = Short.valueOf(typeStr);
        } catch (NumberFormatException e) {
        }
        if (type == null) {
            badRequest(resp, INVALID_PARAMETER);
            return null;
        }
        if (type != TitleCard.TYPE_BEGIN && type != TitleCard.TYPE_END) {
            badRequest(resp, INVALID_PARAMETER);
            return null;
        }
        
        TitleCardManager titleCardMngr = new TitleCardManager();
        
        TitleCard titleCard = titleCardMngr.findByProgramIdAndType(programId, type);
        if (titleCard == null) {
            titleCard = new TitleCard(program.getChannelId(), programId, type);
        }
        
        // message
        String message = req.getParameter("message");
        if (message == null) {
            badRequest(resp, MISSING_PARAMETER);
            return null;
        }
        titleCard.setMessage(NnStringUtil.htmlSafeAndTruncated(message, 2000));
        
        // duration
        String duration = req.getParameter("duration");
        if (duration == null) {
            titleCard.setDuration(TitleCard.DEFAULT_DURATION);
        } else {
            titleCard.setDuration(duration);
        }
        
        // size
        String size = req.getParameter("size");
        if (size == null) {
            titleCard.setSize(TitleCard.DEFAULT_SIZE);
        } else {
            titleCard.setSize(size);
        }
        
        // color
        String color = req.getParameter("color");
        if (color == null) {
            titleCard.setColor(TitleCard.DEFAULT_COLOR);
        } else {
            titleCard.setColor(color);
        }
        
        // effect
        String effect = req.getParameter("effect");
        if (effect == null) {
            titleCard.setEffect(TitleCard.DEFAULT_EFFECT);
        } else {
            titleCard.setEffect(effect);
        }
        
        // align
        String align = req.getParameter("align");
        if (align == null) {
            titleCard.setAlign(TitleCard.DEFAULT_ALIGN);
        } else {
            titleCard.setAlign(align);
        }
        
        // bgColor
        String bgColor = req.getParameter("bgColor");
        if (bgColor == null) {
            //titleCard.setBgColor(TitleCard.DEFAULT_BG_COLOR);
        } else {
            titleCard.setBgColor(bgColor);
        }
        
        // style
        String style = req.getParameter("style");
        if (style == null) {
            titleCard.setStyle(TitleCard.DEFAULT_STYLE);
        } else {
            titleCard.setStyle(style);
        }
        
        // weight
        String weight = req.getParameter("weight");
        if (weight == null) {
            titleCard.setWeight(TitleCard.DEFAULT_WEIGHT);
        } else {
            titleCard.setWeight(weight);
        }
        
        // bgImg
        String bgImage = req.getParameter("bgImage");
        if (bgImage == null) {
            //titleCard.setBgImage(TitleCard.DEFAULT_BG_IMG);
        } else {
            titleCard.setBgImage(bgImage);
        }
        
        titleCard = titleCardMngr.save(titleCard);
        
        titleCard.setMessage(NnStringUtil.revertHtml(titleCard.getMessage()));
        
        return titleCard;
    }
    
    @RequestMapping(value = "title_card/{id}", method = RequestMethod.DELETE)
    public @ResponseBody
    String titleCardDelete(HttpServletResponse resp, HttpServletRequest req,
            @PathVariable("id") String idStr) {
        
        Long id = null;
        try {
            id = Long.valueOf(idStr);
        } catch (NumberFormatException e) {
        }
        if (id == null) {
            notFound(resp, INVALID_PATH_PARAMETER);
            return null;
        }
        
        TitleCardManager titleCardMngr = new TitleCardManager();
        TitleCard titleCard = titleCardMngr.findById(id);
        if (titleCard==null) {
            notFound(resp, "TitleCard Not Found");
            return null;
        }
        
        Long verifiedUserId = userIdentify(req);
        if (verifiedUserId == null) {
            unauthorized(resp);
            return null;
        }
        NnChannel channel = NNF.getChannelMngr().findById(titleCard.getChannelId());
        if ((channel == null) || (verifiedUserId != channel.getUserId())) {
            forbidden(resp);
            return null;
        }
        
        titleCardMngr.delete(titleCard);
        
        return ok(resp);
    }
}
