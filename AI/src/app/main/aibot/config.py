import os
import boto3
from botocore.config import Config
from boto3.dynamodb.conditions import Key, Attr

# Este serviço NÃO roda mais IA/embeddings.
# Ele faz apenas ingestão/processamento e persistência (DynamoDB).

# DynamoDB (optional) - used to update ingestion status
DDB_TABLE = os.getenv("DDB_TABLE_NAME", "fatec-ai-bot-core")
DDB_ENDPOINT = os.getenv("AWS_DYNAMODB_ENDPOINT_OVERRIDE")
AWS_REGION = os.getenv("AWS_DEFAULT_REGION", "us-east-1")

BUCKET_NAME = os.getenv("BUCKET_NAME", "fatec-ai-bot-bucket")
S3_ENDPOINT = os.getenv("AWS_S3_ENDPOINT_OVERRIDE")

aws_access_key_id = os.getenv("AWS_ACCESS_KEY_ID")
aws_secret_access_key = os.getenv("AWS_SECRET_ACCESS_KEY")

aws_client_kwargs = {
	"region_name": AWS_REGION,
	"aws_access_key_id": aws_access_key_id,
	"aws_secret_access_key": aws_secret_access_key,
}

if DDB_ENDPOINT:
	aws_client_kwargs["endpoint_url"] = DDB_ENDPOINT

ddb_config = Config(retries={"max_attempts": 3})
dynamo_resource = boto3.resource(
	"dynamodb",
	config=ddb_config,
	**aws_client_kwargs,
)
dynamo_table = dynamo_resource.Table(DDB_TABLE)

s3_config = Config(retries={"max_attempts": 3})
s3_client_kwargs = dict(aws_client_kwargs)
if S3_ENDPOINT:
	s3_client_kwargs["endpoint_url"] = S3_ENDPOINT

s3_client = boto3.client(
	"s3",
	config=s3_config,
	**s3_client_kwargs,
)
