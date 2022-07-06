/*
 * Copyright 2022 Aleksei Balan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ab.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class AwBuilder {

  public static final int BRIEF_INDEX = 3;

  private final ResourceBundle rb;
  private final StringBuilder sb;
  private int index12;
  private String[] airQuality;

  private String DAY = "%s. %s. High %+.0f.";
  private String NIGHT = "%s. %s. Low %+.0f.";
  private String OUTLOOK = "The outlook.";
  private String TODAY = "Today";
  private String TONIGHT = "Tonight";
  private String WEEK_DAY = "%s";
  private String WEEK_NIGHT = "%s night";
  private String AIR_QUALITY = "%s air quality %s.";
  private String UV_INDEX = "The UV index for %s is %s or %s.";
  private String CONDITIONS = "Weather conditions at %s.";
  private String TEMPERATURE = "%s. Temperature %+.0f.";
  private String WIND = "Wind %s %.0f kilometers per hour.";
  private String PRESSURE = "Barometric pressure %.0f kilopascal.";
  private String HUMIDITY = "Relative humidity %s percent.";

  public AwBuilder(Locale locale) {
    this.sb = new StringBuilder();
    this.airQuality = new String[BRIEF_INDEX];
    rb = ResourceBundle.getBundle("noaa", locale);
    DAY = rb.getString("day");
    NIGHT = rb.getString("night");
    OUTLOOK = rb.getString("outlook");
    TODAY = rb.getString("today");
    TONIGHT = rb.getString("tonight");
    WEEK_DAY = rb.getString("week_day");
    WEEK_NIGHT = rb.getString("week_night");
    AIR_QUALITY = rb.getString("air_quality");
    UV_INDEX = rb.getString("uv_index");
    CONDITIONS = rb.getString("conditions");
    TEMPERATURE = rb.getString("temperature");
    WIND = rb.getString("wind");
    PRESSURE = rb.getString("pressure");
    HUMIDITY = rb.getString("humidity");
  }

  public static final String[] WIND_NAMES = {
      "N", "NbE", "NNE", "NEbN", "NE", "NEbE", "ENE", "EbN", "E", "EbS", "ESE", "SEbE", "SE", "SEbS", "SSE", "SbE",
      "S", "SbW", "SSW", "SWbS", "SW", "SWbW", "WSW", "WbS", "W", "WbN", "WNW", "NWbW", "NW", "NWbN", "NNW", "NbW"};

  public String degreeToDirection(int degrees, int bitWidth) {
    int wind2x = (int) ((degrees + 360) * (1 << (bitWidth + 1)) / 360.0);
    int wind = ((wind2x + 1) >> 1) & ((1 << bitWidth) - 1);
    return WIND_NAMES[wind << (5 - bitWidth)];
  }

  public String directionToString(String direction) {
    List<String> result = new ArrayList<>();
    for (char c : direction.toCharArray()) {
      switch (c) {
        case 'N': result.add(rb.getString("north")); break;
        case 'E': result.add(rb.getString("east")); break;
        case 'S': result.add(rb.getString("south")); break;
        case 'W': result.add(rb.getString("west")); break;
        case 'b': result.add(rb.getString("by")); break;
        default: throw new IllegalStateException(direction);
      }
    }
    return String.join(" ", result);
  }

  private void getWeather12(JsonNode forecast, boolean night) {
    if (index12 < 0) {
      return; // skip the morning forecast
    }
    DayOfWeek dayOfWeek = OffsetDateTime.parse(forecast.at("/Date").asText()).getDayOfWeek();
    String dayOfWeekString = rb.getString(dayOfWeek.toString().toLowerCase());
    // night activity
    dayOfWeekString = String.format(night ? WEEK_NIGHT : WEEK_DAY, dayOfWeekString);
    if ((index12 == 0) || (index12 == 1 && night)) {
      dayOfWeekString = night ? TONIGHT : TODAY;
    }
    boolean brief = index12 >= BRIEF_INDEX;

    if (index12 == BRIEF_INDEX) { // air, outlook
      Arrays.asList(airQuality).forEach(sb::append);
      sb.append(OUTLOOK).append("\n");
    }

    sb.append(String.format(night ? NIGHT : DAY, dayOfWeekString,
        forecast.at(night ? "/Night" : "/Day").at(brief ? "/ShortPhrase" : "/LongPhrase").asText(),
        forecast.at(night ? "/Temperature/Minimum/Value" : "/Temperature/Maximum/Value").asDouble()));

    if (!brief) {
      Map<String, JsonNode> airAndPollen = new HashMap<>();
      for (JsonNode node : forecast.at("/AirAndPollen")) {
        airAndPollen.put(node.at("/Name").asText().toLowerCase(), node);
      }
      JsonNode air = airAndPollen.get("airquality");
      airQuality[index12] = air == null ? ""
          : String.format(AIR_QUALITY, dayOfWeekString, air.at("/Category").asText()) + "\n";

      if (!night) { // UV index
        JsonNode uv = airAndPollen.get("uvindex");
        if (uv != null) {
          sb.append(' ')
              .append(String.format(UV_INDEX, uv.at("/Value").asInt(), uv.at("/Category").asText()));
        }
      }
    }
    sb.append("\n");
  }

  public AwBuilder appendForecast(JsonNode weeklyForecast) {
    ArrayNode dailyForecasts = (ArrayNode) weeklyForecast.at("/DailyForecasts");

    OffsetDateTime effectiveDate = OffsetDateTime.parse(weeklyForecast.at("/Headline/EffectiveDate").asText());
    int hourNow = effectiveDate.getHour();

    // 1. forecast
    index12 = ((hourNow < 5) || (hourNow >= 17)) ? -1 : 0; // nighttime
    for (JsonNode forecast : dailyForecasts) {
      getWeather12(forecast, false);
      index12++;
      getWeather12(forecast, true);
      index12++;
    }
    return this; // fluent pattern
  }

  public AwBuilder appendObservation(JsonNode currentObservation) {
    JsonNode currentObservation0 = currentObservation.get(0);
    // 2. weather conditions
    OffsetDateTime observationDateTime =
        OffsetDateTime.parse(currentObservation0.at("/LocalObservationDateTime").asText());
    sb.append(String.format(CONDITIONS, observationDateTime.format(DateTimeFormatter.ofPattern("h a")))).append("\n");
    sb.append(String.format(TEMPERATURE,
        currentObservation0.at("/WeatherText").asText(),
        currentObservation0.at("/Temperature/Metric/Value").asDouble()))
        .append(" ");
    sb.append(String.format(WIND,
        directionToString(degreeToDirection(currentObservation0.at("/Wind/Direction/Degrees").asInt(), 3)),
        currentObservation0.at("/Wind/Speed/Metric/Value").asDouble()))
        .append("\n");
    sb.append(String.format(PRESSURE,
        currentObservation0.at("/Pressure/Metric/Value").asDouble() / 10.0)).append(" ");
    JsonNode humidity = currentObservation0.at("/RelativeHumidity");
    if (humidity.isInt()) {
      sb.append(String.format(HUMIDITY, humidity.asInt()));
    }
    sb.append("\n");
    return this; // fluent pattern
  }

  public String build() {
    final String s = sb.toString();
    sb.setLength(0); // reusable
    return s;
  }

}
