package examples.resources;

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import io.dropwizard.jersey.errors.ErrorMessage;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * {@link ExceptionMapper} for mapping {@link IllegalArgumentException} (typically thrown from
 * {@link java.util.UUID#fromString(String)}) to responses.
 */
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

  private final Meter exceptions;
  public IllegalArgumentExceptionMapper(MetricRegistry metrics) {
    exceptions = metrics.meter(name(getClass(), "exceptions"));
  }

  @Override
  public Response toResponse(IllegalArgumentException exception) {
    exceptions.mark();
    return Response.status(Status.BAD_REQUEST)
      .type(MediaType.APPLICATION_JSON_TYPE)
      .entity(new ErrorMessage(Status.BAD_REQUEST.getStatusCode(),
        "invalid argument; verify id matches UUID format"))
      .build();
  }
}
