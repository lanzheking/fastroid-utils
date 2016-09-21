package com.honestwalker.androidutils.net;

import com.honestwalker.androidutils.StringUtil;

public class URIUtil {
	public static String getParam(String url,String key) {
		if(StringUtil.isEmptyOrNull(url)) {
			return null;
		} else {
			url = url.toLowerCase();
			key = key.toLowerCase();
			String[] ps = url.split("&");
			String v = null;
			for(String p : ps) {
				if(p.startsWith(key + "=")) {
					v = p.replace(key + "=", "");
					return v;
				}
			}
		}
		return null;
	}
}
