package com.burakcanaksoy.springsecurity.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TotpSetupResponse {
    private String secret;
    private String qrCodeImage;
}