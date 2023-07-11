package org.example;

import org.example.listener.ListenerPlugin;
import org.example.util.ClassLoaderUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author wxn
 * @date 2023/5/14
 */
@SpringBootTest
public class HotPluginTest implements ApplicationContextAware {
    @Autowired
    ClassLoaderUtil classLoaderUtil;

    private DefaultListableBeanFactory defaultListableBeanFactory;

    private ApplicationContext applicationContext;

    @Test
    public void test() throws Exception {
        String jarPath = "C:\\Users\\Lenovo\\Documents\\wxn\\projects\\DynamicPlugins\\plugins\\LoggerPlugin-1.0.jar";
        String classPath = "org.custom.LoggerListener";
        ClassLoader classLoader = classLoaderUtil.getClassLoader(jarPath);
        Class<?> clazz = classLoader.loadClass(classPath);
        System.out.println(clazz.getName());
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
        defaultListableBeanFactory.registerBeanDefinition(clazz.getName(), beanDefinitionBuilder.getRawBeanDefinition());
        ListenerPlugin listenerPlugin = (ListenerPlugin) applicationContext.getBean(clazz.getName());
        System.out.println(listenerPlugin.name());
        defaultListableBeanFactory.removeBeanDefinition(classPath);
        classLoaderUtil.removeJarFile(jarPath);
        Thread.sleep(300000);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
        ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) applicationContext;
        this.defaultListableBeanFactory = (DefaultListableBeanFactory) configurableApplicationContext.getBeanFactory();
    }
}
