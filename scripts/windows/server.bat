cd ../../src
start rmiregistry
start java models.Peer 1 Peer1 8001 localhost 8000 10 localhost
start java models.Peer 2 Peer2 8002 localhost 8000 10 localhost
start java models.Peer 3 Peer3 8003 localhost 8000 10 localhost
