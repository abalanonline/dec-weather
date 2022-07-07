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
import org.junit.Test;

import java.util.Locale;

public class ApplicationTest {

  @Test
  public void test() {
    String city = "56186";
    AccuWeather accuWeather = new AccuWeather(AccuWeather.MOCK_API_KEY);
    Locale localeObject = Locale.CANADA_FRENCH;
    String localeLanguage = localeObject.getLanguage();
    String s = new AwBuilder(localeObject)
        .appendForecast(accuWeather.getWeeklyForecast(city, localeLanguage))
        .appendObservation(accuWeather.getCurrentObservation(city, localeLanguage))
        .build();
    //System.out.println(s);
  }

}
