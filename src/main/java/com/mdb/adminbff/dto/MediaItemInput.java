package com.mdb.adminbff.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MediaItemInput {
    @NotBlank
    private String title;
    private String description;
    private String posterUrl;
    private Integer releaseYear;
    private String status;
}
