package com.nncloudtv.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyListOf;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang.SerializationUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.nncloudtv.model.Mso;
import com.nncloudtv.model.MsoConfig;
import com.nncloudtv.model.NnChannel;
import com.nncloudtv.model.SysTag;
import com.nncloudtv.model.SysTagDisplay;
import com.nncloudtv.model.SysTagMap;
import com.nncloudtv.web.json.cms.Category;
import com.nncloudtv.web.json.cms.Set;

/**
 * This is unit test for ApiMsoService's method, use Mockito mock dependence object.
 * Each test case naming begin with target method name, plus dash and a serial number. 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({MsoConfigManager.class, MsoManager.class})
public class ApiMsoServiceTest {
    
    protected static final Logger log = Logger.getLogger(ApiMsoServiceTest.class.getName());
    
    /** target class for testing */
    private ApiMsoService apiMsoService;
    
    @Mock private SetService setService;
    @Mock private SysTagManager sysTagMngr;
    @Mock private SysTagDisplayManager sysTagDisplayMngr;
    @Mock private SysTagMapManager sysTagMapMngr;
    @Mock private NnChannelManager channelMngr;
    @Mock private StoreService storeService;
    @Mock private StoreListingManager storeListingMngr;
    @Mock private MsoManager msoMngr;
    @Mock private CategoryService categoryService;
    @Mock private MsoConfigManager configMngr;
    
    @Before  
    public void setUp() {
        apiMsoService = new ApiMsoService(setService, sysTagMngr, sysTagDisplayMngr,
                sysTagMapMngr, channelMngr, storeService, storeListingMngr, msoMngr,
                categoryService, configMngr);
    }
    
    @After
    public void tearDown() {
        setService = null;
        sysTagMngr = null;
        sysTagDisplayMngr = null;
        sysTagMapMngr = null;
        channelMngr = null;
        storeService = null;
        storeListingMngr = null;
        msoMngr = null;
        categoryService = null;
        configMngr = null;
        
        apiMsoService = null;
    }
    
    /**
     * normal case and provide lang
     */
    @Test
    public void testMsoSets() {
        
        // input arguments
        final Long msoId = (long) 1;
        final String lang = "zh";
        
        // stubs
        when(setService.findByMsoIdAndLang((Long) anyLong(), anyString())).thenReturn(new ArrayList<Set>());
        
        // execute
        List<Set> actual = apiMsoService.msoSets(msoId, lang);
        
        // verify
        verify(setService).findByMsoIdAndLang(msoId, lang);
        assertEquals(new ArrayList<Set>(), actual);
    }
    
    /**
     * when find nothing (empty result), should obey contract
     */
    @Test
    public void testMsoSetsEmptyResult() {
        
        // input arguments
        final Long msoId = (long) 1;
        final String lang = "zh";
        
        // stubs
        when(setService.findByMsoIdAndLang((Long) anyLong(), anyString())).thenReturn(null); // empty result
        
        // execute
        List<Set> actual = apiMsoService.msoSets(msoId, lang);
        
        // verify
        verify(setService).findByMsoIdAndLang(msoId, lang);
        assertEquals(new ArrayList<Set>(), actual);
    }
    
    /**
     * normal case but not provide lang
     */
    @Test
    public void testMsoSetsNotProvideLang() {
        
        // input arguments
        final Long msoId = (long) 1;
        final String lang = null;
        
        // stubs
        when(setService.findByMsoId((Long) anyLong())).thenReturn(new ArrayList<Set>());
        
        // execute
        List<Set> actual = apiMsoService.msoSets(msoId, lang);
        
        // verify
        verify(setService).findByMsoId(msoId);
        assertEquals(new ArrayList<Set>(), actual);
    }
    
    /**
     * missing required arguments : msoId
     */
    @Test
    public void testMsoSetsMissingArgus() {
        
        // input arguments
        final Long msoId = null;
        final String lang = null;
        
        // execute
        List<Set> actual = apiMsoService.msoSets(msoId, lang);
        
        // verify
        assertEquals(new ArrayList<Set>(), actual);
    }
    
    /**
     * normal case
     */
    @Test
    public void testMsoSetCreate() {
        
        // input arguments
        final Long msoId = (long) 1;
        final short seq = 1;
        final String tag = "tag";
        final String name = "name";
        final short sortingType = SysTag.SORT_SEQ;
        
        // mock data
        Mso mso = new Mso("name", "intro", "contactEmail", Mso.TYPE_MSO);
        mso.setId(msoId);
        
        MsoConfig supportedRegion = new MsoConfig();
        supportedRegion.setMsoId(msoId);
        supportedRegion.setItem(MsoConfig.SUPPORTED_REGION);
        supportedRegion.setValue("zh 台灣");
        
        List<String> spheres = new ArrayList<String>();
        spheres.add("zh");
        
        Set set = new Set();
        set.setMsoId(msoId);
        set.setName(name);
        set.setSeq(seq);
        set.setTag(tag);
        set.setSortingType(sortingType);
        set.setLang("zh");
        
        // stubs
        when(msoMngr.findById((Long) anyLong())).thenReturn(mso);
        when(configMngr.findByMsoAndItem((Mso) anyObject(), anyString())).thenReturn(supportedRegion);
        
        PowerMockito.mockStatic(MsoConfigManager.class);
        when(MsoConfigManager.parseSupportedRegion(anyString())).thenReturn(spheres);
        
        when(setService.create((Set) anyObject())).thenReturn(set);
        
        // execute
        Set expected = (Set) SerializationUtils.clone(set);
        Set actual = apiMsoService.msoSetCreate(msoId, seq, tag, name, sortingType);
        
        // verify
        verify(msoMngr).findById(msoId);
        verify(configMngr).findByMsoAndItem(mso, MsoConfig.SUPPORTED_REGION);
        
        PowerMockito.verifyStatic();
        MsoConfigManager.parseSupportedRegion(supportedRegion.getValue());
        
        ArgumentCaptor<Set> set_arg = ArgumentCaptor.forClass(Set.class);
        verify(setService).create(set_arg.capture());
        assertEquals((Object) msoId, set_arg.getValue().getMsoId());
        assertEquals(name, set_arg.getValue().getName());
        assertEquals(seq, set_arg.getValue().getSeq());
        assertEquals(tag, set_arg.getValue().getTag());
        assertEquals(sortingType, set_arg.getValue().getSortingType());
        assertEquals("zh", set_arg.getValue().getLang());
        
        assertEquals(expected, actual);
    }
    
    /**
     * normal case
     */
    @Test
    public void testSet() {
        
        when(setService.findById(anyLong())).thenReturn(new Set());
        
        Set result = apiMsoService.set(anyLong());
        assertNotNull(result);
        
        verify(setService).findById(anyLong());
    }
    
    /**
     * empty result
     */
    @Test
    public void testSetEmptyResult() {
        
        when(setService.findById(anyLong())).thenReturn(null);
        
        Set result = apiMsoService.set(anyLong());
        assertNull(result);
        
        verify(setService).findById(anyLong());
    }
    
    /**
     * missing arguments : setId
     */
    @Test
    public void testSetMissingArgus() {
        
        Set result = apiMsoService.set(null);
        assertNull(result);
        
        verifyZeroInteractions(setService);
    }
    
    /**
     * normal case : given arguments and return wanted result
     */
    @Test
    public void testSetUpdate() {
        
        // input arguments
        final Long setId = (long) 1;
        final String name = "name";
        final short seq = 1;
        final String tag = "tag";
        final short sortingType = SysTag.SORT_SEQ;
        
        // mock data
        SysTag systag = new SysTag();
        systag.setId(setId);
        SysTagDisplay display = new SysTagDisplay();
        display.setSystagId(setId);
        Set set = new Set();
        set.setName(name);
        set.setSeq(seq);
        set.setTag(tag);
        set.setSortingType(sortingType);
        set.setChannelCnt(0);
        set.setId(setId);
        
        // stubs
        when(sysTagMngr.findById((Long) anyLong())).thenReturn(systag);
        when(sysTagDisplayMngr.findBySysTagId((Long) anyLong())).thenReturn(display);
        when(sysTagMapMngr.findBySysTagId((Long) anyLong())).thenReturn(new ArrayList<SysTagMap>());
        when(sysTagMngr.save((SysTag) anyObject())).thenReturn(systag);
        when(sysTagDisplayMngr.save((SysTagDisplay) anyObject())).thenReturn(display);
        when(setService.composeSet((SysTag) anyObject(), (SysTagDisplay) anyObject())).thenReturn(set);
        
        // execute
        Set expected = (Set) SerializationUtils.clone(set);
        Set actual = apiMsoService.setUpdate(setId, name, seq, tag, sortingType);
        
        // verify
        verify(sysTagMngr).findById(setId);
        verify(sysTagDisplayMngr).findBySysTagId(setId);
        verify(sysTagMapMngr).findBySysTagId(setId);
        
        ArgumentCaptor<SysTag> systag_arg = ArgumentCaptor.forClass(SysTag.class);
        verify(sysTagMngr).save(systag_arg.capture());
        assertEquals(seq, systag_arg.getValue().getSeq());
        assertEquals(sortingType, systag_arg.getValue().getSorting());
        
        ArgumentCaptor<SysTagDisplay> display_arg = ArgumentCaptor.forClass(SysTagDisplay.class);
        verify(sysTagDisplayMngr).save(display_arg.capture());
        assertEquals(name, display_arg.getValue().getName());
        assertEquals(tag, display_arg.getValue().getPopularTag());
        assertEquals(0, display_arg.getValue().getCntChannel());
        
        verify(setService).composeSet(systag, display);
        assertEquals(expected, actual);
    }
    
    /**
     * normal case
     */
    @Test
    public void testSetDelete() {
        
        // input arguments
        final Long setId = (long) 1;
        
        // execute
        apiMsoService.setDelete(setId);
        
        // verify
        verify(setService).delete(setId);
    }
    
    /**
     * missing arguments : setId
     */
    @Test
    public void testSetDeleteMissingArgus() {
        
        // input arguments
        final Long setId = null;
        
        // execute
        apiMsoService.setDelete(setId);
        
        // verify
        verifyZeroInteractions(setService);
    }
    
    /**
     * normal case : order by sequence
     */
    @Test
    public void testSetChannelsOrderBySeq() {
        
        // input arguments
        final Long setId = (long) 1;
        
        // mock data
        SysTag systag = new SysTag();
        systag.setId(setId);
        systag.setType(SysTag.TYPE_SET);
        systag.setSorting(SysTag.SORT_SEQ);
        List<NnChannel> channels = new ArrayList<NnChannel>();
        
        // stubs
        when(sysTagMngr.findById((Long) anyLong())).thenReturn(systag);
        when(setService.getChannelsOrderBySeq((Long) anyLong())).thenReturn(channels);
        when(channelMngr.responseNormalization(anyListOf(NnChannel.class))).thenReturn(channels);
        
        // execute
        List<NnChannel> actual = apiMsoService.setChannels(setId);
        
        // verify
        verify(sysTagMngr).findById(setId);
        verify(setService).getChannelsOrderBySeq(setId);
        verify(setService, never()).getChannelsOrderByUpdateTime((Long) anyLong());
        verify(channelMngr).responseNormalization(anyListOf(NnChannel.class));
        assertNotNull(actual);
    }
    
    /**
     * normal case : order by update time
     */
    @Test
    public void testSetChannelsOrderByUpdateTime() {
        
        // input arguments
        final Long setId = (long) 1;
        
        // mock data
        SysTag systag = new SysTag();
        systag.setId(setId);
        systag.setType(SysTag.TYPE_SET);
        systag.setSorting(SysTag.SORT_DATE);
        List<NnChannel> channels = new ArrayList<NnChannel>();
        
        // stubs
        when(sysTagMngr.findById((Long) anyLong())).thenReturn(systag);
        when(setService.getChannelsOrderByUpdateTime((Long) anyLong())).thenReturn(channels);
        when(channelMngr.responseNormalization(anyListOf(NnChannel.class))).thenReturn(channels);
        
        // execute
        List<NnChannel> actual = apiMsoService.setChannels(setId);
        
        // verify
        verify(sysTagMngr).findById(setId);
        verify(setService).getChannelsOrderByUpdateTime(setId);
        verify(setService, never()).getChannelsOrderBySeq((Long) anyLong());
        verify(channelMngr).responseNormalization(anyListOf(NnChannel.class));
        assertNotNull(actual);
    }
    
    /**
     * missing arguments : setId
     */
    @Test
    public void testSetChannelsMissingArgus() {
        
        // input arguments
        final Long setId = null;
        
        // execute
        List<NnChannel> actual = apiMsoService.setChannels(setId);
        
        // verify
        assertNotNull(actual);
    }
    
    /**
     * normal case
     */
    @Test
    public void testSetChannelAdd() {
        
        // input arguments
        final Long setId = (long) 1;
        final Long channelId = (long) 1;
        final Short timeStart = null;
        final Short timeEnd = null;
        final Boolean alwaysOnTop = null;
        final Boolean featured = null;
        
        // execute
        apiMsoService.setChannelAdd(setId, channelId, timeStart, timeEnd, alwaysOnTop, featured);
        
        // verify
        verify(setService).addChannelToSet(setId, channelId, timeStart, timeEnd, alwaysOnTop, featured);
    }
    
    /**
     * missing arguments : setId, channelId
     */
    @Test
    public void testSetChannelAddMissingArgus() {
        
        // input arguments
        final Long setId = null;
        final Long channelId = null;
        final Short timeStart = null;
        final Short timeEnd = null;
        final Boolean alwaysOnTop = null;
        final Boolean featured = null;
        
        // execute
        apiMsoService.setChannelAdd(setId, channelId, timeStart, timeEnd, alwaysOnTop, featured);
        
        // verify
        verifyZeroInteractions(setService);
    }
    
    /**
     * normal case
     */
    @Test
    public void testSetChannelRemove() {
        
        // input arguments
        final Long setId = (long) 1;
        final Long channelId = (long) 1;
        
        // mocks
        SysTagMap sysTagMap = new SysTagMap(setId, channelId);
        
        // stubs
        when(sysTagMapMngr.findBySysTagIdAndChannelId((Long) anyLong(), (Long) anyLong())).thenReturn(sysTagMap);
        
        // execute
        apiMsoService.setChannelRemove(setId, channelId);
        
        // verify
        verify(sysTagMapMngr).findBySysTagIdAndChannelId(setId, channelId);
        verify(sysTagMapMngr).delete(sysTagMap);
    }
    
    /**
     * missing arguments : setId, channelId
     */
    @Test
    public void testSetChannelRemoveMissingArgus() {
        
        // input arguments
        final Long setId = null;
        final Long channelId = null;
        
        // execute
        apiMsoService.setChannelRemove(setId, channelId);
        
        // verify
        verifyZeroInteractions(sysTagMapMngr);
    }
    
    /**
     * normal case
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testSetChannelsSorting() {
        
        // input arguments
        final Long setId = (long) 1;
        final List<Long> sortedChannels = new ArrayList<Long>();
        sortedChannels.add((long) 3);
        sortedChannels.add((long) 2);
        sortedChannels.add((long) 1);
        
        // mocks
        List<SysTagMap> setChannels = new ArrayList<SysTagMap>();
        for(int count = 1; count <= 3; count++) {
            SysTagMap sysTagMap = new SysTagMap(setId, count);
            sysTagMap.setSeq((short) count);
            setChannels.add(sysTagMap);
        }
        
        // stubs
        when(sysTagMapMngr.findBySysTagId((Long) anyLong())).thenReturn(setChannels);
        
        // execute
        apiMsoService.setChannelsSorting(setId, sortedChannels);
        
        // verify
        verify(sysTagMapMngr).findBySysTagId(setId);
        
        ArgumentCaptor<List<SysTagMap>> sysTagMaps_arg = ArgumentCaptor.forClass((Class) List.class);
        verify(sysTagMapMngr).saveAll(sysTagMaps_arg.capture());
        List<SysTagMap> sysTagMaps = sysTagMaps_arg.getValue();
        assertEquals(3, sysTagMaps.size());
        assertEquals(3, sysTagMaps.get(0).getChannelId());
        assertEquals(2, sysTagMaps.get(1).getChannelId());
        assertEquals(1, sysTagMaps.get(2).getChannelId());
    }
    
    /**
     * normal case : query are channels in store ?
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testStoreChannelsChannelsInStore() {
        
        // input arguments
        final Long msoId = (long) 1;
        final java.util.Set<Long> channelIds = new HashSet<Long>();
        channelIds.add(new Long(1));
        channelIds.add(new Long(2));
        channelIds.add(new Long(3));
        final Long categoryId = null;
        
        // mocks
        List<NnChannel> channels = new ArrayList<NnChannel>();
        
        // stubs
        when(channelMngr.findByIds(anyListOf(Long.class))).thenReturn(channels);
        when(storeService.checkChannelsInMsoStore(anyListOf(NnChannel.class), (Long) anyLong()))
        .thenReturn(new ArrayList<Long>());
        
        // execute
        List<Long> actual = apiMsoService.storeChannels(msoId, channelIds, categoryId);
        
        // verify
        ArgumentCaptor<List<Long>> arg = ArgumentCaptor.forClass((Class) List.class);
        verify(channelMngr).findByIds(arg.capture());
        List<Long> channelIds_arg = arg.getValue();
        assertEquals(3, channelIds_arg.size());
        assertEquals(1, (long) channelIds_arg.get(0));
        assertEquals(2, (long) channelIds_arg.get(1));
        assertEquals(3, (long) channelIds_arg.get(2));
        
        verify(storeService).checkChannelsInMsoStore(channels, msoId);
        assertNotNull(actual);
    }
    
    /**
     * normal case : list channels in category
     */
    @Test
    public void testStoreChannelsChannelsInCategory() {
        
        // input arguments
        final Long msoId = (long) 1;
        final java.util.Set<Long> channelIds = null;
        final Long categoryId = (long) 1;
        
        // stubs
        when(storeService.getChannelIdsFromMsoStoreCategory((Long) anyLong(), (Long) anyLong()))
        .thenReturn(new ArrayList<Long>());
        
        // execute
        List<Long> actual = apiMsoService.storeChannels(msoId, channelIds, categoryId);
        
        // verify
        verifyZeroInteractions(channelMngr);
        verify(storeService).getChannelIdsFromMsoStoreCategory(categoryId, msoId);
        assertNotNull(actual);
    }
    
    /**
     * normal case : list all channels in store
     */
    @Test
    public void testStoreChannelsAllChannelsInStore() {
        
        // input arguments
        final Long msoId = (long) 1;
        final java.util.Set<Long> channelIds = null;
        final Long categoryId = null;
        
        // stubs
        when(storeService.getChannelIdsFromMsoStore((Long) anyLong())).thenReturn(new ArrayList<Long>());
        
        // execute
        List<Long> actual = apiMsoService.storeChannels(msoId, channelIds, categoryId);
        
        // verify
        verifyZeroInteractions(channelMngr);
        verify(storeService).getChannelIdsFromMsoStore(msoId);
        assertNotNull(actual);
    }
    
    /**
     * missing arguments : msoId
     */
    @Test
    public void testStoreChannelsMissingArgus() {
        
        // input arguments
        final Long msoId = null;
        final java.util.Set<Long> channelIds = null;
        final Long categoryId = null;
        
        // execute
        List<Long> actual = apiMsoService.storeChannels(msoId, channelIds, categoryId);
        
        // verify
        verifyZeroInteractions(channelMngr);
        verifyZeroInteractions(storeService);
        assertNotNull(actual);
    }
    
    /**
     * normal case
     */
    @Test
    public void testStoreChannelRemove() {
        
        // input arguments
        final Long msoId = (long) 1;
        final List<Long> channelIds = new ArrayList<Long>();
        channelIds.add(new Long(1));
        
        // execute
        apiMsoService.storeChannelRemove(msoId, channelIds);
        
        // verify
        verify(storeListingMngr).addChannelsToBlackList(channelIds, msoId);
    }
    
    /**
     * missing arguments : msoId, channelIds
     */
    @Test
    public void testStoreChannelRemoveMissingArgus() {
        
        // input arguments
        final Long msoId = null;
        final List<Long> channelIds = null;
        
        // execute
        apiMsoService.storeChannelRemove(msoId, channelIds);
        
        // verify
        verifyZeroInteractions(storeListingMngr);
    }
    
    /**
     * normal case
     */
    @Test
    public void testStoreChannelAdd() {
        
        // input arguments
        final Long msoId = (long) 1;
        final List<Long> channelIds = new ArrayList<Long>();
        channelIds.add(new Long(1));
        
        // execute
        apiMsoService.storeChannelAdd(msoId, channelIds);
        
        // verify
        verify(storeListingMngr).removeChannelsFromBlackList(channelIds, msoId);
    }
    
    /**
     * missing arguments : msoId, channelIds
     */
    @Test
    public void testStoreChannelAddMissingArgus() {
        
        // input arguments
        final Long msoId = null;
        final List<Long> channelIds = null;
        
        // execute
        apiMsoService.storeChannelAdd(msoId, channelIds);
        
        // verify
        verifyZeroInteractions(storeListingMngr);
    }
    
    /**
     * normal case
     */
    @Test
    public void testMso() {
        
        // input argument
        final Long msoId = (long) 1;
        
        // mocks
        Mso mso = new Mso("testName", "testIntro", "testEmail", Mso.TYPE_MSO);
        mso.setId(msoId);
        MsoConfig maxSets = new MsoConfig(msoId, MsoConfig.MAX_SETS, String.valueOf(MsoConfig.MAXSETS_DEFAULT + 1));
        MsoConfig maxChPerSet = new MsoConfig(msoId, MsoConfig.MAX_CH_PER_SET,
                String.valueOf(MsoConfig.MAXCHPERSET_DEFAULT + 1));
        
        // stubs
        when(msoMngr.findById((Long) anyLong(), anyBoolean())).thenReturn(mso);
        when(configMngr.findByMsoAndItem(mso, MsoConfig.MAX_SETS)).thenReturn(maxSets);
        when(configMngr.findByMsoAndItem(mso, MsoConfig.MAX_CH_PER_SET)).thenReturn(maxChPerSet);
        
        PowerMockito.mockStatic(MsoManager.class);
        
        // execute
        Mso actual = apiMsoService.mso(msoId);
        
        // verify
        verify(msoMngr).findById(msoId, true);
        verify(configMngr).findByMsoAndItem(mso, MsoConfig.MAX_SETS);
        verify(configMngr).findByMsoAndItem(mso, MsoConfig.MAX_CH_PER_SET);
        
        PowerMockito.verifyStatic();
        MsoManager.normalize(mso);
        
        assertEquals(MsoConfig.MAXSETS_DEFAULT + 1, actual.getMaxSets());
        assertEquals(MsoConfig.MAXCHPERSET_DEFAULT + 1, actual.getMaxChPerSet());
    }
    
    /**
     * normal case
     */
    @Test
    public void testMsoUpdate() {
        
        // input argument
        final Long msoId = (long) 1;
        final String title = "modifyTitle";
        final String logoUrl = "modifyLogo";
        
        // mocks
        Mso mso = new Mso("testName", "testIntro", "testEmail", Mso.TYPE_MSO);
        mso.setId(msoId);
        
        // stubs
        when(msoMngr.findById((Long) anyLong())).thenReturn(mso);
        when(msoMngr.save((Mso) anyObject())).thenReturn(mso);
        
        PowerMockito.mockStatic(MsoManager.class);
        
        // execute
        Mso actual = apiMsoService.msoUpdate(msoId, title, logoUrl);
        
        // verify
        verify(msoMngr).findById(msoId);
        verify(msoMngr).save(mso);
        
        PowerMockito.verifyStatic();
        MsoManager.normalize(mso);
        
        assertEquals(title, actual.getTitle());
        assertEquals(logoUrl, actual.getLogoUrl());
    }
    
    /**
     * normal case
     */
    @Test
    public void testMsoCategories() {
        
        // input argument
        final Long msoId = (long) 1;
        
        // mocks
        List<Category> categories = new ArrayList<Category>();
        
        // stubs
        when(categoryService.findByMsoId((Long) anyLong())).thenReturn(categories);
        
        // execute
        List<Category> actual = apiMsoService.msoCategories(msoId);
        
        // verify
        verify(categoryService).findByMsoId(msoId);
        assertNotNull(actual);
    }
    
    /**
     * missing arguments : msoId
     */
    @Test
    public void testMsoCategoriesMissingArgus() {
        
        // input argument
        final Long msoId = null;
        
        // execute
        List<Category> actual = apiMsoService.msoCategories(msoId);
        
        // verify
        verifyZeroInteractions(categoryService);
        assertNotNull(actual);
    }
    
    /**
     * normal case
     */
    @Test
    public void testMsoCategoryCreate() {
        
        // input argument
        final Long msoId = (long) 1;
        final Short seq = 1;
        final String zhName = "中文名";
        final String enName ="english name";
        
        // mock
        Category category = new Category();
        
        // stubs
        when(categoryService.create((Category) anyObject())).thenReturn(category);
        
        // execute
        Category actual = apiMsoService.msoCategoryCreate(msoId, seq, zhName, enName);
        
        // verify
        ArgumentCaptor<Category> arg = ArgumentCaptor.forClass(Category.class);
        verify(categoryService).create(arg.capture());
        Category category_arg = arg.getValue();
        assertEquals(msoId, (Long) category_arg.getMsoId());
        assertEquals(seq, (Short) category_arg.getSeq());
        assertEquals(zhName, category_arg.getZhName());
        assertEquals(enName, category_arg.getEnName());
        
        assertEquals(category, actual);
    }
    
    /**
     * normal case
     */
    @Test
    public void testCategory() {
        
        // input argument
        final Long categoryId = (long) 1;
        
        // mock
        Category category = new Category();
        
        // stubs
        when(categoryService.findById((Long) anyLong())).thenReturn(category);
        
        // execute
        Category actual = apiMsoService.category(categoryId);
        
        // verify
        verify(categoryService).findById(categoryId);
        assertEquals(category, actual);
    }
    
    /**
     * missing arguments : categoryId
     */
    @Test
    public void testCategoryMissingArgus() {
        
        // input argument
        final Long categoryId = null;
        
        // execute
        Category actual = apiMsoService.category(categoryId);
        
        // verify
        verifyZeroInteractions(categoryService);
        assertNull(actual);
    }
    
    /**
     * normal case
     */
    @Test
    public void testCategoryUpdate() {
        
        // input argument
        final Long categoryId = (long) 1;
        final Short seq = 1;
        final String zhName = "修改的中文名";
        final String enName = "modified english name";
        
        // mocks
        Category category = new Category();
        category.setId(categoryId);
        
        // stubs
        when(categoryService.findById((Long) anyLong())).thenReturn(category);
        when(categoryService.getCntChannel((Long) anyLong())).thenReturn(3);
        when(categoryService.save((Category) anyObject())).thenReturn(category);
        
        // execute
        Category actual = apiMsoService.categoryUpdate(categoryId, seq, zhName, enName);
        
        // verify
        verify(categoryService).findById(categoryId);
        verify(categoryService).getCntChannel(categoryId);
        
        ArgumentCaptor<Category> arg = ArgumentCaptor.forClass(Category.class);
        verify(categoryService).save(arg.capture());
        Category category_arg = arg.getValue();
        assertEquals(seq, (Short) category_arg.getSeq());
        assertEquals(zhName, category_arg.getZhName());
        assertEquals(enName, category_arg.getEnName());
        assertEquals(3, category_arg.getCntChannel());
        
        assertEquals(category, actual);
    }
    
    /**
     * normal case
     */
    @Test
    public void testCategoryDelete() {
        
        // input argument
        final Long categoryId = (long) 1;
        
        // execute
        apiMsoService.categoryDelete(categoryId);
        
        // verify
        verify(categoryService).delete(categoryId);
    }
    
    /**
     * missing arguments : categoryId
     */
    @Test
    public void testCategoryDeleteMissingArgus() {
        
        // input argument
        final Long categoryId = null;
        
        // execute
        apiMsoService.categoryDelete(categoryId);
        
        // verify
        verifyZeroInteractions(categoryService);
    }
    
    /**
     * normal case
     */
    @Test
    public void testCategoryChannels() {
        
        // input argument
        final Long categoryId = (long) 1;
        
        // mocks
        List<NnChannel> channels = new ArrayList<NnChannel>();
        
        // stubs
        when(categoryService.getChannelsOrderByUpdateTime((Long) anyLong())).thenReturn(channels);
        when(channelMngr.responseNormalization(anyListOf(NnChannel.class))).thenReturn(channels);
        
        // execute
        List<NnChannel> actual = apiMsoService.categoryChannels(categoryId);
        
        // verify
        verify(categoryService).getChannelsOrderByUpdateTime(categoryId);
        verify(channelMngr).responseNormalization(channels);
        assertEquals(channels, actual);
    }
    
    /**
     * missing arguments : categoryId
     */
    @Test
    public void testCategoryChannelsMissingArgus() {
        
        // input argument
        final Long categoryId = null;
        
        // execute
        List<NnChannel> actual = apiMsoService.categoryChannels(categoryId);
        
        // verify
        assertNotNull(actual);
    }
    
    /**
     * normal case : add channels
     */
    @Test
    public void testCategoryChannelAddAddChannels() {
        
        // input argument
        final Category category = new Category();
        category.setId(1);
        category.setMsoId(1);
        
        final List<Long> channelIds = new ArrayList<Long>();
        channelIds.add((long) 1);
        channelIds.add((long) 2);
        channelIds.add((long) 3);
        
        final Long channelId = null;
        final Short seq = null;
        final Boolean alwaysOnTop = null;
        
        // mocks
        List<NnChannel> channels = new ArrayList<NnChannel>();
        List<Long> verifiedChannelIds = new ArrayList<Long>();
        
        // stubs
        when(channelMngr.findByIds(anyListOf(Long.class))).thenReturn(channels);
        when(msoMngr.getPlayableChannels(anyListOf(NnChannel.class), (Long) anyLong())).thenReturn(verifiedChannelIds);
        
        // execute
        apiMsoService.categoryChannelAdd(category, channelIds, channelId, seq, alwaysOnTop);
        
        // verify
        verify(channelMngr).findByIds(channelIds);
        verify(msoMngr).getPlayableChannels(channels, category.getMsoId());
        verify(categoryService).addChannelsToCategory(category.getId(), verifiedChannelIds);
    }
    
    /**
     * normal case : add channel with settings
     */
    @Test
    public void testCategoryChannelAddAddChannelWithSettings() {
        
        // input argument
        final Category category = new Category();
        category.setId(1);
        category.setMsoId(1);
        
        final List<Long> channelIds = null;
        
        final Long channelId = (long) 1;
        final Short seq = 1;
        final Boolean alwaysOnTop = true;
        
        // mocks
        NnChannel channel = new NnChannel("name", "intro", "imageUrl");
        
        // stubs
        when(channelMngr.findById((Long) anyLong())).thenReturn(channel);
        when(msoMngr.isPlayableChannel((NnChannel) anyObject(), (Long) anyLong())).thenReturn(true);
        
        // execute
        apiMsoService.categoryChannelAdd(category, channelIds, channelId, seq, alwaysOnTop);
        
        // verify
        verify(channelMngr).findById(channelId);
        verify(msoMngr).isPlayableChannel(channel, category.getMsoId());
        verify(categoryService).addChannelToCategory(category.getId(), channelId, seq, alwaysOnTop);
    }
    
    /**
     * normal case
     */
    @Test
    public void testCategoryChannelRemove() {
        
        // input argument
        final Long categoryId = (long) 1;
        final List<Long> channelIds = new ArrayList<Long>();
        channelIds.add((long) 1);
        channelIds.add((long) 2);
        channelIds.add((long) 3);
        
        // execute
        apiMsoService.categoryChannelRemove(categoryId, channelIds);
        
        // verify
        verify(categoryService).removeChannelsFromCategory(categoryId, channelIds);
    }
    
    /**
     * missing arguments : categoryId, channelIds
     */
    @Test
    public void testCategoryChannelRemoveMissingArgus() {
        
        // input argument
        final Long categoryId = null;
        final List<Long> channelIds = null;
        
        // execute
        apiMsoService.categoryChannelRemove(categoryId, channelIds);
        
        // verify
        verifyZeroInteractions(categoryService);
    }
    
    /**
     * normal case
     */
    @Test
    public void testMsoSystemCategoryLocks() {
        
        // input argument
        final Long msoId = (long) 1;
        
        // mocks
        List<String> locks = new ArrayList<String>();
        
        // stubs
        when(storeService.getStoreCategoryLocks((Long) anyLong())).thenReturn(locks);
        
        // execute
        List<String> actual = apiMsoService.msoSystemCategoryLocks(msoId);
        
        // verify
        verify(storeService).getStoreCategoryLocks(msoId);
        assertEquals(locks, actual);
    }
    
    /**
     * missing arguments : msoId
     */
    @Test
    public void testMsoSystemCategoryLocksMissingArgus() {
        
        // input argument
        final Long msoId = null;
        
        // execute
        List<String> actual = apiMsoService.msoSystemCategoryLocks(msoId);
        
        // verify
        verifyZeroInteractions(storeService);
        assertNotNull(actual);
    }
    
    /**
     * normal case
     */
    @Test
    public void testMsoSystemCategoryLocksUpdate() {
        
        // input argument
        final Long msoId = (long) 1;
        final List<String> systemCategoryLocks = new ArrayList<String>();
        systemCategoryLocks.add("1");
        systemCategoryLocks.add("2");
        systemCategoryLocks.add("3");
        
        // mocks
        List<String> locks = new ArrayList<String>();
        
        // stubs
        when(storeService.setStoreCategoryLocks((Long) anyLong(), anyListOf(String.class))).thenReturn(locks);
        
        // execute
        List<String> actual = apiMsoService.msoSystemCategoryLocksUpdate(msoId, systemCategoryLocks);
        
        // verify
        verify(storeService).setStoreCategoryLocks(msoId, systemCategoryLocks);
        assertEquals(locks, actual);
    }
    
    /**
     * missing arguments : msoId, systemCategoryLocks
     */
    @Test
    public void testMsoSystemCategoryLocksUpdateMissingArgus() {
        
        // input argument
        final Long msoId = null;
        final List<String> systemCategoryLocks = null;
        
        // execute
        List<String> actual = apiMsoService.msoSystemCategoryLocksUpdate(msoId, systemCategoryLocks);
        
        // verify
        verifyZeroInteractions(storeService);
        assertNotNull(actual);
    }

}
