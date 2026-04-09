package com.checkstock.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SizeOptionDto {
    private String label;
    private boolean available;
    private String group;
}
