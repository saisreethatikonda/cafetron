package com.cafetron.menu.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

//Shape of data the client SENDS to create or edit an item.

public record MenuItemRequest(
        @NotBlank String itemName,
        @Positive double price,
        @Min(0) int stock,
        @NotBlank String foodType,
        @NotNull Long vendorId
) {}
