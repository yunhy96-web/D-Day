package com.dday.family.common;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;

/** 업로드 이미지 공통: 크기 상한 읽기 · JPEG 검증 · 재인코딩 · 원자적 저장. */
public final class Images {
    private Images() {}

    /** max 를 넘으면 BadRequest("size") */
    public static byte[] read(InputStream in, int max) throws IOException {
        byte[] b = in.readNBytes(max + 1);
        if (b.length > max) throw new BadRequest("size");
        return b;
    }

    public static boolean isJpeg(byte[] b) {
        return b.length > 3 && (b[0] & 0xff) == 0xFF && (b[1] & 0xff) == 0xD8 && (b[2] & 0xff) == 0xFF;
    }

    /** 실제로 디코딩되는 JPEG 만. 디코딩 전에 크기부터 확인(작은 파일로 거대 픽셀을 만드는 압축 폭탄 방지). 아니면 BadRequest("image") */
    public static BufferedImage decodeJpeg(byte[] b, int maxSide) {
        if (!isJpeg(b)) throw new BadRequest("image");
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(b))) {
            Iterator<ImageReader> it = ImageIO.getImageReaders(in);
            if (!it.hasNext()) throw new BadRequest("image");
            ImageReader r = it.next();
            try {
                r.setInput(in, true, true);
                if (r.getWidth(0) > maxSide || r.getHeight(0) > maxSide) throw new BadRequest("size");
                return r.read(0);
            } finally { r.dispose(); }
        } catch (IOException | RuntimeException e) {
            if (e instanceof BadRequest br) throw br;
            throw new BadRequest("image");
        }
    }

    /** 원본 확장자 판별. 모르는 형식이면 null */
    public static String originalExt(byte[] b) {
        if (isJpeg(b)) return "jpg";
        if (b.length > 8 && (b[0] & 0xff) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        // ponytail: ftyp 상자만 보므로 MP4·MOV 도 heic 로 분류된다. 동영상(설계 §15 2단계) 넣을 때 브랜드(heic·heix·mif1) 확인으로
        if (b.length > 12 && b[4] == 'f' && b[5] == 't' && b[6] == 'y' && b[7] == 'p') return "heic";
        return null;
    }

    /** 긴 변을 maxSide 이하로 줄여(키우지 않음) 메타데이터 없는 JPEG 로 */
    public static byte[] encodeJpeg(BufferedImage src, int maxSide, float quality) throws IOException {
        double k = Math.min(1.0, (double) maxSide / Math.max(src.getWidth(), src.getHeight()));
        int w = Math.max(1, (int) Math.round(src.getWidth() * k)), h = Math.max(1, (int) Math.round(src.getHeight() * k));
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(bos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(out, null, null), param);
        } finally { writer.dispose(); }
        return bos.toByteArray();
    }

    /** 임시 파일에 쓰고 rename. 반쯤 쓴 파일이 서빙되지 않게 */
    public static void save(Path dir, String name, byte[] data) throws IOException {
        Files.createDirectories(dir);
        Path tmp = Files.createTempFile(dir, "up", ".tmp");
        Files.write(tmp, data);
        Files.move(tmp, dir.resolve(name), StandardCopyOption.ATOMIC_MOVE);
    }
}
