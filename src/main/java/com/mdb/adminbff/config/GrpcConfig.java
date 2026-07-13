package com.mdb.adminbff.config;

import com.mdb.media_data_gateway_service.grpc.MediaServiceGrpc;
import com.mdb.media_data_gateway_service.grpc.TorrentServiceGrpc;
import com.mdb.user_data_gateway_service.grpc.AdminUserServiceGrpc;
import com.mdb.user_data_gateway_service.grpc.UserServiceGrpc;
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
    private String mediaHost;

    @Value("${grpc.client.media-gateway.port}")
    private int mediaPort;

    @Value("${grpc.client.user-gateway.host}")
    private String userHost;

    @Value("${grpc.client.user-gateway.port}")
    private int userPort;

    private ManagedChannel mediaChannel;
    private ManagedChannel userChannel;

    @Bean
    public ManagedChannel mediaChannel(GrpcTraceInterceptor traceInterceptor) {
        this.mediaChannel = ManagedChannelBuilder.forAddress(mediaHost, mediaPort)
                .usePlaintext()
                .intercept(traceInterceptor)
                .build();
        return this.mediaChannel;
    }

    @Bean
    public ManagedChannel userChannel(GrpcTraceInterceptor traceInterceptor) {
        this.userChannel = ManagedChannelBuilder.forAddress(userHost, userPort)
                .usePlaintext()
                .intercept(traceInterceptor)
                .build();
        return this.userChannel;
    }

    @Bean
    public TorrentServiceGrpc.TorrentServiceBlockingStub mediaSourceStub(ManagedChannel mediaChannel) {
        return TorrentServiceGrpc.newBlockingStub(mediaChannel);
    }

    @Bean
    public MediaServiceGrpc.MediaServiceBlockingStub mediaItemStub(ManagedChannel mediaChannel) {
        return MediaServiceGrpc.newBlockingStub(mediaChannel);
    }

    @Bean
    public UserServiceGrpc.UserServiceBlockingStub userStub(ManagedChannel userChannel) {
        return UserServiceGrpc.newBlockingStub(userChannel);
    }

    @Bean
    public AdminUserServiceGrpc.AdminUserServiceBlockingStub adminUserStub(ManagedChannel userChannel) {
        return AdminUserServiceGrpc.newBlockingStub(userChannel);
    }

    @PreDestroy
    public void shutdown() {
        if (mediaChannel != null && !mediaChannel.isShutdown()) {
            mediaChannel.shutdown();
        }
        if (userChannel != null && !userChannel.isShutdown()) {
            userChannel.shutdown();
        }
    }
}
