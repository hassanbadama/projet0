package com.devoir.demo.dto;


import lombok.Data;
import lombok.NoArgsConstructor;

import com.devoir.demo.model.AhpResult;

import lombok.AllArgsConstructor;
// import com.ahp.model.AhpResult;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AhpResponseDto {
    private boolean success;
    private String message;
    private AhpResult result;
}