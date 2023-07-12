package org.custom;

import lombok.extern.slf4j.Slf4j;
import org.apache.dolphinscheduler.spi.params.base.PluginParams;
import org.apache.dolphinscheduler.spi.params.base.Validate;
import org.apache.dolphinscheduler.spi.params.input.InputParam;
import org.example.entity.listenerevent.DsListenerEvent;
import org.example.entity.listenerevent.DsListenerServerAlertEvent;
import org.example.listener.ListenerPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wxn
 * @date 2023/5/14
 */
@Slf4j
public class LoggerListener implements ListenerPlugin {

    @Override
    public String name() {
        return "TestLoggerListener";
    }

    @Override
    public List<PluginParams> params() {
        List<PluginParams> paramsList = new ArrayList<>();
        InputParam param1 = InputParam.newBuilder("param1", "param1")
                .setPlaceholder("please input param1")
                .addValidate(Validate.newBuilder()
                        .setRequired(true)
                        .build())
                .build();
        paramsList.add(param1);
        return paramsList;
    }

    @Override
    public void onMasterDown(DsListenerServerAlertEvent masterDownEvent) {
        String param1 = getParam1(masterDownEvent);
        log.info("TestLoggerListener2.0(param1:{}): master server {} down!",param1,  masterDownEvent.getHost());
    }

    @Override
    public void onMasterTimeout(DsListenerServerAlertEvent masterTimeoutEvent) {
        String param1 = getParam1(masterTimeoutEvent);
        log.info("TestLoggerListener2.0(param1:{}): master server {} timeout!",param1, masterTimeoutEvent.getHost());
    }


    private String getParam1(DsListenerEvent event){
        return event.getListenerInstanceParams().get("param1");
    }


}
