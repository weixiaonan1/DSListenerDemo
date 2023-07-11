package org.example.entity.listenerevent;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * @author wxn
 * @date 2023/7/10
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class DsListenerServerAlertEvent extends DsListenerEvent{
    /**
     * server type :master or worker
     */
    @JsonProperty("type")
    String type;
    @JsonProperty("host")
    String host;
    @JsonProperty("event")
    String event;
    @JsonProperty("warningLevel")
    String warningLevel;
}
