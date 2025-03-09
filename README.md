CS643-852 Cloud Computing
Programming Assignment 1
Author: John Allen
Date: 03/06/2025

-- AWS Image Recognition Pipeline --
Overview:
This project implements an AWS-based image recognition pipeline using EC2, S3, SQS, and Rekognition. It consists of two applications:
    1. CarRecognitionApplication (runs on EC2A):
        - Reads images from an S3 bucket
        - Detects cars using AWS Rekognition (Detect labels with >90% confidence)
        - Sends images containing cars to an SQS queue
    2. TextRecognitionApplication (runs on EC2B):
        - Reads image indexes from the SQS queue
        - Performs text recognition on images
        - Writes detected text to output.txt

-- Prerequisites --
    - An AWS account with access to EC2, S3, SQS, and Rekognition
    - AWS CLI installed and configured (https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html)
    - Maven installed (https://maven.apache.org/install.html)
    - Java 8+ installed (https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/amazon-linux-install.html)
        - Additional Useful Documentation: https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/setup-project-maven.html
        - java -version to check

-- AWS Setup --
    Step 1: Launch EC2 Instances 
        *** For both instances ***
        1. Go to EC2 Console
        2. Click Launch Instance.
        3. Choose Amazon Linux 2 AMI (Free Tier eligible).
        4. Set instance type to t2.micro (Free Tier eligible).
        5. Create a new key pair or select an existing one.
        6. Configure Security Group:
            - Allow SSH (port 22) from My IP
            - Allow HTTP, HTTPS (optional)
        7. Configure Advanced Details:
            - Ensure that the appropriate IAM Instance profile is selected (LabInstanceProfile had been preconfigured for this lab)
        8. Click Launch Instance.
        9. Repeat this step to create EC2B.

    Step 2: Create an SQS Queue
        1. Open the AWS SQS Console
        2. Click Create Queue > FIFO Queue.
        3. Name it car.fifo.
        4. Enable Content-Based Deduplication. (Optional as a workaround has been implemented in the code)
        5. Click Create Queue.

    Step 3: Connect to EC2 Instances (The following processes need to be completed for both instances)
        1. SSH into each instance:
            ssh -i your-key.pem ec2-user@<EC2A_IP>
            ssh -i your-key.pem ec2-user@<EC2B_IP>
        2. Ensure instances are up to date:
            sudo yum update -y
        3. Install Amazon Corretto 8:
            sudo yum install java-1.8.0-amazon-corretto
            sudo yum install java-1.8.0-amazon-corretto-devel
            java -version
        4. AWS CLI Configuration:
            mkdir ~/.aws
            cd ~/.aws

            Open the credentials CSV file and input your generated aws_access_key_id, aws_secret_access_key, aws_session_token in the following format: 

            [default]
            aws_access_key_id = your_access_key_id
            aws_secret_access_key = your_secret_access_key
            aws_session_token = your_session_token

            ***Note*** The credentials file will need to be updated after every session to reflect your new session credentials
            Documentation: https://docs.aws.amazon.com/rekognition/latest/dg/setup-awscli-sdk.html

        Step 4: Ensure Access to S3 Bucket
        1. Go to the provided S3 bucket URL to ensure you can access it and see the contents.
        2. From your EC2 instance command line you can validate you can reach the bucket by typing the command: aws s3 ls s3://njit-cs-643

-- Deploying the Applications --
    Step 1: Clone the Project & Build the JARs
        - On your local machine, run:
            git clone https://github.com/jallen-6386/aws-image-recognition.git
            cd aws-image-recognition
            mvn clean package
    
    Step 2: Transfer JARs to EC2
        - Upload carrecognition JAR to EC2A:
            scp -i your-key.pem carrecognition/target/carrecognition-1.0-SNAPSHOT-jar-with-dependencies.jar ec2-user@<EC2A_IP>:~/
        - Upload textrecognition JAR to EC2B:
            scp -i your-key.pem textrecognition/target/textrecognition-1.0-SNAPSHOT-jar-with-dependencies.jar ec2-user@<EC2B_IP>:~/

-- Running the Applications --
    1. Start TextRecognitionApplication (EC2B)
        - SSH into EC2B:
            ssh -i your-key.pem ec2-user@<EC2B_IP>
        - Run the text recognition app:
            java -jar textrecognition-1.0-SNAPSHOT-jar-with-dependencies.jar

    2. Start CarRecognitionApplication (EC2A)
        - SSH into EC2A:
            ssh -i your-key.pem ec2-user@<EC2A_IP>
        - Run the car recognition app:
            java -jar carrecognition-1.0-SNAPSHOT-jar-with-dependencies.jar
        - Expected Output:
            Car detected in image: 1.jpg
            Sent image 1.jpg to SQS queue
            Car detected in image: 2.jpg
            Sent image 2.jpg to SQS queue
            Car detected in image: 4.jpg
            Sent image 4.jpg to SQS queue
            Car detected in image: 5.jpg
            Sent image 5.jpg to SQS queue
            Car detected in image: 6.jpg
            Sent image 6.jpg to SQS queue
            Car detected in image: 7.jpg
            Sent image 7.jpg to SQS queue
            Sending termination signal (-1) to queue.

-- Viewing Results --
    1. Check Processed Output on EC2B
        - Once TextRecognitionApplication finishes, check output.txt:
            cat output.txt
        - Expected Output:
            2.jpg:
            4.jpg: YHI9 OTZ
            6.jpg:
            7.jpg: Lamborghini LP 610 LB ВО BWW
            5.jpg:
            1.jpg: $ BR8167

-- Other Useful Resources --
    1. https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-sqs-messages.html
    2. https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/examples-s3-buckets.html
    3. https://docs.aws.amazon.com/rekognition/latest/dg/labels-detect-labels-image.html
    4. https://github.com/awsdocs/aws-doc-sdk-examples/tree/main/javav2