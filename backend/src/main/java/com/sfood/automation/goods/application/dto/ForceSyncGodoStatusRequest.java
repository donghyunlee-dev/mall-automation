package com.sfood.automation.goods.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record ForceSyncGodoStatusRequest(
    @NotEmpty List<@NotBlank String> godoGoodsCodes,
    @NotBlank @Pattern(regexp = "Y|N") String targetSoldOutYn
) {
}
