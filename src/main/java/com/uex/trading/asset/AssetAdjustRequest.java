package com.uex.trading.asset;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AssetAdjustRequest {

    @NotBlank(message = "Asset cannot be blank")
    private String asset;

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.00000001", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private String description;
}
