package com.nncloudtv.service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.nncloudtv.dao.NnUserProfileDao;
import com.nncloudtv.lib.NNF;
import com.nncloudtv.model.NnUser;
import com.nncloudtv.model.NnUserProfile;
import com.nncloudtv.web.api.ApiContext;
import com.nncloudtv.web.json.player.PlayerUserProfile;

@Service
public class NnUserProfileManager {

    protected static final Logger log = Logger.getLogger(NnUserProfileManager.class.getName());
    
    private NnUserProfileDao dao = NNF.getProfileDao();
    
    public List<NnUserProfile> findAllByUser(NnUser user) {
        
        if (user == null) { return null; }
        
        return dao.findByUserId(user.getId(), user.getShard());
    }
    
    public NnUserProfile findByUser(NnUser user) {
        
        return dao.findByUser(user);
    }
    
    public static Comparator<NnUserProfile> getComparator() {
        
        // priv from high to low
        return new Comparator<NnUserProfile>() {
            
            public int compare(NnUserProfile profile1, NnUserProfile profile2) {
                
                String priv1 = profile1.getPriv();
                String priv2 = profile2.getPriv();
                
                if (priv1 == null && priv2 == null) {
                    
                    return 0;
                    
                } else if (priv1 == null) {
                    
                    return 1;
                    
                } else if (priv2 == null) {
                    
                    return -1;
                    
                } else {
                    
                    int len = (priv1.length() > priv2.length()) ? priv1.length() : priv2.length();
                    char[] arr1 = priv1.toCharArray();
                    char[] arr2 = priv2.toCharArray();
                    for (int i = 0; i < len; i++) {
                        
                        if (arr1.length <= i) {
                            
                            return 1;
                            
                        } else if (arr2.length <= i) {
                            
                            return -1;
                            
                        } else {
                            
                            if (arr1[i] < arr2[i]) {
                                
                                return 1;
                                
                            } else if (arr1[i] < arr2[i]) {
                                
                                return -1;
                            }
                        }
                    }
                }
                // finally compare msoId
                return (int) (profile1.getMsoId() - profile2.getMsoId());
            }
        };
    }
    
    public NnUserProfile save(NnUser user, NnUserProfile profile) {
        if (profile == null)
            return null;
        return dao.save(user, profile);
    }
    
    public Set<NnUserProfile> search(String keyword, int start, int limit, short shard) {
        
        return dao.search(keyword, start, limit, shard);
    }
    
    /** pick up highest priv profile */
    public NnUserProfile pickupBestProfile(NnUser user) {
        
        if (user == null) { return null; }
        
        List<NnUserProfile> profiles = findAllByUser(user);
        
        if (profiles.isEmpty()) {
            
            return null;
        }
        Collections.sort(profiles, NnUserProfileManager.getComparator());
        
        return profiles.get(0);
    }
    
    public Object getPlayerProfile(NnUser user, short format) {
        
        NnUserProfile profile = user.getProfile();
        String name = profile.getName();
        String email = user.getUserEmail();
        String intro = profile.getIntro();
        String imageUrl = profile.getImageUrl();
        String gender = "";
        if (profile.getGender() != 2)
        	gender = String.valueOf(profile.getGender());
        String year = String.valueOf(profile.getDob());
        String sphere = profile.getSphere();
        String uiLang = profile.getLang();
        String phone = profile.getPhoneNumber();
        if (format == ApiContext.FORMAT_PLAIN) {
            String[] result = { "" };
            result[0] += PlayerApiService.assembleKeyValue("name", name);
            result[0] += PlayerApiService.assembleKeyValue("email", email);
            result[0] += PlayerApiService.assembleKeyValue("description", intro);
            result[0] += PlayerApiService.assembleKeyValue("image", imageUrl);
            result[0] += PlayerApiService.assembleKeyValue("gender", gender);
            result[0] += PlayerApiService.assembleKeyValue("year", year);
            result[0] += PlayerApiService.assembleKeyValue("sphere", sphere);
            result[0] += PlayerApiService.assembleKeyValue("ui-lang", uiLang);
            result[0] += PlayerApiService.assembleKeyValue("phone", phone);
            return result;
        } else {
            PlayerUserProfile json = new PlayerUserProfile();
            json.setName(name);
            json.setEmail(email);
            json.setDescription(intro);
            json.setImage(imageUrl);
            json.setGender(gender);
            json.setYear(year);
            json.setSphere(sphere);
            json.setUiLang(uiLang);
            json.setPhone(phone);
            return json;
        }
    }
    
    public static boolean checkPriv(NnUser user, String required) {
        
        if (required == null || required.matches("[01]+") == false) return false;
        
        NnUserProfile profile = user.getProfile();
        if (profile == null) {
            profile = NNF.getProfileMngr().findByUser(user);
            if (profile == null) {
                profile = new NnUserProfile();
                profile.setPriv(NnUserProfile.PRIV_CMS);
            }
        }
        
        String priv = profile.getPriv();
        if (priv == null)
            priv = NnUserProfile.PRIV_CMS;
        int len = priv.length();
        for (int i = 0; i < required.length(); i++) {
            if (required.charAt(i) == '1' &&
                    (len <= i || priv.charAt(i) != '1')) {
                return false;
            }
        }
        return true;
    }
}
