#!/usr/bin/env bash

set -e

SOURCE_CONNECTION_STRING="DefaultEndpointsProtocol=http;AccountName=factlocal;AccountKey=cmVmb3Jtc2NhbmtleQo=;BlobEndpoint=http://azure-storage-emulator-azurite:10000/factlocal;"

az storage container create --name images --connection-string $SOURCE_CONNECTION_STRING
az storage container create --name csv --connection-string $SOURCE_CONNECTION_STRING
