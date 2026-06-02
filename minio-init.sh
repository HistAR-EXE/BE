#!/bin/sh
set -e

echo "Waiting for MinIO..."
until mc alias set local http://minio:9000 "${MINIO_ROOT_USER}" "${MINIO_ROOT_PASSWORD}" 2>/dev/null; do
  sleep 2
done

echo "Creating bucket: ${MINIO_BUCKET_NAME}"
mc mb --ignore-existing "local/${MINIO_BUCKET_NAME}"
mc anonymous set download "local/${MINIO_BUCKET_NAME}"
echo "MinIO init done."
