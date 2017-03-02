package examples.resources;

import com.codahale.metrics.annotation.Timed;
import examples.Video;
import examples.VideoDao;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.UUID;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST API for {@link Video}s.
 */
@Api("Video API")
@Path("videos")
@Produces(MediaType.APPLICATION_JSON)
public class VideoResource {

  private final VideoDao videoDao;

  public VideoResource(VideoDao videoDao) {
    this.videoDao = videoDao;
  }

  @ApiOperation("Get a video")
  @GET
  @Path("{videoId}")
  @Timed
  public Video get(@PathParam("videoId") String videoId) {
    return videoDao.retrieve(UUID.fromString(videoId));
  }

  @ApiOperation("Create a new video")
  @POST @Consumes(MediaType.APPLICATION_JSON)
  @Timed
  public Response create(Video video) {
    videoDao.save(video);
    return Response.ok().build();
  }

  @ApiOperation("Update an existing video")
  @PUT @Consumes(MediaType.APPLICATION_JSON)
  @Path("{videoId}")
  @Timed
  public Response update(@PathParam("videoId") String videoId, Video video) {
    videoDao.save(video);
    return Response.ok().build();
  }
}
