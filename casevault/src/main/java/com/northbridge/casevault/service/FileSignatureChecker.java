package com.northbridge.casevault.service;

import com.northbridge.casevault.model.EvidenceType;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

// Checks that the CONTENT of a file matches the type its extension claims
public final class FileSignatureChecker {

    private static final byte[] PDF_SIGNATURE = {'%', 'P', 'D', 'F', '-'};
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    private FileSignatureChecker() { }

    public static boolean matches(EvidenceType type, byte[] data) {
        if (data == null || data.length == 0) {
            return false;
        }
        if (type == EvidenceType.PDF) {
            return startsWith(data, PDF_SIGNATURE);
        }
        if (type == EvidenceType.PNG) {
            return startsWith(data, PNG_SIGNATURE);
        }
        return isCleanText(data);       // txt, log, csv
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    // Plain text only: valid UTF-8, no NUL bytes, no unusual control characters
    private static boolean isCleanText(byte[] data) {
        for (byte b : data) {
            int v = b & 0xFF;
            boolean allowedControl = (v == 9 || v == 10 || v == 13);
            if ((v < 0x20 && !allowedControl) || v == 0x7F) {
                return false;
            }
        }
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            decoder.decode(ByteBuffer.wrap(data));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }
}