package com.mjdomin.pdf;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.apache.http.HttpHost;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Properties;

public class PdfToElasticsearch {

    private static final Logger logger = LoggerFactory.getLogger(PdfToElasticsearch.class);

    public static void main(String[] args) {

        // 1. Load properties (pdf directory, Elasticsearch connection details)
        Properties properties = loadProperties();
        String pdfDirectory = properties.getProperty("pdf.directory");
        String elasticsearchHost = properties.getProperty("elasticsearch.host");
        int elasticsearchPort = Integer.parseInt(properties.getProperty("elasticsearch.port"));
        String pdfIndex = properties.getProperty("elasticsearch.pdf-index");

        // 2. Connect to Elasticsearch
        RestClient restClient = RestClient.builder(
                new HttpHost(elasticsearchHost, elasticsearchPort)
        ).build();

        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());

        ElasticsearchClient client = new ElasticsearchClient(transport);

        // 3. Process PDFs in the directory
        try {
            Files.walk(Paths.get(pdfDirectory))
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".pdf"))
                    .forEach(path -> {
                        try {
                            // 3.1 Extract text from PDF
                            String pdfText = extractTextFromPdf(path);

                            // 3.2 Index the content into Elasticsearch
                            indexPdfContent(client, path.getFileName().toString(), pdfText, pdfIndex);

                        } catch (IOException | ElasticsearchException e) {
                            logger.error("Error processing PDF: " + path, e);
                        }
                    });
        } catch (IOException e) {
            logger.error("Error walking directory: " + pdfDirectory, e);
        } finally {
            // 4. Close Elasticsearch connection
            try {
                restClient.close();
            } catch (IOException e) {
                logger.error("Error closing Elasticsearch connection", e);
            }
        }
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try {
            InputStream input = PdfToElasticsearch.class.getClassLoader().getResourceAsStream("application.properties");

            if (input == null) {
                throw new FileNotFoundException("Property file '" + "application.properties" + "' not found in the classpath");
            }
            properties.load(input);

        } catch (IOException ex) {
            logger.error("Error loading properties file", ex);
        }
        return properties;
    }

    private static String extractTextFromPdf(Path pdfFilePath) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfFilePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private static void indexPdfContent(ElasticsearchClient client, String pdfFileName, String pdfText, String pdfIndex) throws ElasticsearchException, IOException {

        // 1. Encode PDF text to Base64
        String base64PdfText = Base64.getEncoder().encodeToString(pdfText.getBytes(StandardCharsets.UTF_8));

        // 2. Create a JSON object with Base64 encoded data
        JsonObject jsonObject = Json.createObjectBuilder()
                .add("data", base64PdfText)
                .build();

        // 3. Create an Index Request with the attachment pipeline
        IndexRequest<JsonObject> request = IndexRequest.of(b ->
                b.index(pdfIndex) // Specify your index name
                        .id(pdfFileName)
                        .document(jsonObject)
                        .pipeline("attachment") // Use the attachment pipeline
        );

        // 4. Index the document
        IndexResponse response = client.index(request);

        // 5. Log the result
        logger.info("Indexed PDF: {}, result: {}", pdfFileName, response.result().name());
    }
}
