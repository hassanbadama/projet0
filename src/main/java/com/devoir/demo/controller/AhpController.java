package com.devoir.demo.controller;


// import com.ahp.dto.AhpRequestDto;
// import com.ahp.dto.AhpResponseDto;
// import com.ahp.model.AhpResult;
// import com.ahp.service.AhpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.devoir.demo.dto.AhpRequestDto;
import com.devoir.demo.dto.AhpResponseDto;
import com.devoir.demo.model.AhpResult;
import com.devoir.demo.service.AhpService;

import jakarta.servlet.http.HttpSession;
import java.util.*;

@Controller
public class AhpController {
    
    @Autowired
    private AhpService ahpService;
    
    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        // Initialiser les sessions
        session.setAttribute("criteria", new ArrayList<String>());
        session.setAttribute("alternatives", new ArrayList<String>());
        session.setAttribute("scores", new HashMap<String, Map<String, Double>>());
        session.setAttribute("pairwiseMatrix", new HashMap<String, Map<String, Double>>());
        
        model.addAttribute("saatyScale", ahpService.getSaatyScale());
        return "index";
    }
    
    @PostMapping("/api/criteria")
    @ResponseBody
    public Map<String, Object> addCriteria(@RequestBody Map<String, List<String>> request, HttpSession session) {
        List<String> criteria = request.get("criteria");
        session.setAttribute("criteria", criteria);
        
        // Initialiser la matrice de comparaison
        Map<String, Map<String, Double>> pairwiseMatrix = new HashMap<>();
        for (String c1 : criteria) {
            pairwiseMatrix.put(c1, new HashMap<>());
            for (String c2 : criteria) {
                if (c1.equals(c2)) {
                    pairwiseMatrix.get(c1).put(c2, 1.0);
                } else {
                    pairwiseMatrix.get(c1).put(c2, 1.0);
                }
            }
        }
        session.setAttribute("pairwiseMatrix", pairwiseMatrix);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("criteria", criteria);
        return response;
    }
    
    @PostMapping("/api/alternatives")
    @ResponseBody
    public Map<String, Object> addAlternatives(@RequestBody Map<String, List<String>> request, HttpSession session) {
        List<String> alternatives = request.get("alternatives");
        session.setAttribute("alternatives", alternatives);
        
        // Initialiser les scores
        List<String> criteria = (List<String>) session.getAttribute("criteria");
        Map<String, Map<String, Double>> scores = new HashMap<>();
        for (String criterion : criteria) {
            scores.put(criterion, new HashMap<>());
            for (String alternative : alternatives) {
                scores.get(criterion).put(alternative, 0.0);
            }
        }
        session.setAttribute("scores", scores);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("alternatives", alternatives);
        return response;
    }
    
    @PostMapping("/api/scores")
    @ResponseBody
    public Map<String, Object> saveScores(@RequestBody Map<String, Map<String, Map<String, Double>>> request, 
                                          HttpSession session) {
        Map<String, Map<String, Double>> scores = request.get("scores");
        session.setAttribute("scores", scores);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return response;
    }
    
    @PostMapping("/api/pairwise")
    @ResponseBody
    public Map<String, Object> updatePairwise(@RequestBody Map<String, Object> request, HttpSession session) {
        String criterion1 = (String) request.get("criterion1");
        String criterion2 = (String) request.get("criterion2");
        Double value = ((Number) request.get("value")).doubleValue();
        
        Map<String, Map<String, Double>> pairwiseMatrix = 
            (Map<String, Map<String, Double>>) session.getAttribute("pairwiseMatrix");
        
        pairwiseMatrix.get(criterion1).put(criterion2, value);
        pairwiseMatrix.get(criterion2).put(criterion1, 1.0 / value);
        
        session.setAttribute("pairwiseMatrix", pairwiseMatrix);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return response;
    }
    
    @PostMapping("/api/calculate")
    @ResponseBody
    public AhpResponseDto calculate(@RequestBody AhpRequestDto request) {
        try {
            AhpResult result = ahpService.calculateAhp(request);
            return new AhpResponseDto(true, "Calcul effectué avec succès", result);
        } catch (Exception e) {
            return new AhpResponseDto(false, "Erreur lors du calcul: " + e.getMessage(), null);
        }
    }
    
    @GetMapping("/session-data")
    @ResponseBody
    public Map<String, Object> getSessionData(HttpSession session) {
        Map<String, Object> data = new HashMap<>();
        data.put("criteria", session.getAttribute("criteria"));
        data.put("alternatives", session.getAttribute("alternatives"));
        data.put("scores", session.getAttribute("scores"));
        data.put("pairwiseMatrix", session.getAttribute("pairwiseMatrix"));
        return data;
    }
    
    @PostMapping("/reset")
    public String reset(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}