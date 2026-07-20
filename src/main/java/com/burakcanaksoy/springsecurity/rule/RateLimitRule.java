package com.burakcanaksoy.springsecurity.rule;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class RateLimitRule {
    private String pathPattern;
    private int capacity;
    private Duration refillDuration;
}
