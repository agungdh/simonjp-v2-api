package id.my.agungdh.simonjp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = "[A-Za-z0-9._-]+", message = "username hanya boleh huruf, angka, titik, underscore, dan dash")
    String username,

    @NotBlank
    @Size(max = 255)
    String password
) {
}
