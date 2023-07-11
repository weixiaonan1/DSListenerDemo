package org.example.listener;

import org.apache.dolphinscheduler.spi.params.base.PluginParams;
import org.example.entity.listenerevent.DsListenerServerAlertEvent;
import org.example.entity.listenerevent.DsListenerTaskAlertEvent;
import org.example.entity.listenerevent.DsListenerWorkflowAlertEvent;

import java.util.List;

/**
 * @author wxn
 * @date 2023/5/15
 */
public interface ListenerPlugin {
    String name();

    List<PluginParams> params();

    /**
     * api: master/worker 失连/超时
     */
    default void onMasterDown(DsListenerServerAlertEvent masterDownEvent) {
    }

    default void onMasterTimeout(DsListenerServerAlertEvent masterTimeoutEvent) {
    }

    /**
     * master：工作流开始/结束/失败等
     */
    default void onWorkflowStart(DsListenerWorkflowAlertEvent workflowStartEvent) {
    }

    default void onWorkflowEnd(DsListenerWorkflowAlertEvent workflowEndEvent) {
    }

    /**
     * worker：任务开始/结束/失败等
     */
    default void onTaskStart(DsListenerTaskAlertEvent taskStartEvent) {
    }

    default void onTaskEnd(DsListenerTaskAlertEvent taskEndEvent) {
    }

    /**
     * api server：工作流创建/更新/删除
     */

}
