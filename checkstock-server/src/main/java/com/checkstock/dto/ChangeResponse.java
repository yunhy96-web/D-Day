package com.checkstock.dto;

import com.checkstock.entity.SizeChange;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChangeResponse {
    private String id;
    private String productId;
    private List<SizeOptionDto> becameAvailable;
    private List<SizeOptionDto> becameSoldOut;
    private int oldAvailableCount;
    private int newAvailableCount;
    private LocalDateTime detectedAt;

    private static final ObjectMapper mapper = new ObjectMapper();

    public static ChangeResponse from(SizeChange change) {
        ChangeResponse res = new ChangeResponse();
        res.setId(change.getId());
        res.setProductId(change.getProductId());
        res.setOldAvailableCount(change.getOldAvailableCount());
        res.setNewAvailableCount(change.getNewAvailableCount());
        res.setDetectedAt(change.getDetectedAt());
        try {
            res.setBecameAvailable(mapper.readValue(change.getBecameAvailable(), new TypeReference<>() {}));
        } catch (Exception e) {
            res.setBecameAvailable(List.of());
        }
        try {
            res.setBecameSoldOut(mapper.readValue(change.getBecameSoldOut(), new TypeReference<>() {}));
        } catch (Exception e) {
            res.setBecameSoldOut(List.of());
        }
        return res;
    }
}
