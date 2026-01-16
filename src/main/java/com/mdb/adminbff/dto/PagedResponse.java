package com.mdb.adminbff.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {
    private Integer page;
    private Integer limit;
    private Integer totalDocs;
    private Integer totalPages;
    private java.util.List<T> items;
}
