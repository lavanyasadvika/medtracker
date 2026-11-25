#!/usr/bin/env bash
# Runs the Azure CLI via Microsoft's official Docker image instead of a
# local install — avoids needing sudo/apt/venv access on this machine.
mkdir -p "$HOME/.azure"
docker run --rm \
  -v "$HOME/.azure:/root/.azure" \
  -v "$PWD:/workdir" \
  -w /workdir \
  mcr.microsoft.com/azure-cli \
  az "$@"
