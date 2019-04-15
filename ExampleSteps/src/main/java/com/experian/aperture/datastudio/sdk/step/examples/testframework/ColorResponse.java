package com.experian.aperture.datastudio.sdk.step.examples.testframework;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Model representing the color response from the rest api.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ColorResponse {
    @JsonProperty("name")
    private final String name;

    @JsonProperty("color")
    private final String color;

    @JsonProperty("year")
    private final String year;

    @JsonCreator
    public ColorResponse(@JsonProperty("name") final String name,
                         @JsonProperty("color") final String color,
                         @JsonProperty("year") final String year) {
        this.name = name;
        this.color = color;
        this.year = year;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public String getYear() {
        return year;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ColorResponse that = (ColorResponse) o;
        return Objects.equals(name, that.name)
                && Objects.equals(color, that.color)
                && Objects.equals(year, that.year);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, color, year);
    }
}
