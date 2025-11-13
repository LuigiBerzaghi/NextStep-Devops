package com.softcode.nextstep.service.ai;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.xml.sax.ContentHandler;

@Slf4j
@Component
public class ResumeTextExtractor {

    private final AutoDetectParser parser = new AutoDetectParser();

    public String extractText(byte[] fileBytes, String fileName) {
        try (InputStream input = new ByteArrayInputStream(fileBytes)) {
            ContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
            ParseContext context = new ParseContext();
            parser.parse(input, handler, metadata, context);
            String text = handler.toString();
            if (StringUtils.hasText(text)) {
                return text;
            }
        } catch (Exception ex) {
            log.warn("Falha ao extrair texto do currículo {}, fallback para texto bruto.", fileName, ex);
        }
        return new String(fileBytes, StandardCharsets.UTF_8);
    }
}
