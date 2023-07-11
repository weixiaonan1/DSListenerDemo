package org.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.dolphinscheduler.spi.params.PluginParamsTransfer;
import org.apache.dolphinscheduler.spi.utils.JSONUtils;
import org.example.entity.ListenerPluginInstance;
import org.example.entity.PluginDefine;
import org.example.entity.Result;
import org.example.listener.ListenerPlugin;
import org.example.mapper.ListenerEventMapper;
import org.example.mapper.ListenerPluginInstanceMapper;
import org.example.mapper.PluginDefineMapper;
import org.example.util.ClassLoaderUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wxn
 * @date 2023/5/14
 */
@Component
@Slf4j
public class ListenerPluginService implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {
    private DefaultListableBeanFactory defaultListableBeanFactory;

    private ApplicationContext applicationContext;

    @Resource
    private PluginDefineMapper pluginDefineMapper;

    @Resource
    private ListenerPluginInstanceMapper pluginInstanceMapper;

    @Resource
    private ListenerEventMapper listenerEventMapper;

    @Resource
    private ClassLoaderUtil classLoaderUtil;

    private final String path = "C:\\Users\\Lenovo\\Documents\\wxn\\projects\\DynamicPlugins\\plugins\\";

    private final ConcurrentHashMap<Integer, ListenerPlugin> listenerPlugins = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Integer, ListenerInstancePostService> listenerInstancePostServices = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) applicationContext;
        this.defaultListableBeanFactory = (DefaultListableBeanFactory) configurableApplicationContext.getBeanFactory();
    }

    @Override
    public void onApplicationEvent(@NotNull ContextRefreshedEvent event) {
//        List<PluginDefine> pluginDefines = pluginDefineMapper.selectList(new QueryWrapper<>());
//        log.info("init listener plugins");
//        for (PluginDefine pluginDefine : pluginDefines) {
//            try {
//                ListenerPlugin plugin = getListenerPluginFromJar(pluginDefine.getPluginLocation(), pluginDefine.getPluginClassName());
//                listenerPlugins.put(pluginDefine.getId(), plugin);
//                log.info("init listener plugin {}", pluginDefine.getPluginName());
//            } catch (Exception e) {
//                log.error("failed when init listener plugin {}", pluginDefine.getPluginName(), e);
//            }
//        }
//        log.info("init listener instances");
//        List<ListenerPluginInstance> pluginInstances = pluginInstanceMapper.selectList(new QueryWrapper<>());
//        for (ListenerPluginInstance pluginInstance : pluginInstances) {
//            int pluginId = pluginInstance.getPluginDefineId();
//            if (!listenerPlugins.containsKey(pluginId)) {
//                log.error("failed to init listener instance {} because listener plugin {} cannot be loaded", pluginInstance.getInstanceName(), pluginId);
//                continue;
//            }
//            ListenerInstancePostService listenerInstancePostService = new ListenerInstancePostService(pluginInstance, listenerEventMapper, listenerPlugins.get(pluginId));
//            listenerInstancePostService.start();
//            listenerInstancePostServices.put(pluginInstance.getId(), listenerInstancePostService);
//            log.info("init listener instance {}：", pluginInstance.getInstanceName());
//        }
    }

    public Result registerListenerPlugin(MultipartFile file, String classPath) {
        String fileName = file.getOriginalFilename();
        String filePath = path + fileName;
        try {
            File dest = new File(filePath);
            Files.copy(file.getInputStream(), dest.toPath());
            ListenerPlugin plugin = getListenerPluginFromJar(filePath, classPath);
            PluginDefine pluginDefine = PluginDefine.builder()
                    .pluginName(plugin.name())
                    .pluginParams(JSONUtils.toJsonString(plugin.params()))
                    .pluginType("listener")
                    .pluginLocation(filePath)
                    .pluginClassName(classPath)
                    .createTime(new Date())
                    .updateTime(new Date())
                    .build();
            pluginDefineMapper.insert(pluginDefine);
            listenerPlugins.put(pluginDefine.getId(), plugin);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return Result.fail("failed when upload jar：" + e.getMessage());
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage(), e);
            return Result.fail("cannot load class：" + e.getMessage());
        } catch (Exception e) {
            return Result.fail("failed when register listener plugin：" + e.getMessage());
        }
        return Result.success();
    }

    public Result removeListenerPlugin(int id) {
        PluginDefine plugin = pluginDefineMapper.selectById(id);
        if (Objects.isNull(plugin)) {
            return Result.fail(String.format("listener plugin %d not exist", id));
        }
        LambdaQueryWrapper<ListenerPluginInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ListenerPluginInstance::getPluginDefineId, id);
        List<ListenerPluginInstance> pluginInstances = pluginInstanceMapper.selectList(wrapper);
        if (CollectionUtils.isNotEmpty(pluginInstances)) {
            return Result.fail(String.format("please remove listener instances of plugin %s first", plugin.getPluginName()));
        }
        try {
            removeBean(plugin.getPluginName());
            Files.delete(new File(plugin.getPluginLocation()).toPath());
            pluginDefineMapper.deleteById(id);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return Result.fail("failed when remove jar：" + e.getMessage());
        } catch (Exception e) {
            return Result.fail("failed when register listener plugin：" + e.getMessage());
        }
        return Result.success();
    }

    @Transactional
    public Result createListenerInstance(int pluginDefineId, String instanceName, String pluginInstanceParams, String listenerEventType) {
        if (!listenerPlugins.containsKey(pluginDefineId)){
            return Result.fail(String.format("failed when register listener instance %s because listener plugin %d cannot loaded", instanceName, pluginDefineId));
        }
        ListenerPluginInstance listenerPluginInstance = new ListenerPluginInstance();
        String paramsMapJson = parsePluginParamsMap(pluginInstanceParams);
        listenerPluginInstance.setInstanceName(instanceName);
        listenerPluginInstance.setPluginInstanceParams(paramsMapJson);
        listenerPluginInstance.setPluginDefineId(pluginDefineId);
        listenerPluginInstance.setListenerEventType(listenerEventType);
        pluginInstanceMapper.insert(listenerPluginInstance);
        ListenerInstancePostService listenerInstancePostService = new ListenerInstancePostService(listenerPluginInstance, listenerEventMapper, listenerPlugins.get(pluginDefineId));
        listenerInstancePostService.start();
        listenerInstancePostServices.put(listenerPluginInstance.getId(), listenerInstancePostService);
        return Result.success(listenerPluginInstance);
    }

    private String parsePluginParamsMap(String pluginParams) {
        Map<String, String> paramsMap = PluginParamsTransfer.getPluginParamsMap(pluginParams);
        return JSONUtils.toJsonString(paramsMap);
    }

    private ListenerPlugin getListenerPluginFromJar(String filePath, String classPath) throws ClassNotFoundException {
        ClassLoader classLoader = classLoaderUtil.getClassLoader(filePath);
        assert classLoader != null;
        Class<?> clazz = classLoader.loadClass(classPath);
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
        defaultListableBeanFactory.registerBeanDefinition(clazz.getName(), beanDefinitionBuilder.getRawBeanDefinition());
        return (ListenerPlugin) applicationContext.getBean(clazz.getName());
    }

    public Object getBean(String name) {
        return applicationContext.getBean(name);
    }

    public boolean containsBean(String name) {
        return applicationContext.containsBean(name);
    }

    public void removeBean(String name) {
        defaultListableBeanFactory.removeBeanDefinition(name);
    }
}
