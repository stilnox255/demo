package de.ingoschindler.infrastructure;

import de.ingoschindler.infrastructure.web.errors.DatabaseExceptionMapper;
import de.ingoschindler.kernel.problem.ProblemDetails;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

class DatabaseExceptionMapperTest {

    DatabaseExceptionMapper mapper = new DatabaseExceptionMapper();

    @Test
    void constraintViolationReturns409WithProblemType() {
        var cause = mock(org.hibernate.exception.ConstraintViolationException.class);
        var ex = new jakarta.persistence.PersistenceException("constraint violation", cause);

        var response = mapper.toResponse(ex);

        assertEquals(409, response.getStatus());
        var body = (ProblemDetails) response.getEntity();
        assertEquals(URI.create("urn:starter:error:constraint-violation"), body.type());
        assertEquals(409, body.status());
    }

    @Test
    void generalPersistenceExceptionReturns500WithoutInternalDetails() {
        var ex = new jakarta.persistence.PersistenceException("SELECT * FROM users WHERE password='secret'");

        var response = mapper.toResponse(ex);

        assertEquals(500, response.getStatus());
        var body = (ProblemDetails) response.getEntity();
        assertEquals("An unexpected database error occurred", body.detail());
        assertFalse(body.detail().contains("secret"));
    }
}
