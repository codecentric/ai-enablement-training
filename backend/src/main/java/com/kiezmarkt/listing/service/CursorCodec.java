package com.kiezmarkt.listing.service;

import com.kiezmarkt.listing.error.InvalidCursorException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Opaque, base64-encoded page offsets. A client must not construct, parse or
 * persist a cursor itself (contracts/api.yaml); this codec is the only place
 * that does.
 */
public final class CursorCodec {

    private static final String PREFIX = "o:";

    private CursorCodec() {
    }

    public static String encode(int offset) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((PREFIX + offset).getBytes(StandardCharsets.UTF_8));
    }

    public static int decode(String cursor) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            if (!decoded.startsWith(PREFIX)) {
                throw new IllegalArgumentException();
            }
            int offset = Integer.parseInt(decoded.substring(PREFIX.length()));
            if (offset < 0) {
                throw new IllegalArgumentException();
            }
            return offset;
        } catch (RuntimeException e) {
            throw new InvalidCursorException();
        }
    }
}
