package org.opentripplanner.graph_builder.module;

import com.conveyal.gtfs.GTFSFeed;
import com.conveyal.gtfs.model.*;
import org.mapdb.Fun;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.graph_builder.linking.SimpleStreetSplitter;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.osm.DefaultWayPropertySetSource;
import org.opentripplanner.graph_builder.module.osm.OpenStreetMapModule;
import org.opentripplanner.openstreetmap.impl.AnyFileBasedOpenStreetMapProviderImpl;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Get fake graphs.
 */
public class FakeGraph {
    /** Build a graph in Columbus, OH with no transit */
    public static Graph buildGraphNoTransit () throws UnsupportedEncodingException {
        Graph gg = new Graph();

        OpenStreetMapModule loader = new OpenStreetMapModule();
        loader.setDefaultWayPropertySetSource(new DefaultWayPropertySetSource());
        AnyFileBasedOpenStreetMapProviderImpl provider = new AnyFileBasedOpenStreetMapProviderImpl();

        File file = new File(
                URLDecoder.decode(FakeGraph.class.getResource("columbus.osm.pbf").getFile(),
                        "UTF-8"));

        provider.setPath(file);
        loader.setProvider(provider);

        loader.buildGraph(gg, new HashMap<Class<?>, Object>());
        return gg;
    }

    /** Add transit (not just stops) to a Columbus graph */
    public static void addTransit (Graph gg) throws Exception {
        // using conveyal GTFS lib to build GTFS so a lot of code does not have to be rewritten later
        // once we're using the conveyal GTFS lib for everything we ought to be able to do this
        // without even writing out the GTFS to a file.
        GTFSFeed feed = new GTFSFeed();
        Agency a = new Agency();
        a.agency_id = "agency";
        a.agency_name = "Agency";
        a.agency_timezone = "America/New_York";
        a.agency_url = new URL("http://www.example.com");
        feed.agency.put("agency", a);

        Route r = new Route();
        r.route_short_name = "1";
        r.route_long_name = "High Street";
        r.route_type = 3;
        r.agency = a;
        r.route_id = "route";
        feed.routes.put(r.route_id, r);

        Service s = new Service("service");
        s.calendar = new Calendar();
        s.calendar.service = s;
        s.calendar.monday = s.calendar.tuesday = s.calendar.wednesday = s.calendar.thursday = s.calendar.friday =
                s.calendar.saturday = s.calendar.sunday = 1;
        s.calendar.start_date = 19991231;
        s.calendar.end_date = 21001231;
        feed.services.put(s.service_id, s);

        com.conveyal.gtfs.model.Stop s1 = new com.conveyal.gtfs.model.Stop();
        s1.stop_id = s1.stop_name = "s1";
        s1.stop_lat = 40.2182;
        s1.stop_lon = -83.0889;
        feed.stops.put(s1.stop_id, s1);

        com.conveyal.gtfs.model.Stop s2 = new com.conveyal.gtfs.model.Stop();
        s2.stop_id = s2.stop_name = "s2";
        s2.stop_lat = 39.9621;
        s2.stop_lon = -83.0007;
        feed.stops.put(s2.stop_id, s2);

        // make timetabled trips
        for (int departure = 7 * 3600; departure < 20 * 3600; departure += 600) {
            Trip t = new Trip();
            t.trip_id = "trip" + departure;
            t.service = s;
            t.route = r;
            feed.trips.put(t.trip_id, t);

            StopTime st1 = new StopTime();
            st1.trip_id = t.trip_id;
            st1.arrival_time = departure;
            st1.departure_time = departure;
            st1.stop_id = s1.stop_id;
            st1.stop_sequence = 1;
            feed.stop_times.put(new Fun.Tuple2(st1.trip_id, st1.stop_sequence), st1);

            StopTime st2 = new StopTime();
            st2.trip_id = t.trip_id;
            st2.arrival_time = departure + 500;
            st2.departure_time = departure + 500;
            st2.stop_sequence = 2;
            st2.stop_id = s2.stop_id;
            feed.stop_times.put(new Fun.Tuple2(st2.trip_id, st2.stop_sequence), st2);
        }

        File tempFile = File.createTempFile("gtfs", ".zip");
        feed.toFile(tempFile.getAbsolutePath());

        // phew. load it into the graph.
        GtfsModule gtfs = new GtfsModule(Arrays.asList(new GtfsBundle(tempFile)));
        gtfs.buildGraph(gg, new HashMap<>());
    }

    /** Add a regular grid of stops to the graph */
    public static void addRegularStopGrid(Graph g) {
        int count = 0;
        for (double lat = 39.9058; lat < 40.0281; lat += 0.005) {
            for (double lon = -83.1341; lon < -82.8646; lon += 0.005) {
                String id = "" + count++;
                AgencyAndId aid = new AgencyAndId("TEST", id);
                Stop stop = new Stop();
                stop.setLat(lat);
                stop.setLon(lon);
                stop.setName(id);
                stop.setCode(id);
                stop.setId(aid);

                new TransitStop(g, stop);
                count++;
            }
        }
    }

    /** add some extra stops to the graph */
    public static void addExtraStops (Graph g) {
        int count = 0;
        double lon = -83;
        for (double lat = 40; lat < 40.01; lat += 0.005) {
            String id = "EXTRA_" + count++;
            AgencyAndId aid = new AgencyAndId("EXTRA", id);
            Stop stop = new Stop();
            stop.setLat(lat);
            stop.setLon(lon);
            stop.setName(id);
            stop.setCode(id);
            stop.setId(aid);

            new TransitStop(g, stop);
            count++;
        }

        // add some duplicate stops
        lon = -83.1341 + 0.1;

        for (double lat = 39.9058; lat < 40.0281; lat += 0.005) {
            String id = "" + count++;
            AgencyAndId aid = new AgencyAndId("EXTRA", id);
            Stop stop = new Stop();
            stop.setLat(lat);
            stop.setLon(lon);
            stop.setName(id);
            stop.setCode(id);
            stop.setId(aid);

            new TransitStop(g, stop);
            count++;
        }

        // add some almost duplicate stops
        lon = -83.1341 + 0.15;

        for (double lat = 39.9059; lat < 40.0281; lat += 0.005) {
            String id = "" + count++;
            AgencyAndId aid = new AgencyAndId("EXTRA", id);
            Stop stop = new Stop();
            stop.setLat(lat);
            stop.setLon(lon);
            stop.setName(id);
            stop.setCode(id);
            stop.setId(aid);

            new TransitStop(g, stop);
            count++;
        }
    }

    /** link the stops in the graph */
    public static void link (Graph g) {
        SimpleStreetSplitter linker = new SimpleStreetSplitter(g);
        linker.link();
    }

}
