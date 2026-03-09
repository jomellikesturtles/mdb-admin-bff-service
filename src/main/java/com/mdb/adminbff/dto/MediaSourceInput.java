package com.mdb.adminbff.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaSourceInput implements Serializable {
    private static final long serialVersionUID = 1L;

    private String imdbId;

    @NotBlank(message = "Name is required")
    private String name;
    
    private String sourceHash;
    
    private String resourceUri;
    
    private Long size;
    
    private String status;
    
    private Double progress;
}
