package com.eliasnvx.krylix;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Krylix {
    public static final String MOD_ID = "krylix";
    public static final String MOD_NAME = "Krylix";

    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    public static void init() {
        LOGGER.info("Initializing " + MOD_NAME);
    }
}
