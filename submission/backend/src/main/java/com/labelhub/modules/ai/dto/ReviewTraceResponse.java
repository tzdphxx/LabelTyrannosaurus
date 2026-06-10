package com.labelhub.modules.ai.dto;

import java.util.List;
import java.util.Map;

public record ReviewTraceResponse(String strategy,
                                  String strategyLabel,
                                  String summary,
                                  List<ReviewTraceStep> steps,
                                  Map<String, Object> metrics) {

    public record ReviewTraceStep(String name,
                                  String role,
                                  String decision,
                                  String score,
                                  String confidence,
                                  String status,
                                  String reason) {
    }
}
