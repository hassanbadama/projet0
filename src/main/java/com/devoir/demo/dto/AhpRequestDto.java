package com.devoir.demo.dto;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AhpRequestDto {
    private List<String> criteria;
    private List<String> alternatives;
    private Map<String, Map<String, Double>> scores;  // criterion -> alternative -> score
    private Map<String, Map<String, Double>> pairwiseMatrix;  // criterion1 -> criterion2 -> value
}
