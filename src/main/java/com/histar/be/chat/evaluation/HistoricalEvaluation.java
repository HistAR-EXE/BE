package com.histar.be.chat.evaluation;

import java.util.List;

public record HistoricalEvaluation(boolean passed, List<String> reasons) {}
