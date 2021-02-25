cd ../../src
start rmiregistry
start java models.Peer 2.0 1 Peer1 8000 230.0.0.1 8001 230.0.0.2 8002 230.0.0.3
start java models.Peer 2.0 2 Peer2 8000 230.0.0.1 8001 230.0.0.2 8002 230.0.0.3
start java models.Peer 2.0 3 Peer3 8000 230.0.0.1 8001 230.0.0.2 8002 230.0.0.3
