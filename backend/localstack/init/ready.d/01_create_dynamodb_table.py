import json
import os
import time

import boto3
from botocore.exceptions import ClientError


def _env_bool(name: str, default: bool) -> bool:
    v = os.getenv(name)
    if v is None:
        return default
    return v.strip().lower() in ("1", "true", "yes", "y", "on")


AWS_REGION = os.getenv("AWS_DEFAULT_REGION", "us-east-1")
TABLE_NAME = os.getenv("DDB_TABLE_NAME", "fatec-ai-bot-core")
SEED_EXAMPLES = _env_bool("DDB_SEED_EXAMPLES", True)

ENDPOINT_URL = os.getenv("LOCALSTACK_ENDPOINT", "http://localhost:4566")


def main() -> None:
    ddb = boto3.client(
        "dynamodb",
        endpoint_url=ENDPOINT_URL,
        region_name=AWS_REGION,
        aws_access_key_id=os.getenv("AWS_ACCESS_KEY_ID", "test"),
        aws_secret_access_key=os.getenv("AWS_SECRET_ACCESS_KEY", "test"),
    )

    try:
        ddb.describe_table(TableName=TABLE_NAME)
        print(f"[localstack-init] DynamoDB table '{TABLE_NAME}' already exists")
    except ClientError as e:
        if e.response["Error"]["Code"] != "ResourceNotFoundException":
            raise

        print(f"[localstack-init] Creating DynamoDB table '{TABLE_NAME}' (region={AWS_REGION})")
        ddb.create_table(
            TableName=TABLE_NAME,
            AttributeDefinitions=[
                {"AttributeName": "pk", "AttributeType": "S"},
                {"AttributeName": "sk", "AttributeType": "S"},
                {"AttributeName": "gsi1pk", "AttributeType": "S"},
                {"AttributeName": "gsi1sk", "AttributeType": "S"},
                {"AttributeName": "gsi2pk", "AttributeType": "S"},
                {"AttributeName": "gsi2sk", "AttributeType": "S"},
            ],
            KeySchema=[
                {"AttributeName": "pk", "KeyType": "HASH"},
                {"AttributeName": "sk", "KeyType": "RANGE"},
            ],
            GlobalSecondaryIndexes=[
                {
                    "IndexName": "gsi1-email",
                    "KeySchema": [
                        {"AttributeName": "gsi1pk", "KeyType": "HASH"},
                        {"AttributeName": "gsi1sk", "KeyType": "RANGE"},
                    ],
                    "Projection": {"ProjectionType": "ALL"},
                    "ProvisionedThroughput": {"ReadCapacityUnits": 1, "WriteCapacityUnits": 1},
                },
                {
                    "IndexName": "gsi2-unit-content",
                    "KeySchema": [
                        {"AttributeName": "gsi2pk", "KeyType": "HASH"},
                        {"AttributeName": "gsi2sk", "KeyType": "RANGE"},
                    ],
                    "Projection": {"ProjectionType": "ALL"},
                    "ProvisionedThroughput": {"ReadCapacityUnits": 1, "WriteCapacityUnits": 1},
                },
            ],
            BillingMode="PAY_PER_REQUEST",
        )

        # Wait active
        for _ in range(60):
            status = ddb.describe_table(TableName=TABLE_NAME)["Table"]["TableStatus"]
            if status == "ACTIVE":
                break
            time.sleep(1)

        print(f"[localstack-init] DynamoDB table '{TABLE_NAME}' created")

    if not SEED_EXAMPLES:
        print("[localstack-init] Example seed disabled (DDB_SEED_EXAMPLES=false)")
        return

    # Seed (best-effort idempotent)
    examples = [
        {
            "pk": {"S": "FatecItaquera#USERS"},
            "sk": {"S": "email@admin.com"},
            "name": {"S": "Admin User"},
            "password_hash": {"S": "hashed_admin123"},
            "entityType": {"S": "ADMIN"},
            "role": {"S": "SUPER_ADMIN"},
            "status": {"S": "active"},
            "created_at": {"N": "1743667200"},
            "updated_at": {"N": "1743667200"},
        },
        {
            "pk": {"S": "Unidades"},
            "sk": {"S": "FatecItaquera"},
            "entityType": {"S": "UNIT"},
            "cidade": {"S": "Sao Paulo"},
            "status": {"S": "ACTIVE"},
        },
        {
            "pk": {"S": "FatecItaquera#Conteudos"},
            "sk": {"S": "NomeConteudo"},
            "entityType": {"S": "CONTENT"},
            "s3Key": {"S": "fatec-itaquera/conteudos/NomeConteudo"},
            "status": {"S": "ACTIVE"},
            "gsi2pk": {"S": "UNIT#FatecItaquera#CONTENT"},
            "gsi2sk": {"S": "STATUS#ACTIVE#TS#2026-03-31T00:00:00Z#NomeConteudo"},
        },
    ]

    for item in examples:
        try:
            ddb.put_item(
                TableName=TABLE_NAME,
                Item=item,
                ConditionExpression="attribute_not_exists(pk) AND attribute_not_exists(sk)",
            )
        except ClientError as e:
            if e.response["Error"]["Code"] != "ConditionalCheckFailedException":
                raise

    print("[localstack-init] Example seed finished")


if __name__ == "__main__":
    main()

