//package org.example;
//
//import java.io.File;
//import java.net.URL;
//import java.net.URLClassLoader;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Objects;
//
///**
// * @author wxn
// * @date 2023/5/14
// */
//public class HotClassLoader extends URLClassLoader {
//    private static final Map<String, Long> jarUpdateTimeMap;
//
//    private ListenerPluginService springInjectService;
//
//    static {
//        jarUpdateTimeMap = new HashMap<>();
//    }
//
//    public HotClassLoader(ClassLoader parent) {
//        super(new URL[0], parent);
//    }
//
//    public HotClassLoader(ListenerPluginService springInjectService, ClassLoader parent) {
//        super(new URL[0], parent);
//        this.springInjectService = springInjectService;
//    }
//
//    public void loadJar(String jarPath, String pluginClass) throws Exception {
//        Long lastModifyTime = jarUpdateTimeMap.getOrDefault(jarPath, -1L);
//        if (Objects.equals(lastModifyTime, 0L)){
//            System.out.println("正在加载，请稍等");
//            return;
//        }
//        File file = new File(jarPath);
//        if (!file.exists()){
//            System.out.println("获取插件失败");
//            return;
//        }
//        long currentJarModifyTime = file.getAbsoluteFile().lastModified();
//        if (Objects.equals(lastModifyTime, currentJarModifyTime)){
//            System.out.println("已经加载过该插件");
//        }
//        ClassLoader classLoader = ClassLoaderUtil.getClassLoader(jarPath);
//        Class<?> clazz = classLoader.loadClass(pluginClass);
//        springInjectService.registerBean(clazz.getName(), clazz);
//        jarUpdateTimeMap.put(jarPath,currentJarModifyTime);
//    }
//}
