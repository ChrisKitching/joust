#!/bin/sh
# Script to cleanup the integration test environment. Executed in pre-integration-test phase.
rm -Rv compilationResults 2> /dev/null
mkdir -p compilationResults/opt
mkdir compilationResults/noOpt
