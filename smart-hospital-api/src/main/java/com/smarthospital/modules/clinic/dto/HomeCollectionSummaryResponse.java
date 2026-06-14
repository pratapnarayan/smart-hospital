package com.smarthospital.modules.clinic.dto;

import java.util.List;
import java.util.Map;

public record HomeCollectionSummaryResponse(
        long total,
        Map<String, Long> byStatus,
        List<HomeCollectionResponse> collections
) {}
