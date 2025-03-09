package carrecognition;

import com.amazonaws.regions.Regions;
// AWS Rekognition Service
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.*;
// AWS S3 Service
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
// AWS SQS Service
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.*;

import java.util.List;
import java.util.UUID;

public class CarRecognitionApplication {

    public static void main(String[] args) {
        // Define AWS Resources
        String bucketName = "njit-cs-643"; // The S3 bucket containing images
        String queueName = "car.fifo"; // SQS FIFO queue for passing car image indexes
        String queueGroup = "group1"; // Required for FIFO queue

        // Initialize AWS Clients
        AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
        AmazonRekognition rekognition = AmazonRekognitionClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
        AmazonSQS sqs = AmazonSQSClientBuilder.standard().withRegion(Regions.US_EAST_1).build();

        // Start processing images in the S3 bucket
        processBucketImages(s3, rekognition, sqs, bucketName, queueName, queueGroup);
    }

    public static void processBucketImages(AmazonS3 s3, AmazonRekognition rekognition, AmazonSQS sqs,
                                           String bucketName, String queueName, String queueGroup) {

        // Retrieve or create SQS queue
        String queueUrl = "";
        try {
            ListQueuesResult listQueuesResult = sqs.listQueues();
            if (listQueuesResult.getQueueUrls().isEmpty()) {
                // If no queue exists, create one
                CreateQueueRequest createQueueRequest = new CreateQueueRequest(queueName)
                        .addAttributesEntry("FifoQueue", "true")
                        .addAttributesEntry("ContentBasedDeduplication", "true");
                sqs.createQueue(createQueueRequest);
            }
            queueUrl = sqs.getQueueUrl(queueName).getQueueUrl();
        } catch (Exception e) {
            System.err.println("Error creating or accessing SQS queue: " + e.getMessage());
            return;
        }

        // Retrieve image list from S3 bucket
        ListObjectsV2Result listObjectsResult = s3.listObjectsV2(bucketName);
        List<S3ObjectSummary> images = listObjectsResult.getObjectSummaries();

        // Process each image
        for (S3ObjectSummary image : images) {
            String imageName = image.getKey();
            Image img = new Image().withS3Object(new S3Object().withBucket(bucketName).withName(imageName));

            // AWS Rekognition: Detect labels with >90% confidence
            DetectLabelsRequest request = new DetectLabelsRequest()
                    .withImage(img)
                    .withMaxLabels(10)
                    .withMinConfidence(90F);
            DetectLabelsResult result = rekognition.detectLabels(request);

            // Check if a car is detected
            for (Label label : result.getLabels()) {
                if (label.getName().equalsIgnoreCase("Car")) {
                    System.out.println("Car detected in image: " + imageName);

                    // Send the image index to SQS with unique MessageDeduplicationId
                    SendMessageRequest sendMsgRequest = new SendMessageRequest()
                            .withQueueUrl(queueUrl)
                            .withMessageBody(imageName)
                            .withMessageGroupId(queueGroup)  // Required for FIFO queue
                            .withMessageDeduplicationId(UUID.randomUUID().toString()); // Unique ID to prevent deduplication errors
                    sqs.sendMessage(sendMsgRequest);

                    System.out.println("Sent image " + imageName + " to SQS queue");
                    break; // Move to next image
                }
            }
        }

        // Send termination signal (-1) to indicate the end of processing
        System.out.println("Sending termination signal (-1) to queue.");
        SendMessageRequest terminationMsgRequest = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody("-1")
                .withMessageGroupId(queueGroup)
                .withMessageDeduplicationId(UUID.randomUUID().toString()); // Unique ID for termination message
        sqs.sendMessage(terminationMsgRequest);
    }
}