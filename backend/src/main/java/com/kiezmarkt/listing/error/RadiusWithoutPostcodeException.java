package com.kiezmarkt.listing.error;

import org.springframework.http.HttpStatus;

public class RadiusWithoutPostcodeException extends ProblemException {
    public RadiusWithoutPostcodeException() {
        super(HttpStatus.BAD_REQUEST, ProblemTypes.RADIUS_WITHOUT_POSTCODE, "Radius filter needs a postcode",
                "radiusKm was supplied without postcode.");
    }
}
