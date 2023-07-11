package org.example.entity.listenerevent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @author wxn
 * @date 2023/7/10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DsListenerEvent {
    protected Map<String, String> listenerInstanceParams;
}
