package org.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.example.enums.ListenerPluginInstanceStatus;

import java.util.Date;

/**
 * @author wxn
 * @date 2023/7/10
 */
@Data
@TableName("t_ds_listener_plugin_instance")
public class ListenerPluginInstance {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * plugin_define_id
     */
    @TableField(value = "plugin_define_id", updateStrategy = FieldStrategy.NEVER)
    private int pluginDefineId;

    /**
     * alert plugin instance name
     */
    @TableField("instance_name")
    private String instanceName;

    /**
     * plugin_instance_params
     */
    @TableField("plugin_instance_params")
    private String pluginInstanceParams;

    /**
     * plugin_instance_params
     * 0 normal, 1 updating
     */
    @TableField("plugin_instance_status")
    private ListenerPluginInstanceStatus pluginInstanceStatus;

    /**
     * listener_event_type
     */
    @TableField("listener_event_type")
    private String listenerEventType;

    /**
     * create_time
     */
    @TableField("create_time")
    private Date createTime;

    /**
     * update_time
     */
    @TableField("update_time")
    private Date updateTime;

}
