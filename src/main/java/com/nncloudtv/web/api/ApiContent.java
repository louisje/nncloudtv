package com.nncloudtv.web.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.util.IOUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.nncloudtv.dao.NnEpisodeDao;
import com.nncloudtv.exception.ZeroLengthException;
import com.nncloudtv.lib.NNF;
import com.nncloudtv.lib.NnDateUtil;
import com.nncloudtv.lib.NnNetUtil;
import com.nncloudtv.lib.NnStringUtil;
import com.nncloudtv.lib.QueueFactory;
import com.nncloudtv.lib.SearchLib;
import com.nncloudtv.lib.stream.StreamFactory;
import com.nncloudtv.model.LocaleTable;
import com.nncloudtv.model.Mso;
import com.nncloudtv.model.NnChannel;
import com.nncloudtv.model.NnChannelPref;
import com.nncloudtv.model.NnEpisode;
import com.nncloudtv.model.NnProgram;
import com.nncloudtv.model.NnUser;
import com.nncloudtv.model.NnUserProfile;
import com.nncloudtv.model.SysTag;
import com.nncloudtv.model.SysTagDisplay;
import com.nncloudtv.model.TitleCard;
import com.nncloudtv.model.YtProgram;
import com.nncloudtv.service.CategoryService;
import com.nncloudtv.service.MsoConfigManager;
import com.nncloudtv.service.NnChannelManager;
import com.nncloudtv.service.NnChannelPrefManager;
import com.nncloudtv.service.NnEpisodeManager;
import com.nncloudtv.service.NnUserProfileManager;
import com.nncloudtv.service.PlayerApiService;
import com.nncloudtv.service.TitleCardManager;
import com.nncloudtv.web.json.cms.Category;

@Controller
@RequestMapping("api")
public class ApiContent extends ApiGeneric {
    
    protected static Logger log = Logger.getLogger(ApiContent.class.getName());
    
    @RequestMapping(value = "channels/{channelId}/autosharing/facebook", method = RequestMethod.DELETE)
    public @ResponseBody
    void facebookAutosharingDelete(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("channelId") String channelIdStr) {
        
        ApiContext ctx = new ApiContext(req);
        NnChannel channel = NNF.getChannelMngr().findById(channelIdStr);
        if (channel == null) {
            notFound(resp, CHANNEL_NOT_FOUND);
            return;
        }
        
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            
            unauthorized(resp);
            return;
            
        } else if (!user.getIdStr().equals(channel.getUserIdStr())) {
            
            forbidden(resp);
            return;
        }
        
        NnChannelPrefManager prefMngr = NNF.getChPrefMngr();
        
        prefMngr.delete(prefMngr.findByChannelIdAndItem(channel.getId(), NnChannelPref.FB_AUTOSHARE));
        
        msgResponse(resp, OK);
    }
    
    @RequestMapping(value = "channels/{channelId}/autosharing/brand", method = RequestMethod.GET)
    public @ResponseBody
    Map<String, Object> brandAutosharingGet(HttpServletRequest req,
            HttpServletResponse resp,
            @PathVariable("channelId") String channelIdStr) {
        
        Long channelId = null;
        try {
            channelId = Long.valueOf(channelIdStr);
        } catch (NumberFormatException e) {
            notFound(resp, INVALID_PATH_PARAM);
            return null;
        }
        
        NnChannel channel = NNF.getChannelMngr().findById(channelId);
        if (channel == null) {
            notFound(resp, CHANNEL_NOT_FOUND);
            return null;
        }
        
        NnChannelPref pref = NNF.getChPrefMngr().getBrand(channel.getId());
        Mso mso = NNF.getMsoMngr().findByName(pref.getValue());
        String brand = pref.getValue();
        if (NNF.getMsoMngr().isValidBrand(channel, mso) == false) {
            brand = Mso.NAME_SYS;
        }
        
        Map<String, Object> result = new TreeMap<String, Object>();
        result.put("brand", brand);
        
        return result;
    }
    
    @RequestMapping(value = "channels/{channelId}/autosharing/brand", method = RequestMethod.PUT)
    public @ResponseBody void brandAutosharingSet(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("channelId") String channelIdStr) {
        
        ApiContext ctx = new ApiContext(req);
        NnChannel channel = NNF.getChannelMngr().findById(channelIdStr);
        if (channel == null) {
            notFound(resp, CHANNEL_NOT_FOUND);
            return;
        }
        
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            unauthorized(resp);
            return;
        } else if (!user.getIdStr().equals(channel.getUserIdStr())) {
            forbidden(resp);
            return;
        }
        
        // brand
        String brand = ctx.getParam("brand");
        if (brand == null) {
            badRequest(resp, MISSING_PARAMETER);
            return;
        }
        Mso mso = NNF.getMsoMngr().findByName(brand);
        if (mso == null) {
            badRequest(resp, INVALID_PARAMETER);
            return;
        }
        if (NNF.getMsoMngr().isValidBrand(channel, mso) == false) {
            badRequest(resp, INVALID_PARAMETER);
            return;
        }
        
        NNF.getChPrefMngr().setBrand(channel.getId(), mso);
        
        msgResponse(resp, OK);
    }
    
    @RequestMapping(value = "channels/{channelId}/autosharing/validBrands", method = RequestMethod.GET)
    public @ResponseBody
    List<Map<String, Object>> validBrandsAutosharingGet(HttpServletRequest req,
            HttpServletResponse resp,
            @PathVariable("channelId") String channelIdStr) {
        
        Long channelId = null;
        try {
            channelId = Long.valueOf(channelIdStr);
        } catch (NumberFormatException e) {
            notFound(resp, INVALID_PATH_PARAM);
            return null;
        }
        
        NnChannel channel = NNF.getChannelMngr().findById(channelId);
        if (channel == null) {
            notFound(resp, CHANNEL_NOT_FOUND);
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
            notFound(resp, INVALID_PATH_PARAM);
            return null;
        }
        YtProgram ytProgram = NNF.getProgramMngr().findYtProgramById(ytProgramId);
        if (ytProgram == null) {
            notFound(resp, PROGRAM_NOT_FOUND);
            return null;
        }
        return ytProgram;
    }
    
    @RequestMapping(value = "programs/{programId}.ts", method = {RequestMethod.HEAD, RequestMethod.GET})
    public void programStream(@PathVariable("programId") String programIdStr,
            HttpServletRequest req, HttpServletResponse resp) {
        
        Long programId = null;
        try {
            programId = Long.valueOf(programIdStr);
        } catch (NumberFormatException e) {
        }
        if (programId == null) {
            notFound(resp, INVALID_PATH_PARAM);
            return;
        }
        NnProgram program = NNF.getProgramMngr().findById(programId);
        if (program == null) {
            notFound(resp, PROGRAM_NOT_FOUND);
            return;
        }
        
        resp.setContentType("video/mp2t");
        if (!req.getMethod().equalsIgnoreCase("HEAD")) {
            try {
                
                StreamFactory.streaming(program.getFileUrl(), resp.getOutputStream());
                
            } catch (IOException e) {
                
                log.info(e.getClass().getName());
                log.info(e.getMessage());
                internalError(resp);
                
            } catch (ZeroLengthException e) {
                
                notFound(resp);
            }
        }
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
            notFound(resp, INVALID_PATH_PARAM);
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
        
        ApiContext ctx = new ApiContext(req);
        Long programId = null;
        try {
            programId = Long.valueOf(programIdStr);
        } catch (NumberFormatException e) {
        }
        if (programId == null) {
            notFound(resp, INVALID_PATH_PARAM);
            return null;
        }
        
        NnProgram program = NNF.getProgramMngr().findById(programId);
        if (program == null) {
            notFound(resp, PROGRAM_NOT_FOUND);
            return null;
        }
        
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            unauthorized(resp);
            return null;
        }
        NnChannel channel = NNF.getChannelMngr().findById(program.getChannelId());
        if ((channel == null) || (!user.getIdStr().equals(channel.getUserIdStr()))) {
            forbidden(resp);
            return null;
        }
        
        // name
        String name = ctx.getParam("name");
        if (name != null) {
            program.setName(NnStringUtil.truncateUTF8(name));
        }
        
        // intro
        String intro = ctx.getParam("intro");
        if (intro != null) {
            program.setIntro(NnStringUtil.truncateUTF8(intro));
        }
        
        // imageUrl
        String imageUrl = ctx.getParam("imageUrl");
        if (imageUrl != null) {
            program.setImageUrl(imageUrl);
        }
        
        // subSeq
        String subSeqStr = ctx.getParam("subSeq");
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
        Short startTime = NnStringUtil.evalShort(ctx.getParam("startTime"));
        if (startTime != null)
            program.setStartTime(startTime);
        
        // endTime
        Short endTime = NnStringUtil.evalShort(ctx.getParam("endTime"));
        if (endTime != null)
            program.setEndTime(endTime);
        
        // duration
        Short duration = NnStringUtil.evalShort(ctx.getParam("duration"));
        if (duration != null)
            program.setDuration(duration);
        
        // cntPoi
        Short cntPoi = NnStringUtil.evalShort(ctx.getParam("cntPoi"));
        if (cntPoi != null)
            program.setCntPoi(cntPoi);
        
        program = NNF.getProgramMngr().save(program);
        
        program.setName(NnStringUtil.revertHtml(program.getName()));
        program.setIntro(NnStringUtil.revertHtml(program.getIntro()));
        
        return program;
    }
    
    @RequestMapping(value = "programs/{programId}", method = RequestMethod.DELETE)
    public @ResponseBody
    void programDelete(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("programId") String programIdStr) {
        
        ApiContext ctx = new ApiContext(req);
        NnProgram program = NNF.getProgramMngr().findById(programIdStr);
        if (program == null) {
            msgResponse(resp, PROGRAM_NOT_FOUND);
            return;
        }
        
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            unauthorized(resp);
            return;
        }
        NnChannel channel = NNF.getChannelMngr().findById(program.getChannelId());
        if ((channel == null) || (!user.getIdStr().equals(channel.getUserIdStr()))) {
            forbidden(resp);
            return;
        }
        
        NNF.getProgramMngr().delete(program);
        
        msgResponse(resp, OK);
    }
    
    // delete programs in one episode
    @RequestMapping(value = "episodes/{episodeId}/programs", method = RequestMethod.DELETE)
    public @ResponseBody
    void programsDelete(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("episodeId") String episodeIdStr) {
        
        ApiContext ctx = new ApiContext(req);
        NnEpisode episode = NNF.getEpisodeMngr().findById(episodeIdStr);
        if (episode == null) {
            notFound(resp, EPISODE_NOT_FOUND);
            return;
        }
        
        NnChannel channel = NNF.getChannelMngr().findById(episode.getChannelId());
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            
            unauthorized(resp);
            return;
            
        } else if (channel == null || !user.getIdStr().equals(channel.getUserIdStr())) {
            
            forbidden(resp);
            return;
        }
        
        String programStr = ctx.getParam("programs");
        if (programStr == null) {
            badRequest(resp, MISSING_PARAMETER);
            return;
        }
        log.info(programStr);
        
        String[] split = programStr.split(",");
        List<Long> list = new ArrayList<Long>();
        for (String programIdStr : split) {
            
            Long programId = NnStringUtil.evalLong(programIdStr);
            if (programId != null) {
                list.add(programId);
            }
        }
        
        List<NnProgram> programs = NNF.getProgramMngr().findAllByIds(list);
        Iterator<NnProgram> it = programs.iterator();
        while (it.hasNext()) {
            
            NnProgram program = it.next();
            if (program.getEpisodeId() != episode.getId()) {
                
                it.remove();
                continue;
            }
            
        }
        
        log.info("program delete count = " + programs.size());
        
        NNF.getProgramMngr().deleteAll(programs);
        
        msgResponse(resp, OK);
    }
    
    @RequestMapping(value = "episodes/{episodeId}/programs", method = RequestMethod.POST)
    public @ResponseBody
    NnProgram programCreate(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("episodeId") String episodeIdStr) {
        
        ApiContext ctx = new ApiContext(req);
        Long episodeId = null;
        try {
            episodeId = Long.valueOf(episodeIdStr);
        } catch (NumberFormatException e) {
        }
        if (episodeId == null) {
            notFound(resp, INVALID_PATH_PARAM);
            return null;
        }
        NnEpisode episode = NNF.getEpisodeMngr().findById(episodeId);
        if (episode == null) {
            notFound(resp, EPISODE_NOT_FOUND);
            return null;
        }
        
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            unauthorized(resp);
            return null;
        }
        NnChannel channel = NNF.getChannelMngr().findById(episode.getChannelId());
        if ((channel == null) || (!user.getIdStr().equals(channel.getUserIdStr()))) {
            forbidden(resp);
            return null;
        }
        
        // name
        String name = ctx.getParam("name");
        if (name == null) {
            badRequest(resp, MISSING_PARAMETER);
            return null;
        }
        name = NnStringUtil.htmlSafeAndTruncated(name);
        
        // intro
        String intro = ctx.getParam("intro");
        if (intro != null) {
            intro = NnStringUtil.htmlSafeAndTruncated(intro);
        }
        
        // imageUrl
        String imageUrl = ctx.getParam("imageUrl");
        if (imageUrl == null) {
            imageUrl = NnChannel.IMAGE_WATERMARK_URL;
        }
        
        NnProgram program = new NnProgram(episode.getChannelId(), episodeId, name, intro, imageUrl);
        program.setPublic(true);
        
        // fileUrl
        String fileUrl = ctx.getParam("fileUrl");
        if (fileUrl == null) {
            badRequest(resp, MISSING_PARAMETER);
            return null;
        }
        program.setFileUrl(fileUrl);
        
        // contentType
        program.setContentType(NnProgram.CONTENTTYPE_YOUTUBE);
        String contentTypeStr = ctx.getParam("contentType");
        if (contentTypeStr != null) {
            
            Short contentType = NnStringUtil.evalShort(contentTypeStr);
            if (contentType == null) {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
            program.setContentType(contentType);
        }
        
        // duration
        Short duration = NnStringUtil.evalShort(ctx.getParam("duration"));
        if (duration == null)
            program.setDuration((short) 0);
        else
            program.setDuration(duration);
        
        // cntPoi
        Short cntPoi = NnStringUtil.evalShort(ctx.getParam("cntPoi"));
        if (cntPoi != null)
            program.setCntPoi(cntPoi);
        
        // startTime
        Short startTime = NnStringUtil.evalShort(ctx.getParam("startTime"));
        if (startTime == null)
            program.setStartTime(0);
        else
            program.setStartTime(startTime);
        
        // endTime
        Short endTime = NnStringUtil.evalShort(ctx.getParam("endTime"));
        if (endTime == null)
            program.setEndTime(0);
        else
            program.setEndTime(endTime);
        
        // subSeq
        String subSeqStr = ctx.getParam("subSeq");
        if (subSeqStr == null || subSeqStr.isEmpty()) {
            
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
        program.setPublishDate(NnDateUtil.now());
        program.setPublic(true);
        
        program = NNF.getProgramMngr().create(episode, program);
        
        program.setName(NnStringUtil.revertHtml(program.getName()));
        program.setIntro(NnStringUtil.revertHtml(program.getIntro()));
        
        return program;
    }
    
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "channels", method = RequestMethod.GET)
    public @ResponseBody
    List<NnChannel> channelSearch(HttpServletRequest req,
            HttpServletResponse resp,
            @RequestParam(required = false, value = "mso") String msoName,
            @RequestParam(required = false, value = "sphere") String sphereStr,
            @RequestParam(required = false, value = "channels") String channelParam,
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
            
        } else if (channelParam != null) {
            
            Set<Long> channelIdSet = new HashSet<Long>();
            for (String channelIdStr : channelParam.split(",")) {
                Long channelId = NnStringUtil.evalLong(channelIdStr);
                if (channelId != null) {
                    channelIdSet.add(channelId);
                }
            }
            
            results = channelMngr.findByIds(channelIdSet);
            
            log.info("total channels = " + results.size());
            
        } else if (keyword != null && keyword.length() > 0) {
            
            log.info("keyword: " + keyword);
            List<String> sphereList = new ArrayList<String>();
            String sphereFilter = null;
            if (sphereStr == null && msoName != null) {
                storeOnly = true;
                System.out.println("[channel_search] mso = " + msoName);
                List<String> spheres = MsoConfigManager.getSuppoertedRegion(mso, true);
                sphereStr = StringUtils.join(spheres, ',');
                System.out.println("[channel_search] mso supported region = " + sphereStr);
            }
            if (sphereStr != null && !sphereStr.isEmpty()) {
                storeOnly = true;
                String[] sphereArr = new String[0];
                sphereArr = sphereStr.split(",");
                for (String sphere : sphereArr) {
                    sphereList.add(NnStringUtil.escapedQuote(sphere));
                }
                sphereFilter = "sphere in (" + StringUtils.join(sphereList, ',') + ")";
                log.info("sphere filter = " + sphereFilter);
            }
            String type = req.getParameter("type");
            List<NnChannel> channels = new ArrayList<NnChannel>();
            if (type != null && type.equalsIgnoreCase("solr")) {
                log.info("search from Solr");
                Stack<?> stack = channelMngr.searchSolr(SearchLib.CORE_NNCLOUDTV, keyword, (storeOnly ? SearchLib.STORE_ONLY : null), sphereFilter, false, 0, 150);
                channels.addAll((List<NnChannel>) stack.pop());
                long solrNum = (Long) stack.pop();
                log.info("counts from solr = " + solrNum);
            } else {
                channels = channelMngr.search(keyword, (storeOnly ? SearchLib.STORE_ONLY : null), sphereFilter, false, 0, 150);
            }
            System.out.println(String.format("[channel_search] found %d channels", channels.size()));
            
            if (sphereFilter == null) {
                
                List<NnUser> users = new ArrayList<NnUser>();
                short[] shards = { NnUser.SHARD_CHINESE, NnUser.SHARD_DEFAULT };
                for (short shard : shards) {
                    
                    Set<NnUserProfile> profiles = NNF.getProfileMngr().search(keyword, 0, 30, shard);
                    Set<Long> userIdSet = new HashSet<Long>();
                    for (NnUserProfile profile : profiles) {
                        userIdSet.add(profile.getUserId());
                    }
                    
                    List<NnUser> shardUsers = NNF.getUserMngr().findAllByIds(userIdSet, shard);
                    System.out.println(String.format("[channel_search] found %d profiles which in %d users from shard %d", profiles.size(), shardUsers.size(), shard));
                    
                    users.addAll(shardUsers);
                }
                System.out.println(String.format("[channel_search] total %d users found", users.size()));
                
                List<NnChannel> userChannels = channelMngr.findByUsers(users, 150);
                int recognized = 0;
                for (NnChannel channel : userChannels) {
                    
                    if (channel.getStatus() == NnChannel.STATUS_SUCCESS && channel.isPublic()) {
                        
                        if (sphereList.isEmpty() || sphereList.contains(channel.getSphere())) {
                            
                            channels.add(channel);
                            recognized++;
                        }
                    }
                }
                System.out.println(String.format("[channel_search] %d channels obtained from user where %d of them are recognized", userChannels.size(), recognized));
            }
            
            System.out.println("[channel_search] total channels = " + channels.size());
            if (msoName != null) {
                // mso blacklist
                List<Long> blackList = NNF.getStoreListingMngr().findChannelIdsByMsoId(mso.getId());
                Iterator<NnChannel> it = channels.iterator();
                while (it.hasNext()) {
                    NnChannel channel = it.next();
                    if (blackList.contains(channel.getId())) {
                        it.remove();
                        continue;
                    }
                }
                System.out.println("[channel_search] total channels (filtered by mso blacklist) = " + channels.size());
            }
            results = channels;
            
            Collections.sort(results, NnChannelManager.getComparator("updateDate"));
            
        } else if (ytPlaylistIdStr != null || ytUserIdStr != null) {
            
            if (ytPlaylistIdStr != null) {
                
                String sourceUrl = "http://www.youtube.com/view_play_list?p=" + ytPlaylistIdStr;
                NnChannel result = channelMngr.findBySourceUrl(sourceUrl);
                if (result != null) {
                    results.add(result);
                }
                
            } else if (ytUserIdStr != null) {
                
                String sourceUrl = "http://www.youtube.com/user/" + ytUserIdStr;
                NnChannel result = channelMngr.findBySourceUrl(sourceUrl);
                if (result != null) {
                    results.add(result);
                }
            }
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
            notFound(resp, INVALID_PATH_PARAM);
            return null;
        }
        
        NnChannelManager channelMngr = NNF.getChannelMngr();
        NnChannel channel = channelMngr.findById(channelIdStr);
        if (channel == null) {
            notFound(resp, CHANNEL_NOT_FOUND);
            return null;
        }
        
        channelMngr.populateCategoryId(channel);
        if (channel.isReadonly() == false) {
            channelMngr.populateMoreImageUrl(channel);
            channelMngr.populateCntItem(channel);
            channelMngr.populateSocialFeeds(channel);
            channelMngr.populateBannerImageUrl(channel);
        }
        channelMngr.populateAutoSync(channel);
        channelMngr.normalize(channel);
        
        return channel;
    }
    
    @RequestMapping(value = "channels/{channelId}", method = RequestMethod.PUT)
    public @ResponseBody
    NnChannel channelUpdate(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("channelId") String channelIdStr) {
        
        ApiContext ctx = new ApiContext(req);
        NnChannelManager channelMngr = NNF.getChannelMngr();
        NnChannel channel = channelMngr.findById(channelIdStr);
        if (channel == null) {
            notFound(resp, CHANNEL_NOT_FOUND);
            return null;
        }
        
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            
            unauthorized(resp);
            return null;
            
        } else if (!user.getIdStr().equals(channel.getUserIdStr())) {
            
            forbidden(resp);
            return null;
        }
        
        // name
        String name = ctx.getParam("name");
        if (name != null) {
            channel.setName(NnStringUtil.truncateUTF8(name));
        }
        
        // intro
        String intro = ctx.getParam("intro");
        if (intro != null) {
            channel.setIntro(NnStringUtil.truncateUTF8(intro, NnStringUtil.VERY_LONG_STRING_LENGTH));
        }
        
        // lang
        String lang = ctx.getParam("lang");
        if (lang != null) {
            channel.setLang(lang);
        }
        
        // sphere
        String sphere = ctx.getParam("sphere");
        if (sphere != null) {
            channel.setSphere(sphere);
        }
        
        // isPublic
        Boolean isPublic = NnStringUtil.evalBool(ctx.getParam("isPublic"));
        if (isPublic != null) {
            channel.setPublic(isPublic);
        }
        
        // paidChannel
        Boolean paidChannel = NnStringUtil.evalBool(ctx.getParam("paidChannel"));
        if (paidChannel != null) {
            channel.setPaidChannel(paidChannel);
        }
        
        // tag
        String tag = ctx.getParam("tag");
        if (tag != null) {
            channel.setTag(tag);
        }
        
        // imageUrl
        String imageUrl = ctx.getParam("imageUrl");
        if (imageUrl != null) {
            channel.setImageUrl(imageUrl);
        }
        
        // categoryId
        Long categoryId = null;
        String categoryIdStr = ctx.getParam("categoryId");
        if (categoryIdStr != null) {
            
            categoryId = NnStringUtil.evalLong(categoryIdStr);
            if (categoryId != null && CategoryService.isSystemCategory(categoryId)) {
                
                NNF.getCategoryService().setupChannelCategory(categoryId, channel.getId());
                channel.setCategoryId(categoryId);
            }
        }
        
        // updateDate
        String updateDateStr = ctx.getParam("updateDate");
        if (updateDateStr != null) {
            channel.setUpdateDate(NnDateUtil.now());
        }
        
        // sorting
        Short sorting = null;
        String sortingStr = ctx.getParam("sorting");
        if (sortingStr != null) {
            sorting = NnStringUtil.evalShort(sortingStr);
            if (sorting != null) {
                channel.setSorting(sorting);
                NNF.getProgramMngr().resetCache(channel.getId());
            }
        }
        
        // status
        Short status = NnStringUtil.evalShort(ctx.getParam("status"));
        if (status != null && status != channel.getStatus()) {
            user.setProfile(NNF.getProfileMngr().pickupBestProfile(user));
            if (NnUserProfileManager.checkPriv(user, NnUserProfile.PRIV_SYSTEM_STORE))
                channel.setStatus(status);
        }
        
        // autoSync
        String autoSync = ctx.getParam("autoSync");
        if (autoSync != null) {
            channelMngr.populateAutoSync(channel.getId(), autoSync);
        }
        // bannerImageUrl
        String bannerImage = ctx.getParam("bannerImageUrl");
        if (bannerImage != null) {
            channelMngr.populateBannerImageUrl(channel.getId(), bannerImage);
        }
        // socialFeeds
        String socialFeeds = ctx.getParam("socialFeeds");
        if (socialFeeds != null) {
            channelMngr.populateSocialFeeds(channel.getId(), socialFeeds);
        }
        channel = channelMngr.save(channel);
        
        // syncNow
        if (NnStringUtil.evalBool(ctx.getParam("syncNow"), false)) {
            
            channel = NnChannelManager.syncNow(channel);
        }
        
        return channel;
    }
    
    // TODO remove
    @RequestMapping(value = "channels/{channelId}/youtubeSyncData", method = RequestMethod.PUT)
    public void channelYoutubeDataSync(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("channelId") String channelIdStr) {
        
        ApiContext ctx = new ApiContext(req);
        NnChannel channel = NNF.getChannelMngr().findById(channelIdStr);
        if (channel == null) {
            notFound(resp, CHANNEL_NOT_FOUND);
            return;
        }
        
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            
            unauthorized(resp);
            return;
            
        } else if (!user.getIdStr().equals(channel.getUserIdStr())) {
            
            forbidden(resp);
            return;
        }
        
        channel.setReadonly(true);
        channel = NNF.getChannelMngr().save(channel);
        
        Map<String, String> obj = new HashMap<String, String>();
        obj.put("id",          channel.getIdStr());
        obj.put("sourceUrl",   channel.getSourceUrl());
        obj.put("contentType", String.valueOf(channel.getContentType()));
        obj.put("isRealtime",  "true");
        
        String response = NnNetUtil.urlPostWithJson("http://" + MsoConfigManager.getCrawlerDomain() + "/ytcrawler/crawlerAPI.php", obj);
        
        if (response != null && response.trim().equalsIgnoreCase("Ack")) {
            
            msgResponse(resp, OK);
            
        } else {
            
            channel.setReadonly(false);
            NNF.getChannelMngr().save(channel);
            
            msgResponse(resp, "NOT_OK");
        }
    }
    
    @RequestMapping(value = "tags", method = RequestMethod.GET)
    public @ResponseBody String[] tags(HttpServletRequest req, HttpServletResponse resp) {
        
        ApiContext ctx = new ApiContext(req);
        String categoryIdStr = ctx.getParam("categoryId");
        if (categoryIdStr == null) {
            badRequest(resp, MISSING_PARAMETER);
            return null;
        }
        String lang = ctx.getParam(ApiContext.PARAM_LANG, LocaleTable.LANG_EN);
        
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
            
            badRequest(resp, CATEGORY_NOT_FOUND);
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
        
        ApiContext ctx = new ApiContext(req);
        String lang = ctx.getParam(ApiContext.PARAM_LANG, LocaleTable.LANG_EN);
        
        List<Category> categories = NNF.getCategoryService().getSystemCategories(lang);
        
        return categories;
    }
    
    @RequestMapping(value = "store", method = RequestMethod.GET)
    public @ResponseBody
    List<Long> storeChannels(HttpServletRequest req, HttpServletResponse resp) {
        
        // categoryId
        String categoryIdStr = req.getParameter("categoryId");
        if (categoryIdStr == null) {
            badRequest(resp, MISSING_PARAMETER);
            return null;
        }
        log.info("categoryId = " + categoryIdStr);
        Long categoryId = NnStringUtil.evalLong(categoryIdStr);
        if (categoryId == null || !CategoryService.isSystemCategory(categoryId)) {
            badRequest(resp, INVALID_PARAMETER);
            return null;
        }
        
        // sphere
        List<String> spheres = null;
        String sphereParam = req.getParameter("sphere");
        if (sphereParam != null && !sphereParam.isEmpty())
            spheres = new ArrayList<String>(Arrays.asList(sphereParam.split(",")));
        List<Long> results = new ArrayList<Long>();
        List<NnChannel> channels = NNF.getCategoryService().getCategoryChannels(categoryId, spheres);
        for (NnChannel channel : channels)
            results.add(channel.getId());
        return results;
    }
    
    // TODO cache
    @RequestMapping(value = "channels/{channelId}.m3u8", method = RequestMethod.GET)
    public void channelM3U8(HttpServletResponse resp, HttpServletRequest req,
            @PathVariable("channelId") String channelIdStr) {
        
        Long channelId = NnStringUtil.evalLong(channelIdStr);
        if (channelId == null) {
            notFound(resp, INVALID_PATH_PARAM);
            return;
        }
        
        NnChannel channel = NNF.getChannelMngr().findById(channelId);
        if (channel == null) {
            notFound(resp, CHANNEL_NOT_FOUND);
            return;
        }
        
        ApiContext ctx = new ApiContext(req);
        List<NnEpisode> episodes = NNF.getEpisodeMngr().findByChannelId(channelId);
        if (channel.getSorting() == NnChannel.SORT_POSITION_REVERSE) {
            Collections.sort(episodes, NnEpisodeManager.getComparator("reverse"));
        } else if (channel.getSorting() == NnChannel.SORT_TIMED_LINEAR) {
            Collections.sort(episodes, NnEpisodeManager.getComparator("timedLinear"));
        } else {
            Collections.sort(episodes, NnEpisodeManager.getComparator("seq"));
        }
        if (episodes.size() > PlayerApiService.PAGING_ROWS)
            episodes = episodes.subList(0, PlayerApiService.PAGING_ROWS); // trim
        
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, NnStringUtil.UTF8));
            
            int totalDuration = 0;
            for (NnEpisode episode : episodes) {
                totalDuration += episode.getDuration();
            }
            
            writer.println("#EXTM3U");
            writer.println("#EXT-X-TARGETDURATION:" + totalDuration);
            writer.println("#EXT-X-MEDIA-SEQUENCE:1");
            for (NnEpisode episode : episodes) {
                
                writer.println("#EXTINF:" + episode.getDuration() + "," + episode.getName());
                writer.println(ctx.getRoot() + "/api/episodes/" + episode.getId() + ".ts");
            }
            writer.println("#EXT-X-ENDLIST");
            writer.flush();
            resp.setContentType(VND_APPLE_MPEGURL);
            resp.setContentLength(baos.size());
            IOUtils.copy(new ByteArrayInputStream(baos.toByteArray()), resp.getOutputStream());
            
        } catch (UnsupportedEncodingException e) {
            
            log.warning(e.getMessage());
            internalError(resp);
            return;
            
        } catch (IOException e) {
            
            log.info(e.getMessage());
            
        } finally {
            
            try {
                resp.flushBuffer();
            } catch (IOException e) {
            }
        }
    }
    
    @RequestMapping(value = "channels/{channelId}/episodes", method = RequestMethod.GET)
    public @ResponseBody
    List<NnEpisode> channelEpisodes(HttpServletResponse resp,
            HttpServletRequest req,
            @PathVariable("channelId") String channelIdStr) {
        
        Long channelId = NnStringUtil.evalLong(channelIdStr);
        if (channelId == null) {
            notFound(resp, INVALID_PATH_PARAM);
            return null;
        }
        
        NnChannel channel = NNF.getChannelMngr().findById(channelId);
        if (channel == null) {
            notFound(resp, CHANNEL_NOT_FOUND);
            return null;
        }
        
        // page
        String pageStr = req.getParameter("page");
        Long page = NnStringUtil.evalLong(pageStr);
        if (page == null) {
            page = (long) 0;
        }
        
        // rows
        String rowsStr = req.getParameter("rows");
        Long rows = NnStringUtil.evalLong(rowsStr);
        if (rows == null) {
            rows = (long) 0;
        }
        NnEpisodeManager episodeMngr = NNF.getEpisodeMngr();
        List<NnEpisode> results = new ArrayList<NnEpisode>();
        if (page > 0 && rows > 0) {
            
            if (channel.getSorting() == NnChannel.SORT_POSITION_REVERSE) {
                results = episodeMngr.listV2(page, rows, "seq DESC", "channelId = " + channelId);
            } else if (channel.getSorting() == NnChannel.SORT_TIMED_LINEAR) {
                results = episodeMngr.listV2(page - 1, rows, NnEpisodeDao.V2_LINEAR_SORTING, "channelId = " + channelId);
            } else {
                results = episodeMngr.listV2(page, rows, "seq ASC", "channelId = " + channelId);
            }
        } else {
            results = episodeMngr.findByChannelId(channelId);
            if (channel.getSorting() == NnChannel.SORT_POSITION_REVERSE) {
                Collections.sort(results, NnEpisodeManager.getComparator("reverse"));
            } else if (channel.getSorting() == NnChannel.SORT_TIMED_LINEAR) {
                Collections.sort(results, NnEpisodeManager.getComparator("timedLinear"));
            } else {
                Collections.sort(results, NnEpisodeManager.getComparator("seq"));
            }
        }
        
        episodeMngr.normalize(results);
        for (NnEpisode episode : results)
            episode.setPlaybackUrl(NnStringUtil.getSharingUrl(false, null, episode.getChannelId(), episode.getId()));
        
        return results;
    }
    
    @RequestMapping(value = "channels/{channelId}/episodes/sorting", method = RequestMethod.PUT)
    public @ResponseBody
    void channelEpisodesSorting(HttpServletRequest req,
            HttpServletResponse resp, @PathVariable("channelId") String channelIdStr) {
        
        ApiContext ctx = new ApiContext(req);
        NnChannel channel = NNF.getChannelMngr().findById(channelIdStr);
        if (channel == null) {
            notFound(resp, CHANNEL_NOT_FOUND);
            return;
        }
        
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            
            unauthorized(resp);
            return;
            
        } else if (!user.getIdStr().equals(channel.getUserIdStr())) {
            
            forbidden(resp);
            return;
        }
        
        String episodeParam = req.getParameter("episodes");
        if (episodeParam == null) {
            
            NNF.getEpisodeMngr().reorderChannelEpisodes(channel.getId());
            
            msgResponse(resp, OK);
            return;
        }
        String[] splitted = episodeParam.split(",");
        ArrayList<Long> episodeIdList = new ArrayList<Long>();
        List<NnEpisode> episodes = NNF.getEpisodeMngr().findByChannelId(channel.getId());
        
        for (String episodeIdStr : splitted) {
            
            Long episodeId = NnStringUtil.evalLong(episodeIdStr);
            if (episodeId != null) {
                episodeIdList.add(episodeId);
            }
        }
        
        if (episodeIdList.size() != episodes.size()) {
            
            log.info(String.format("%d not equal %d", episodeIdList.size(), episodes.size()));
            badRequest(resp, INVALID_PARAMETER);
            return;
        }
        
        for (NnEpisode episode : episodes) {
            
            int index = episodeIdList.indexOf(episode.getId());
            if (index < 0) {
                
                log.info(String.format("episodeId %d is not matched", episode.getId()));
                badRequest(resp, INVALID_PARAMETER);
                return;
            }
            
            episode.setSeq(index + 1);
        }
        
        NNF.getEpisodeMngr().saveAll(episodes);
        NNF.getChannelMngr().renewUpdateDateOnly(channel);
        
        msgResponse(resp, OK);
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
            notFound(resp, CHANNEL_NOT_FOUND);
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
            
            results = NNF.getEpisodeMngr().listV2(page, rows, "seq ASC", "channelId = " + channelId);
            
        } else {
            
            results = NNF.getEpisodeMngr().findByChannelId(channelId);
            
        }
        if (results == null) {
            return new ArrayList<NnEpisode>();
        }
        
        Collections.sort(results, NnEpisodeManager.getComparator("seq"));
        
        for (NnEpisode episode : results) {
            
            NNF.getEpisodeMngr().normalize(episode);
            episode.setPlaybackUrl(NnStringUtil.getSharingUrl(false, null, episode.getChannelId(), episode.getId()));
        }
        
        return results;
    }
    
    @RequestMapping(value = "episodes/{episodeId}", method = RequestMethod.DELETE)
    public @ResponseBody
    void episodeDelete(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("episodeId") String episodeIdStr) {
        
        ApiContext ctx = new ApiContext(req);
        NnEpisode episode = NNF.getEpisodeMngr().findById(episodeIdStr);
        if (episode == null) {
            
            msgResponse(resp, EPISODE_NOT_FOUND);
            return;
        }
        
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            unauthorized(resp);
            return;
        }
        NnChannelManager channelMngr = NNF.getChannelMngr();
        NnChannel channel = channelMngr.findById(episode.getChannelId());
        if ((channel == null) || (!user.getIdStr().equals(channel.getUserIdStr()))) {
            forbidden(resp);
            return;
        }
        
        // delete episode
        NNF.getEpisodeMngr().delete(episode);
        
        // re-calcuate episode count
        if (channel != null) {
            channel.setCntEpisode(channelMngr.calcuateEpisodeCount(channel));
            channelMngr.save(channel);
        }
        
        msgResponse(resp, OK);
    }
    
    @RequestMapping(value = "episodes/{episodeId}.ts", method = RequestMethod.GET)
    public void episodeStream(HttpServletRequest req, HttpServletResponse resp, @PathVariable("episodeId") String episodeIdStr) {
        
        Long episodeId = null;
        try {
            episodeId = Long.valueOf(episodeIdStr);
        } catch (NumberFormatException e) {
        }
        if (episodeId == null) {
            notFound(resp, INVALID_PATH_PARAM);
            return;
        }
        NnEpisode episode = NNF.getEpisodeMngr().findById(episodeId);
        if (episode == null) {
            
            notFound(resp, EPISODE_NOT_FOUND);
            return;
        }
        List<NnProgram> programs = NNF.getProgramMngr().findByEpisodeId(episodeId);
        
        resp.setContentType("video/mp2t");
        if (!req.getMethod().equalsIgnoreCase("HEAD")) {
            try {
                int zeroLengthCnt = 0;
                ServletOutputStream out = resp.getOutputStream();
                
                for (NnProgram program : programs) {
                    
                    try {
                        StreamFactory.streaming(program.getFileUrl(), out);
                    } catch (ZeroLengthException e) {
                        zeroLengthCnt += 1;
                    }
                }
                
                if (zeroLengthCnt > 0 && zeroLengthCnt == programs.size()) {
                    notFound(resp);
                    return;
                }
                
            } catch (IOException e) {
                
                log.info(e.getClass().getName());
                log.info(e.getMessage());
                internalError(resp);
                return;
            }
        }
    }
    
    @RequestMapping(value = "episodes/{episodeId}.m3u8", method = RequestMethod.GET)
    public void episodeM3U8(HttpServletRequest req, HttpServletResponse resp, @PathVariable("episodeId") String episodeIdStr) {
        
        ApiContext ctx = new ApiContext(req);
        Long episodeId = null;
        try {
            episodeId = Long.valueOf(episodeIdStr);
        } catch (NumberFormatException e) {
        }
        if (episodeId == null) {
            notFound(resp, INVALID_PATH_PARAM);
            return;
        }
        NnEpisode episode = NNF.getEpisodeMngr().findById(episodeId);
        if (episode == null) {
            
            notFound(resp, EPISODE_NOT_FOUND);
            return;
        }
        
        List<NnProgram> programs = NNF.getProgramMngr().findByEpisodeId(episodeId);
        
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, NnStringUtil.UTF8));
            
            writer.println("#EXTM3U");
            writer.println("#EXT-X-TARGETDURATION:" + episode.getDuration());
            writer.println("#EXT-X-MEDIA-SEQUENCE:1");
            for (NnProgram program : programs) {
                
                writer.println("#EXTINF:" + program.getDurationInt() + "," + program.getName());
                writer.println(ctx.getRoot() + "/api/programs/" + program.getId() + ".ts");
            }
            writer.println("#EXT-X-ENDLIST");
            writer.flush();
            
            resp.setContentType(VND_APPLE_MPEGURL);
            resp.setContentLength(baos.size());
            IOUtils.copy(new ByteArrayInputStream(baos.toByteArray()), resp.getOutputStream());
            
        } catch (UnsupportedEncodingException e) {
            
            log.warning(e.getMessage());
            internalError(resp);
            return;
            
        } catch (IOException e) {
            
            log.info(e.getMessage());
            
        } finally {
            
            try {
                resp.flushBuffer();
            } catch (IOException e) {
            }
        }
    }
    
    @RequestMapping(value = "episodes/{episodeId}", method = RequestMethod.GET)
    public @ResponseBody NnEpisode episode(HttpServletRequest req, HttpServletResponse resp, @PathVariable("episodeId") String episodeIdStr) {
        
        Long episodeId = null;
        try {
            episodeId = Long.valueOf(episodeIdStr);
        } catch (NumberFormatException e) {
        }
        if (episodeId == null) {
            notFound(resp, INVALID_PATH_PARAM);
            return null;
        }
        
        NnEpisode episode = NNF.getEpisodeMngr().findById(episodeId);
        if (episode == null) {
            notFound(resp, EPISODE_NOT_FOUND);
            return null;
        }
        
        NNF.getEpisodeMngr().normalize(episode);
        
        return episode;
    }
    
    @RequestMapping(value = "episodes/{episodeId}", method = RequestMethod.PUT)
    public @ResponseBody
    NnEpisode episodeUpdate(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("episodeId") String episodeIdStr) {
        
        boolean dirty = false;
        ApiContext ctx = new ApiContext(req);
        NnEpisodeManager episodeMngr = NNF.getEpisodeMngr();
        
        NnEpisode episode = episodeMngr.findById(episodeIdStr);
        if (episode == null) {
            notFound(resp, EPISODE_NOT_FOUND);
            return null;
        }
        
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            unauthorized(resp);
            return null;
        }
        NnChannelManager channelMngr = NNF.getChannelMngr();
        NnChannel channel = channelMngr.findById(episode.getChannelId());
        if ((channel == null) || (!user.getIdStr().equals(channel.getUserIdStr()))) {
            forbidden(resp);
            return null;
        }
        
        // name
        String name = ctx.getParam("name");
        if (name != null) {
            episode.setName(NnStringUtil.truncateUTF8(name));
        }
        
        // intro
        String intro = ctx.getParam("intro");
        if (intro != null) {
            episode.setIntro(NnStringUtil.truncateUTF8(intro, NnStringUtil.VERY_LONG_STRING_LENGTH));
        }
        
        // contentType
        String contentTypeStr = ctx.getParam("contentType");
        if (contentTypeStr != null) {
            Short contentType = NnStringUtil.evalShort(contentTypeStr);
            if (contentType != null) {
                episode.setContentType(contentType);
            }
        }
        
        // imageUrl
        String imageUrl = ctx.getParam("imageUrl");
        if (imageUrl != null) {
            if (imageUrl.equals(episode.getImageUrl()) == false) {
                
                if (episode.getContentType() == NnEpisode.CONTENTTYPE_UPLOADED) {
                    dirty = true;
                }
                episode.setImageUrl(imageUrl);
            }
        }
        
        // scheduleDate
        String scheduleDateStr = ctx.getParam("scheduleDate");
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
        String publishDateStr = ctx.getParam("publishDate");
        if (publishDateStr != null) {
            
            log.info("publishDate = " + publishDateStr);
            
            if (publishDateStr.isEmpty()) {
                
                log.info("set publishDate to null");
                episode.setPublishDate(null);
                
            } else if (publishDateStr.equalsIgnoreCase("NOW")) {
                
                episode.setPublishDate(NnDateUtil.now());
                
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
        
        Long storageId = NnStringUtil.evalLong(ctx.getParam("storageId"));
        if (storageId != null) {
            episode.setStorageId(storageId);
        }
        
        boolean autoShare = false;
        // isPublic
        String isPublicStr = ctx.getParam("isPublic");
        if (isPublicStr != null) {
            Boolean isPublic = Boolean.valueOf(isPublicStr);
            if (isPublic != null) {
                if (episode.isPublic() == false && isPublic == true) {
                    autoShare = true;
                }
                episode.setPublic(isPublic);
            }
        }
        
        // duration
        String durationStr = ctx.getParam("duration");
        if (durationStr != null) {
            Integer duration = NnStringUtil.evalInt(durationStr);
            if (duration != null && duration >= 0) {
                episode.setDuration(duration);
            } else {
                episode.setDuration(episodeMngr.calculateEpisodeDuration(episode));
            }
        } else {
            episode.setDuration(episodeMngr.calculateEpisodeDuration(episode));
        }
        
        // seq
        String seqStr = ctx.getParam("seq");
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
        
        episode = episodeMngr.save(episode);
        
        episode.setName(NnStringUtil.revertHtml(episode.getName()));
        episode.setIntro(NnStringUtil.revertHtml(episode.getIntro()));
        
        // mark as hook position
        if (autoShare == true) {
            episodeMngr.autoShareToFacebook(episode);
            channelMngr.renewUpdateDateOnly(NNF.getChannelMngr().findById(episode.getChannelId()));
        }
        
        if (dirty) {
            QueueFactory.add("/podcastAPI/processThumbnail?episode=" + episode.getId(), null);
        }
        
        return episode;
    }
    
    @RequestMapping(value = "channels/{channelId}/episodes", method = RequestMethod.POST)
    public @ResponseBody NnEpisode episodeCreate(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("channelId") String channelIdStr) {
        
        boolean dirty = false;
        ApiContext ctx = new ApiContext(req);
        NnChannelManager channelMngr = NNF.getChannelMngr();
        
        NnChannel channel = channelMngr.findById(channelIdStr);
        if (channel == null) {
            notFound(resp, CHANNEL_NOT_FOUND);
            return null;
        }
        
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            
            unauthorized(resp);
            return null;
            
        } else if (!user.getIdStr().equals(channel.getUserIdStr())) {
            
            forbidden(resp);
            return null;
        }
        
        // name
        String name = ctx.getParam("name");
        if (name == null || name.isEmpty()) {
            
            badRequest(resp, MISSING_PARAMETER);
            return null;
        }
        name = NnStringUtil.truncateUTF8(name);
        
        // intro
        String intro = ctx.getParam("intro");
        if (intro != null && intro.length() > 0) {
            intro = NnStringUtil.truncateUTF8(intro, NnStringUtil.VERY_LONG_STRING_LENGTH);
        }
        
        // contentType
        Short contentType = NnStringUtil.evalShort(ctx.getParam("contentType"));
        if (contentType == null) {
            contentType = NnEpisode.CONTENTTYPE_GENERAL;
        }
        
        // imageUrl
        String imageUrl = ctx.getParam("imageUrl");
        if (imageUrl == null) {
            imageUrl = NnChannel.IMAGE_WATERMARK_URL;
        } else if (contentType == NnEpisode.CONTENTTYPE_UPLOADED) {
            dirty = true;
        }
        
        NnEpisode episode = new NnEpisode(channel.getId());
        episode.setName(name);
        episode.setIntro(intro);
        episode.setImageUrl(imageUrl);
        episode.setChannelId(channel.getId());
        episode.setContentType(contentType);
        
        // scheduleDate
        String scheduleDateStr = ctx.getParam("scheduleDate");
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
        
        // duration
        String durationStr = ctx.getParam("duration");
        if (durationStr != null) {
            Integer duration = NnStringUtil.evalInt(durationStr);
            if (duration != null && duration >= 0) {
                episode.setDuration(duration);
            }
        }
        
        // publishDate
        String publishDateStr = ctx.getParam("publishDate");
        if (publishDateStr != null) {
            
            log.info("publishDate = " + publishDateStr);
            
            if (publishDateStr.isEmpty()) {
                
                log.info("set publishDate to null");
                episode.setPublishDate(null);
                
            } else if (publishDateStr.equalsIgnoreCase("NOW")) {
                
                episode.setPublishDate(NnDateUtil.now());
                
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
        String isPublicStr = ctx.getParam("isPublic");
        if (isPublicStr != null) {
            Boolean isPublic = Boolean.valueOf(isPublicStr);
            if (isPublic != null) {
                if (isPublic == true) {
                    autoShare = true;
                }
                episode.setPublic(isPublic);
            }
        }
        
        Long storageId = NnStringUtil.evalLong(ctx.getParam("storageId"));
        if (storageId != null) {
            episode.setStorageId(storageId);
        }
        
        Short seq = NnStringUtil.evalShort(ctx.getParam("seq"));
        episode.setSeq(seq == null ? 0 : seq);
        
        NnEpisodeManager episodeMngr = NNF.getEpisodeMngr();
        
        episode = episodeMngr.save(episode);
        if (episode.getSeq() == 0) {
            episodeMngr.reorderChannelEpisodes(channel.getId());
        }
        
        episode.setName(NnStringUtil.revertHtml(episode.getName()));
        episode.setIntro(NnStringUtil.revertHtml(episode.getIntro()));
        
        channel.setCntEpisode(channelMngr.calcuateEpisodeCount(channel));
        channelMngr.save(channel);
        
        // mark as hook position 
        if (autoShare == true) {
            episodeMngr.autoShareToFacebook(episode);
            channelMngr.renewUpdateDateOnly(channel);
        }
        
        if (dirty) {
            QueueFactory.add("/podcastAPI/processThumbnail?episode=" + episode.getId(), null);
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
            notFound(resp, INVALID_PATH_PARAM);
            return null;
        }
        
        NnEpisode episode = NNF.getEpisodeMngr().findById(episodeId);
        if (episode == null) {
            
            notFound(resp, EPISODE_NOT_FOUND);
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
            notFound(resp, INVALID_PATH_PARAM);
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
            notFound(resp, INVALID_PATH_PARAM);
            return null;
        }
        
        NnEpisode episode = NNF.getEpisodeMngr().findById(episodeId);
        if (episode == null) {
            
            notFound(resp, EPISODE_NOT_FOUND);
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
        
        ApiContext ctx = new ApiContext(req);
        NnProgram program = NNF.getProgramMngr().findById(programIdStr);
        if (program == null) {
            notFound(resp, PROGRAM_NOT_FOUND);
            return null;
        }
        
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            unauthorized(resp);
            return null;
        }
        NnChannel channel = NNF.getChannelMngr().findById(program.getChannelId());
        if ((channel == null) || (!user.getIdStr().equals(channel.getUserIdStr()))) {
            forbidden(resp);
            return null;
        }
        
        // type
        Short type = NnStringUtil.evalShort(ctx.getParam("type"));
        if (type == null) {
            badRequest(resp, INVALID_PARAMETER);
            return null;
        }
        
        TitleCardManager titleCardMngr = new TitleCardManager();
        
        TitleCard titleCard = titleCardMngr.findByProgramIdAndType(program.getId(), type);
        if (titleCard == null) {
            titleCard = new TitleCard(program.getChannelId(), program.getId(), type);
        }
        
        // message
        String message = ctx.getParam("message");
        if (message == null) {
            badRequest(resp, MISSING_PARAMETER);
            return null;
        }
        titleCard.setMessage(NnStringUtil.htmlSafeAndTruncated(message, 2000));
        
        // duration
        String duration = ctx.getParam("duration");
        if (duration == null) {
            titleCard.setDuration(TitleCard.DEFAULT_DURATION);
        } else {
            titleCard.setDuration(duration);
        }
        
        // size
        String size = ctx.getParam("size");
        if (size == null) {
            titleCard.setSize(TitleCard.DEFAULT_SIZE);
        } else {
            titleCard.setSize(size);
        }
        
        // color
        String color = ctx.getParam("color");
        if (color == null) {
            titleCard.setColor(TitleCard.DEFAULT_COLOR);
        } else {
            titleCard.setColor(color);
        }
        
        // effect
        String effect = ctx.getParam("effect");
        if (effect == null) {
            titleCard.setEffect(TitleCard.DEFAULT_EFFECT);
        } else {
            titleCard.setEffect(effect);
        }
        
        // align
        String align = ctx.getParam("align");
        if (align == null) {
            titleCard.setAlign(TitleCard.DEFAULT_ALIGN);
        } else {
            titleCard.setAlign(align);
        }
        
        // bgColor
        String bgColor = ctx.getParam("bgColor");
        if (bgColor == null) {
            //titleCard.setBgColor(TitleCard.DEFAULT_BG_COLOR);
        } else {
            titleCard.setBgColor(bgColor);
        }
        
        // style
        String style = ctx.getParam("style");
        if (style == null) {
            titleCard.setStyle(TitleCard.DEFAULT_STYLE);
        } else {
            titleCard.setStyle(style);
        }
        
        // weight
        String weight = ctx.getParam("weight");
        if (weight == null) {
            titleCard.setWeight(TitleCard.DEFAULT_WEIGHT);
        } else {
            titleCard.setWeight(weight);
        }
        
        // bgImg
        String bgImage = ctx.getParam("bgImage");
        if (bgImage != null) {
            titleCard.setBgImage(bgImage);
        }
        
        titleCard = titleCardMngr.save(titleCard);
        
        titleCard.setMessage(NnStringUtil.revertHtml(titleCard.getMessage()));
        
        return titleCard;
    }
    
    @RequestMapping(value = "title_card/{id}", method = RequestMethod.DELETE)
    public @ResponseBody
    void titleCardDelete(HttpServletResponse resp, HttpServletRequest req,
            @PathVariable("id") String idStr) {
        
        ApiContext ctx = new ApiContext(req);
        TitleCard titleCard = NNF.getTitleCardMngr().findById(idStr);
        if (titleCard == null) {
            notFound(resp, TITLECARD_NOT_FOUND);
            return;
        }
        
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            unauthorized(resp);
            return;
        }
        NnChannel channel = NNF.getChannelMngr().findById(titleCard.getChannelId());
        if ((channel == null) || (!user.getIdStr().equals(channel.getUserIdStr()))) {
            forbidden(resp);
            return;
        }
        
        NNF.getTitleCardMngr().delete(titleCard);
        
        msgResponse(resp, OK);
    }
}
