package com.nncloudtv.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.anyShort;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.logging.Logger;

import javax.servlet.http.HttpSession;

//import junit.framework.Assert; The old method (of Junit 3),
// see http://stackoverflow.com/questions/291003/differences-between-2-junit-assert-classes

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.nncloudtv.lib.CacheFactory;
import com.nncloudtv.model.Mso;
import com.nncloudtv.model.NnUser;
import com.nncloudtv.model.NnUserProfile;
import com.nncloudtv.web.api.ApiContext;
import com.nncloudtv.web.api.NnStatusCode;

@RunWith(MockitoJUnitRunner.class)
public class PlayerApiServiceTest {

    protected static final Logger log = Logger.getLogger(PlayerApiServiceTest.class.getName());
    
    private PlayerApiService service;
    
    private MockHttpServletRequest req;
    private MockHttpServletResponse resp;
    @Mock private NnUserManager mockUserMngr;
    @Mock private MsoManager mockMsoMngr;
    @Mock private NnChannelManager mockChMngr;
    @Mock private MsoConfigManager mockConfigMngr;
    @Mock private NnUserPrefManager mockPrefMngr;
    @Mock private NnUserProfileManager mockProfileMngr;

    @Before
    public void setUp() {
        
        CacheFactory.isEnabled = false;
        
        req = new MockHttpServletRequest();
        resp = new MockHttpServletResponse();
        
        req.addHeader(ApiContext.HEADER_USER_AGENT, MockHttpServletRequest.class.getName());
        HttpSession session = req.getSession();
        session.setMaxInactiveInterval(60);
        service = spy(new PlayerApiService(mockUserMngr, mockMsoMngr, mockChMngr,
                mockConfigMngr, mockPrefMngr, mockProfileMngr, null, null));
        System.out.println("@Before - setUp");
    }
    
    @After
    public void tearDown() {
        req = null;
        resp = null;
        mockUserMngr = null;
        mockMsoMngr = null;
        mockChMngr = null;
        mockConfigMngr = null;
        mockPrefMngr = null;
        mockProfileMngr = null;
        
        service = null;
    }
    
    @Test
    public void testBrandInfo() {
        
        // input arguments
        String os = null;
        
        // mock data
        String locale = "zh";
        Object brandInfo = "brandInfo";
        Object result = "result";
        
        // stubs
        when(service.findLocaleByHttpRequest((MockHttpServletRequest) anyObject())).thenReturn(locale);
        when(mockMsoMngr.getBrandInfo((MockHttpServletRequest) anyObject(), (Mso) anyObject(),
                anyString(), anyShort(), anyString(), anyLong(), anyString(), anyString())).thenReturn(brandInfo);
        when(service.assembleMsgs(anyInt(), anyObject())).thenReturn(result);
        
        // execute
        Object actual = service.brandInfo(os, req);
        
        // verify
        verify(service).findLocaleByHttpRequest(req);
        verify(mockMsoMngr).getBrandInfo((MockHttpServletRequest) anyObject(), (Mso) anyObject(),
                anyString(), anyShort(), anyString(), anyLong(), anyString(), anyString());
        verify(service).assembleMsgs(NnStatusCode.SUCCESS, brandInfo);
        assertEquals(result, actual);
    }
    
    @Test
    public void testSetProfile() {
        
        // input arguments
        final String userToken = "mock-user-token-xxoo";
        final String items = "name,phone";
        final String name = "MockUser";
        final String phone = "7777777";
        final String values = name + "," + phone;
        
        // mock data
        final Long msoId = (long) 1;
        Mso mso = new Mso("name", "intro", "contactEmail", Mso.TYPE_MSO);
        mso.setId(msoId);
        
        final Long userId = (long) 1;
        NnUser user = new NnUser("_mock_@9x9.tv", "_password_", NnUser.TYPE_USER);
        user.setId(userId);
        
        // stubs
        when(mockMsoMngr.getByNameFromCache(anyString())).thenReturn(mso);
        when(mockUserMngr.findByToken(anyString(), anyLong())).thenReturn(user);
        when(mockProfileMngr.findByUser((NnUser) anyObject())).thenReturn(null);
        when(mockProfileMngr.save((NnUser) anyObject(), (NnUserProfile) anyObject())).thenReturn(null);
        
        // execute
        service.prepService(req, resp);
        Object actual = service.setUserProfile(userToken, items, values, req);
        
        // verify
        verify(mockMsoMngr).getByNameFromCache(anyString());
        verify(mockUserMngr).findByToken(userToken, msoId);
        verify(mockProfileMngr).findByUser(user);
        
        ArgumentCaptor<NnUserProfile> arg = ArgumentCaptor.forClass(NnUserProfile.class);
        verify(mockProfileMngr).save(eq(user), arg.capture());
        NnUserProfile profile = arg.getValue();
        assertEquals(msoId, (Long) profile.getMsoId());
        assertEquals(userId, (Long) profile.getUserId());
        assertEquals(name, profile.getName());
        assertEquals(phone, profile.getPhoneNumber());
        
        assertNotNull(actual);
    }
    /*
    @Test
    public void testLogin() {
        String email = "a@a.com";
        String password = "123456";
        Object loginObj = service.login(email, password, req, resp);
        
        assertNotNull(loginObj);
    }
    
    @Test
    public void testQuickLogin() {
        String email = "a@a.com";
        String password = "123456";        
        Object userInfo = service.login(email, password, req, resp);
        System.out.println(userInfo);
        
        assertNotNull(userInfo);
    }
    */
}
