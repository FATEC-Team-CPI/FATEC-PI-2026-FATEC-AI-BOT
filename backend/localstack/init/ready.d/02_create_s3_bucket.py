import os

import boto3
from botocore.exceptions import ClientError


AWS_REGION = os.getenv("AWS_DEFAULT_REGION", "us-east-1")
BUCKET_NAME = os.getenv("BUCKET_NAME", "fatec-ai-bot-bucket")
ENDPOINT_URL = os.getenv("LOCALSTACK_ENDPOINT", "http://localhost:4566")


def main() -> None:
    s3 = boto3.client(
        "s3",
        endpoint_url=ENDPOINT_URL,
        region_name=AWS_REGION,
        aws_access_key_id=os.getenv("AWS_ACCESS_KEY_ID", "test"),
        aws_secret_access_key=os.getenv("AWS_SECRET_ACCESS_KEY", "test"),
    )

    try:
        s3.head_bucket(Bucket=BUCKET_NAME)
        print(f"[localstack-init] Bucket '{BUCKET_NAME}' already exists")
        return
    except ClientError:
        pass

    s3.create_bucket(Bucket=BUCKET_NAME)
    print(f"[localstack-init] Bucket '{BUCKET_NAME}' created")


if __name__ == "__main__":
    main()

