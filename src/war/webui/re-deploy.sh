#!/bin/sh

set -x

WEB_APP_DIR=/var/lib/tomcat/webapps
APP_NAME=webui

echo "Removing  $WEB_APP_DIR/$APP_NAME"
/bin/rm -rf $WEB_APP_DIR/$APP_NAME
/etc/init.d/tomcat restart