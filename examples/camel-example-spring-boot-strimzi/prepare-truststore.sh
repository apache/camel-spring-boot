#!/usr/bin/env bash

rm truststore.jks

kubectl extract secret/my-cluster-cluster-ca-cert --keys=ca.crt --to=- -n camel-example-strimzi > ca.crt

echo "yes" | keytool -import -trustcacerts -file ca.crt -keystore truststore.jks -storepass 123456

kubectl create configmap truststore-config --from-file=truststore.jks=truststore.jks -n camel-example-strimzi

rm ca.crt