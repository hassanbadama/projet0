package com.devoir.demo.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Score {
    private String criterionName;
    private String alternativeName;
    private Double value;
}