package com.gwf;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by gaowenfeng on 2017/11/7.
 */
@Configuration
public class MyConfig {

    @Bean
    public TransportClient client() {
        try {
            //创建结点（可以根据情况创建多个结点或者一个结点）
            InetSocketTransportAddress node = new InetSocketTransportAddress(
                    InetAddress.getByName("127.0.0.1"),
                    9300
            );
            InetSocketTransportAddress slave1 = new InetSocketTransportAddress(
                    InetAddress.getByName("127.0.0.1"),
                    9301
            );
            InetSocketTransportAddress slave2 = new InetSocketTransportAddress(
                    InetAddress.getByName("127.0.0.1"),
                    9302
            );
            //设置settings，默认的cluster.name为elasticsearch
            Settings settings = Settings.builder()
                    .put("cluster.name", "gwf")
                    .build();

            //创建客户端，如果使用默认配置，传参为Settings.EMPTY
            TransportClient client = new PreBuiltTransportClient(settings);
            //添加结点
            client.addTransportAddress(node);
            client.addTransportAddress(slave1);
            client.addTransportAddress(slave2);
            return client;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

}
