package org.example.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * @author wxn
 * @date 2023/7/10
 */
public enum ListenerPluginInstanceStatus {
    /**
     * 0 normal, 1 updating
     */
    NORMAL(0, "normal"),
    UPDATING(1, "updating");

    ListenerPluginInstanceStatus(int code, String descp) {
        this.code = code;
        this.descp = descp;
    }

    @EnumValue
    private final int code;
    private final String descp;

    public int getCode() {
        return code;
    }

    public String getDescp() {
        return descp;
    }
}
