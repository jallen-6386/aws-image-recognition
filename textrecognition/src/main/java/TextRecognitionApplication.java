package textrecognition;

import com.amazonaws.regions.Regions;
// AWS Rekognition Service
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.*;
// AWS S3 Service
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
// AWS SQS Service
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.*;

import java.io.*;
import java.util.*;

public class TextRecognitionApplication {
    private static final String OUTPUT_FILENAME = "output.txt";

    public static void main(String[] args) {
        // Define AWS Resources
        String bucketName = "njit-cs-643";
        String queueName = "car.fifo";

        // Initialize AWS Clients
        AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
        AmazonRekognition rekognition = AmazonRekognitionClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
        AmazonSQS sqs = AmazonSQSClientBuilder.standard().withRegion(Regions.US_EAST_1).build();

        // Process images retrieved from the SQS queue
        processCarImages(s3, rekognition, sqs, bucketName, queueName);
    }

    public static void processCarImages(AmazonS3 s3, AmazonRekognition rekognition, AmazonSQS sqs,
                                        String bucketName, String queueName) {

        // Retrieve queue URL
        String queueUrl = sqs.getQueueUrl(queueName).getQueueUrl();

        // Process images from the queue
        Map<String, String> results = new HashMap<>();
        boolean continueProcessing = true;

        while (continueProcessing) {
            List<Message> messages = sqs.receiveMessage(queueUrl).getMessages();

            for (Message message : messages) {
                String imageName = message.getBody();

                if (imageName.equals("-1")) {
                    // Termination signal received, stop processing
                    continueProcessing = false;
                } else {
                    // Process text recognition for this image
                    Image img = new Image().withS3Object(new S3Object().withBucket(bucketName).withName(imageName));
                    DetectTextRequest request = new DetectTextRequest().withImage(img);
                    DetectTextResult result = rekognition.detectText(request);

                    // Extract detected words
                    StringBuilder detectedText = new StringBuilder();
                    for (TextDetection textDetection : result.getTextDetections()) {
                        if (textDetection.getType().equals("WORD")) {
                            detectedText.append(textDetection.getDetectedText()).append(" ");
                        }
                    }

                    // Store results
                    results.put(imageName, detectedText.toString().trim());
                }

                // Delete processed message from the queue
                sqs.deleteMessage(queueUrl, message.getReceiptHandle());
            }
        }

        // Write results to output.txt
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILENAME))) {
            for (Map.Entry<String, String> entry : results.entrySet()) {
                writer.write(entry.getKey() + ": " + entry.getValue());
                writer.newLine();
            }
            System.out.println("Results saved to output.txt");
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }

    }
}