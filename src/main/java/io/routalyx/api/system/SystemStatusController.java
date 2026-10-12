package io.routalyx.api.system;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemStatusController {

    @GetMapping("/status")
    public SystemStatusResponse getStatus() {
        return new SystemStatusResponse(
                "Routalyx",
                "UP",
                "0.1.0"
        );
    }
}
