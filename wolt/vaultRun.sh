#!/bin/bash
#
# Copyright (c) 2020 Oracle and/or its affiliates.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

if [ ! "$(docker ps -q -f name=vault)" ]; then
  if [ "$(docker ps -aq -f status=exited -f name=vault)" ]; then
    # Clean up exited container
    docker rm vault
  fi
  # Run test Vault in new container, stop it by pressing Ctrl+C
   docker run -it \
   --cap-add=IPC_LOCK \
   -e VAULT_DEV_ROOT_TOKEN_ID=myroot \
   -d --name=vault \
   -p8200:8200 \
   vault
fi