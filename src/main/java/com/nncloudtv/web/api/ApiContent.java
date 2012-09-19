package com.nncloudtv.web.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.nncloudtv.lib.NnStringUtil;
import com.nncloudtv.model.Category;
import com.nncloudtv.model.CategoryMap;
import com.nncloudtv.model.LangTable;
import com.nncloudtv.model.NnAd;
import com.nncloudtv.model.NnChannel;
import com.nncloudtv.model.NnChannelPref;
import com.nncloudtv.model.NnEpisode;
import com.nncloudtv.model.NnProgram;
import com.nncloudtv.model.NnUserLibrary;
import com.nncloudtv.model.NnUserPref;
import com.nncloudtv.model.TitleCard;
import com.nncloudtv.service.CategoryManager;
import com.nncloudtv.service.NnAdManager;
import com.nncloudtv.service.NnChannelManager;
import com.nncloudtv.service.NnChannelPrefManager;
import com.nncloudtv.service.NnEpisodeManager;
import com.nncloudtv.service.NnProgramManager;
import com.nncloudtv.service.NnUserLibraryManager;
import com.nncloudtv.service.TitleCardManager;

@Controller
@RequestMapping("api")
public class ApiContent extends ApiGeneric {
    
    protected static Logger log = Logger.getLogger(ApiContent.class.getName());
    
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
        
        NnChannelManager channelMngr = new NnChannelManager();
        NnChannel channel = channelMngr.findById(channelId);
        if (channel == null) {
            notFound(resp, "Channel Not Found");
            return null;
        }
        
        NnChannelPref pref = null;
        NnChannelPrefManager prefMngr = new NnChannelPrefManager();
        
        // fbUserId
        pref = prefMngr
                .findByChannelIdAndItem(channelId, NnUserPref.FB_USER_ID);
        if (pref != null) {
            prefMngr.delete(pref);
        }
        
        // accessToken
        pref = prefMngr.findByChannelIdAndItem(channelId, NnUserPref.FB_TOKEN);
        if (pref != null) {
            prefMngr.delete(pref);
        }
        
        return "OK";
    }
    
    @RequestMapping(value = "channels/{channelId}/autosharing/facebook", method = RequestMethod.POST)
    public @ResponseBody
    String facebookAutosharingCreate(HttpServletRequest req,
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
        
        NnChannelManager channelMngr = new NnChannelManager();
        NnChannel channel = channelMngr.findById(channelId);
        if (channel == null) {
            notFound(resp, "Channel Not Found");
            return null;
        }
        
        String fbUserId = req.getParameter("userId");
        String accessToken = req.getParameter("accessToken");
        if (fbUserId == null || accessToken == null) {
            
            badRequest(resp, MISSING_PARAMETER);
            return null;
        }
        
        NnChannelPref pref = null;
        NnChannelPrefManager prefMngr = new NnChannelPrefManager();
        
        // fbUserId
        pref = prefMngr
                .findByChannelIdAndItem(channelId, NnUserPref.FB_USER_ID);
        if (pref != null) {
            pref.setValue(fbUserId);
        } else {
            pref = new NnChannelPref(channel, NnUserPref.FB_USER_ID, fbUserId);
        }
        prefMngr.save(pref);
        
        // accessToken
        pref = prefMngr.findByChannelIdAndItem(channelId, NnUserPref.FB_TOKEN);
        if (pref != null) {
            pref.setValue(accessToken);
        } else {
            pref = new NnChannelPref(channel, NnUserPref.FB_TOKEN, accessToken);
        }
        prefMngr.save(pref);
        
        return "OK";
    }
    
    @RequestMapping(value = "channels/{channelId}/autosharing/facebook", method = RequestMethod.GET)
    public @ResponseBody
    Map<String, Object> facebookAutosharing(HttpServletRequest req,
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
        
        NnChannelManager channelMngr = new NnChannelManager();
        NnChannel channel = channelMngr.findById(channelId);
        if (channel == null) {
            notFound(resp, "Channel Not Found");
            return null;
        }
        
        NnChannelPrefManager prefMngr = new NnChannelPrefManager();
        NnChannelPref pref = null;
        Map<String, Object> result = new TreeMap<String, Object>();
        String fbUserId = null;
        String accessToken = null;
        
        // fbUserId
        pref = prefMngr
                .findByChannelIdAndItem(channelId, NnUserPref.FB_USER_ID);
        if (pref != null) {
            fbUserId = pref.getValue();
            result.put("userId", fbUserId);
        }
        
        // accessToken
        pref = prefMngr.findByChannelIdAndItem(channelId, NnUserPref.FB_TOKEN);
        if (pref != null) {
            accessToken = pref.getValue();
            result.put("accessToken", accessToken);
        }
        
        if (accessToken != null && fbUserId != null) {
            return result;
        }
        
        return null;
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
        
        NnProgramManager programMngr = new NnProgramManager();
        NnProgram program = programMngr.findById(programId);
        if (program == null) {
            notFound(resp, "Pogram Not Found");
            return null;
        }
        
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
        
        NnProgramManager programMngr = new NnProgramManager();
        NnProgram program = programMngr.findById(programId);
        if (program == null) {
            notFound(resp, "Program Not Found");
            return null;
        }
        
        // name
        String name = req.getParameter("name");
        if (name != null) {
            program.setName(NnStringUtil.htmlSafeChars(name));
        }
        
        // intro
        String intro = req.getParameter("intro");
        if (intro != null) {
            program.setIntro(NnStringUtil.htmlSafeChars(intro));
        }
        
        // comment
        String comment = req.getParameter("comment");
        if (comment != null) {
            program.setComment(NnStringUtil.htmlSafeChars(comment));
        }
        
        // imageUrl
        String imageUrl = req.getParameter("imageUrl");
        if (imageUrl != null) {
            program.setImageUrl(imageUrl);
        }
        
        // seq
        String seqStr = req.getParameter("seq");
        if (seqStr != null) {
            Short seq = null;
            try {
                seq = Short.valueOf(seqStr);
            } catch (NumberFormatException e) {
            }
            if (seq == null) {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            } else {
                program.setSeq(seq);
            }
        }
        
        // subSeq
        String subSeqStr = req.getParameter("subSeq");
        if (subSeqStr != null) {
            Short subSeq = null;
            try {
                subSeq = Short.valueOf(seqStr);
            } catch (NumberFormatException e) {
            }
            if (subSeq == null) {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            } else {
                program.setSeq(subSeq);
            }
        }
        
        // startTime
        String startTimeStr = req.getParameter("startTime");
        if (startTimeStr != null) {
            Integer startTime = null;
            try {
                startTime = Integer.valueOf(startTimeStr);
            } catch (NumberFormatException e) {
            }
            if (startTime == null) {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
            program.setStartTime(startTime);
        }
        
        // endTime
        String endTimeStr = req.getParameter("endTime");
        if (endTimeStr != null) {
            Integer endTime = null;
            try {
                endTime = Integer.valueOf(endTimeStr);
            } catch (NumberFormatException e) {
            }
            if (endTime == null) {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
            program.setStartTime(endTime);
        }
        
        return programMngr.save(program);
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
        
        NnProgramManager programMngr = new NnProgramManager();
        
        NnProgram program = programMngr.findById(programId);
        if (program == null) {
            return "Program Not Found";
        }
        
        long episodeId = program.getEpisodeId();
        
        TitleCardManager titleCardMngr = new TitleCardManager();
        
        List<TitleCard> titleCards = titleCardMngr.findByProgramId(programId);
        for (int i = 0; i < titleCards.size(); i++) {
            titleCardMngr.delete(titleCards.get(i));
        }
        
        programMngr.delete(program);
        
        if (episodeId > 0) {
            
            programMngr.reorderEpisodePrograms(episodeId);
            
        }
        
        return "OK";
    }
    
    @RequestMapping(value = "channels/{channelId}", method = RequestMethod.GET)
    public @ResponseBody
    NnChannel channel(HttpServletRequest req, HttpServletResponse resp,
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
        
        NnChannelManager channelMngr = new NnChannelManager();
        NnChannel channel = channelMngr.findById(channelId);
        if (channel == null) {
            notFound(resp, "Channel Not Found");
            return null;
        }
        
        channel.setName(NnStringUtil.revertHtml(channel.getName()));
        channel.setIntro(NnStringUtil.revertHtml(channel.getIntro()));
        
        return channel;
    }
    
    @RequestMapping(value = "channels/{channelId}", method = RequestMethod.PUT)
    public @ResponseBody
    NnChannel channelUpdate(HttpServletRequest req, HttpServletResponse resp,
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
        
        NnChannelManager channelMngr = new NnChannelManager();
        NnChannel channel = channelMngr.findById(channelId);
        if (channel == null) {
            notFound(resp, "Channel Not Found");
            return null;
        }
        
        // name
        String name = req.getParameter("name");
        if (name != null) {
            channel.setName(NnStringUtil.htmlSafeAndTruncated(name));
        }
        
        // intro
        String intro = req.getParameter("intro");
        if (intro != null) {
            channel.setIntro(NnStringUtil.htmlSafeAndTruncated(intro));
        }
        
        // lang
        String lang = req.getParameter("lang");
        if (lang != null && NnStringUtil.validateLangCode(lang) != null) {
            
            channel.setLang(lang);
        }
        
        // sphere
        String sphere = req.getParameter("sphere");
        if (sphere != null && NnStringUtil.validateLangCode(sphere) != null) {
            
            channel.setSphere(sphere);
        }
        
        // isPublic
        String isPublicStr = req.getParameter("isPublic");
        if (isPublicStr != null) {
            Boolean isPublic = Boolean.valueOf(isPublicStr);
            channel.setPublic(isPublic);
        }
        
        // tag
        String tag = req.getParameter("tag");
        if (tag != null) {
            channel.setTag(tag);
        }
        
        // imageUrl
        String imageUrl = req.getParameter("imageUrl");
        if (imageUrl != null) {
            channel.setImageUrl(imageUrl);
        }
        
        // categoryId
        String categoryIdStr = req.getParameter("categoryId");
        if (categoryIdStr != null) {
            
            Long categoryId = null;
            try {
                categoryId = Long.valueOf(categoryIdStr);
            } catch (NumberFormatException e) {
            }
            if (categoryId == null) {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
            
            CategoryManager catMngr = new CategoryManager();
            Category category = catMngr.findById(categoryId);
            if (category == null) {
                badRequest(resp, "Category Not Found");
                return null;
            }
            
            // category mapping
            if (categoryId != channel.getCategoryId()) {
                
                List<Category> origCategories = catMngr.findByChannelId(channelId);
                
                for (Category cat : origCategories) {
                    catMngr.delete(cat);
                }
                
                catMngr.save(new CategoryMap(categoryId, channelId));
                if (channel.getSphere() != null && channel.getSphere().equalsIgnoreCase(LangTable.OTHER)) {
                    
                    Category twin = catMngr.findTwin(category);
                    if (twin != null) {
                        catMngr.save(new CategoryMap(twin.getId(), channelId));
                    }
                }
            }
        }
        
        return channelMngr.save(channel);
    }
    
    @RequestMapping(value = "channels/{channelId}/programs", method = RequestMethod.GET)
    public @ResponseBody
    List<NnProgram> channelPrograms(HttpServletRequest req,
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
        
        NnProgramManager programMngr = new NnProgramManager();
        NnChannelManager channelMngr = new NnChannelManager();
        NnChannel channel = channelMngr.findById(channelId);
        
        if (channel == null) {
            notFound(resp, "Channel Not Found");
            return null;
        }
        
        List<NnProgram> results = programMngr.findByChannelId(channelId);
        Collections.sort(results, programMngr.getProgramSeqComparator());
        
        return results;
    }
    
    @RequestMapping(value = "tags", method = RequestMethod.GET)
    public @ResponseBody String[] tags(HttpServletRequest req, HttpServletResponse resp) {
        
        String categoryIdStr = req.getParameter("categoryId");
        if (categoryIdStr == null) {
            badRequest(resp, MISSING_PARAMETER);
            return null;
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
        
        CategoryManager catMngr = new CategoryManager();
        Category category = catMngr.findById(categoryId);
        if (category == null) {
            
            badRequest(resp, "Category Not Found");
            return null;
        }
        
        String result = category.getTag();
        if (result == null || result.length() == 0) {
            return new String[0];
        }
        return result.split(",");
    }
    
    @RequestMapping(value = "categories", method = RequestMethod.GET)
    public @ResponseBody
    List<Category> categories(HttpServletRequest req, HttpServletResponse resp) {
        
        String lang = req.getParameter("lang");
        CategoryManager catMngr = new CategoryManager();
        List<Category> categories;
        
        if (lang != null) {
            categories = catMngr.findByLang(lang);
        } else {
            categories = catMngr.findAll();
        }
        
        Collections.sort(categories, catMngr.getCategorySeqComparator());
        
        return categories;
    }
    
    @RequestMapping(value = "channels/{channelId}/episodes", method = RequestMethod.GET)
    public @ResponseBody
    List<NnEpisode> episodePrograms(HttpServletResponse resp,
            HttpServletRequest req,
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
        
        NnChannelManager channelMngr = new NnChannelManager();
        
        NnChannel channel = channelMngr.findById(channelId);
        if (channel == null) {
            notFound(resp, "Channel Not Found");
            return null;
        }
        
        NnEpisodeManager episodeMngr = new NnEpisodeManager();
        
        List<NnEpisode> results = episodeMngr.findByChannelId(channelId);
        Collections.sort(results, episodeMngr.getEpisodeSeqComparator());
        
        return results;
    }
    
    /**
     * channelId, seq, file URL are required, others optional.
     */
    // TODO: need to rewrite
    @RequestMapping(value = "channels/{channelId}/programs/{seq}", method = RequestMethod.POST)
    public @ResponseBody
    NnProgram addProgram(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("channelId") String channelIdStr,
            @PathVariable("seq") String seqStr) {
        
        Long channelId = null;
        try {
            channelId = Long.valueOf(channelIdStr);
        } catch (NumberFormatException e) {
        }
        if (channelId == null) {
            notFound(resp, INVALID_PATH_PARAMETER);
            return null;
        }
        NnChannelManager channelMngr = new NnChannelManager();
        NnChannel channel = channelMngr.findById(channelId);
        if (channel == null) {
            notFound(resp, "Channel Not Found");
            return null;
        }
        
        // fileUrl
        String fileUrl = req.getParameter("fileUrl");
        if (fileUrl == null) {
            badRequest(resp, MISSING_PARAMETER);
            return null;
        }
        
        Short seq = null;
        try {
            seq = Short.valueOf(seqStr);
        } catch (NumberFormatException e) {
        }
        if (seq == null) {
            notFound(resp, INVALID_PATH_PARAMETER);
            return null;
        }
        
        String name = req.getParameter("name");
        if (name == null) {
            name = "";
        }
        
        String intro = req.getParameter("intro");
        if (intro == null) {
            intro = "";
        }
        
        String imageUrl = req.getParameter("imageUrl"); // format check ?
        if (imageUrl == null) {
            imageUrl = "";
        }
        
        NnProgram program = new NnProgram(channelId,
                NnStringUtil.htmlSafeChars(name),
                NnStringUtil.htmlSafeChars(intro), imageUrl);
        
        program.setFileUrl(fileUrl);
        // do something to get metadata from youtube and set it up
        
        program.setSeq(seqStr);
        
        String comment = req.getParameter("comment");
        if (comment != null) {
            program.setComment(NnStringUtil.htmlSafeChars(comment));
        }
        
        String startTime = req.getParameter("startTime"); // format check ?
        if (startTime != null) {
            program.setStartTime(startTime);
        }
        
        String endTime = req.getParameter("endTime"); // format check ?
        if (endTime != null) {
            program.setEndTime(endTime);
        }
        
        NnProgramManager programMngr = new NnProgramManager();
        programMngr.create(channel, program);
        
        // how to get newly created program at this moment
        
        return program;
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
        
        TitleCardManager titleCardMngr = new TitleCardManager();
        NnProgramManager programMngr = new NnProgramManager();
        
        List<NnProgram> programs = programMngr.findByEpisodeId(episodeId);
        List<TitleCard> results = new ArrayList<TitleCard>();
        
        for (NnProgram program : programs) {
            results.addAll(titleCardMngr.findByProgramId(program.getId()));
        }
        
        for (TitleCard result : results) {
            result.setMessage(NnStringUtil.revertHtml(result.getMessage()));
        }
        
        return results;
    }
    
    @RequestMapping(value = "programs/{episodeId}/shopping_info", method = RequestMethod.DELETE)
    public @ResponseBody
    String shoppingInfoDelete(HttpServletRequest req, HttpServletResponse resp,
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
        
        NnAdManager adMngr = new NnAdManager();
        NnEpisodeManager episodeMngr = new NnEpisodeManager();
        
        NnEpisode episode = episodeMngr.findById(episodeId);
        if (episode == null) {
            
            notFound(resp, "Episode Not Found");
            return null;
        }
        
        episode.setAdId(0);
        episodeMngr.save(episode);
        
        NnAd nnad = adMngr.findByEpisode(episode);
        if (nnad != null) {
            adMngr.delete(nnad);
        }
        
        return "OK";
    }
    
    @RequestMapping(value = "programs/{episodeId}/shopping_info", method = RequestMethod.POST)
    public @ResponseBody
    NnAd shoppingInfoCreate(HttpServletRequest req, HttpServletResponse resp,
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
        
        NnAdManager adMngr = new NnAdManager();
        NnEpisodeManager episodeMngr = new NnEpisodeManager();
        
        NnEpisode episode = episodeMngr.findById(episodeId);
        if (episode == null) {
            notFound(resp, "Episode Not Found");
            return null;
        }
        
        NnAd nnad = adMngr.findByEpisode(episode);
        if (nnad == null) {
            nnad = new NnAd();
        }
        
        // merchantEmail
        String merchantEmail = req.getParameter("merchantEmail");
        if (merchantEmail != null) {
            nnad.setMerchantEmail(merchantEmail);
        }
        
        // message
        String message = req.getParameter("message");
        if (message != null) {
            nnad.setMessage(NnStringUtil.htmlSafeAndTruncated(message));
        }
        
        // url
        String url = req.getParameter("url");
        if (url != null) {
            nnad.setUrl(url);
        }
        
        resp.setStatus(201);
        
        return adMngr.save(nnad, episode);
    }
    
    @RequestMapping(value = "episodes/{episodeId}/shopping_info", method = RequestMethod.GET)
    public @ResponseBody
    NnAd shoppingInfo(HttpServletRequest req, HttpServletResponse resp,
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
        
        NnAdManager adMngr = new NnAdManager();
        NnEpisodeManager episodeMngr = new NnEpisodeManager();
        
        NnEpisode episode = episodeMngr.findById(episodeId);
        if (episode == null) {
            
            notFound(resp, "Episode Not Found");
            return null;
        }
        
        NnAd nnad = adMngr.findByEpisode(episode);
        nnad.setMessage(NnStringUtil.revertHtml(nnad.getMessage()));
        
        return nnad;
    }
    
    // TODO: refine
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
        NnProgramManager programMngr = new NnProgramManager();
        NnProgram program = programMngr.findById(programId);
        if (program == null) {
            notFound(resp, "Program Not Found");
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
        if ((type == TitleCard.TYPE_BEGIN) || (type == TitleCard.TYPE_END)) {
            // pass
        } else {
            badRequest(resp, INVALID_PARAMETER);
            return null;
        }
        
        String message = req.getParameter("message");
        if (message == null) {
            badRequest(resp, MISSING_PARAMETER);
            return null;
        }
        
        
        
        TitleCard titleCard = null;
        TitleCardManager titleCardMngr = new TitleCardManager();
        List<TitleCard> titleCards = titleCardMngr.findByProgramId(programId);
        for (int i = 0; i < titleCards.size(); i++) {
            if (titleCards.get(i).getType() == type) {
                titleCard = titleCards.get(i); // read as it already exist, update operation
                break;
            }
        }
        if (titleCard == null) {
            long channelId = program.getChannelId();
            titleCard = new TitleCard(channelId, programId, type); // create as it not exist, add operation
            titleCard = titleCardMngr.create(titleCard);
        }
        
        // do set and save
        
        String duration = req.getParameter("duration");
        if (duration != null) {
            titleCard.setDuration(duration);
        }
        
        titleCard.setMessage(NnStringUtil.htmlSafeAndTruncated(message));
        
        String size = req.getParameter("size");
        if (size != null) {
            titleCard.setSize(size);
        }
        
        String color = req.getParameter("color");
        if (color != null) {
            titleCard.setColor(color);
        }
        
        String effect = req.getParameter("effect");
        if (effect != null) {
            titleCard.setEffect(effect);
        }
        
        String align = req.getParameter("align");
        if (align != null) {
            titleCard.setAlign(align);
        }
        
        String bgColor = req.getParameter("bgColor");
        if (bgColor != null) {
            titleCard.setBgColor(bgColor);
        }
        
        String style = req.getParameter("style");
        if (style != null) {
            titleCard.setStyle(style);
        }
        
        String weight = req.getParameter("weight");
        if (style != null) {
            titleCard.setWeight(weight);
        }
        
        String bgImage = req.getParameter("bgImage");
        if (bgImage != null) {
            titleCard.setBgImage(bgImage);
        }
        
        return titleCardMngr.save(titleCard);
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
        
        titleCardMngr.delete(titleCard);
        
        return "OK";
    }
    
    @RequestMapping(value = "my_uploads/{id}", method = RequestMethod.GET)
    public @ResponseBody
    NnUserLibrary userUpload(HttpServletRequest req, HttpServletResponse resp,
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
        
        NnUserLibraryManager libMngr = new NnUserLibraryManager();
        NnUserLibrary lib = libMngr.findById(id);
        if (lib == null) {
            notFound(resp, "Item Not Found");
            return null;
        }
        
        lib.setName(NnStringUtil.revertHtml(lib.getName()));
        
        return lib;
    }
    
    @RequestMapping(value = "my_uploads/{id}", method = RequestMethod.PUT)
    public @ResponseBody
    NnUserLibrary userUploadUpdate(HttpServletRequest req, HttpServletResponse resp,
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
        
        NnUserLibraryManager libMngr = new NnUserLibraryManager();
        NnUserLibrary lib = libMngr.findById(id);
        if (lib == null) {
            notFound(resp, "Item Not Found");
            return null;
        }
        
        String name = req.getParameter("name");
        if (name != null) {
            lib.setName(NnStringUtil.htmlSafeAndTruncated(name));
        }
        
        String imageUrl = req.getParameter("imageUrl");
        if (imageUrl != null) {
            lib.setImageUrl(imageUrl);
        }
        
        return libMngr.save(lib);
    }
    
    @RequestMapping(value = { "my_uploads/{id}", "my_library/{id}" }, method = RequestMethod.DELETE)
    public @ResponseBody
    String userUploadsDelete(HttpServletRequest req, HttpServletResponse resp,
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
        
        NnUserLibraryManager libMngr = new NnUserLibraryManager();
        NnUserLibrary lib = libMngr.findById(id);
        if (lib == null) {
            return "Item Not Found";
        }
        
        libMngr.delete(lib);
        
        return "OK";
    }
}
