package com.devoir.demo.service;


// import com.ahp.dto.AhpRequestDto;
// import com.ahp.model.AhpResult;
import org.springframework.stereotype.Service;

import com.devoir.demo.dto.AhpRequestDto;
import com.devoir.demo.model.AhpResult;

import java.util.*;

@Service
public class AhpService {
    
    private static final double CONSISTENCY_THRESHOLD = 0.1;
    
    // Échelle de Saaty
    private static final Map<Integer, String> SAATY_SCALE = Map.of(
        1, "Également important",
        2, "Faiblement plus important",
        3, "Modérément plus important",
        4, "Plus important",
        5, "Fortement plus important",
        6, "Très fortement plus important",
        7, "Très fortement important",
        8, "Extrêmement plus important",
        9, "Extrêmement important"
    );
    
    // Indices de consistance aléatoire pour n jusqu'à 15
    private static final double[] RANDOM_INDEX = {
        0, 0, 0.58, 0.9, 1.12, 1.24, 1.32, 1.41, 1.45, 1.49, 1.51, 1.48, 1.56, 1.57, 1.59
    };
    
    /**
     * Calcule la matrice de décision AHP
     */
    public AhpResult calculateAhp(AhpRequestDto request) {
        List<String> criteria = request.getCriteria();
        List<String> alternatives = request.getAlternatives();
        Map<String, Map<String, Double>> pairwiseMatrix = request.getPairwiseMatrix();
        Map<String, Map<String, Double>> scores = request.getScores();
        
        // Étape 1: Calculer les poids des critères
        double[][] matrix = convertToMatrix(criteria, pairwiseMatrix);
        double[] criteriaWeights = calculatePriorityVector(matrix);
        
        // Étape 2: Vérifier la consistance
        Map<String, Object> consistencyResult = checkConsistency(matrix, criteriaWeights);
        boolean isConsistent = (boolean) consistencyResult.get("isConsistent");
        double cr = (double) consistencyResult.get("cr");
        double lambdaMax = (double) consistencyResult.get("lambdaMax");
        double ci = (double) consistencyResult.get("ci");
        
        // Étape 3: Normaliser les scores des alternatives
        Map<String, Map<String, Double>> normalizedScores = normalizeScores(criteria, alternatives, scores);
        
        // Étape 4: Calculer les scores finaux des alternatives
        Map<String, Double> finalScores = calculateFinalScores(criteria, alternatives, criteriaWeights, normalizedScores);
        
        // Étape 5: Trouver la meilleure alternative
        String bestAlternative = "";
        double bestScore = -1;
        for (Map.Entry<String, Double> entry : finalScores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestAlternative = entry.getKey();
            }
        }
        
        // Étape 6: Préparer les poids des critères
        Map<String, Double> criteriaWeightMap = new HashMap<>();
        for (int i = 0; i < criteria.size(); i++) {
            criteriaWeightMap.put(criteria.get(i), criteriaWeights[i]);
        }
        
        // Étape 7: Générer les raisons d'inconsistance si nécessaire
        List<String> inconsistencyReasons = new ArrayList<>();
        if (!isConsistent) {
            inconsistencyReasons = generateInconsistencyReasons(matrix, criteriaWeights, cr);
        }
        
        return new AhpResult(
            isConsistent, cr, lambdaMax, ci,
            criteriaWeightMap, finalScores,
            bestAlternative, bestScore,
            inconsistencyReasons
        );
    }
    
    /**
     * Convertit la matrice de comparaison par paires en tableau 2D
     */
    private double[][] convertToMatrix(List<String> criteria, Map<String, Map<String, Double>> pairwiseMatrix) {
        int n = criteria.size();
        double[][] matrix = new double[n][n];
        
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    matrix[i][j] = 1.0;
                } else {
                    String crit1 = criteria.get(i);
                    String crit2 = criteria.get(j);
                    if (pairwiseMatrix.containsKey(crit1) && pairwiseMatrix.get(crit1).containsKey(crit2)) {
                        matrix[i][j] = pairwiseMatrix.get(crit1).get(crit2);
                    } else {
                        matrix[i][j] = 1.0 / pairwiseMatrix.get(crit2).get(crit1);
                    }
                }
            }
        }
        return matrix;
    }
    
    /**
     * Calcule le vecteur priorité (poids normalisés)
     * Méthode de la moyenne normalisée
     */
    private double[] calculatePriorityVector(double[][] matrix) {
        int n = matrix.length;
        
        // Normaliser la matrice
        double[][] normalized = new double[n][n];
        for (int j = 0; j < n; j++) {
            double colSum = 0;
            for (int i = 0; i < n; i++) {
                colSum += matrix[i][j];
            }
            for (int i = 0; i < n; i++) {
                normalized[i][j] = matrix[i][j] / colSum;
            }
        }
        
        // Calculer la moyenne de chaque ligne
        double[] priority = new double[n];
        for (int i = 0; i < n; i++) {
            double rowSum = 0;
            for (int j = 0; j < n; j++) {
                rowSum += normalized[i][j];
            }
            priority[i] = rowSum / n;
        }
        
        return priority;
    }
    
    /**
     * Vérifie la consistance de la matrice
     */
    private Map<String, Object> checkConsistency(double[][] matrix, double[] priority) {
        int n = matrix.length;
        
        // Calculer lambda max
        double lambdaMax = 0;
        for (int i = 0; i < n; i++) {
            double rowSum = 0;
            for (int j = 0; j < n; j++) {
                rowSum += matrix[i][j] * priority[j];
            }
            lambdaMax += rowSum / priority[i];
        }
        lambdaMax /= n;
        
        // Indice de consistance
        double ci = (lambdaMax - n) / (n - 1);
        
        // Ratio de consistance
        double ri = n <= RANDOM_INDEX.length ? RANDOM_INDEX[n - 1] : 1.59;
        double cr = ci / ri;
        
        boolean isConsistent = cr < CONSISTENCY_THRESHOLD;
        
        Map<String, Object> result = new HashMap<>();
        result.put("isConsistent", isConsistent);
        result.put("cr", cr);
        result.put("lambdaMax", lambdaMax);
        result.put("ci", ci);
        
        return result;
    }
    
    /**
     * Normalise les scores des alternatives
     */
    private Map<String, Map<String, Double>> normalizeScores(
            List<String> criteria, 
            List<String> alternatives,
            Map<String, Map<String, Double>> scores) {
        
        Map<String, Map<String, Double>> normalized = new HashMap<>();
        
        for (String criterion : criteria) {
            normalized.put(criterion, new HashMap<>());
            
            // Calculer la somme des scores pour ce critère
            double sum = 0;
            for (String alternative : alternatives) {
                Double score = scores.getOrDefault(criterion, new HashMap<>())
                                     .getOrDefault(alternative, 0.0);
                sum += score;
            }
            
            // Normaliser
            if (sum > 0) {
                for (String alternative : alternatives) {
                    Double score = scores.getOrDefault(criterion, new HashMap<>())
                                         .getOrDefault(alternative, 0.0);
                    normalized.get(criterion).put(alternative, score / sum);
                }
            } else {
                // Distribution égale si tous les scores sont 0
                double equalShare = 1.0 / alternatives.size();
                for (String alternative : alternatives) {
                    normalized.get(criterion).put(alternative, equalShare);
                }
            }
        }
        
        return normalized;
    }
    
    /**
     * Calcule les scores finaux des alternatives
     */
    private Map<String, Double> calculateFinalScores(
            List<String> criteria,
            List<String> alternatives,
            double[] criteriaWeights,
            Map<String, Map<String, Double>> normalizedScores) {
        
        Map<String, Double> finalScores = new HashMap<>();
        
        for (String alternative : alternatives) {
            double total = 0;
            for (int i = 0; i < criteria.size(); i++) {
                String criterion = criteria.get(i);
                total += criteriaWeights[i] * normalizedScores.get(criterion).get(alternative);
            }
            finalScores.put(alternative, total);
        }
        
        return finalScores;
    }
    
    /**
     * Génère les raisons possibles d'inconsistance
     */
    private List<String> generateInconsistencyReasons(double[][] matrix, double[] priority, double cr) {
        List<String> reasons = new ArrayList<>();
        int n = matrix.length;
        
        reasons.add(String.format("Le Ratio de Consistance (CR = %.4f) dépasse le seuil acceptable de %.1f", 
                                  cr, CONSISTENCY_THRESHOLD));
        
        // Vérifier les écarts importants
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double expected = priority[i] / priority[j];
                double actual = matrix[i][j];
                double ratio = Math.abs(actual - expected) / expected;
                if (ratio > 2) {
                    reasons.add(String.format("Écart important entre %s et %s (attendu: %.2f, saisi: %.2f)",
                                             getCriterionName(i), getCriterionName(j), expected, actual));
                }
            }
        }
        
        // Vérifier les incohérences de transitivité
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                for (int k = j + 1; k < n; k++) {
                    double product = matrix[i][j] * matrix[j][k];
                    if (Math.abs(product - matrix[i][k]) / matrix[i][k] > 2) {
                        reasons.add(String.format("Problème de transitivité entre %s, %s et %s",
                                                  getCriterionName(i), getCriterionName(j), getCriterionName(k)));
                    }
                }
            }
        }
        
        reasons.add("Solutions: Réviser les comparaisons incohérentes, réduire les écarts extrêmes, " +
                   "vérifier la logique de transitivité des préférences");
        
        return reasons;
    }
    
    private String getCriterionName(int index) {
        String[] names = {"Critère A", "Critère B", "Critère C", "Critère D", "Critère E"};
        return index < names.length ? names[index] : "Critère " + (char)('A' + index);
    }
    
    public Map<Integer, String> getSaatyScale() {
        return SAATY_SCALE;
    }
}