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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

@Slf4j
public class AccuWeather {

  public static final String DATA_SERVICE = "https://dataservice.accuweather.com/";
  public static final String MOCK_API_KEY = "00000000000000000000000000000000";

  private String apikey;
  private final ObjectMapper objectMapper;

  public AccuWeather(String apikey) {
    this.apikey = apikey;
    objectMapper = new ObjectMapper();
  }

  public JsonNode readMock(String resourceName) {
    log.info("accuweather mock: " + resourceName);
    ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    try {
      return objectMapper.readTree(classloader.getResourceAsStream(resourceName));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public JsonNode getJson(String restUrl, String location, String language) {
    log.info("accuweather: " + restUrl + " " + location + " " + language);
    RestTemplate restTemplate = new RestTemplate();
    byte[] bytes = restTemplate.getForObject(DATA_SERVICE + restUrl + '/' + location
        + "?apikey=" + apikey + "&details=true&metric=true&language=" + language, byte[].class);

    try {
      return objectMapper.readTree(new ByteArrayInputStream(bytes));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public JsonNode getWeeklyForecast(String location, String language) {
    if (apikey.equals(MOCK_API_KEY)) {
      return readMock("accuweather_5day_" + language + ".json");
    }
    return getJson("forecasts/v1/daily/5day", location, language);
  }

  public JsonNode getCurrentObservation(String location, String language) {
    if (apikey.equals(MOCK_API_KEY)) {
      return readMock("accuweather_current_" + language + ".json");
    }
    return getJson("currentconditions/v1", location, language);
  }

}
