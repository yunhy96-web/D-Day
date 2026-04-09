package com.checkstock.dto;

import com.checkstock.entity.SizeSnapshot;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SnapshotResponse {
    private String id;
    private String productId;
    private List<SizeOptionDto> sizes;
    private int availableCount;
    private int totalCount;
    private LocalDateTime checkedAt;

    private static final ObjectMapper mapper = new ObjectMapper();

    public static SnapshotResponse from(SizeSnapshot snapshot) {
        SnapshotResponse res = new SnapshotResponse();
        res.setId(snapshot.getId());
        res.setProductId(snapshot.getProductId());
        res.setAvailableCount(snapshot.getAvailableCount());
        res.setTotalCount(snapshot.getTotalCount());
        res.setCheckedAt(snapshot.getCheckedAt());
        try {
            res.setSizes(mapper.readValue(snapshot.getSizes(), new TypeReference<>() {}));
        } catch (Exception e) {
            res.setSizes(List.of());
        }
        return res;
    }
}
