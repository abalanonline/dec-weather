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

package ab;

import ab.weather.AccuWeather;
import ab.weather.AwBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Optional;

@SpringBootApplication
@RestController
@EnableCaching
public class Application {

  private final AccuWeather accuWeather;

  public Application() {
    String envKey = "ACCUWEATHER_API_KEY";
    String apiKey = System.getenv(envKey);
    if (apiKey == null) throw new IllegalStateException(envKey + " not set");
    this.accuWeather = new AccuWeather(apiKey);
  }

  @GetMapping(value = "/{city:\\d+}", produces = MediaType.TEXT_PLAIN_VALUE)
  @Cacheable(cacheNames="get", key="#city + #locale + (T(java.time.Instant).now().getEpochSecond() / 7200)") // 2h
  public String get(@PathVariable String city, @RequestParam Optional<String> locale) {
    String localeString = locale.orElse("en_US");
    Locale localeObject = Locale.forLanguageTag(localeString.replace('_', '-'));
    String localeLanguage = localeObject.getLanguage();
    return new AwBuilder(localeObject)
        .appendForecast(accuWeather.getWeeklyForecast(city, localeLanguage))
        .appendObservation(accuWeather.getCurrentObservation(city, localeLanguage))
        .build();
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

}
