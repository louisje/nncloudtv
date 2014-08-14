package com.nncloudtv.web.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.GetFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.nncloudtv.dao.MsoConfigDao;
import com.nncloudtv.dao.MsoDao;
import com.nncloudtv.lib.CacheFactory;
import com.nncloudtv.lib.FacebookLib;
import com.nncloudtv.model.Mso;
import com.nncloudtv.service.MsoConfigManager;
import com.nncloudtv.service.MsoManager;
import com.nncloudtv.service.PlayerApiService;
import com.nncloudtv.wrapper.NNFWrapper;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MsoManager.class,FacebookLib.class,CacheFactory.class,PlayerApiController.class})
public class PlayerApiControllerTest {
    
    protected static final Logger log = Logger.getLogger(PlayerApiControllerTest.class.getName());
    
    private PlayerApiController playerAPI;
    
    private MockHttpServletRequest req;
    private MockHttpServletResponse resp;
    
    @Before
    public void setUp() {
        req = new MockHttpServletRequest();
        resp = new MockHttpServletResponse(); 
        playerAPI = new PlayerApiController();
    }
    
    @After
    public void tearDown() {
        
        req = null;
        resp = null;
        playerAPI = null;
        
        NNFWrapper.empty();
    }
    
    private void setUpMemCacheMock(MemcachedClient cache) {
        
        CacheFactory.isEnabled = true;
        CacheFactory.isRunning = true;
        
        PowerMockito.spy(CacheFactory.class);
        try {
            PowerMockito.doReturn(cache).when(CacheFactory.class, "getSharedClient");
            PowerMockito.doReturn(cache).when(CacheFactory.class, "getClient");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // default return null for any kind of key
        GetFuture<Object> future = Mockito.mock(GetFuture.class);
        when(cache.asyncGet(anyString())).thenReturn(future);
        try {
            when(future.get(anyInt(), (TimeUnit) anyObject())).thenReturn(null);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
    
    private void recordMemoryCacheGet(MemcachedClient cache, String key, Object returnObj) {
        
        GetFuture<Object> future = Mockito.mock(GetFuture.class);
        when(cache.asyncGet(key)).thenReturn(future);
        try {
            when(future.get(anyInt(), (TimeUnit) anyObject())).thenReturn(returnObj);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
    
    @Test
    public void testBrandInfoNotExistMso() {
        
        // input
        String brandName = "notExist";
        String os = "web";
        String version = "40";
        req.setParameter("v", version);
        req.setParameter("format", "text");
        req.setParameter("lang", "zh");
        req.setParameter("os", os);
        req.setParameter("mso", brandName);
        
        // mock object
        MemcachedClient cache = Mockito.mock(MemcachedClient.class);
        MsoDao msoDao = Mockito.mock(MsoDao.class);
        NNFWrapper.setMsoDao(msoDao);
        MsoConfigDao configDao = Mockito.mock(MsoConfigDao.class); // can't inject
        MsoConfigManager configMngr = Mockito.mock(MsoConfigManager.class); // so mock MsoConfigManager
        NNFWrapper.setConfigMngr(configMngr);
        
        PlayerApiService playerApiService = PowerMockito.spy(new PlayerApiService());
        
        // mock data
        int id = 1;
        String name = Mso.NAME_9X9;
        String title = "title";
        String logoUrl = "logoUrl";
        String jingleUrl = "jingleUrl";
        String preferredLangCode = "preferredLangCode";
        
        Mso mso = new Mso(name, "intro", "email", Mso.TYPE_NN);
        mso.setId(id);
        mso.setTitle(title);
        mso.setLogoUrl(logoUrl);
        mso.setJingleUrl(jingleUrl);
        mso.setLang(preferredLangCode);
        
        String brandInfo = "";
        brandInfo += PlayerApiService.assembleKeyValue("key", String.valueOf(mso.getId()));
        brandInfo += PlayerApiService.assembleKeyValue("name", mso.getName());
        
        // stubs
        // only mso=9x9 available from cache and database
        setUpMemCacheMock(cache);
        String cacheKey = "mso(" + Mso.NAME_9X9 + ")";
        recordMemoryCacheGet(cache, cacheKey, mso);
        when(msoDao.findByName(Mso.NAME_9X9)).thenReturn(mso);
        
        // inject playerApiService in local new
        try {
            PowerMockito.whenNew(PlayerApiService.class).withNoArguments().thenReturn(playerApiService);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // brandInfo from cache
        cacheKey = CacheFactory.getBrandInfoKey(mso, os, PlayerApiService.FORMAT_PLAIN);
        recordMemoryCacheGet(cache, cacheKey, brandInfo);
        
        // execute
        Object actual = playerAPI.brandInfo(brandName, os, version, null, req, resp);
        
        // verify
        // actual always return null, need another way to verify result.
        ArgumentCaptor<Object> captureActual = ArgumentCaptor.forClass(Object.class);
        verify(playerApiService).response(captureActual.capture());
        String actual2 = (String) captureActual.getValue();
        assertTrue("Not exist mso should return as mso=9x9 brand info.", actual2.contains(brandInfo));
    }
    
    @Test
    public void testFbLogin() {
        
        CacheFactory.isEnabled = false;
        MsoManager mockMsoMngr = Mockito.mock(MsoManager.class);
        NNFWrapper.setMsoMngr(mockMsoMngr);
        
        // input arguments
        final String referrer = "http://www.mock.com/signin";
        req.setRequestURI("/playerAPI/fbLogin");
        req.addHeader(ApiContext.HEADER_REFERRER, referrer);
        
        // mock data
        final Long msoId = (long) 1;
        Mso mso = new Mso("9x9", "intro", "contactEmail", Mso.TYPE_NN);
        mso.setId(msoId);
        
        final String dialogOAuthPath = "dialogOAuthPath";
        
        // stubs
        when(mockMsoMngr.getByNameFromCache(anyString())).thenReturn(mso);
        
        PowerMockito.mockStatic(MsoManager.class);
        when(MsoManager.isNNMso((Mso) anyObject())).thenReturn(true);
        
        when(mockMsoMngr.findOneByName(anyString())).thenReturn(mso);
        
        PowerMockito.mockStatic(FacebookLib.class);
        when(FacebookLib.getDialogOAuthPath(anyString(), anyString(), (Mso) anyObject())).thenReturn(dialogOAuthPath);
        
        // execute
        String result = playerAPI.fbLogin(req);
        
        // verify
        verify(mockMsoMngr).getByNameFromCache(anyString());
        
        PowerMockito.verifyStatic();
        MsoManager.isNNMso(mso);
        
        verify(mockMsoMngr).findOneByName(null);
        
        final String protocol = "http://";
        final String domain = "www.localhost"; // if wrong here, look ApiContext implement
        final String path = "/fb/login";
        PowerMockito.verifyStatic();
        FacebookLib.getDialogOAuthPath(referrer, protocol + domain + path, mso);
        
        assertTrue(
                "The url redirection string slould start with 'redirect:'",
                result.matches("^redirect:.*$"));
        
        assertEquals("redirect:" + dialogOAuthPath, result);
    }
    
}