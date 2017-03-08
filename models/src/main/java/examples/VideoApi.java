package examples;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

/**
 * JAX-RS API for interacting with Videos.
 */
public interface VideoApi {

  /**
   * @param videoId target {@link Video#getVideoId()}
   * @return the matching {@link Video}, or null
   */
  @GET @Path("{videoId}")
  Video get(@PathParam("videoId") String videoId);

  /**
   * @param video the video to create
   * @return status response
   */
  @POST @Consumes(MediaType.APPLICATION_JSON)
  Video create(Video video);

  /**
   *
   * @param video the video to update
   * @return status response
   */
  @PUT @Consumes(MediaType.APPLICATION_JSON)
  Video update(Video video);

}
