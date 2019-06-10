package io.kurumi.ntt.fragment.twitter.archive;


import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import io.kurumi.ntt.db.Data;
import io.kurumi.ntt.fragment.twitter.track.TrackTask;
import io.kurumi.ntt.utils.Html;
import twitter4j.User;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.not;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;
import static java.util.Arrays.asList;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class UserArchive {
    
    public static Data<UserArchive> data = new Data<UserArchive>(UserArchive.class);

    public static UserArchive get(Long id) { return data.getById(id); }
    
    public static UserArchive get(String screenName) {
		
		return data.collection.find(regex("screenName",ReUtil.escape(screenName),"i")).first();
		
	}
    
    public static boolean contains(Long id) { return data.containsId(id); }
    
    public static boolean contains(String screenName) {
		
		return data.collection.count(regex("user",ReUtil.escape(screenName),"i")) > 0;
		
	}
    
    public static UserArchive save(User user) {

        if (user == null) return null;
        
        UserArchive archive;

        if (data.containsId(user.getId())) {

            archive = data.getById(user.getId());
            
            if (archive.read(user)) data.setById(archive.id,archive);

        } else {

            archive = new UserArchive();

            archive.isDisappeared = false;

            archive.id = user.getId();
            
            archive.read(user);
            
            data.setById(user.getId(),archive);

        }

        
        return archive;

    }

    public static void saveDisappeared(Long da) {

        UserArchive user = data.getById(da);

        if (user != null) {

            user.isDisappeared = true;

            data.setById(da,user);

        }

    }
    
    
    public Long id;
    public Long createdAt;

    public String name;
    public String screenName;
    public String bio;
    public String photoUrl;
	
	public String bannerUrl;
	public String url;
	
    public Boolean isProtected;

    public Boolean isDisappeared;
    
    private transient String oldScreename;
    public transient String oldPhotoUrl;
    public transient String oldBannerUrl;
    
    public String oldScreenName() {
        
        return oldScreename == null ? screenName : oldScreename;
        
    }
    
    public boolean read(User user) {

        if (user == null && !isDisappeared) {

            isDisappeared = true;

            TrackTask.onUserChange(this,"用户被冻结或已停用 :)");

			return true;
			
        }
        
        if (user == null && isDisappeared) {
            
            return false;
            
        }

        boolean change = false;
        StringBuilder str = new StringBuilder();
        String split = "\n--------------------------------\n";

        String nameL = name;

        if (isDisappeared)  {

            isDisappeared = false;

            str.append(split).append("用户被取消了冻结/重新启用 :)");

            change = true;

        }

        if (!(name = user.getName()).equals(nameL)) {

            str.append(split).append("名称更改 : ").append(nameL).append(" ------> ").append(name);

            change = true;

        }

        String screenNameL = screenName;

        if (!(screenName = user.getScreenName()).equals(screenNameL)) {

            str.append(split).append("用户名更改 : @").append(screenNameL).append(" ------> @").append(screenName);

            oldScreename = screenNameL;
            
            change = true;

        }

        String bioL = bio;

        if (!ObjectUtil.equal(bio = user.getDescription(),bioL)) {

            str.append(split).append("简介更改 : \n\n").append(bioL).append(" \n\n ------> \n\n").append(bio);

            change = true;

        }

    oldPhotoUrl = photoUrl;
		
        if ((!ObjectUtil.equal(photoUrl = user.getOriginalProfileImageURLHttps(),oldPhotoUrl))) {

            str.append(split).append("头像更改 : " + Html.a("新头像",photoUrl));

            change = true;

        } else oldPhotoUrl = null;

        Boolean protectL = isProtected;

        if (protectL != (isProtected = user.isProtected())) {

            str.append(split).append("保护状态更改 : ").append(isProtected ? "开启了锁推" : "关闭了锁推");

            change = true;

        }
		
		oldBannerUrl = bannerUrl;
		
		if (!ObjectUtil.equal(bannerUrl =  user.getProfileBannerURL(),oldBannerUrl)) {
			
			str.append(split).append("横幅更改 : " + Html.a("新横幅",photoUrl));

            change = true;
			
		} else oldBannerUrl = null;
		
		
		String urlL = url;
		
		if (!ObjectUtil.equal(url = user.getURL(),urlL)) {
			
			str.append(split).append("链接更改 : \n\n").append(urlL).append(" \n\n ------> \n\n").append(url);

            change = true;
			
		}
		
        if (createdAt == null) {

           createdAt = user.getCreatedAt().getTime();

            change = false;

        }

        if (change) {

            TrackTask.onUserChange(this,str.toString());
            
        }
        
        return change;

    }
    
    public String urlHtml() {

        return Html.a(name,url());

    }

    public String url() {

        return "https://twitter.com/" + screenName;

    }


}