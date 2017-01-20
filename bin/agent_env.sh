#!/bin/bash
cd `dirname $0`
BIN_DIR=`pwd`
cd ..
DEPLOY_DIR=`pwd`
CONF_DIR=$DEPLOY_DIR/conf

export AGENT_PATH=" -javaagent:$DEPLOY_DIR/agent/apm_agent.jar=-path:$DEPLOY_DIR  "
echo $AGENT_PATH
