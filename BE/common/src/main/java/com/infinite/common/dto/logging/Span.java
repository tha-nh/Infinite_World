package com.infinite.common.dto.logging;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Span {
    private String traceId;
    private String spanId;
    private String parentSpanId;
    private String name;
    private long startTime;
    private long endTime;

    public long getDuration() {
        return endTime - startTime;
    }

}