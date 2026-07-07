package com.gui.car_rental_payment_service.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.gui.car_rental_payment_service.dtos.MockPixResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.UUID;

public class MockPixService {
    public static MockPixResult generate(UUID sagaId, String baseUrl) throws Exception {
        String confirmUrl = baseUrl + "/payments/" + sagaId + "/mock-confirm";

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(confirmUrl, BarcodeFormat.QR_CODE, 300, 300);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(bitMatrix);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());

        return new MockPixResult(base64, confirmUrl);
    }
}
