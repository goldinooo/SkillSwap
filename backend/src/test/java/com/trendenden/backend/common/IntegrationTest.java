package com.trendenden.backend.common;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Full-context test against the real database and the real security chains.
 *
 * <p>{@code @Transactional} rolls each test back. MockMvc invokes the controller on the
 * test's own thread, so the test's transaction is the one the service joins and nothing
 * survives the method. That is what lets these run repeatedly against the Compose
 * database without leaving rows behind or depending on test order.
 *
 * <p>Requires {@code docker compose up -d}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public @interface IntegrationTest {
}
