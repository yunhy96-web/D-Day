package com.thaiuniontour.common.storage;

/**
 * 파일 저장 포트 (확장 seam). 상품 이미지 업로드 등.
 * 구현 교체로 무수정 확장: LocalVolumeStorage(MVP, 06-infra.md) → S3/MinIO(추후).
 */
public interface FileStorage {

    /**
     * 파일을 저장하고 접근 가능한 URL을 반환한다.
     *
     * @param path        저장 경로/키 (예: "products/{id}/main.jpg")
     * @param content     파일 바이트
     * @param contentType MIME 타입
     * @return 접근 URL (예: "/uploads/products/{id}/main.jpg")
     */
    String store(String path, byte[] content, String contentType);
}
