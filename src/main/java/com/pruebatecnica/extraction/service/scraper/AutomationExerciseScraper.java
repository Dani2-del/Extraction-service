package com.pruebatecnica.extraction.service.scraper;

import com.pruebatecnica.extraction.exception.ScrapingException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrae informacion de producto desde el HTML publico de
 * https://automationexercise.com/product_details/{id}
 *
 * IMPORTANTE: los selectores CSS de este scraper fueron definidos a partir
 * de una inspeccion manual de la estructura HTML del sitio en el momento de
 * escribir este codigo. Si el sitio cambia su marcado, estos selectores
 * deberan ajustarse. Se recomienda verificar contra el HTML real antes de
 * dar por definitiva la implementacion.
 */
@Component
public class AutomationExerciseScraper {

    private static final String BASE_URL = "https://automationexercise.com/product_details/";
    private static final Pattern PRICE_PATTERN = Pattern.compile("([0-9]+(\\.[0-9]+)?)");

    //Extrae la información de un producto a partir de su ID externo (el número de producto en el sitio web).
    public ScrapedProduct fetchProduct(String externalId) {
        String url = BASE_URL + externalId;
        try {
            Document doc = Jsoup.connect(url)
                    .timeout(8000)
                    .userAgent("Mozilla/5.0 (compatible; ExtractionServiceBot/1.0)")
                    .get();

            Element infoBlock = doc.selectFirst(".product-information");
            if (infoBlock == null) {
                throw new ScrapingException("No se encontro el bloque de informacion del producto para id " + externalId);
            }

            String name = safeText(infoBlock.selectFirst("h2"));
            if (name.isBlank()) {
                throw new ScrapingException("Producto no encontrado o pagina invalida para id " + externalId);
            }

            String category = extractLabelled(infoBlock, "Category");
            String availability = extractLabelled(infoBlock, "Availability");
            String condition = extractLabelled(infoBlock, "Condition");
            String brand = extractLabelled(infoBlock, "Brand");
            BigDecimal price = extractPrice(infoBlock);

            return ScrapedProduct.builder()
                    .externalId(externalId)
                    .name(name)
                    .price(price)
                    .category(category)
                    .availability(availability)
                    .condition(condition)
                    .brand(brand)
                    .sourceUrl(url)
                    .build();

        } catch (ScrapingException e) {
            throw e;
        } catch (Exception e) {
            throw new ScrapingException("Fallo al extraer el producto " + externalId + ": " + e.getMessage(), e);
        }
    }

    //para que en ves de lanzar NullPointerException, devuelva un string vacio si el elemento es nulo
    private String safeText(Element el) {
        return el == null ? "" : el.text().trim();
    }

    /**
     * Busca un parrafo del tipo "Category: Women > Tops" o "Availability: In Stock"
     * dentro del bloque de informacion y devuelve el valor asociado a la etiqueta.
     */

    //Si no encuentra el parrafo o no coincide con la etiqueta, devuelve null.
    private String extractLabelled(Element infoBlock, String label) {
        for (Element p : infoBlock.select("p")) {
            String text = p.text().trim();
            if (text.startsWith(label + ":")) {
                return text.substring(label.length() + 1).trim();
            }
        }
        return null;
    }

    //Busca el precio dentro del bloque de informacion y lo devuelve como BigDecimal.
    //Si no encuentra el precio o no puede parsearlo, devuelve null.
    private BigDecimal extractPrice(Element infoBlock) {
        Element priceEl = infoBlock.selectFirst("span span");
        String text = priceEl != null ? priceEl.text() : safeText(infoBlock.selectFirst("span"));
        if (text == null) {
            return null;
        }
        Matcher matcher = PRICE_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return new BigDecimal(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
