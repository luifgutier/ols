#!/bin/bash


if [ $(cat /opt/log2.txt | grep -c "actualizar") -eq 1 ]
then
	echo 'seguir' > /opt/log2.txt
	
	echo 'paso 7' >> /opt/log.txt
	cd /opt/OLS/ols-apps/ols-loading-app/target  >> /opt/log.txt
	echo 'paso 8' >> /opt/log.txt
	java -jar ols-indexer.jar  >> /opt/log.txt
	echo 'paso 9' >> /opt/log.txt
	cd /opt/tomcat/bin/  >> /opt/log.txt
	echo 'paso 10' >> /opt/log.txt
	/bin/bash startup.sh  >> /opt/log.txt

fi


