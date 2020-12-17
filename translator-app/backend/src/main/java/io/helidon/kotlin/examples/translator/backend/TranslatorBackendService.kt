/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.kotlin.examples.translator.backend

import io.helidon.webserver.*
import java.util.*

/**
 * Translator backend service.
 */
class TranslatorBackendService : Service {
    companion object {
        private const val CZECH = "czech"
        private const val SPANISH = "spanish"
        private const val CHINESE = "chinese"
        private const val HINDI = "hindi"
        private const val ITALIAN = "italian"
        private const val FRENCH = "french"
        private const val SEPARATOR = "."
        private val TRANSLATED_WORDS_REPOSITORY: MutableMap<String, String> = HashMap()

        init {
            //translation for word "cloud"
            TRANSLATED_WORDS_REPOSITORY["cloud" + SEPARATOR + CZECH] = "oblak"
            TRANSLATED_WORDS_REPOSITORY["cloud" + SEPARATOR + SPANISH] = "nube"
            TRANSLATED_WORDS_REPOSITORY["cloud" + SEPARATOR + CHINESE] = "云"
            TRANSLATED_WORDS_REPOSITORY["cloud" + SEPARATOR + HINDI] = "बादल"
            TRANSLATED_WORDS_REPOSITORY["cloud" + SEPARATOR + ITALIAN] = "nube"
            TRANSLATED_WORDS_REPOSITORY["cloud" + SEPARATOR + FRENCH] = "nuage"

            //one two three four five six seven eight nine ten
            //jedna dvě tři čtyři pět šest sedm osm devět deset
            //uno dos tres cuatro cinco seis siete ocho nueve diez
            //一二三四五六七八九十
            //एक दो तीन चार पांच छ सात आठ नौ दस
            // uno due tre quattro cinque sei sette otto nove dieci
            // un deux trois quatre cinq six sept huit neuf dix

            //translation for word "one"
            TRANSLATED_WORDS_REPOSITORY["one" + SEPARATOR + CZECH] = "jedna"
            TRANSLATED_WORDS_REPOSITORY["one" + SEPARATOR + SPANISH] = "uno"
            TRANSLATED_WORDS_REPOSITORY["one" + SEPARATOR + CHINESE] = "一"
            TRANSLATED_WORDS_REPOSITORY["one" + SEPARATOR + HINDI] = "एक"
            TRANSLATED_WORDS_REPOSITORY["one" + SEPARATOR + ITALIAN] = "uno"
            TRANSLATED_WORDS_REPOSITORY["one" + SEPARATOR + FRENCH] = "un"
            //translation for word "two"
            TRANSLATED_WORDS_REPOSITORY["two" + SEPARATOR + CZECH] = "dvě"
            TRANSLATED_WORDS_REPOSITORY["two" + SEPARATOR + SPANISH] = "dos"
            TRANSLATED_WORDS_REPOSITORY["two" + SEPARATOR + CHINESE] = "二"
            TRANSLATED_WORDS_REPOSITORY["two" + SEPARATOR + HINDI] = "दो"
            TRANSLATED_WORDS_REPOSITORY["two" + SEPARATOR + ITALIAN] = "due"
            TRANSLATED_WORDS_REPOSITORY["two" + SEPARATOR + FRENCH] = "deux"
            //translation for word "three"
            TRANSLATED_WORDS_REPOSITORY["three" + SEPARATOR + CZECH] = "tři"
            TRANSLATED_WORDS_REPOSITORY["three" + SEPARATOR + SPANISH] = "tres"
            TRANSLATED_WORDS_REPOSITORY["three" + SEPARATOR + CHINESE] = "三"
            TRANSLATED_WORDS_REPOSITORY["three" + SEPARATOR + HINDI] = "तीन"
            TRANSLATED_WORDS_REPOSITORY["three" + SEPARATOR + ITALIAN] = "tre"
            TRANSLATED_WORDS_REPOSITORY["three" + SEPARATOR + FRENCH] = "trois"
            //translation for word "four"
            TRANSLATED_WORDS_REPOSITORY["four" + SEPARATOR + CZECH] = "čtyři"
            TRANSLATED_WORDS_REPOSITORY["four" + SEPARATOR + SPANISH] = "cuatro"
            TRANSLATED_WORDS_REPOSITORY["four" + SEPARATOR + CHINESE] = "四"
            TRANSLATED_WORDS_REPOSITORY["four" + SEPARATOR + HINDI] = "चार"
            TRANSLATED_WORDS_REPOSITORY["four" + SEPARATOR + ITALIAN] = "quattro"
            TRANSLATED_WORDS_REPOSITORY["four" + SEPARATOR + FRENCH] = "quatre"
            //translation for word "five"
            TRANSLATED_WORDS_REPOSITORY["five" + SEPARATOR + CZECH] = "pět"
            TRANSLATED_WORDS_REPOSITORY["five" + SEPARATOR + SPANISH] = "cinco"
            TRANSLATED_WORDS_REPOSITORY["five" + SEPARATOR + CHINESE] = "五"
            TRANSLATED_WORDS_REPOSITORY["five" + SEPARATOR + HINDI] = "पांच"
            TRANSLATED_WORDS_REPOSITORY["five" + SEPARATOR + ITALIAN] = "cinque"
            TRANSLATED_WORDS_REPOSITORY["five" + SEPARATOR + FRENCH] = "cinq"
            //translation for word "six"
            TRANSLATED_WORDS_REPOSITORY["six" + SEPARATOR + CZECH] = "šest"
            TRANSLATED_WORDS_REPOSITORY["six" + SEPARATOR + SPANISH] = "seis"
            TRANSLATED_WORDS_REPOSITORY["six" + SEPARATOR + CHINESE] = "六"
            TRANSLATED_WORDS_REPOSITORY["six" + SEPARATOR + HINDI] = "छ"
            TRANSLATED_WORDS_REPOSITORY["six" + SEPARATOR + ITALIAN] = "sei"
            TRANSLATED_WORDS_REPOSITORY["six" + SEPARATOR + FRENCH] = "six"
            //translation for word "seven"
            TRANSLATED_WORDS_REPOSITORY["seven" + SEPARATOR + CZECH] = "sedm"
            TRANSLATED_WORDS_REPOSITORY["seven" + SEPARATOR + SPANISH] = "siete"
            TRANSLATED_WORDS_REPOSITORY["seven" + SEPARATOR + CHINESE] = "七"
            TRANSLATED_WORDS_REPOSITORY["seven" + SEPARATOR + HINDI] = "सात"
            TRANSLATED_WORDS_REPOSITORY["seven" + SEPARATOR + ITALIAN] = "sette"
            TRANSLATED_WORDS_REPOSITORY["seven" + SEPARATOR + FRENCH] = "sept"
            //translation for word "eight"
            TRANSLATED_WORDS_REPOSITORY["eight" + SEPARATOR + CZECH] = "osm"
            TRANSLATED_WORDS_REPOSITORY["eight" + SEPARATOR + SPANISH] = "ocho"
            TRANSLATED_WORDS_REPOSITORY["eight" + SEPARATOR + CHINESE] = "八"
            TRANSLATED_WORDS_REPOSITORY["eight" + SEPARATOR + HINDI] = "आठ"
            TRANSLATED_WORDS_REPOSITORY["eight" + SEPARATOR + ITALIAN] = "otto"
            TRANSLATED_WORDS_REPOSITORY["eight" + SEPARATOR + FRENCH] = "huit"
            //translation for word "nine"
            TRANSLATED_WORDS_REPOSITORY["nine" + SEPARATOR + CZECH] = "devět"
            TRANSLATED_WORDS_REPOSITORY["nine" + SEPARATOR + SPANISH] = "nueve"
            TRANSLATED_WORDS_REPOSITORY["nine" + SEPARATOR + CHINESE] = "九"
            TRANSLATED_WORDS_REPOSITORY["nine" + SEPARATOR + HINDI] = "नौ"
            TRANSLATED_WORDS_REPOSITORY["nine" + SEPARATOR + ITALIAN] = "nove"
            TRANSLATED_WORDS_REPOSITORY["nine" + SEPARATOR + FRENCH] = "neuf"
            //translation for word "ten"
            TRANSLATED_WORDS_REPOSITORY["ten" + SEPARATOR + CZECH] = "deset"
            TRANSLATED_WORDS_REPOSITORY["ten" + SEPARATOR + SPANISH] = "diez"
            TRANSLATED_WORDS_REPOSITORY["ten" + SEPARATOR + CHINESE] = "十"
            TRANSLATED_WORDS_REPOSITORY["ten" + SEPARATOR + HINDI] = "दस"
            TRANSLATED_WORDS_REPOSITORY["ten" + SEPARATOR + ITALIAN] = "dieci"
            TRANSLATED_WORDS_REPOSITORY["ten" + SEPARATOR + FRENCH] = "dix"
        }
    }

    override fun update(rules: Routing.Rules) {
        rules[Handler { request: ServerRequest, response: ServerResponse -> getText(request, response) }]
    }

    private fun getText(request: ServerRequest, response: ServerResponse) {
        val query = request.queryParams().first("q")
                .orElseThrow { BadRequestException("missing query parameter 'q'") }
        val language = request.queryParams().first("lang")
                .orElseThrow { BadRequestException("missing query parameter 'lang'") }
        val translation: String?
        translation = when (language) {
            CZECH -> TRANSLATED_WORDS_REPOSITORY[query + SEPARATOR + CZECH]
            SPANISH -> TRANSLATED_WORDS_REPOSITORY[query + SEPARATOR + SPANISH]
            CHINESE -> TRANSLATED_WORDS_REPOSITORY[query + SEPARATOR + CHINESE]
            HINDI -> TRANSLATED_WORDS_REPOSITORY[query + SEPARATOR + HINDI]
            ITALIAN -> TRANSLATED_WORDS_REPOSITORY[query + SEPARATOR + ITALIAN]
            FRENCH -> TRANSLATED_WORDS_REPOSITORY[query + SEPARATOR + FRENCH]
            else -> {
                response.status(404)
                        .send(String.format(
                                "Language '%s' not in supported. Supported languages: %s, %s, %s, %s.",
                                language,
                                CZECH, SPANISH, CHINESE, HINDI))
                return
            }
        }
        if (translation != null) {
            response.send(translation)
        } else {
            response.status(404)
                    .send(String.format("Word '%s' not in the dictionary", query))
        }
    }
}