package com.leyou.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "ly.filter")
public class AllowFilterProperties {

    private List<String> allowPaths; //所有通过的路径

}
