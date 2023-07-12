package org.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.dolphinscheduler.spi.utils.JSONUtils;
import org.example.entity.ListenerEvent;
import org.example.entity.ListenerPluginInstance;
import org.example.entity.listenerevent.DsListenerServerAlertEvent;
import org.example.entity.listenerevent.DsListenerTaskAlertEvent;
import org.example.entity.listenerevent.DsListenerWorkflowAlertEvent;
import org.example.enums.PostStatus;
import org.example.listener.ListenerPlugin;
import org.example.mapper.ListenerEventMapper;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author wxn
 * @date 2023/7/9
 */
@Slf4j
public class ListenerInstancePostService extends Thread{
    private volatile boolean isStopped = false;
    private ListenerPlugin listenerPlugin;
    private ListenerPluginInstance listenerPluginInstance;
    private final ListenerEventMapper listenerEventMapper;
    private Map<String,String> listenerInstanceParams;

    public ListenerInstancePostService(ListenerPluginInstance listenerPluginInstance, ListenerEventMapper listenerEventMapper, ListenerPlugin listenerPlugin){
        log.info("listener plugin instance {} service start!", listenerPluginInstance.getInstanceName());
        this.listenerPlugin = listenerPlugin;
        this.listenerPluginInstance = listenerPluginInstance;
        this.listenerEventMapper = listenerEventMapper;
        this.listenerInstanceParams = JSONUtils.toMap(listenerPluginInstance.getPluginInstanceParams());
    }

    @Override
    public synchronized void start() {
        super.setName("ListenerInstancePostService-" + listenerPluginInstance.getInstanceName());
        super.start();
    }

    @Override
    public void run() {
        while (!isStopped) {
            try {
                LambdaQueryWrapper<ListenerEvent> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(ListenerEvent::getPluginInstanceId, listenerPluginInstance.getId())
                        .last("limit 100");
                List<ListenerEvent> eventList = listenerEventMapper.selectList(wrapper);
                if (CollectionUtils.isEmpty(eventList)) {
                    log.info("There is not waiting listener events");
                    continue;
                }
                this.post(eventList);
            } catch (Exception e) {
                log.error("Alert sender thread meet an exception", e);
            } finally {
                try {
                    Thread.sleep(10000);
                } catch (final InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    log.error("Current thread sleep error", interruptedException);
                }
            }
        }
    }

    public void setStopped(boolean isStopped){
        log.info("set stopped {}", isStopped);
        this.isStopped = isStopped;
    }

    private void post(List<ListenerEvent> eventList){
        // TODO: 这里的ListenerEvent，是否需要每个类型的event创建一个类，像Spark一样。各个event内部装什么？
        // TODO: 这里可以复用告警消息中的ServerAlertContent，ProcessAlertContent，TaskAlertContent，但是创建删除修改等需要自己创建
        for (ListenerEvent event: eventList){
            String eventType = event.getEventType();
            try {
                if ("MasterDownEvent".equals(eventType) && listenerListenSpecificEvent(eventType)){
                    DsListenerServerAlertEvent dsListenerEvent = JSONUtils.parseObject(event.getContent(), DsListenerServerAlertEvent.class);
                    dsListenerEvent.setListenerInstanceParams(listenerInstanceParams);
                    listenerPlugin.onMasterDown(dsListenerEvent);
                } else if ("MasterTimeoutEvent".equals(eventType) && listenerListenSpecificEvent(eventType)) {
                    DsListenerServerAlertEvent dsListenerEvent = JSONUtils.parseObject(event.getContent(), DsListenerServerAlertEvent.class);
                    dsListenerEvent.setListenerInstanceParams(listenerInstanceParams);
                    listenerPlugin.onMasterTimeout(dsListenerEvent);
                } else if ("WorkflowStartEvent".equals(eventType) && listenerListenSpecificEvent(eventType)) {
                    DsListenerWorkflowAlertEvent dsListenerEvent =
                            JSONUtils.parseObject(event.getContent(), DsListenerWorkflowAlertEvent.class);
                    dsListenerEvent.setListenerInstanceParams(listenerInstanceParams);
                    listenerPlugin.onWorkflowStart(dsListenerEvent);
                } else if ("WorkflowEndEvent".equals(eventType) && listenerListenSpecificEvent(eventType)) {
                    DsListenerWorkflowAlertEvent dsListenerEvent =
                            JSONUtils.parseObject(event.getContent(), DsListenerWorkflowAlertEvent.class);
                    dsListenerEvent.setListenerInstanceParams(listenerInstanceParams);
                    listenerPlugin.onWorkflowEnd(dsListenerEvent);
                } else if ("TaskStartEvent".equals(eventType) && listenerListenSpecificEvent(eventType)) {
                    DsListenerTaskAlertEvent dsListenerEvent =
                            JSONUtils.parseObject(event.getContent(), DsListenerTaskAlertEvent.class);
                    dsListenerEvent.setListenerInstanceParams(listenerInstanceParams);
                    listenerPlugin.onTaskStart(dsListenerEvent);
                }else if ("TaskEndEvent".equals(eventType) && listenerListenSpecificEvent(eventType)) {
                    DsListenerTaskAlertEvent dsListenerEvent =
                            JSONUtils.parseObject(event.getContent(), DsListenerTaskAlertEvent.class);
                    dsListenerEvent.setListenerInstanceParams(listenerInstanceParams);
                    listenerPlugin.onTaskEnd(dsListenerEvent);
                }else {
                    throw new Exception(String.format("unknown listener event type %s", eventType));
                }
            }catch (Exception e){
                log.error("post listener event failed, event id:{}", event.getId(),e);
                event.setPostStatus(PostStatus.EXECUTION_FAILURE);
                event.setLog(e.toString());
                event.setUpdateTime(new Date());
                listenerEventMapper.updateById(event);
                continue;
            }
            log.info("listener event {} post successfully, delete", event.getId());
            listenerEventMapper.deleteById(event);
        }
    }

    private boolean listenerListenSpecificEvent(String eventType){
        return listenerPluginInstance.getListenerEventType().contains(eventType);
    }

    public void updateListenerPlugin(ListenerPlugin listenerPlugin){
        this.listenerPlugin = listenerPlugin;
    }

    public void updateListenerPluginInstance(ListenerPluginInstance listenerPluginInstance){
        this.listenerPluginInstance = listenerPluginInstance;
        this.listenerInstanceParams = JSONUtils.toMap(listenerPluginInstance.getPluginInstanceParams());
    }

}
