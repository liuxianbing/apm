#!/bin/bash
cd `dirname $0`
BIN_DIR=`pwd`
cd ..
DEPLOY_DIR=`pwd`
CONF_DIR=$DEPLOY_DIR/conf

[ -z ${JAVA_HOME} ] && JAVA_HOME=/home/dc/java
# check the jvm version, we need 1.6+
    local JAVA_VERSION=$(${JAVA_HOME}/bin/java -version 2>&1|awk -F '"' '/version/&&$2>"1.5"{print $2}')
    [[ ! -x ${JAVA_HOME} || -z ${JAVA_VERSION} ]] && exit_on_err 1 "illegal ENV, please set \$JAVA_HOME to JDK6+"

    # reset BOOT_CLASSPATH
    [ -f ${JAVA_HOME}/lib/tools.jar ] && BOOT_CLASSPATH=-Xbootclasspath/a:${JAVA_HOME}/lib/tools.jar

${JAVA_HOME}/bin/java ${BOOT_CLASSPATH} -jar $DEPLOY_DIR/agent/apm_agent.jar $@
