
theme: /
    state: Start
        q!: $regex</start>
        a: Привет! Я бот-помощник. Могу рассказать о погоде и курсах валют. Спроси меня о погоде или стоимости доллара, евро и юаня.

    state: hello
        intent!: /hello
        a: Привет! Рад тебя видеть. Хочешь узнать погоду или курсы валют?

    state: weather
        intent!: /weather
        script:
            var query = ($request.query || "").toLowerCase();
            var city = "Санкт-Петербург";

            if (query.indexOf("москв") !== -1) {
                city = "Москва";
            } else if (/петербург|питер|спб/.test(query)) {
                city = "Санкт-Петербург";
            } else if (query.indexOf("казан") !== -1) {
                city = "Казань";
            } else if (query.indexOf("новосибирск") !== -1) {
                city = "Новосибирск";
            } else if (query.indexOf("екатеринбург") !== -1) {
                city = "Екатеринбург";
            } else if (query.indexOf("варшав") !== -1) {
                city = "Варшава";
            } else {
                var match = query.match(/(?:^|\s)(?:в|во)\s+([а-яёa-z-]+)/i);
                if (match) {
                    city = match[1];
                }
            }

            var geo = $http.get(
                "https://geocoding-api.open-meteo.com/v1/search?name=${name}&count=1&language=ru",
                {
                    query: {name: city},
                    dataType: "json",
                    timeout: 8000
                }
            );

            if (!geo.isOk || !geo.data || !geo.data.results || !geo.data.results.length) {
                $reactions.answer("Не удалось найти город. Попробуй указать название в начальной форме, например: погода в Москве.");
            } else {
                var location = geo.data.results[0];

                var forecast = $http.get(
                    "https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&current=temperature_2m,weather_code&daily=temperature_2m_max,temperature_2m_min,weather_code&timezone=auto&forecast_days=2",
                    {
                        query: {
                            lat: location.latitude,
                            lon: location.longitude
                        },
                        dataType: "json",
                        timeout: 8000
                    }
                );

                if (!forecast.isOk || !forecast.data || !forecast.data.current) {
                    $reactions.answer("Сейчас не получается получить погоду. Попробуй позже.");
                } else {
                    var tomorrow = query.indexOf("завтра") !== -1;
                    var data = forecast.data;
                    var code = tomorrow && data.daily
                        ? data.daily.weather_code[1]
                        : data.current.weather_code;

                    var description = "переменная погода";

                    if (code === 0) {
                        description = "ясно";
                    } else if (code <= 3) {
                        description = "облачно";
                    } else if (code >= 45 && code <= 48) {
                        description = "туман";
                    } else if (code >= 51 && code <= 67) {
                        description = "дождь";
                    } else if (code >= 71 && code <= 77) {
                        description = "снег";
                    } else if (code >= 80 && code <= 82) {
                        description = "ливень";
                    } else if (code >= 95) {
                        description = "гроза";
                    }

                    if (tomorrow && data.daily && data.daily.temperature_2m_min.length > 1) {
                        $reactions.answer(
                            "Завтра в городе " + location.name +
                            " ожидается: " + description +
                            ". Температура от " +
                            Math.round(data.daily.temperature_2m_min[1]) +
                            " до " +
                            Math.round(data.daily.temperature_2m_max[1]) +
                            " °C. Источник: Open-Meteo."
                        );
                    } else {
                        $reactions.answer(
                            "Сейчас в городе " + location.name +
                            ": " + description +
                            ", температура " +
                            Math.round(data.current.temperature_2m) +
                            " °C. Источник: Open-Meteo."
                        );
                    }
                }
            }

    state: currency
        intent!: /currency
        script:
            var query = ($request.query || "").toLowerCase();

            var response = $http.get(
                "https://open.er-api.com/v6/latest/USD",
                {
                    dataType: "json",
                    timeout: 8000
                }
            );

            if (!response.isOk || !response.data || response.data.result !== "success") {
                $reactions.answer("Не удалось получить курсы валют. Попробуй позже.");
            } else {
                var rates = response.data.rates;

                if (!rates || !rates.RUB || !rates.EUR || !rates.CNY) {
                    $reactions.answer("Сервис вернул неполные данные о курсах валют.");
                } else {
                    var usd = rates.RUB;
                    var eur = rates.RUB / rates.EUR;
                    var cny = rates.RUB / rates.CNY;

                    var answer = "";

                    if (/доллар|usd/.test(query)) {
                        answer = "1 доллар = " + usd.toFixed(2) + " руб.";
                    } else if (/евр|eur/.test(query)) {
                        answer = "1 евро = " + eur.toFixed(2) + " руб.";
                    } else if (/юан|cny/.test(query)) {
                        answer = "1 юань = " + cny.toFixed(2) + " руб.";
                    } else {
                        answer =
                            "Курсы валют к рублю:\n" +
                            "1 USD = " + usd.toFixed(2) + " руб.\n" +
                            "1 EUR = " + eur.toFixed(2) + " руб.\n" +
                            "1 CNY = " + cny.toFixed(2) + " руб.";
                    }

                    $reactions.answer(
                        answer + "\nИсточник: ExchangeRate-API.com. Справочный курс."
                    );
                }
            }

    state: NoMatch || noContext=true
        event!: noMatch
        a: Извини, я не понял твой запрос. Я умею рассказывать о погоде и курсах валют. Например, спроси: «Какая погода в Москве?» или «Курс доллара».
