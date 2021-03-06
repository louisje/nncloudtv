package com.nncloudtv.web.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.nncloudtv.lib.NNF;
import com.nncloudtv.lib.NnStringUtil;
import com.nncloudtv.model.NnChannel;
import com.nncloudtv.model.NnProgram;
import com.nncloudtv.model.NnUser;
import com.nncloudtv.model.Poi;
import com.nncloudtv.model.PoiCampaign;
import com.nncloudtv.model.PoiEvent;
import com.nncloudtv.model.PoiPoint;
import com.nncloudtv.service.PoiPointManager;
import com.nncloudtv.service.TagManager;

@Controller
@RequestMapping("api")
public class ApiPoi extends ApiGeneric {
    
    protected static Logger log = Logger.getLogger(ApiPoi.class.getName());
    
    @RequestMapping(value = "users/{userId}/poi_campaigns", method = RequestMethod.GET)
    public @ResponseBody
    List<PoiCampaign> userCampaigns(HttpServletRequest req,
            HttpServletResponse resp,
            @PathVariable("userId") String userIdStr) {
        
        ApiContext ctx = new ApiContext(req);
        Long userId = NnStringUtil.evalLong(userIdStr);
        if (userId == null) {
            notFound(resp, INVALID_PATH_PARAM);
            return null;
        }
        
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            unauthorized(resp);
            return null;
        } else if (user.getId() != userId) {
            forbidden(resp);
            return null;
        }
        
        List<PoiCampaign> results = NNF.getPoiCampaignMngr().findByUserId(userId);
        if (results == null) {
            return new ArrayList<PoiCampaign>();
        }
        
        for (PoiCampaign result : results) {
            result.setName(NnStringUtil.revertHtml(result.getName()));
        }
        
        return results;
    }
    
    @RequestMapping(value = "users/{userId}/poi_campaigns", method = RequestMethod.POST)
    public @ResponseBody
    PoiCampaign userCampaignCreate(HttpServletRequest req,
            HttpServletResponse resp,
            @PathVariable("userId") String userIdStr) {
        
        ApiContext ctx = new ApiContext(req);
        Long userId = NnStringUtil.evalLong(userIdStr);
        if (userId == null) {
            notFound(resp, INVALID_PATH_PARAM);
            return null;
        }
        
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            unauthorized(resp);
            return null;
        } else if (user.getId() != userId) {
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
        
        PoiCampaign campaign = new PoiCampaign();
        campaign.setMsoId(user.getMsoId());
        campaign.setUserId(userId);
        campaign.setName(name);
        
        // startDate
        Long startDateLong = null;
        String startDateStr = ctx.getParam("startDate");
        if (startDateStr != null) {
            
            startDateLong = NnStringUtil.evalLong(startDateStr);
            if (startDateLong == null) {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
        }
        
        // endDate
        Long endDateLong = null;
        String endDateStr = ctx.getParam("endDate");
        if (endDateStr != null) {
            
            endDateLong = NnStringUtil.evalLong(endDateStr);
            if (endDateLong == null) {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
        }
        
        if (startDateStr != null && endDateStr != null) {
            if (endDateLong < startDateLong) { 
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
            campaign.setStartDate(new Date(startDateLong));
            campaign.setEndDate(new Date(endDateLong));
        } else if (startDateStr == null && endDateStr == null) {
            campaign.setStartDate(null);
            campaign.setEndDate(null);
        } else { // should be pair
            badRequest(resp, INVALID_PARAMETER);
            return null;
        }
        
        campaign = NNF.getPoiCampaignMngr().save(campaign);
        campaign.setName(NnStringUtil.revertHtml(campaign.getName()));
        
        return campaign;
    }
    
    @RequestMapping(value = "poi_campaigns/{campaignId}", method = RequestMethod.GET)
    public @ResponseBody
    PoiCampaign campaign(HttpServletRequest req,
            HttpServletResponse resp,
            @PathVariable("campaignId") String campaignIdStr) {
        
        ApiContext ctx = new ApiContext(req);
        Long campaignId = NnStringUtil.evalLong(campaignIdStr);
        if (campaignId == null) {
            notFound(resp, INVALID_PATH_PARAM);
            return null;
        }
        
        PoiCampaign canpaign = NNF.getPoiCampaignMngr().findById(campaignId);
        if (canpaign == null) {
            notFound(resp, "Campaign Not Found");
            return null;
        }
        
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            unauthorized(resp);
            return null;
        } else if (user.getId() != canpaign.getUserId()) {
            forbidden(resp);
            return null;
        }
        
        canpaign.setName(NnStringUtil.revertHtml(canpaign.getName()));
        
        return canpaign;
    }
    
    @RequestMapping(value = "poi_campaigns/{poiCampaignId}", method = RequestMethod.PUT)
    public @ResponseBody
    PoiCampaign campaignUpdate(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("poiCampaignId") String poiCampaignIdStr) {
        
        ApiContext ctx = new ApiContext(req);
        Long campaignId = NnStringUtil.evalLong(poiCampaignIdStr);
        if (campaignId == null) {
            notFound(resp, INVALID_PATH_PARAM);
            return null;
        }
        PoiCampaign campaign = NNF.getPoiCampaignMngr().findById(campaignId);
        if (campaign == null) {
            notFound(resp, "Campaign Not Found");
            return null;
        }
        
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            unauthorized(resp);
            return null;
        } else if (user.getId() != campaign.getUserId()) {
            forbidden(resp);
            return null;
        }
        
        // name
        String name = ctx.getParam("name");
        if (name != null) {
            name = NnStringUtil.htmlSafeAndTruncated(name);
            campaign.setName(name);
        }
        
        // startDate
        Long startDateLong = null;
        String startDateStr = ctx.getParam("startDate");
        if (startDateStr != null) {
            
            startDateLong = NnStringUtil.evalLong(startDateStr);
            if (startDateLong == null) {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
        }
        
        // endDate
        Long endDateLong = null;
        String endDateStr = ctx.getParam("endDate");
        if (endDateStr != null) {
            
            endDateLong = NnStringUtil.evalLong(endDateStr);
            if (endDateLong == null) {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
        }
        
        if (startDateStr != null && endDateStr != null) {
            if (endDateLong < startDateLong) { 
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
            campaign.setStartDate(new Date(startDateLong));
            campaign.setEndDate(new Date(endDateLong));
        } else if (startDateStr == null && endDateStr == null) {
            // skip
        } else { // should be pair
            badRequest(resp, INVALID_PARAMETER);
            return null;
        }
        
        campaign = NNF.getPoiCampaignMngr().save(campaign);
        
        campaign.setName(NnStringUtil.revertHtml(campaign.getName()));
        
        return campaign;
    }
    
    @RequestMapping(value = "poi_campaigns/{poiCampaignId}", method = RequestMethod.DELETE)
    public @ResponseBody
    void campaignDelete(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("poiCampaignId") String poiCampaignIdStr) {
        
        ApiContext ctx = new ApiContext(req);
        Long poiCampaignId = NnStringUtil.evalLong(poiCampaignIdStr);
        if (poiCampaignId == null) {
            notFound(resp, INVALID_PATH_PARAM);
            return;
        }
        
        PoiCampaign campaign = NNF.getPoiCampaignMngr().findById(poiCampaignId);
        if (campaign == null) {
            notFound(resp, "Campaign Not Found");
            return;
        }
        
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            unauthorized(resp);
            return;
        } else if (user.getId() != campaign.getUserId()) {
            forbidden(resp);
            return;
        }
        
        NNF.getPoiCampaignMngr().delete(campaign);
        
        msgResponse(resp, OK);
    }
    
    @RequestMapping(value = "poi_campaigns/{poiCampaignId}/pois", method = RequestMethod.GET)
    public @ResponseBody
    List<Poi> campaignPois(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("poiCampaignId") String poiCampaignIdStr) {
        
        ApiContext ctx = new ApiContext(req);
        Long poiCampaignId = NnStringUtil.evalLong(poiCampaignIdStr);
        if (poiCampaignId == null) {
            notFound(resp, INVALID_PATH_PARAM);
            return null;
        }
        
        PoiCampaign campaign = NNF.getPoiCampaignMngr().findById(poiCampaignId);
        if (campaign == null) {
            notFound(resp, "Campaign Not Found");
            return null;
        }
        
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            unauthorized(resp);
            return null;
        } else if (user.getId() != campaign.getUserId()) {
            forbidden(resp);
            return null;
        }
        
        // poiPointId
        Long poiPointId = null;
        String poiPointIdStr = ctx.getParam("poiPointId");
        if (poiPointIdStr != null) {
            poiPointId = NnStringUtil.evalLong(poiPointIdStr);
            if (poiPointId == null) {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
        }
        
        if (poiPointId != null) {
            // find pois with point
            return NNF.getPoiMngr().findByPointId(poiPointId);
        } else {
            // find pois with campaign
            return NNF.getPoiCampaignMngr().findPoisByCampaignId(campaign.getId());
        }
    }
    
    @RequestMapping(value = "poi_campaigns/{poiCampaignId}/pois", method = RequestMethod.POST)
    public @ResponseBody
    Poi campaignPoiCreate(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("poiCampaignId") String campaignIdStr) {
        
        // TODO: auth check
        
        ApiContext ctx = new ApiContext(req);
        Long poiCampaignId = NnStringUtil.evalLong(campaignIdStr);
        if (poiCampaignId == null) {
            notFound(resp, INVALID_PATH_PARAM);
            return null;
        }
        
        PoiCampaign campaign = NNF.getPoiCampaignMngr().findById(poiCampaignId);
        if (campaign == null) {
            notFound(resp, "Campaign Not Found");
            return null;
        }
        
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            unauthorized(resp);
            return null;
        } else if (user.getId() != campaign.getUserId()) {
            forbidden(resp);
            return null;
        }
        
        // pointId
        Long pointId = null;
        String pointIdStr = ctx.getParam("pointId");
        if (pointIdStr != null) {
            pointId = NnStringUtil.evalLong(pointIdStr);
            if (pointId == null) {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
        } else {
            badRequest(resp, MISSING_PARAMETER);
            return null;
        }
        
        PoiPoint point = NNF.getPoiPointMngr().findById(pointId);
        if (point == null) {
            badRequest(resp, INVALID_PARAMETER);
            return null;
        }
        
        // eventId
        Long eventId = null;
        String eventIdStr = ctx.getParam("eventId");
        if (eventIdStr != null) {
            eventId = NnStringUtil.evalLong(eventIdStr);
            if (eventId == null) {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
        } else {
            badRequest(resp, MISSING_PARAMETER);
            return null;
        }
        
        PoiEvent event = NNF.getPoiEventMngr().findById(eventId);
        if (event == null) {
            badRequest(resp, INVALID_PARAMETER);
            return null;
        }
        
        // create the poi
        Poi poi = new Poi();
        poi.setCampaignId(campaign.getId());
        poi.setPointId(point.getId());
        poi.setEventId(event.getId());
        
        // startDate
        String startDateStr = ctx.getParam("startDate");
        if (startDateStr != null && startDateStr.length() > 0) {
            Long startDateLong = NnStringUtil.evalLong(startDateStr);
            if (startDateLong == null) {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
            
            poi.setStartDate(new Date(startDateLong));
        } else {
            poi.setStartDate(null);
        }
        
        // endDate
        String endDateStr = ctx.getParam("endDate");
        if (endDateStr != null && endDateStr.length() > 0) {
            Long endDateLong = NnStringUtil.evalLong(endDateStr);
            if (endDateLong == null) {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
            
            poi.setEndDate(new Date(endDateLong));
        } else {
            poi.setEndDate(null);
        }
        
        // hoursOfWeek
        String hoursOfWeek = ctx.getParam("hoursOfWeek");
        if (hoursOfWeek != null) {
            if (hoursOfWeek.matches("[01]{168}")) {
                // valid hoursOfWeek format
            } else {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
            
            poi.setHoursOfWeek(hoursOfWeek);
        } else {
            hoursOfWeek = "";
            for (int i = 1; i <= 7; i++) { // maybe type 111... in the code, will execute faster
                hoursOfWeek = hoursOfWeek.concat("111111111111111111111111");
            }
            
            poi.setHoursOfWeek(hoursOfWeek);
        }
        
        return NNF.getPoiMngr().save(poi);
    }
    
    @RequestMapping(value = "pois/{poiId}", method = RequestMethod.GET)
    public @ResponseBody
    Poi poi(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("poiId") String poiIdStr) {
        
        ApiContext ctx = new ApiContext(req);
        Long poiId = NnStringUtil.evalLong(poiIdStr);
        if (poiId == null) {
            notFound(resp, INVALID_PATH_PARAM);
            return null;
        }
        
        Poi poi = NNF.getPoiCampaignMngr().findPoiById(poiId);
        if (poi == null) {
            notFound(resp, "Poi Not Found");
            return null;
        }
        
        PoiCampaign campaign = NNF.getPoiCampaignMngr().findById(poi.getCampaignId());
        if (campaign == null) {
            // ownership crashed
            // TODO: log
            internalError(resp);
            return null;
        }
        
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            unauthorized(resp);
            return null;
        } else if (user.getId() != campaign.getUserId()) {
            forbidden(resp);
            return null;
        }
        
        return poi;
    }
    
    @RequestMapping(value = "pois/{poiId}", method = RequestMethod.PUT)
    public @ResponseBody
    Poi poiUpdate(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("poiId") String poiIdStr) {
        
        ApiContext ctx = new ApiContext(req);
        Long poiId = NnStringUtil.evalLong(poiIdStr);
        if (poiId == null) {
            notFound(resp, INVALID_PATH_PARAM);
            return null;
        }
        
        Poi poi = NNF.getPoiCampaignMngr().findPoiById(poiId);
        if (poi == null) {
            notFound(resp, "Poi Not Found");
            return null;
        }
        
        PoiCampaign campaign = NNF.getPoiCampaignMngr().findById(poi.getCampaignId());
        if (campaign == null) {
            // ownership crashed
            // TODO: log
            internalError(resp);
            return null;
        }
        
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            unauthorized(resp);
            return null;
        } else if (user.getId() != campaign.getUserId()) {
            forbidden(resp);
            return null;
        }
        
        // startDate
        String startDateStr = ctx.getParam("startDate");
        if (startDateStr != null && startDateStr.length() > 0) {
            Long startDateLong = NnStringUtil.evalLong(startDateStr);
            if (startDateLong == null) {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
            
            poi.setStartDate(new Date(startDateLong));
        }
        
        // endDate
        String endDateStr = ctx.getParam("endDate");
        if (endDateStr != null && endDateStr.length() > 0) {
            Long endDateLong = NnStringUtil.evalLong(endDateStr);
            if (endDateLong == null) {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
            
            poi.setEndDate(new Date(endDateLong));
        }
        
        // hoursOfWeek
        String hoursOfWeek = ctx.getParam("hoursOfWeek");
        if (hoursOfWeek != null) {
            if (hoursOfWeek.matches("[01]{168}")) {
                // valid hoursOfWeek format
                poi.setHoursOfWeek(hoursOfWeek);
            } else {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
        }
        
        return NNF.getPoiMngr().save(poi);
    }
    
    @RequestMapping(value = "pois/{poiId}", method = RequestMethod.DELETE)
    public @ResponseBody
    void poiDelete(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("poiId") String poiIdStr) {
        
        ApiContext ctx = new ApiContext(req);
        Long poiId = NnStringUtil.evalLong(poiIdStr);
        if (poiId == null) {
            notFound(resp, INVALID_PATH_PARAM);
            return;
        }
        
        Poi poi = NNF.getPoiCampaignMngr().findPoiById(poiId);
        if (poi == null) {
            notFound(resp, "Poi Not Found");
            return;
        }
        
        PoiCampaign campaign = NNF.getPoiCampaignMngr().findById(poi.getCampaignId());
        if (campaign == null) {
            // ownership crashed
        }
        
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            unauthorized(resp);
            return;
        } else if (user.getId() != campaign.getUserId()) {
            forbidden(resp);
            return;
        }
        
        NNF.getPoiMngr().delete(poi);
        
        msgResponse(resp, OK);
    }
    
    @RequestMapping(value = "programs/{programId}/poi_points", method = RequestMethod.GET)
    public @ResponseBody
    List<PoiPoint> programPoints(HttpServletRequest req,
            HttpServletResponse resp,
            @PathVariable("programId") String programIdStr) {
        
        Long programId = NnStringUtil.evalLong(programIdStr);
        if (programId == null) {
            notFound(resp, INVALID_PATH_PARAM);
            return null;
        }
        
        NnProgram program = NNF.getProgramMngr().findById(programId);
        if (program == null) {
            notFound(resp, "Program Not Found");
            return null;
        }
        
        List<PoiPoint> points = NNF.getPoiPointMngr().findByProgramId(program.getId());
        Collections.sort(points, PoiPointManager.getStartTimeComparator());
        for (PoiPoint point : points) {
            point.setName(NnStringUtil.revertHtml(point.getName()));
        }
        
        return points;
    }
    
    @RequestMapping(value = "programs/{programId}/poi_points", method = RequestMethod.POST)
    public @ResponseBody
    PoiPoint programPointCreate(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("programId") String programIdStr) {
        
        ApiContext ctx = new ApiContext(req);
        Long programId = NnStringUtil.evalLong(programIdStr);
        if (programId == null) {
            notFound(resp, INVALID_PATH_PARAM);
            return null;
        }
        
        NnProgram program = NNF.getProgramMngr().findById(programId);
        if (program == null) {
            notFound(resp, PROGRAM_NOT_FOUND);
            return null;
        }
        
        NnChannel channel = NNF.getChannelMngr().findById(program.getChannelId());
        if (channel == null) {
            // ownership crashed, it is orphan object
            forbidden(resp);
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
        
        // targetId
        Long targetId = program.getId();
        
        // targetType
        Short targetType = PoiPoint.TYPE_SUBEPISODE;
        
        // name
        String name = ctx.getParam("name");
        if (name == null) {
            badRequest(resp, MISSING_PARAMETER);
            return null;
        }
        name = NnStringUtil.htmlSafeAndTruncated(name);
        
        // startTime & endTime
        Integer startTime = null;
        Integer endTime = null;
        String startTimeStr = ctx.getParam("startTime");
        String endTimeStr = ctx.getParam("endTime");
        if (startTimeStr == null || endTimeStr == null) {
            badRequest(resp, MISSING_PARAMETER);
            return null;
        } else {
            try {
                startTime = Integer.valueOf(startTimeStr);
                endTime = Integer.valueOf(endTimeStr);
            } catch (NumberFormatException e) {
            }
            if ((startTime == null) || (startTime < 0) || (endTime == null) || (endTime <= 0) || (endTime - startTime <= 0)) {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
        }
        // collision check
        PoiPoint point = new PoiPoint();
        point.setTargetId(targetId);
        point.setType(targetType);
        point.setName(name);
        point.setStartTime(startTime);
        point.setEndTime(endTime);
        
        // tag
        String tag = ctx.getParam("tag");;
        if (tag != null) {
            point.setTag(TagManager.processTagText(tag));
        }
        
        // active, default : true
        Boolean active = true;
        String activeStr = ctx.getParam("active");
        if (activeStr != null) {
            active = Boolean.valueOf(activeStr);
        }
        point.setActive(active);
        
        point = NNF.getPoiPointMngr().save(point);
        point.setName(NnStringUtil.revertHtml(point.getName()));
        return point;
    }
    
    @RequestMapping(value = "poi_points/{poiPointId}", method = RequestMethod.GET)
    public @ResponseBody
    PoiPoint point(HttpServletRequest req,
            HttpServletResponse resp,
            @PathVariable("poiPointId") String poiPointIdStr) {
        
        Long poiPointId = NnStringUtil.evalLong(poiPointIdStr);
        if (poiPointId == null) {
            notFound(resp, INVALID_PATH_PARAM);
            return null;
        }
        
        PoiPoint point = NNF.getPoiPointMngr().findById(poiPointId);
        if (point == null) {
            notFound(resp, "PoiPoint Not Found");
            return null;
        }
        
        point.setName(NnStringUtil.revertHtml(point.getName()));
        return point;
    }
    
    @RequestMapping(value = "poi_points/{pointId}", method = RequestMethod.PUT)
    public @ResponseBody
    PoiPoint pointUpdate(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("pointId") String pointIdStr) {
        
        ApiContext ctx = new ApiContext(req);
        Long pointId = NnStringUtil.evalLong(pointIdStr);
        if (pointId == null) {
            notFound(resp, INVALID_PATH_PARAM);
            return null;
        }
        
        PoiPoint point = NNF.getPoiPointMngr().findById(pointId);
        if (point == null) {
            notFound(resp, "PoiPoint Not Found");
            return null;
        }
        
        Long ownerUserId = NNF.getPoiPointMngr().findOwner(point);
        if (ownerUserId == null) { // no one can access orphan object
            forbidden(resp);
            return null;
        }
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            unauthorized(resp);
            return null;
        } else if (user.getId() != ownerUserId) {
            forbidden(resp);
            return null;
        }
        
        // name
        String name = ctx.getParam("name");
        if (name != null) {
            name = NnStringUtil.htmlSafeAndTruncated(name);
            point.setName(name);
        }
        
        if (point.getType() == PoiPoint.TYPE_SUBEPISODE) {
            
            // startTime
            Integer startTime = null;
            String startTimeStr = ctx.getParam("startTime");
            if (startTimeStr != null) {
                try {
                    startTime = Integer.valueOf(startTimeStr);
                } catch (NumberFormatException e) {
                }
                if ((startTime == null) || (startTime < 0)) {
                    badRequest(resp, INVALID_PARAMETER);
                    return null;
                }
            } else {
                // origin setting
                startTime = point.getStartTimeInt();
            }
            
            // endTime
            Integer endTime = null;
            String endTimeStr = ctx.getParam("endTime");
            if (endTimeStr != null) {
                try {
                    endTime = Integer.valueOf(endTimeStr);
                } catch (NumberFormatException e) {
                }
                if ((endTime == null) || (endTime <= 0)) {
                    badRequest(resp, INVALID_PARAMETER);
                    return null;
                }
            } else {
                // origin setting
                endTime = point.getEndTimeInt();
            }
            
            if (endTime - startTime <= 0) {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
            // collision check
            point.setStartTime(startTime);
            point.setEndTime(endTime);
        }
        
        // tag
        String tagText = ctx.getParam("tag");
        String tag = null;
        if (tagText != null) {
            tag = TagManager.processTagText(tagText);
            point.setTag(tag);
        }
        
        // active
        Boolean active;
        String activeStr = ctx.getParam("active");
        if (activeStr != null) {
            active = Boolean.valueOf(activeStr);
            point.setActive(active);
        }
        
        point = NNF.getPoiPointMngr().save(point);
        point.setName(NnStringUtil.revertHtml(point.getName()));
        return point;
    }
    
    @RequestMapping(value = "poi_points/{poiPointId}", method = RequestMethod.DELETE)
    public @ResponseBody
    void pointDelete(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("poiPointId") String poiPointIdStr) {
        
        ApiContext ctx = new ApiContext(req);
        Long poiPointId = NnStringUtil.evalLong(poiPointIdStr);
        if (poiPointId == null) {
            notFound(resp, INVALID_PATH_PARAM);
            return;
        }
        
        PoiPoint point = NNF.getPoiPointMngr().findById(poiPointId);
        if (point == null) {
            notFound(resp, "PoiPoint Not Found");
            return;
        }
        
        Long ownerUserId = NNF.getPoiPointMngr().findOwner(point);
        if (ownerUserId == null) { // orphan object
            forbidden(resp);
            return;
        }
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            unauthorized(resp);
            return;
        } else if (user.getId() != ownerUserId) {
            forbidden(resp);
            return;
        }
        
        NNF.getPoiPointMngr().delete(point);
        
        msgResponse(resp, OK);
    }
    
    @RequestMapping(value = "users/{userId}/poi_events", method = RequestMethod.POST)
    public @ResponseBody
    PoiEvent eventCreate(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("userId") String userIdStr) {
        
        ApiContext ctx = new ApiContext(req);
        Long userId = NnStringUtil.evalLong(userIdStr);
        if (userId == null) {
            notFound(resp, INVALID_PATH_PARAM);
            return null;
        }
        
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            unauthorized(resp);
            return null;
        } else if (user.getId() != userId) {
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
        
        // type
        Short type = null;
        String typeStr = ctx.getParam("type");
        if (typeStr != null) {
            try {
                type = Short.valueOf(typeStr);
            } catch (NumberFormatException e) {
            }
            if (type == null) {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
        } else {
            badRequest(resp, MISSING_PARAMETER);
            return null;
        }
        
        // context
        String context = ctx.getParam("context");
        if (context == null) {
            badRequest(resp, MISSING_PARAMETER);
            return null;
        }
        
        PoiEvent event = new PoiEvent();
        event.setUserId(user.getId());
        event.setMsoId(user.getMsoId());
        event.setName(name);
        event.setType(type);
        event.setContext(context);
        
        // notifyMsg
        if (event.getType() == PoiEvent.TYPE_INSTANTNOTIFICATION ||
             event.getType() == PoiEvent.TYPE_SCHEDULEDNOTIFICATION) {
            
            String notifyMsg = ctx.getParam("notifyMsg");
            if (notifyMsg == null) {
                badRequest(resp, MISSING_PARAMETER);
                return null;
            }
            notifyMsg = NnStringUtil.htmlSafeAndTruncated(notifyMsg);
            event.setNotifyMsg(notifyMsg);
        }
        
        // notifyScheduler
        if (event.getType() == PoiEvent.TYPE_SCHEDULEDNOTIFICATION) {
            String notifyScheduler = ctx.getParam("notifyScheduler");
            if (notifyScheduler == null) {
                badRequest(resp, MISSING_PARAMETER);
                return null;
            }
            String[] timestampList = notifyScheduler.split(",");
            Long timestamp = null;
            for (String timestampStr : timestampList) {
                
                timestamp = NnStringUtil.evalLong(timestampStr);
                if (timestamp == null) {
                    badRequest(resp, INVALID_PARAMETER);
                    return null;
                }
            }
            event.setNotifyScheduler(notifyScheduler);
        }
        
        PoiEvent result = NNF.getPoiEventMngr().save(event);
        
        result.setName(NnStringUtil.revertHtml(result.getName()));
        if (result.getType() == PoiEvent.TYPE_INSTANTNOTIFICATION ||
                result.getType() == PoiEvent.TYPE_SCHEDULEDNOTIFICATION) {
            result.setNotifyMsg(NnStringUtil.revertHtml(result.getNotifyMsg()));
        }
        
        return result;
    }
    
    @RequestMapping(value = "poi_events/{poiEventId}", method = RequestMethod.GET)
    public @ResponseBody
    PoiEvent event(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("poiEventId") String poiEventIdStr) {
        
        ApiContext ctx = new ApiContext(req);
        Long poiEventId = NnStringUtil.evalLong(poiEventIdStr);
        if (poiEventId == null) {
            notFound(resp, INVALID_PATH_PARAM);
            return null;
        }
        
        PoiEvent event = NNF.getPoiEventMngr().findById(poiEventId);
        if (event == null) {
            notFound(resp, "PoiEvent Not Found");
            return null;
        }
        
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            unauthorized(resp);
            return null;
        } else if (user.getId() != event.getUserId()) {
            forbidden(resp);
            return null;
        }
        
        event.setName(NnStringUtil.revertHtml(event.getName()));
        if (event.getType() == PoiEvent.TYPE_INSTANTNOTIFICATION ||
                event.getType() == PoiEvent.TYPE_SCHEDULEDNOTIFICATION) {
            event.setNotifyMsg(NnStringUtil.revertHtml(event.getNotifyMsg()));
        }
        
        return event;
    }
    
    @RequestMapping(value = "poi_events/{poiEventId}", method = RequestMethod.PUT)
    public @ResponseBody
    PoiEvent eventUpdate(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("poiEventId") String poiEventIdStr) {
        
        ApiContext ctx = new ApiContext(req);
        Long poiEventId = NnStringUtil.evalLong(poiEventIdStr);
        if (poiEventId == null) {
            notFound(resp, INVALID_PATH_PARAM);
            return null;
        }
        
        PoiEvent event = NNF.getPoiEventMngr().findById(poiEventId);
        if (event == null) {
            notFound(resp, "PoiEvent Not Found");
            return null;
        }
        
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            unauthorized(resp);
            return null;
        } else if (user.getId() != event.getUserId()) {
            forbidden(resp);
            return null;
        }
        
        // name
        String name = ctx.getParam("name");
        if (name != null) {
            name = NnStringUtil.htmlSafeAndTruncated(name);
            event.setName(name);
        }
        
        Boolean shouldContainNotifyMsg = false; // TODO rewrite flag control
        Boolean shouldContainNotifyScheduler = false; // TODO rewrite flag control
        // type
        Short type = null;
        String typeStr = ctx.getParam("type");
        if (typeStr != null) {
            try {
                type = Short.valueOf(typeStr);
            } catch (NumberFormatException e) {
            }
            if (type == null) {
                badRequest(resp, INVALID_PARAMETER);
                return null;
            }
            
            Short originType = event.getType();
            if (originType == PoiEvent.TYPE_POPUP || originType == PoiEvent.TYPE_HYPERLINK ||
                 originType == PoiEvent.TYPE_POLL) {
                if (type == PoiEvent.TYPE_INSTANTNOTIFICATION || type == PoiEvent.TYPE_SCHEDULEDNOTIFICATION) {
                    shouldContainNotifyMsg = true;
                }
            }
            if (originType == PoiEvent.TYPE_POPUP || originType == PoiEvent.TYPE_HYPERLINK ||
                    originType == PoiEvent.TYPE_POLL || originType == PoiEvent.TYPE_INSTANTNOTIFICATION) {
                   if (type == PoiEvent.TYPE_SCHEDULEDNOTIFICATION) {
                       shouldContainNotifyScheduler = true;
                   }
            }
            
            event.setType(type);
        }
        
        // context
        String context = ctx.getParam("context");
        if (context != null) {
            event.setContext(context);
        }
        
        // notifyMsg
        if (event.getType() == PoiEvent.TYPE_INSTANTNOTIFICATION ||
             event.getType() == PoiEvent.TYPE_SCHEDULEDNOTIFICATION) {
            String notifyMsg = ctx.getParam("notifyMsg");
            if (shouldContainNotifyMsg == true && notifyMsg == null) {
                badRequest(resp, MISSING_PARAMETER);
                return null;
            }
            if (notifyMsg != null) {
                notifyMsg = NnStringUtil.htmlSafeAndTruncated(notifyMsg);
                event.setNotifyMsg(notifyMsg);
            }
        }
        
        // notifyScheduler
        if (event.getType() == PoiEvent.TYPE_SCHEDULEDNOTIFICATION) {
            String notifyScheduler = ctx.getParam("notifyScheduler");
            if (shouldContainNotifyScheduler == true && notifyScheduler == null) {
                badRequest(resp, MISSING_PARAMETER);
                return null;
            }
            if (notifyScheduler != null) {
                String[] timestampList = notifyScheduler.split(",");
                Long timestamp = null;
                for (String timestampStr : timestampList) {
                    
                    timestamp = NnStringUtil.evalLong(timestampStr);
                    if (timestamp == null) {
                        badRequest(resp, INVALID_PARAMETER);
                        return null;
                    }
                }
                event.setNotifyScheduler(notifyScheduler);
            }
        }
        
        PoiEvent result = NNF.getPoiEventMngr().save(event);
        
        result.setName(NnStringUtil.revertHtml(result.getName()));
        if (result.getType() == PoiEvent.TYPE_INSTANTNOTIFICATION ||
                result.getType() == PoiEvent.TYPE_SCHEDULEDNOTIFICATION) {
            result.setNotifyMsg(NnStringUtil.revertHtml(result.getNotifyMsg()));
        }
        
        return result;
    }
    
    @RequestMapping(value = "poi_events/{eventId}", method = RequestMethod.DELETE)
    public @ResponseBody
    void eventDelete(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("eventId") String eventIdStr) {
        
        ApiContext ctx = new ApiContext(req);
        PoiEvent event = NNF.getPoiEventMngr().findById(eventIdStr);
        if (event == null) {
            notFound(resp, "PoiEvent Not Found");
            return;
        }
        
        NnUser user = ctx.getAuthenticatedUser();
        if (user == null) {
            unauthorized(resp);
            return;
        } else if (user.getId() != event.getUserId()) {
            forbidden(resp);
            return;
        }
        
        NNF.getPoiEventMngr().delete(event);
        
        msgResponse(resp, OK);
    }
    
    @RequestMapping(value = "channels/{channelId}/poi_points", method = RequestMethod.GET)
    public @ResponseBody
    List<PoiPoint> channelPoints(HttpServletRequest req, HttpServletResponse resp,
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
        
        List<PoiPoint> results = NNF.getPoiPointMngr().findByChannel(channel.getId());
        for (PoiPoint result : results) {
            
            result.setName(NnStringUtil.revertHtml(result.getName()));
        }
        
        return results;
    }
    
    @RequestMapping(value = "channels/{channelId}/poi_points", method = RequestMethod.POST)
    public @ResponseBody
    PoiPoint channelPointCreate(HttpServletRequest req, HttpServletResponse resp,
            @PathVariable("channelId") String channelIdStr) {
        
        ApiContext ctx = new ApiContext(req);
        NnChannel channel = NNF.getChannelMngr().findById(channelIdStr);
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
        
        // targetId
        Long targetId = channel.getId();
        
        // targetType
        Short targetType = PoiPoint.TYPE_CHANNEL;
        
        // name
        String name = ctx.getParam("name");
        if (name == null) {
            badRequest(resp, MISSING_PARAMETER);
            return null;
        }
        name = NnStringUtil.htmlSafeAndTruncated(name);
        
        PoiPoint point = new PoiPoint();
        point.setTargetId(targetId);
        point.setType(targetType);
        point.setName(name);
        point.setStartTime(0);
        point.setEndTime(0);
        
        // tag
        String tag = ctx.getParam("tag");
        if (tag != null) {
            tag = TagManager.processTagText(tag);
            point.setTag(tag);
        }
        
        // active, default : true
        Boolean active = true;
        String activeStr = ctx.getParam("active");
        if (activeStr != null) {
            active = Boolean.valueOf(activeStr);
        }
        point.setActive(active);
        
        point = NNF.getPoiPointMngr().save(point);
        point.setName(NnStringUtil.revertHtml(point.getName()));
        return point;
    }
}
