#!/bin/bash
# Exclusion list for camel-spring-boot incremental builds
# Override the default exclusion list from apache/camel which contains modules
# that don't exist in camel-spring-boot

# camel-spring-boot has fewer generated/meta modules to exclude
# Most exclusions from main Camel don't apply here
EXCLUSION_LIST=""
