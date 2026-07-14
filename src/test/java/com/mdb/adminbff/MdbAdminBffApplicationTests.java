package com.mdb.adminbff;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import com.mdb.user_data_gateway_service.grpc.UserServiceGrpc;
import com.mdb.media_data_gateway_service.grpc.MediaServiceGrpc;
import com.mdb.media_data_gateway_service.grpc.TorrentServiceGrpc;

@SpringBootTest
@ActiveProfiles("test")
class MdbAdminBffApplicationTests {

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private com.mdb.user_data_gateway_service.grpc.AdminUserServiceGrpc.AdminUserServiceBlockingStub adminUserStub;

    @MockBean
    private UserServiceGrpc.UserServiceBlockingStub userStub;

    @MockBean
    private MediaServiceGrpc.MediaServiceBlockingStub mediaItemStub;

    @MockBean
    private TorrentServiceGrpc.TorrentServiceBlockingStub mediaSourceStub;

    @Test
    void contextLoads() {
    }

}
