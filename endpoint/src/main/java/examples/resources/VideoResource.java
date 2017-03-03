package examples.resources;

import com.codahale.metrics.annotation.Counted;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import com.google.common.hash.BloomFilter;
import examples.Video;
import examples.VideoDao;
import io.dropwizard.jersey.errors.ErrorMessage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

/**
 * REST API for {@link Video}s.
 */
@Api("Video API")
@Path("videos")
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class VideoResource {

  private final VideoDao videoDao;
  private final VideoBloomFilterManager bloomFilterManager;
  public VideoResource(VideoDao videoDao, VideoBloomFilterManager bloomFilterManager) {
    this.videoDao = videoDao;
    this.bloomFilterManager = bloomFilterManager;
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
  @Timed
  public Response update(Video video) {
    videoDao.save(video);
    return Response.ok().build();
  }

  @ApiOperation("Get a Bloom Filter of video IDs")
  @GET @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Path("bloomFilter")
  @Timed
  public StreamingOutput getBloomFilter() {
    BloomFilter<String> filter = bloomFilterManager.getVideoIdFilter();
    if(filter != null) {
      return new StreamingOutput() {
        @Override
        public void write(OutputStream output) throws IOException, WebApplicationException {
          filter.writeTo(output);
        }
      };
    }

    return null;
  }

  @ApiOperation("Update the Bloom Filter (async)")
  @POST
  @Path("bloomFilter")
  @Counted
  public Response updateBloomFilter() {
    boolean accepted = bloomFilterManager.updateFilter();
    if(accepted) {
      return Response
        .ok(new ErrorMessage(Status.OK.getStatusCode(), null))
        .build();
    } else {
      return Response
        .status(Status.BAD_REQUEST)
        .entity(new ErrorMessage(Status.BAD_REQUEST.getStatusCode(), "Update requests are rate limited; try again later"))
        .build();
    }
  }
}
