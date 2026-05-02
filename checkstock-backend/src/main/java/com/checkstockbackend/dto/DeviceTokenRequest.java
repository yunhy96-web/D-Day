package com.checkstockbackend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceTokenRequest {
    private String expoPushToken;
    private String platform;
}
