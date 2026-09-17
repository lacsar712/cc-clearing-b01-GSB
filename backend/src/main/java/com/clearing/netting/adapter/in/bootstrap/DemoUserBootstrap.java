package com.clearing.netting.adapter.in.bootstrap;

import com.clearing.netting.application.AuthApplicationService;
import com.clearing.netting.domain.model.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoUserBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUserBootstrap.class);

    private final AuthApplicationService authService;

    public DemoUserBootstrap(AuthApplicationService authService) {
        this.authService = authService;
    }

    @Override
    public void run(ApplicationArguments args) {
        authService.ensureUser("operator", "op123456", UserRole.OPERATOR);
        authService.ensureUser("viewer", "view123456", UserRole.VIEWER);
        log.info("Demo users ensured: operator, viewer");
    }
}
