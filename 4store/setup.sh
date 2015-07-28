#!/bin/bash
PASSWORD="$1"
4s-backend-setup -v -v --node 0 --cluster 1 --segments 16 --password "$PASSWORD" tripspace
