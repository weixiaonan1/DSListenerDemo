package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.listener.ListenerPlugin;
import org.junit.jupiter.api.Nested;

import java.util.Date;

/**
 * @author wxn
 * @date 2023/7/10
 */
@Data
@TableName("t_ds_plugin_define")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginDefine {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * plugin name
     */
    @TableField("plugin_name")
    private String pluginName;

    /**
     * plugin_type
     */
    @TableField("plugin_type")
    private String pluginType;

    /**
     * plugin_params
     */
    @TableField("plugin_params")
    private String pluginParams;

    /**
     * plugin_location
     */
    @TableField("plugin_location")
    private String pluginLocation;

    /**
     * plugin full class name
     */
    @TableField("plugin_class_name")
    private String pluginClassName;

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

    public PluginDefine(String pluginName, String pluginType, String pluginParams) {
        this.pluginName = pluginName;
        this.pluginType = pluginType;
        this.pluginParams = pluginParams;
        this.createTime = new Date();
        this.updateTime = new Date();
    }
}
