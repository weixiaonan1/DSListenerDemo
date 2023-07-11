package org.example.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wxn
 * @date 2023/5/14
 */
@Slf4j
@Component
public class ClassLoaderUtil {
    private final ConcurrentHashMap<String, URLClassLoader> localCache = new ConcurrentHashMap<>();

    public URLClassLoader getClassLoader(String jarPath) {
        try {
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            URLClassLoader classLoader = new URLClassLoader(new URL[]{}, ClassLoader.getSystemClassLoader());
            method.invoke(classLoader, new URL("file:///" + jarPath));
            localCache.put(jarPath, classLoader);
            return classLoader;
        } catch (Exception e) {
            log.error("get class loader error", e);
            return null;
        }
    }

    public void removeJarFile(String jarPath){
        URLClassLoader classLoader = localCache.get(jarPath);
    }
}