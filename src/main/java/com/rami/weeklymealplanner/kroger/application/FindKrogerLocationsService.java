package com.rami.weeklymealplanner.kroger.application;

import com.rami.weeklymealplanner.kroger.api.LocationSummaryResponse;
import com.rami.weeklymealplanner.kroger.domain.Department;
import com.rami.weeklymealplanner.kroger.domain.DayHours;
import com.rami.weeklymealplanner.kroger.domain.Location;
import com.rami.weeklymealplanner.kroger.domain.LocationsResponse;
import com.rami.weeklymealplanner.kroger.infrastructure.KrogerLocationsHttpClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class FindKrogerLocationsService {

    private final KrogerLocationsHttpClient locationsClient;

    public FindKrogerLocationsService(KrogerLocationsHttpClient locationsClient) {
        this.locationsClient = locationsClient;
    }

    public List<LocationSummaryResponse> find(String zipCode, int radiusMiles, int limit) throws IOException {
        LocationsResponse response = locationsClient.getLocations(zipCode, radiusMiles, limit);

        List<LocationSummaryResponse> out = new ArrayList<>();
        if (response == null || response.data() == null) return out;

        for (Location loc : response.data()) {
            String city = loc.address() != null ? safe(loc.address().city()) : "";
            String state = loc.address() != null ? safe(loc.address().state()) : "";
            String zip = loc.address() != null ? safe(loc.address().zipCode()) : "";

            String timezone = loc.hours() != null ? safe(loc.hours().timezone()) : "";
            String todayHours = extractTodayHours(loc);

            boolean hasDelivery = false; // deptId 0E
            boolean hasPickup = false;   // deptId 94

            if (loc.departments() != null) {
                for (Department d : loc.departments()) {
                    if (d == null || d.departmentId() == null) continue;
                    if ("0E".equalsIgnoreCase(d.departmentId())) hasDelivery = true;
                    if ("94".equalsIgnoreCase(d.departmentId())) hasPickup = true;
                }
            }

            out.add(new LocationSummaryResponse(
                    safe(loc.locationId()),
                    safe(loc.name()),
                    safe(loc.chain()),
                    city,
                    state,
                    zip,
                    safe(loc.phone()),
                    timezone,
                    todayHours,
                    hasPickup,
                    hasDelivery
            ));
        }

        return out;
    }

    private String extractTodayHours(Location loc) {
        if (loc.hours() == null) return "";

        DayOfWeek dow = LocalDate.now().getDayOfWeek();
        DayHours dh = switch (dow) {
            case MONDAY -> loc.hours().monday();
            case TUESDAY -> loc.hours().tuesday();
            case WEDNESDAY -> loc.hours().wednesday();
            case THURSDAY -> loc.hours().thursday();
            case FRIDAY -> loc.hours().friday();
            case SATURDAY -> loc.hours().saturday();
            case SUNDAY -> loc.hours().sunday();
        };

        if (dh == null) return "";
        if (Boolean.TRUE.equals(dh.open24())) return "Open 24h";

        String open = safe(dh.open());
        String close = safe(dh.close());
        if (open.isEmpty() && close.isEmpty()) return "";
        return open + "-" + close;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
