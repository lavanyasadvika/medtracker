package com.medtracker.external;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class ZXingService {

    // TRY_HARDER trades a little decode time for much better tolerance of the skew, glare and
    // low contrast typical of a phone photo of a blister strip — without it, MultiFormatReader
    // gives up after a single fast pass and misses barcodes that are clearly visible to the eye.
    private static final Map<DecodeHintType, Object> DECODE_HINTS = new EnumMap<>(DecodeHintType.class);
    static {
        DECODE_HINTS.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
    }

    public Optional<String> decode(MultipartFile image) {
        try {
            return decode(image.getBytes());
        } catch (IOException ex) {
            log.warn("Unable to read uploaded image for barcode decoding: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<String> decode(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return Optional.empty();
        }

        try {
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (bufferedImage == null) {
                return Optional.empty();
            }

            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(bufferedImage)));
            Result result = new MultiFormatReader().decode(bitmap, DECODE_HINTS);
            return Optional.ofNullable(result.getText()).filter(text -> !text.isBlank());
        } catch (NotFoundException ex) {
            log.debug("No barcode detected in uploaded image");
            return Optional.empty();
        } catch (IOException ex) {
            log.warn("Unable to decode uploaded image for barcode: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
