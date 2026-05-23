package com.portfolio.auth.client;

import com.portfolio.auth.core.protocol.dto.ServiceResponse;

public final class ClientCli {
    private ClientCli() {
    }

    public static void main(String[] args) throws Exception {
        ServiceResponse response = AuthFlowRunner.localDemo().run();
        System.out.println(response.serviceMessage());
    }
}
