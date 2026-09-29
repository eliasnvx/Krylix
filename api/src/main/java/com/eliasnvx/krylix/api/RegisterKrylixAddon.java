package com.eliasnvx.krylix.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link KrylixAddon} for discovery on NeoForge. Ignored on Fabric, which uses the
 * {@value KrylixAddon#FABRIC_ENTRYPOINT} entrypoint instead.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RegisterKrylixAddon {
}
