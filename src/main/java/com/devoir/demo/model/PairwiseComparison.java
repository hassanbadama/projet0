package com.devoir.demo.model;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PairwiseComparison {
    private String criterion1;
    private String criterion2;
    private Double value;
}