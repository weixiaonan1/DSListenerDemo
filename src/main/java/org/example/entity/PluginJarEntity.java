package org.example.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.net.URLClassLoader;
import java.util.jar.JarFile;

/**
 * @author wxn
 * @date 2023/7/11
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PluginJarEntity implements Serializable {
    private String jarPath;
    private URLClassLoader urlClassLoader;
    private JarFile jarFile;
}
