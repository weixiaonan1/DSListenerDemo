package org.example.controller;

import org.example.entity.Result;
import org.example.service.ListenerPluginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author wxn
 * @date 2023/5/14
 */

@RestController
@RequestMapping("/listener")
public class ListenerPluginController {
    @Autowired
    ListenerPluginService listenerPluginService;

    @PostMapping("/plugin/upload")
    public Result registerPlugin(@RequestParam("pluginJar") MultipartFile file, @RequestParam("classPath") String classPath) {
        return listenerPluginService.registerListenerPlugin(file, classPath);
    }

    @PostMapping("/instance")
    public Result createPluginInstance(@RequestParam(value = "pluginDefineId") int pluginDefineId,
                                       @RequestParam(value = "instanceName") String instanceName,
                                       @RequestParam(value = "pluginInstanceParams") String pluginInstanceParams) {
        return listenerPluginService.createListenerInstance(pluginDefineId, instanceName, pluginInstanceParams, "MasterDownEvent,MasterTimeoutEvent,TaskStartEvent");
    }

    @PostMapping("/plugin/remove")
    public Result removePlugin(@RequestParam("id") int pluginId) {
        return listenerPluginService.removeListenerPlugin(pluginId);
    }


}
