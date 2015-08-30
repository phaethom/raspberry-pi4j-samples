#!/bin/bash
echo Reads an ADC, feeds a WebSocket server, drives a relay, sends and receives SMSs.
#
if [ "$PI4J_HOME" = "" ]
then
  PI4J_HOME=/opt/pi4j
fi
#
CP=./classes
CP=$CP:$PI4J_HOME/lib/pi4j-core.jar
CP=$CP:./lib/java_websocket.jar
CP=$CP:./lib/jansi-1.9.jar
CP=$CP:../ADC/classes
CP=$CP:../FONA/classes
#
# See props.propeties
#
COMMAND="java $JAVA_OPTS -cp $CP -Dws.uri=ws://localhost:9876/ adc.levelreader.main.LelandPrototype $*"
echo Runnig $COMMAND
#
sudo $COMMAND
