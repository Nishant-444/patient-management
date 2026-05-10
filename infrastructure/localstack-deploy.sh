#!/bin/bash

set -e

export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1

aws --endpoint-url=http://localhost:4566 s3 mb s3://cfn-templates 2>/dev/null || true

aws --endpoint-url=http://localhost:4566 s3 cp \
    ./cdk.out/localstack.template.json \
    s3://cfn-templates/template.json

aws --endpoint-url=http://localhost:4566 cloudformation create-stack \
    --stack-name localstack \
    --template-url http://s3.localhost.localstack.cloud:4566/cfn-templates/template.json \
    --capabilities CAPABILITY_IAM

aws --endpoint-url=http://localhost:4566 elbv2 describe-load-balancers \
    --query "LoadBalancers[0].DNSName" --output text