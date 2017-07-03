package com.gary.trc.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author zhouxianjun(Gary)
 * @ClassName:
 * @Description:
 * @date 17-7-3 下午2:50
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServerRegCache {
    private String service;
    private String version;
    private String address;
}
