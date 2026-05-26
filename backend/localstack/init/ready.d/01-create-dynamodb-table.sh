#!/bin/sh
set -eu

TABLE_NAME="${DDB_TABLE_NAME:-fatec-ai-bot-core}"
AWS_REGION="${AWS_DEFAULT_REGION:-us-east-1}"
SEED_EXAMPLES="${DDB_SEED_EXAMPLES:-true}"

put_item_if_not_exists() {
	item_json="$1"
	key_json="$2"

	if awslocal dynamodb get-item \
		--table-name "$TABLE_NAME" \
		--key "$key_json" \
		--consistent-read \
		--region "$AWS_REGION" | grep -q '"Item"'; then
		return 0
	fi

	awslocal dynamodb put-item \
		--table-name "$TABLE_NAME" \
		--item "$item_json" \
		--condition-expression "attribute_not_exists(pk) AND attribute_not_exists(sk)" \
		--region "$AWS_REGION" >/dev/null
}

if awslocal dynamodb describe-table --table-name "$TABLE_NAME" --region "$AWS_REGION" >/dev/null 2>&1; then
	echo "[localstack-init] DynamoDB table '$TABLE_NAME' already exists"
else
	echo "[localstack-init] Creating DynamoDB table '$TABLE_NAME' in region '$AWS_REGION'"
	awslocal dynamodb create-table \
		--table-name "$TABLE_NAME" \
		--attribute-definitions \
			AttributeName=pk,AttributeType=S \
			AttributeName=sk,AttributeType=S \
			AttributeName=gsi1pk,AttributeType=S \
			AttributeName=gsi1sk,AttributeType=S \
			AttributeName=gsi2pk,AttributeType=S \
			AttributeName=gsi2sk,AttributeType=S \
		--key-schema \
			AttributeName=pk,KeyType=HASH \
			AttributeName=sk,KeyType=RANGE \
		--global-secondary-indexes \
			'[{"IndexName":"gsi1-email","KeySchema":[{"AttributeName":"gsi1pk","KeyType":"HASH"},{"AttributeName":"gsi1sk","KeyType":"RANGE"}],"Projection":{"ProjectionType":"ALL"}},{"IndexName":"gsi2-unit-content","KeySchema":[{"AttributeName":"gsi2pk","KeyType":"HASH"},{"AttributeName":"gsi2sk","KeyType":"RANGE"}],"Projection":{"ProjectionType":"ALL"}}]' \
		--billing-mode PAY_PER_REQUEST \
		--region "$AWS_REGION"
	echo "[localstack-init] DynamoDB table '$TABLE_NAME' created"
fi

if [ "$SEED_EXAMPLES" != "true" ]; then
	echo "[localstack-init] Example seed disabled (DDB_SEED_EXAMPLES=$SEED_EXAMPLES)"
	exit 0
fi

echo "[localstack-init] Seeding example items for single-table model"
put_item_if_not_exists \
	'{"pk":{"S":"FatecItaquera#USERS"},"sk":{"S":"email@admin.com"},"name":{"S":"Admin User"},"password_hash":{"S":"hashed_admin123"},"entityType":{"S":"ADMIN"},"role":{"S":"SUPER_ADMIN"},"status":{"S":"active"},"created_at":{"N":"1743667200"},"updated_at":{"N":"1743667200"}}' \
	'{"pk":{"S":"FatecItaquera#USERS"},"sk":{"S":"email@admin.com"}}'

put_item_if_not_exists \
	'{"pk":{"S":"Unidades"},"sk":{"S":"FatecItaquera"},"entityType":{"S":"UNIT"},"cidade":{"S":"Sao Paulo"},"status":{"S":"ACTIVE"}}' \
	'{"pk":{"S":"Unidades"},"sk":{"S":"FatecItaquera"}}'

put_item_if_not_exists \
	'{"pk":{"S":"FatecItaquera#Conteudos"},"sk":{"S":"NomeConteudo"},"entityType":{"S":"CONTENT"},"s3Key":{"S":"fatec-itaquera/conteudos/NomeConteudo"},"status":{"S":"ACTIVE"},"gsi2pk":{"S":"UNIT#FatecItaquera#CONTENT"},"gsi2sk":{"S":"STATUS#ACTIVE#TS#2026-03-31T00:00:00Z#NomeConteudo"}}' \
	'{"pk":{"S":"FatecItaquera#Conteudos"},"sk":{"S":"NomeConteudo"}}'

echo "[localstack-init] Example seed finished"