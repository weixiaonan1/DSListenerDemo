package org.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.dolphinscheduler.spi.params.PluginParamsTransfer;
import org.apache.dolphinscheduler.spi.utils.JSONUtils;
import org.example.entity.ListenerEvent;
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
import java.util.*;
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
        List<PluginDefine> pluginDefines = pluginDefineMapper.selectList(new QueryWrapper<>());
        log.info("init listener plugins");
        for (PluginDefine pluginDefine : pluginDefines) {
            try {
                ListenerPlugin plugin = getListenerPluginFromJar(pluginDefine.getPluginLocation(), pluginDefine.getPluginClassName());
                listenerPlugins.put(pluginDefine.getId(), plugin);
                log.info("init listener plugin {}", pluginDefine.getPluginName());
            } catch (Exception e) {
                log.error("failed when init listener plugin {}", pluginDefine.getPluginName(), e);
            }
        }
        log.info("init listener instances");
        List<ListenerPluginInstance> pluginInstances = pluginInstanceMapper.selectList(new QueryWrapper<>());
        for (ListenerPluginInstance pluginInstance : pluginInstances) {
            int pluginId = pluginInstance.getPluginDefineId();
            if (!listenerPlugins.containsKey(pluginId)) {
                log.error("failed to init listener instance {} because listener plugin {} cannot be loaded", pluginInstance.getInstanceName(), pluginId);
                continue;
            }
            ListenerInstancePostService listenerInstancePostService = new ListenerInstancePostService(pluginInstance, listenerEventMapper, listenerPlugins.get(pluginId));
            listenerInstancePostService.start();
            listenerInstancePostServices.put(pluginInstance.getId(), listenerInstancePostService);
            log.info("init listener instance {}：", pluginInstance.getInstanceName());
        }
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

    public Result updateListenerPlugin(int id, MultipartFile file, String classPath) {
        if (!listenerPlugins.containsKey(id)) {
            return Result.fail(String.format("listener plugin %d not exist in concurrent hash map", id));
        }
        // 先把所有的实例都暂停
        LambdaQueryWrapper<ListenerPluginInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(ListenerPluginInstance::getId)
                .eq(ListenerPluginInstance::getPluginDefineId, id);
        List<ListenerPluginInstance> instances = pluginInstanceMapper.selectList(wrapper);
        List<ListenerInstancePostService> services = new ArrayList<>();
        for (ListenerPluginInstance instance : instances) {
            if (listenerInstancePostServices.containsKey(instance.getId())) {
                services.add(listenerInstancePostServices.get(instance.getId()));
            }
        }
        services.forEach(x -> x.setStopped(true));
        PluginDefine plugin = pluginDefineMapper.selectById(id);

        try {
            //TODO: 卸载旧的plugin（在卸载结束但是没有加载新插件时服务宕机，会造成插件及实例不可用，消息堆积（可以强制要求新jar包和旧jar包名称不相同来规避，这样可以在更新完成后才删除旧jar包，如果出现之前的问题，可以在服务重启时自行恢复）
            classLoaderUtil.removeJarFile(plugin.getPluginLocation());
            defaultListableBeanFactory.removeBeanDefinition(plugin.getPluginClassName());
            Files.delete(new File(plugin.getPluginLocation()).toPath());
            //安装新的plugin
            String fileName = file.getOriginalFilename();
            String filePath = path + fileName;
            File dest = new File(filePath);
            Files.copy(file.getInputStream(), dest.toPath());
            ListenerPlugin newPlugin = getListenerPluginFromJar(filePath, classPath);
            PluginDefine pluginDefine = PluginDefine.builder()
                    .id(id)
                    .pluginName(newPlugin.name())
                    .pluginParams(JSONUtils.toJsonString(newPlugin.params()))
                    .pluginType("listener")
                    .pluginLocation(filePath)
                    .pluginClassName(classPath)
                    .updateTime(new Date())
                    .build();
            pluginDefineMapper.updateById(pluginDefine);
            listenerPlugins.put(id, newPlugin);
            //TODO: 恢复实例运行，如果插件的参数被更新了，那么实例的参数值也应该跟着变化，否则前端没办法根据新的插件参数修改实例了
            services.forEach(x -> {
                x.updateListenerPlugin(newPlugin);
                x.setStopped(false);
            });
            return Result.success();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return Result.fail("failed when remove jar：" + e.getMessage());
        } catch (Exception e) {
            return Result.fail("failed when register listener plugin：" + e.getMessage());
        }

    }

    public Result removeListenerPlugin(int id) {
        if (!listenerPlugins.containsKey(id)) {
            return Result.fail(String.format("listener plugin %d not exist in concurrent hash map", id));
        }
        PluginDefine plugin = pluginDefineMapper.selectById(id);
        if (Objects.isNull(plugin)) {
            return Result.fail(String.format("listener plugin %d not exist in db", id));
        }
        LambdaQueryWrapper<ListenerPluginInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ListenerPluginInstance::getPluginDefineId, id);
        List<ListenerPluginInstance> pluginInstances = pluginInstanceMapper.selectList(wrapper);
        if (CollectionUtils.isNotEmpty(pluginInstances)) {
            return Result.fail(String.format("please remove listener instances of plugin %s first", plugin.getPluginName()));
        }
        try {
            classLoaderUtil.removeJarFile(plugin.getPluginLocation());
            defaultListableBeanFactory.removeBeanDefinition(plugin.getPluginClassName());
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
        if (!listenerPlugins.containsKey(pluginDefineId)) {
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

    public Result updateListenerInstance(int instanceId, String instanceName, String pluginInstanceParams, String listenerEventType) {
        if (!listenerInstancePostServices.containsKey(instanceId)) {
            return Result.fail(String.format("failed when update listener instance %s because listener instance %d not exist in map", instanceName, instanceId));
        }
        ListenerInstancePostService instancePostService = listenerInstancePostServices.get(instanceId);
        instancePostService.setStopped(true);
        ListenerPluginInstance listenerPluginInstance = new ListenerPluginInstance();
        listenerPluginInstance.setId(instanceId);
        listenerPluginInstance.setInstanceName(instanceName);
        listenerPluginInstance.setPluginInstanceParams(pluginInstanceParams);
        listenerPluginInstance.setListenerEventType(listenerEventType);
        listenerPluginInstance.setUpdateTime(new Date());
        pluginInstanceMapper.updateById(listenerPluginInstance);
        instancePostService.updateListenerPluginInstance(listenerPluginInstance);
        instancePostService.setStopped(false);
        return Result.success();
    }

    public Result removeListenerInstance(int id) {
        if (!listenerInstancePostServices.containsKey(id)) {
            return Result.fail(String.format("listener instance service %d not exist in concurrent hash map", id));
        }
        ListenerPluginInstance instance = pluginInstanceMapper.selectById(id);
        if (Objects.isNull(instance)) {
            return Result.fail(String.format("listener instance %d not exist in db", id));
        }
        //停止服务线程
        ListenerInstancePostService listenerInstancePostService = listenerInstancePostServices.get(id);
        listenerInstancePostService.setStopped(true);
        listenerInstancePostServices.remove(id);
        //删除该监听实例
        pluginInstanceMapper.deleteById(id);
        //删除该监听实力所有的消息（可能有失败的，可能有删除监听示例时未来得及处理的）
        LambdaQueryWrapper<ListenerEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ListenerEvent::getPluginInstanceId, id);
        listenerEventMapper.delete(wrapper);
        return Result.success();
    }

    private String parsePluginParamsMap(String pluginParams) {
        Map<String, String> paramsMap = PluginParamsTransfer.getPluginParamsMap(pluginParams);
        return JSONUtils.toJsonString(paramsMap);
    }

    private ListenerPlugin getListenerPluginFromJar(String filePath, String classPath) throws Exception {
        ClassLoader classLoader = classLoaderUtil.getClassLoader(filePath);
        Class<?> clazz = classLoader.loadClass(classPath);
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
        defaultListableBeanFactory.registerBeanDefinition(clazz.getName(), beanDefinitionBuilder.getRawBeanDefinition());
        return (ListenerPlugin) applicationContext.getBean(clazz.getName());
    }
}
