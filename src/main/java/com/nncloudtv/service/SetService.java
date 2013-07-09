package com.nncloudtv.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.nncloudtv.lib.NnStringUtil;
import com.nncloudtv.model.NnChannel;
import com.nncloudtv.model.SysTag;
import com.nncloudtv.model.SysTagDisplay;
import com.nncloudtv.model.SysTagMap;
import com.nncloudtv.web.json.cms.Set;

@Service
public class SetService {
    
    protected static final Logger log = Logger.getLogger(SetService.class.getName());
    
    private SysTagManager sysTagMngr;
    private SysTagDisplayManager sysTagDisplayMngr;
    private SysTagMapManager sysTagMapMngr;
    private NnChannelManager channelMngr;
    
    @Autowired
    public SetService(SysTagManager sysTagMngr, SysTagDisplayManager sysTagDisplayMngr,
                        SysTagMapManager sysTagMapMngr, NnChannelManager channelMngr) {
        this.sysTagMngr = sysTagMngr;
        this.sysTagDisplayMngr = sysTagDisplayMngr;
        this.sysTagMapMngr = sysTagMapMngr;
        this.channelMngr = channelMngr;
    }
    
    /** build Set from SysTag and SysTagDisplay */
    private Set composeSet(SysTag set, SysTagDisplay setMeta) {
        
        Set setResp = new Set();
        setResp.setId(set.getId());
        setResp.setMsoId(set.getMsoId());
        setResp.setDisplayId(setMeta.getId());
        setResp.setChannelCnt(setMeta.getCntChannel());
        setResp.setLang(setMeta.getLang());
        setResp.setSeq(set.getSeq());
        setResp.setTag(setMeta.getPopularTag());
        setResp.setName(NnStringUtil.revertHtml(setMeta.getName()));
        setResp.setSortingType(set.getSorting());
        
        return setResp;
    }
    
    /** find Sets that owned by Mso with specify display language */
    public List<Set> findByMsoIdAndLang(Long msoId, String lang) {
        
        List<Set> results = new ArrayList<Set>();
        Set result = null;
        
        if (msoId == null) {
            return new ArrayList<Set>();
        }
        
        //List<SysTag> results = dao.findByMsoIdAndType(msoId, SysTag.TYPE_SET);
        List<SysTag> sets = sysTagMngr.findByMsoIdAndType(msoId, SysTag.TYPE_SET);
        if (sets == null || sets.size() == 0) {
            return new ArrayList<Set>();
        }
        
        SysTagDisplay setMeta = null;
        for (SysTag set : sets) {
            
            if (lang != null) {
                setMeta = sysTagDisplayMngr.findBySysTagIdAndLang(set.getId(), lang);
            } else {
                setMeta = sysTagDisplayMngr.findBySysTagId(set.getId());
            }
            
            if (setMeta != null) {
                result = composeSet(set, setMeta);
                results.add(result);
            } else {
                if (lang == null) {
                    log.warning("invalid structure : SysTag's Id=" + set.getId() + " exist but not found any of SysTagDisPlay");
                } else {
                    log.info("SysTag's Id=" + set.getId() + " exist but not found match SysTagDisPlay for lang=" + lang);
                }
            }
        }
        
        return results;
    }
    
    /** find Sets that owned by Mso */
    public List<Set> findByMsoId(Long msoId) {
        
        if (msoId == null) {
            return new ArrayList<Set>();
        }
        
        return findByMsoIdAndLang(msoId, null);
    }
    
    /** find Set by SysTag's Id */
    public Set findById(Long setId) {
        
        if (setId == null) {
            return null;
        }
        
        SysTag set = sysTagMngr.findById(setId);
        if (set == null || set.getType() != SysTag.TYPE_SET) {
            return null;
        }
        
        SysTagDisplay setMeta = sysTagDisplayMngr.findBySysTagId(set.getId());
        if (setMeta == null) {
            log.warning("invalid structure : SysTag's Id=" + set.getId() + " exist but not found any of SysTagDisPlay");
            return null;
        }
        
        return composeSet(set, setMeta);
    }
    
    /** get Channels from Set ordered by Seq, the Channels populate additional information (TimeStart, TimeEnd, Seq, AlwaysOnTop)
     *  retrieve from SysTagMap */
    public List<NnChannel> getChannelsOrderBySeq(Long setId) {
        
        if (setId == null) {
            return new ArrayList<NnChannel>();
        }
        
        List<SysTagMap> sysTagMaps = sysTagMapMngr.findBySysTagId(setId);
        if (sysTagMaps == null || sysTagMaps.size() == 0) {
            return new ArrayList<NnChannel>();
        }
        
        List<Long> channelIdList = new ArrayList<Long>();
        for (SysTagMap item : sysTagMaps) {
            channelIdList.add(item.getChannelId());
        }
        List<NnChannel> channels = channelMngr.findByIds(channelIdList);
        if (channels == null || channels.size() == 0) {
            return new ArrayList<NnChannel>();
        }
        
        Map<Long, NnChannel> channelMap = new TreeMap<Long, NnChannel>();
        for (NnChannel channel : channels) {
            channelMap.put(channel.getId(), channel);
        }
        List<NnChannel> results = new ArrayList<NnChannel>();
        NnChannel result = null;
        for (SysTagMap item : sysTagMaps) {
            result = channelMap.get(item.getChannelId());
            if (result != null) {
                result.setTimeStart(item.getTimeStart());
                result.setTimeEnd(item.getTimeEnd());
                result.setSeq(item.getSeq());
                result.setAlwaysOnTop(item.isAlwaysOnTop());
                results.add(result);
            } else {
                // TODO : Channel not exist
            }
        }
        
        return results;
    }
    
    /** get Channels from Set ordered by UpdateTime, Channel with AlwaysOnTop set to True will put in the head of results,
     *  the Channels populate additional information (TimeStart, TimeEnd, Seq, AlwaysOnTop) retrieve from SysTagMap */
    public List<NnChannel> getChannelsOrderByUpdateTime(Long setId) {
        
        if (setId == null) {
            return new ArrayList<NnChannel>();
        }
        List<NnChannel> channels = getChannelsOrderBySeq(setId);
        if (channels == null) {
            return new ArrayList<NnChannel>();
        }
        if (channels.size() < 2) {
            return channels;
        }
        
        List<NnChannel> results = new ArrayList<NnChannel>();
        List<NnChannel> orderedChannels = new ArrayList<NnChannel>();
        for (NnChannel channel : channels) {
            if (channel.isAlwaysOnTop() == true) {
                results.add(channel);
            } else {
                orderedChannels.add(channel);
            }
        }
        
        Collections.sort(orderedChannels, channelMngr.getChannelComparator("default"));
        results.addAll(orderedChannels);
        
        return results;
    }
    
    /** check if input Channel Ids represent all Channels in the Set */
    public boolean isContainAllChannels(Long setId, List<Long> channelIds) {
        
        if (setId == null || channelIds == null) {
            return false;
        }
        
        // it must same as setChannels's result
        List<SysTagMap> setChannels = sysTagMapMngr.findBySysTagId(setId);
        if (setChannels == null) {
            if (channelIds.size() == 0) {
                return true;
            } else {
                return false;
            }
        }
        
        int index;
        for (SysTagMap channel : setChannels) {
            index = channelIds.indexOf(channel.getChannelId());
            if (index > -1) {
                // pass
            } else {
                // input missing this Channel ID 
                return false;
            }
        }
        
        if (setChannels.size() != channelIds.size()) {
            // input contain duplicate or other Channel Id
            return false;
        }
        
        return true;
    }
    
    /** service for ApiMso.msoSets
     *  @param msoId required, Mso's Id
     *  @param lang optional, filter for Set's lang */
    public List<Set> msoSets(Long msoId, String lang) {
        
        if (msoId == null) {
            return new ArrayList<Set>();
        }
        
        List<Set> results = null;
        if (lang != null) {
            results = findByMsoIdAndLang(msoId, lang);
        } else {
            results = findByMsoId(msoId);
        }
        
        if (results == null) {
            return new ArrayList<Set>();
        }
        return results;
    }
    
    /** service for ApiMso.set
     *  @param setId required, SysTag's Id with SysTag's type = Set */
    public Set set(Long setId) {
        
        if (setId == null) {
            return null;
        }
        
        return findById(setId);
    }
    
    /** service for ApiMso.setUpdate
     *  @param setId required, SysTag's Id with SysTag's type = Set
     *  @param name optional, Set's name save in SysTagDisplay's name
     *  @param seq optional, Set's seq save in SysTag's seq
     *  @param tag optional, Set's tag save in SysTagDisplay's popularTag
     *  @param sortingType optional, Set's sortingType save in SysTag's sorting */
    public Set setUpdate(Long setId, String name, Short seq, String tag, Short sortingType) {
        
        if (setId == null) {
            return null;
        }
        SysTag set = sysTagMngr.findById(setId);
        if (set == null) {
            return null;
        }
        SysTagDisplay setMeta = sysTagDisplayMngr.findBySysTagId(set.getId());
        if (setMeta == null) {
            log.warning("invalid structure : SysTag's Id=" + set.getId() + " exist but not found any of SysTagDisPlay");
            return null;
        }
        
        if (name != null) {
            setMeta.setName(name);
        }
        if (seq != null) {
            set.setSeq(seq);
        }
        if (tag != null) {
            setMeta.setPopularTag(tag);
        }
        if (sortingType != null) {
            set.setSorting(sortingType);
        }
        // automated update cntChannel
        List<SysTagMap> channels = sysTagMapMngr.findBySysTagId(set.getId());
        setMeta.setCntChannel(channels.size());
        
        if (seq != null || sortingType != null) {
            set = sysTagMngr.save(set);
        }
        setMeta = sysTagDisplayMngr.save(setMeta);
        
        return composeSet(set, setMeta);
    }
    
    /** service for ApiMso.setChannels
     *  @param setId required, SysTag's Id with SysTag's type = Set */
    public List<NnChannel> setChannels(Long setId) {
        
        if (setId == null) {
            return new ArrayList<NnChannel>();
        }
        
        SysTag set = sysTagMngr.findById(setId);
        if (set == null || set.getType() != SysTag.TYPE_SET) {
            return new ArrayList<NnChannel>();
        }
        
        List<NnChannel> results = null;
        if (set.getSorting() == SysTag.SORT_SEQ) {
            results = getChannelsOrderBySeq(set.getId());
        }
        if (set.getSorting() == SysTag.SORT_DATE) {
            results = getChannelsOrderByUpdateTime(set.getId());
        }
        
        if (results == null) {
            return new ArrayList<NnChannel>();
        }
        
        results = channelMngr.responseNormalization(results);
        if (results.size() > 0) { // dependence with front end use case
            channelMngr.populateMoreImageUrl(results.get(0));
        }
        return results;
    }
    
    /** service for ApiMso.setChannelAdd
     *  @param setId required, SysTag's Id with SysTag's type = Set
     *  @param channelId required, Channel's Id
     *  @param timeStart optional, set a period start that Channel appear in the Set
     *  @param timeEnd optional, set a period end that Channel appear in the Set
     *  @param alwaysOnTop optional, put this Channel in the head when Channels sorting by update time get from Set */
    public void setChannelAdd(Long setId, Long channelId, Short timeStart, Short timeEnd, Boolean alwaysOnTop) {
        
        if (setId == null || channelId == null) {
            return ;
        }
        
        // create if not exist
        SysTagMap sysTagMap = sysTagMapMngr.findBySysTagIdAndChannelId(setId, channelId);
        if (sysTagMap == null) {
            sysTagMap = new SysTagMap(setId, channelId);
            sysTagMap.setSeq((short) 0);
            sysTagMap.setTimeStart((short) 0);
            sysTagMap.setTimeEnd((short) 0);
            sysTagMap.setAlwaysOnTop(false);
        }
        
        if (timeStart != null) {
            sysTagMap.setTimeStart(timeStart);
        }
        if (timeEnd != null) {
            sysTagMap.setTimeEnd(timeEnd);
        }
        if (alwaysOnTop != null) {
            sysTagMap.setAlwaysOnTop(alwaysOnTop);
        }
        
        sysTagMapMngr.save(sysTagMap);
    }
    
    /** service for ApiMso.setChannelRemove
     *  @param setId required, SysTag's Id with SysTag's type = Set
     *  @param channelId required, Channel's Id */
    public void setChannelRemove(Long setId, Long channelId) {
        
        if (setId == null || channelId == null) {
            return ;
        }
        
        SysTagMap sysTagMap = sysTagMapMngr.findBySysTagIdAndChannelId(setId, channelId);
        if (sysTagMap == null) {
            // do nothing
        } else {
            sysTagMapMngr.delete(sysTagMap);
        }
    }
    
    /** service for ApiMso.setChannelsSorting
     *  @param setId required, SysTag's Id with SysTag's type = Set
     *  @param sortedChannels required, the Channel Ids from Set to be sorted */
    public void setChannelsSorting(Long setId, List<Long> sortedChannels) {
        
        if (setId == null || sortedChannels == null) {
            return ;
        }
        
        // it must same as setChannels's result, order by seq asc
        List<SysTagMap> setChannels = sysTagMapMngr.findBySysTagId(setId);
        
        List<Long> oldSequence = new ArrayList<Long>();
        List<Long> remainder = new ArrayList<Long>();
        for (SysTagMap channel : setChannels) {
            oldSequence.add(channel.getChannelId());
            remainder.add(channel.getChannelId());
        }
        
        List<SysTagMap> newSequence = new ArrayList<SysTagMap>();
        for (Long channelId : sortedChannels) {
            int index = oldSequence.indexOf(channelId);
            if (index > -1) {
                newSequence.add(setChannels.get(index));
                remainder.remove(channelId);
            }
        }
        
        // add remainder channels to the end of list
        for (Long channelId : remainder) {
            int index = oldSequence.indexOf(channelId);
            if (index > -1) {
                newSequence.add(setChannels.get(index));
            }
        }
        
        int seq = 1;
        for (SysTagMap channel : newSequence) {
            channel.setSeq((short) seq);
            seq++;
        }
        
        sysTagMapMngr.saveAll(newSequence);
    }

}