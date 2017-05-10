package com.gary.trc.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 17-4-26 下午4:16
 */
@Slf4j
public class Utils {
    public static <T> Constructor<T> getConstructorByParent(Class<T> classes, Class<?> ...parentParams) throws NoSuchMethodException {
        for (Constructor<?> constructor : classes.getDeclaredConstructors()) {
            Class<?>[] types = constructor.getParameterTypes();
            if (parentParams == null && types == null) return (Constructor<T>) constructor;
            if (parentParams != null && parentParams.length == types.length) {
                boolean ok = true;
                for (int i = 0; i < parentParams.length; i++) {
                    if (!parentParams[i].isAssignableFrom(types[i]) && !types[i].isAssignableFrom(parentParams[i])) {
                        ok = false;
                        break;
                    }
                }

                if (ok) return (Constructor<T>) constructor;
            }
        }
        return classes.getConstructor(parentParams);
    }

    public static String mapToQuery(Map<String, Object> map, String seq, String eq, boolean encode, String enc) throws UnsupportedEncodingException {
        if (map == null || map.isEmpty()) return "";
        List<String> list = new ArrayList<>();
        for (String key : map.keySet()) {
            Object value = map.get(key);
            if (value instanceof List) {
                for (Object o : (List) value) {
                    addValue(list, key, o, eq, encode, enc);
                }
            } else {
                addValue(list, key, value, eq, encode, enc);
            }
        }
        return StringUtils.join(list.toArray(), seq == null ? "&" : seq);
    }

    public static Map<String, Object> queryToMap(String query) {
        return queryToMap(query, null, null, false, null);
    }

    public static Map<String, Object> queryToMap(String query, String seq, String eq, boolean decode, String enc) {
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.isEmpty(query)) return map;
        String[] qs = query.split(seq == null ? "&" : seq);
        eq = eq == null ? "=" : eq;
        for (String q : qs) {
            q = q.replaceAll("\\+", "%20");
            int index = q.indexOf(eq);
            String key = index >= 0 ? q.substring(0, index) : q;
            String value = index >= 0 ? q.substring(index + eq.length()) : "";
            if (decode) {
                try {
                    key = URLDecoder.decode(key, enc);
                    value = URLDecoder.decode(value, enc);
                } catch (UnsupportedEncodingException e) {
                    log.warn("queryToMap decode error", e);
                }
            }
            if (!map.containsKey(key)) {
                map.put(key, value);
            } else {
                Object val = map.get(key);
                if (val instanceof List) {
                    ((List) val).add(value);
                } else {
                    List<String> values = new ArrayList<>();
                    values.add(val.toString());
                    values.add(value);
                    map.put(key, values);
                }
            }
        }
        return map;
    }

    public static String methodJoin(Class c) {
        Method[] methods = c.getDeclaredMethods();
        String[] array = new String[methods.length];
        for (int i = 0; i < methods.length; i++) {
            array[i] = methods[i].getName();
        }
        return StringUtils.join(array, ",");
    }

    public static void createPath(CuratorFramework zkClient, String path, CreateMode mode) throws Exception {
        Stat stat = zkClient.checkExists().forPath(path);
        if (stat == null) {
            zkClient.create()
                    .creatingParentsIfNeeded()
                    .withMode(mode)
                    .forPath(path);
            log.info("create path:{} for zookeeper.", path);
        }
    }

    public static Object getMapValue(Map<String, Object> map, String key) {
        if (map == null || !map.containsKey(key)) return null;
        return map.get(key);
    }

    public static int getMapInt(Map<String, Object> map, String key, int defaultValue) {
        Object val = getMapValue(map, key);
        if (val == null) return defaultValue;
        try {
            return Integer.valueOf(val.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static long getMapLong(Map<String, Object> map, String key, long defaultValue) {
        Object val = getMapValue(map, key);
        if (val == null) return defaultValue;
        try {
            return Long.valueOf(val.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static String getMapString(Map<String, Object> map, String key, String defaultValue) {
        Object val = getMapValue(map, key);
        if (val == null) return defaultValue;
        return val.toString();
    }

    public static boolean getMapBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object val = getMapValue(map, key);
        if (val == null) return defaultValue;
        return Boolean.valueOf(val.toString());
    }

    public static <T> Collection<T> getMapCollection(Map<String, Object> map, String key, Collection<T> defaultValue) {
        Object val = getMapValue(map, key);
        if (val == null) return defaultValue;
        if (val instanceof Collection) {
            return (Collection<T>) val;
        }
        return defaultValue;
    }

    public static Set<Map<String, Object>> urlParse(List<String> urls) {
        Set<String> urlSet = new HashSet<>(urls);
        Set<Map<String, Object>> result = new HashSet<>();
        for (String url : urlSet) {
            result.add(queryToMap(url));
        }
        return result;
    }

    public static <T> T findForArray(T[] array, T value) {
        if (array != null && array.length > 0) {
            for (T t : array) {
                if (t != null && t.equals(value)) {
                    return t;
                }
            }
        }
        return null;
    }

    private static void addValue(List<String> list, String key, Object value, String eq, boolean encode, String enc) throws UnsupportedEncodingException {
        list.add(key + (eq == null ? "=" : eq) + (value == null ? "" : encode ? URLEncoder.encode(value.toString(), enc) : value.toString()));
    }
}
