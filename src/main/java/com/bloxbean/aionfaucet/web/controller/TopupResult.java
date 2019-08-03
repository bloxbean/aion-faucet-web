package com.bloxbean.aionfaucet.web.controller;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopupResult {
    private String txHash;
    private String error;
}
