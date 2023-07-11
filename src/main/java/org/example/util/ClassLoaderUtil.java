package org.example.util;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.PluginJarEntity;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;

/**
 * @author wxn
 * @date 2023/5/14
 */
@Slf4j
@Component
public class ClassLoaderUtil {
    private final ConcurrentHashMap<String, PluginJarEntity> localCache = new ConcurrentHashMap<>();

    public URLClassLoader getClassLoader(String jarPath) throws Exception {
            Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            if (!method.isAccessible()) {
                method.setAccessible(true);
            }
            URLClassLoader classLoader = new URLClassLoader(new URL[]{}, ClassLoader.getSystemClassLoader());
            File jarFile = new File(jarPath);
            method.invoke(classLoader, jarFile.toURI().toURL());
            PluginJarEntity jarEntity = PluginJarEntity.builder()
                    .jarPath(jarPath)
                    .urlClassLoader(classLoader)
                    .jarFile(new JarFile(jarFile))
                    .build();
            localCache.put(jarPath, jarEntity);
            return classLoader;
    }

    public void removeJarFile(String jarPath) throws Exception{
            PluginJarEntity jarEntity = localCache.get(jarPath);
            jarEntity.getUrlClassLoader().close();
            jarEntity.getJarFile().close();
            localCache.remove(jarEntity.getJarPath());
    }
}