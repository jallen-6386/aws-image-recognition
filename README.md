### CS643-852 Cloud Computing
### Programming Assignment 1
**Author:** John Allen  
**Date:** 03/06/2025



## **AWS Image Recognition Pipeline**

### **Overview**
This project implements an **AWS-based image recognition pipeline** using **EC2, S3, SQS, and Rekognition**. It consists of two applications:

1. **CarRecognitionApplication (runs on EC2A):**
    - Reads images from an S3 bucket
    - Detects cars using AWS Rekognition (Detect labels with >90% confidence)
    - Sends images containing cars to an SQS queue

2. **TextRecognitionApplication (runs on EC2B):**
    - Reads image indexes from the SQS queue
    - Performs text recognition on images
    - Writes detected text to `output.txt`



### **Prerequisites**
- An **AWS account** with access to **EC2, S3, SQS, and Rekognition**
- **AWS CLI installed and configured** ([AWS CLI Installation Guide](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html))
- **Maven installed** ([Maven Installation Guide](https://maven.apache.org/install.html))
- **Java 8+ installed** ([Amazon Corretto Installation Guide](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/amazon-linux-install.html))
- **Additional Useful Documentation:** [AWS Java SDK](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/setup-project-maven.html)

To check your Java version:
```bash
java -version
```



### **AWS Setup**

#### **Step 1: Launch EC2 Instances**
1. Go to **EC2 Console**
2. Click **Launch Instance**
3. Choose **Amazon Linux 2 AMI (Free Tier eligible)**
4. Set instance type to **t2.micro (Free Tier eligible)**
5. **Create a new key pair** or select an existing one
6. Configure **Security Group**:
    - Allow **SSH (port 22)** from **My IP**
    - Allow **HTTP, HTTPS** (optional)
7. Configure **Advanced Details**:
    - Ensure the appropriate **IAM Instance profile** is selected (LabInstanceProfile preconfigured for this lab)
8. Click **Launch Instance**
9. **Repeat this step to create EC2B**

#### **Step 2: Create an SQS Queue**
1. Open the **AWS SQS Console**
2. Click **Create Queue** → **FIFO Queue**
3. Name it `car.fifo`
4. Enable **Content-Based Deduplication** *(optional; a workaround has been implemented in the code)*
5. Click **Create Queue**

#### **Step 3: Connect to EC2 Instances**
_(The following steps need to be completed for both instances)_
```bash
ssh -i your-key.pem ec2-user@<EC2A_IP>
ssh -i your-key.pem ec2-user@<EC2B_IP>
```

Update instances:
```bash
sudo yum update -y
```

Install **Amazon Corretto 8**:
```bash
sudo yum install java-1.8.0-amazon-corretto -y
sudo yum install java-1.8.0-amazon-corretto-devel -y
java -version
```

Configure **AWS CLI**:
```bash
mkdir ~/.aws
cd ~/.aws
```
Edit the **credentials file**:
```ini
[default]
aws_access_key_id = your_access_key_id
aws_secret_access_key = your_secret_access_key
aws_session_token = your_session_token
```
_(Update session credentials each time the session expires)_

Ensure **S3 bucket access**:
```bash
aws s3 ls s3://njit-cs-643
```



### **Deploying the Applications**

#### **Step 1: Clone the Project & Build the JARs**
```bash
git clone https://github.com/jallen-6386/aws-image-recognition.git
cd aws-image-recognition
mvn clean package
```

#### **Step 2: Transfer JARs to EC2**
##### **Upload `carrecognition` JAR to EC2A:**
```bash
scp -i your-key.pem carrecognition/target/carrecognition-1.0-SNAPSHOT-jar-with-dependencies.jar ec2-user@<EC2A_IP>:~/
```
##### **Upload `textrecognition` JAR to EC2B:**
```bash
scp -i your-key.pem textrecognition/target/textrecognition-1.0-SNAPSHOT-jar-with-dependencies.jar ec2-user@<EC2B_IP>:~/
```



### **Running the Applications**

#### **Start `TextRecognitionApplication` (EC2B)**
```bash
ssh -i your-key.pem ec2-user@<EC2B_IP>
java -jar textrecognition-1.0-SNAPSHOT-jar-with-dependencies.jar
```

#### **Start `CarRecognitionApplication` (EC2A)**
```bash
ssh -i your-key.pem ec2-user@<EC2A_IP>
java -jar carrecognition-1.0-SNAPSHOT-jar-with-dependencies.jar
```

#### **Expected Output:**
```plaintext
Car detected in image: 1.jpg
Sent image 1.jpg to SQS queue
Car detected in image: 2.jpg
Sent image 2.jpg to SQS queue
Sending termination signal (-1) to queue.
```



### **Viewing Results**
Check `output.txt` on EC2B:
```bash
cat output.txt
```
**Expected Output:**
```plaintext
2.jpg:
4.jpg: YHI9 OTZ
6.jpg:
7.jpg: Lamborghini LP 610 LB ВО BWW
5.jpg:
1.jpg: $ BR8167
```



### **Other Useful Resources**
1. [AWS SQS Messages](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-sqs-messages.html)
2. [AWS S3 Buckets](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/examples-s3-buckets.html)
3. [AWS Rekognition Labels](https://docs.aws.amazon.com/rekognition/latest/dg/labels-detect-labels-image.html)
4. [AWS Java SDK GitHub](https://github.com/awsdocs/aws-doc-sdk)

