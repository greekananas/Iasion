package orufeo.iasion.utils;

import java.util.Map;

public class CookiesValueTL {

	public static final ThreadLocal<Map<String, String>> customerThreadLocal = new ThreadLocal<Map<String, String>>();

	public static void set(Map<String, String> cookiesValueMap) {
		customerThreadLocal.set(cookiesValueMap);
	}

	public static void remove() {
		customerThreadLocal.remove();
	}

	public static Map<String, String> get() {
		return customerThreadLocal.get();
	}

}