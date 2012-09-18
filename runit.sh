#!/bin/bash
mvn clean package appassembler:assemble
cd target/jcr-runner
sh bin/jcr-runner
