package com.rami.weeklymealplanner.kroger.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rami.weeklymealplanner.kroger.api.LocationSummaryResponse;
import com.rami.weeklymealplanner.kroger.domain.Address;
import com.rami.weeklymealplanner.kroger.domain.DayHours;
import com.rami.weeklymealplanner.kroger.domain.Department;
import com.rami.weeklymealplanner.kroger.domain.Hours;
import com.rami.weeklymealplanner.kroger.domain.Location;
import com.rami.weeklymealplanner.kroger.domain.LocationsResponse;
import com.rami.weeklymealplanner.kroger.infrastructure.KrogerLocationsHttpClient;

@ExtendWith(MockitoExtension.class)
class FindKrogerLocationsServiceTest {

    @Mock
    private KrogerLocationsHttpClient locationsClient;

    @InjectMocks
    private FindKrogerLocationsService service;

    @Test
    void findReturnsEmptyListWhenResponseIsNull() throws IOException {
        when(locationsClient.getLocations("85338", 10, 5)).thenReturn(null);

        List<LocationSummaryResponse> out = service.find("85338", 10, 5);

        assertThat(out).isEmpty();
    }

    @Test
    void findReturnsEmptyListWhenResponseDataIsNull() throws IOException {
        when(locationsClient.getLocations("85338", 10, 5))
                .thenReturn(new LocationsResponse(null, null));

        List<LocationSummaryResponse> out = service.find("85338", 10, 5);

        assertThat(out).isEmpty();
    }

    @Test
    void findMapsLocationAndDepartmentFlags() throws IOException {
        DayHours everyDay = new DayHours("08:00", "20:00", false);
        Hours hours = new Hours(
                "America/Phoenix",
                "-07:00",
                false,
                everyDay, everyDay, everyDay, everyDay, everyDay, everyDay, everyDay
        );

        Location location = new Location(
                "01400433",
                "433",
                "12",
                "Kroger",
                "Kroger Marketplace",
                "623-555-1212",
                new Address("123 Main", "Goodyear", "AZ", "85338", "Maricopa"),
                null,
                hours,
                List.of(
                        new Department("94", "Pickup", null, null, null, null, false),
                        new Department("0E", "Delivery", null, null, null, null, false)
                )
        );

        when(locationsClient.getLocations("85338", 10, 5))
                .thenReturn(new LocationsResponse(List.of(location), null));

        List<LocationSummaryResponse> out = service.find("85338", 10, 5);

        assertThat(out).hasSize(1);
        LocationSummaryResponse item = out.getFirst();
        assertThat(item.locationId()).isEqualTo("01400433");
        assertThat(item.name()).isEqualTo("Kroger Marketplace");
        assertThat(item.city()).isEqualTo("Goodyear");
        assertThat(item.state()).isEqualTo("AZ");
        assertThat(item.zipCode()).isEqualTo("85338");
        assertThat(item.timezone()).isEqualTo("America/Phoenix");
        assertThat(item.todayHours()).isEqualTo("08:00-20:00");
        assertThat(item.pickup()).isTrue();
        assertThat(item.delivery()).isTrue();
    }

    @Test
    void findFormatsOpen24Hours() throws IOException {
        DayHours everyDay = new DayHours(null, null, true);
        Hours hours = new Hours(
                "America/Phoenix",
                "-07:00",
                true,
                everyDay, everyDay, everyDay, everyDay, everyDay, everyDay, everyDay
        );

        Location location = new Location(
                "1",
                "1",
                "1",
                "Kroger",
                "Store",
                "623",
                new Address("123 Main", "Goodyear", "AZ", "85338", "Maricopa"),
                null,
                hours,
                List.of()
        );

        when(locationsClient.getLocations("85338", 10, 5))
                .thenReturn(new LocationsResponse(List.of(location), null));

        List<LocationSummaryResponse> out = service.find("85338", 10, 5);

        assertThat(out).hasSize(1);
        assertThat(out.getFirst().todayHours()).isEqualTo("Open 24h");
    }

    @Test
    void findHandlesNullFieldsSafely() throws IOException {
        Location location = new Location(
                null, null, null, null, null, null,
                null,
                null,
                null,
                null
        );

        when(locationsClient.getLocations("85338", 10, 5))
                .thenReturn(new LocationsResponse(List.of(location), null));

        List<LocationSummaryResponse> out = service.find("85338", 10, 5);

        assertThat(out).hasSize(1);
        LocationSummaryResponse item = out.getFirst();
        assertThat(item.locationId()).isEmpty();
        assertThat(item.name()).isEmpty();
        assertThat(item.city()).isEmpty();
        assertThat(item.state()).isEmpty();
        assertThat(item.zipCode()).isEmpty();
        assertThat(item.todayHours()).isEmpty();
        assertThat(item.pickup()).isFalse();
        assertThat(item.delivery()).isFalse();
    }
}
