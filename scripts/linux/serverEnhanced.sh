#!/usr/bin/env bash
cd ../../src
bash -c "pkill rmiregistry" # Kills rmiregistry
rmiregistry &
gnome-terminal -- bash -c " java models.Peer 2.0 1 Peer1 8000 230.0.0.1 8001 230.0.0.2 8002 230.0.0.3 ; exec bash"; 
gnome-terminal -- bash -c " java models.Peer 2.0 2 Peer2 8000 230.0.0.1 8001 230.0.0.2 8002 230.0.0.3 ; exec bash"; 
gnome-terminal -- bash -c " java models.Peer 2.0 3 Peer3 8000 230.0.0.1 8001 230.0.0.2 8002 230.0.0.3 ; exec bash"; 
