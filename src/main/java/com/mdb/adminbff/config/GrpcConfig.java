package com.mdb.adminbff.config;

import com.mdb.media_data_gateway_service.grpc.TorrentServiceGrpc;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class GrpcConfig {

    @Value("${grpc.client.media-gateway.host}")
    private String host;

    @Value("${grpc.client.media-gateway.port}")
    private int port;

    private ManagedChannel channel;

    @Bean
    public ManagedChannel managedChannel(GrpcTraceInterceptor traceInterceptor) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .intercept(traceInterceptor)
                .build();
        return this.channel;
    }

    @Bean
    public TorrentServiceGrpc.TorrentServiceBlockingStub mediaSourceStub(ManagedChannel channel) {
        return TorrentServiceGrpc.newBlockingStub(channel);
    }

    @PreDestroy
    public void shutdown() {
        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
        }
    }
}
