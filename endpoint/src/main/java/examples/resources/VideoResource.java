package examples.resources;

import com.codahale.metrics.annotation.Counted;
import com.codahale.metrics.annotation.Timed;
import com.google.common.hash.BloomFilter;
import examples.Video;
import examples.VideoApi;
import examples.VideoDao;
import io.dropwizard.jersey.errors.ErrorMessage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
public class VideoResource implements VideoApi {

  private final VideoDao videoDao;
  private final VideoBloomFilterManager bloomFilterManager;
  public VideoResource(VideoDao videoDao, VideoBloomFilterManager bloomFilterManager) {
    this.videoDao = videoDao;
    this.bloomFilterManager = bloomFilterManager;
  }

  @Override
  @ApiOperation("Get a video")
  @GET @Path("{videoId}")
  @Timed
  public Video get(@PathParam("videoId") String videoId) {
    return videoDao.retrieve(UUID.fromString(videoId));
  }

  @Override
  @ApiOperation("Create a new video")
  @Timed
  public Video create(Video video) {
    return videoDao.save(video);
  }

  @Override
  @ApiOperation("Update an existing video")
  @Timed
  public Video update(Video video) {
    return videoDao.save(video);
  }

  /**
   * Returns a Bloom Filter of known {@link Video#getVideoId()}s.
   *
   * @return the serialized bloomFilter, if available
   */
  @GET @Path("bloomFilter")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @ApiOperation("Get a Bloom Filter of video IDs")
  @Timed
  public Response getBloomFilter() {
    BloomFilter<String> filter = bloomFilterManager.getVideoIdFilter();
    if(filter != null) {
      return Response.ok(new StreamingOutput() {
        @Override
        public void write(OutputStream output) throws IOException, WebApplicationException {
          filter.writeTo(output);
        }
      }).build();
    }

    return Response.noContent().build();
  }

  /**
   * Request that the server update the Bloom Filter of known {@link Video#getVideoId()}s.
   * Implementations are likely to be asynchronous.
   *
   * @return status response
   */
  @POST @Path("bloomFilter")
  @ApiOperation("Update the Bloom Filter (async)")
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
        .entity(new ErrorMessage(
          Status.BAD_REQUEST.getStatusCode(),
          "Update requests are rate limited; try again after " + bloomFilterManager.getUpdateThreshold()))
        .build();
    }
  }
}
