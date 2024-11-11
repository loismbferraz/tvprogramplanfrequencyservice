package com.challenge.tvprogramplanfrequencyservice.api.openapi;

/** OpenApi information. */
public final class OpenApi {

  private OpenApi() {}

  public static final class StatusCode {
    /** HTTP response code 200. */
    public static final String OK = "200";

    /** HTTP response code 204. */
    public static final String NO_CONTENT = "204";

    /** HTTP response code 400. */
    public static final String BAD_REQUEST = "400";

    /** HTTP response code 404. */
    public static final String NOT_FOUND = "404";

    /** HTTP response code 500. */
    public static final String INTERNAL_SERVER_ERROR = "500";

    /** HTTP response code 503. */
    public static final String SERVICE_UNAVAILABLE = "503";

    /** Default constructor. */
    private StatusCode() {}
  }

  public static final class ResponseEntity {
    /** Example response for aggregated TV shows by TV show. */
    public static final String AGGREGATED_BY_TVSHOW =
        """
                          [
                            {
                              "id": "eb11b3d1-dff7-4136-bbea-d3756f90cde0",
                              "title": "The Voice of Germany",
                              "description": "Competition to find Great Singers",
                              "tvShowAirings": [
                                {
                                  "id": "e9291a95-c8f1-44ec-a734-9e745162dd71",
                                  "season": 4,
                                  "episode": 17,
                                  "startTime": "2024-10-01T11:30:00+02:00",
                                  "endTime": "2024-10-01T12:25:00+02:00"
                                },
                                {
                                  "id": "b989155a-8cf2-45e3-b1eb-6b049e8a25a6",
                                  "season": 3,
                                  "episode": 11,
                                  "startTime": "2024-10-01T06:30:00+02:00",
                                  "endTime": "2024-10-01T07:25:00+02:00"
                                }
                              ]
                            },
                            {
                              "id": "b1742fdd-348d-48cf-8dc6-9fb43cb97239",
                              "title": "Germany's Next Topmodel",
                              "description": "Competition to find Germany's next top model",
                              "tvShowAirings": [
                                {
                                  "id": "95d3449d-9cdc-44d2-bd97-881ab58e1f8b",
                                  "season": 1,
                                  "episode": 7,
                                  "startTime": "2024-10-01T15:00:00+02:00",
                                  "endTime": "2024-10-01T16:25:00+02:00"
                                }
                              ]
                            }
                          ]
                          """;

    /** Example response for TV shows ordered by occurrences. */
    public static final String ORDERED_BY_OCCURRECNCES =
        """
                          [
                              {
                                 "id": "b1742fdd-348d-48cf-8dc6-9fb43cb97239",
                                 "title": "Germany's Next Topmodel",
                                 "description": "Competition to find Germany's next top model",
                                 "occurrences": 3
                               },
                               {
                                 "id": "eb11b3d1-dff7-4136-bbea-d3756f90cde0",
                                 "title": "The Voice of Germany",
                                 "description": "Competition to find Great Singers",
                                 "occurrences": 6
                               },
                               {
                                 "id": "e9f4074c-2144-414f-a6f5-1941627dbe19",
                                 "title": "Galileo",
                                 "description": "Galileo World, Technic and Info Show",
                                 "occurrences": 7
                               }
                          ]
                          """;
  }
}
