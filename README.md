# Process PDF and Index into Elasticsearch

This project demonstrates how to extract text from PDF files within a specified directory and index the content into Elasticsearch using the attachment processor. It utilizes Java 17 and the Elasticsearch Java client.

## Prerequisites

* **Java 17:** Ensure you have Java 17 installed on your system.
* **Elasticsearch 8.15.0:** Have Elasticsearch 8.15.0 up and running. You can use the provided `docker-compose.yaml` file in the "docker" directory to spin up a Docker container.
* **Maven:**  This project uses Maven for dependency management and building.

## Setup

1. **Start Elasticsearch:**
    * Navigate to the "docker" directory.
    * Run `docker-compose up` to start the Elasticsearch container.

2. **Configure Elasticsearch:**
    * Import the Postman collection located in the "postman" directory.
    * If necessary, update the "host" collection variable to match your Elasticsearch instance's address.
    * Execute the "configure attachments" request to set up the attachment processor.
    * Execute the "create index" request to create the index where PDF content will be stored.

3. **Project Configuration:**
    * Open the `application.properties` file in the project root directory.
    * Fill in the following properties:
        * `pdf.directory`: The path to the directory containing your PDF files.
        * `elasticsearch.host`: The hostname or IP address of your Elasticsearch instance.
        * `elasticsearch.port`: The port on which Elasticsearch is listening (typically 9200).

## Running the Project

1. **Build the Project:**
    * Open a terminal or command prompt.
    * Navigate to the project root directory.
    * Run `mvn clean install` to build the project.

2. **Execute the Main Class:**
    * Run `mvn exec:java` to execute the main class (`PdfToElasticsearch`).
    * The project will process the PDFs in the specified directory, extract their text content, and index it into Elasticsearch.

3. **View Results:**
    * Use the "get index data" request in the Postman collection to retrieve the indexed PDF data from Elasticsearch.
    * You can customize the filter in the request body to refine your search.

## Additional Notes

* The project uses the Apache PDFBox library for PDF text extraction.
* The Elasticsearch attachment processor is utilized to handle PDF indexing and make the content searchable.
* Consider enhancing error handling and logging for a more robust application.

Feel free to contribute or report issues!