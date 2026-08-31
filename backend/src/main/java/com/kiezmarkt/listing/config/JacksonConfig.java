package com.kiezmarkt.listing.config;

import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Money is an integer number of cents, never a float, at any layer
 * (domain.md invariant 3). Jackson's default coercion silently truncates a
 * JSON float like {@code 25.99} into a {@code Long} field as {@code 25}
 * instead of rejecting it; this makes that a hard failure instead, so a
 * float price is a {@code 400 validation-failed} as contracts/api.yaml
 * requires.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer moneyCoercionCustomizer() {
        return builder -> builder.postConfigurer(mapper ->
                mapper.coercionConfigFor(LogicalType.Integer)
                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail));
    }
}
