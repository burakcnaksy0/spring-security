package com.burakcanaksoy.springsecurity.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;
@Data
@Builder
public class MfaEnableResponse {
    private String message;
    private List<String> recoveryCodes;
}