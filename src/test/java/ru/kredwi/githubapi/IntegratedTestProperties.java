package ru.kredwi.githubapi;

import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

public class IntegratedTestProperties extends Properties {

    public IntegratedTestProperties() {

        try {
            load(
                    Objects.requireNonNull(getClass().getResourceAsStream("/integrated-test.properties"),
                            "Integrated file with properties is not found")
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (isEmpty())
            throw new IllegalStateException("Properties is empty after loading");
    }
}
