package com.devoir.demo.model;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Map;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AhpResult {
    private boolean consistent;
    private Double consistencyRatio;
    private Double lambdaMax;
    private Double consistencyIndex;
    private Map<String, Double> criteriaWeights;
    private Map<String, Double> alternativeScores;
    private String bestAlternative;
    private Double bestScore;
    private List<String> inconsistencyReasons;
}
